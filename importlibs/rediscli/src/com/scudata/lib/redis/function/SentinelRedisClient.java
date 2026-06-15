package com.scudata.lib.redis.function;

import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 哨兵模式（Sentinel）Redis 客户端实现。
 * <p>
 * <b>设计说明：</b>
 * 当前 Jedis 3.6.1 jar 中的 {@link JedisSentinelPool#initSentinels} 存在内部 bug，
 * 即使 TCP 连通哨兵也无法完成 Master 发现。因此本实现绕过 JedisSentinelPool，
 * 手动执行以下流程：
 * <ol>
 *   <li>通过原始 Socket 连接每个哨兵节点</li>
 *   <li>发送 {@code SENTINEL get-master-addr-by-name <masterName>} 命令</li>
 *   <li>解析响应获取当前 Master 的 host:port</li>
 *   <li>基于 {@link JedisPool} 创建到 Master 的连接池</li>
 *   <li>后续命令通过此连接池执行，与单机模式完全一致</li>
 * </ol>
 * </p>
 *
 * <b>连接字符串格式：</b>
 * <pre>{@code sentinel://masterName@host1:port1,host2:port2,host3:port3}</pre>
 *
 * @author RedisCli
 */
public class SentinelRedisClient implements IRedisClient {

    /** Master 连接池（非 JedisSentinelPool，手动从哨兵发现后创建） */
    private volatile JedisPool masterPool;

    /** Master 名称 */
    private final String masterName;

    /** Sentinel 节点地址集合 */
    private final Set<String> sentinelSet;

    /** Redis 密码 */
    private final String password;

    /** 超时时间 */
    private final int timeout;

    /**
     * 构造哨兵模式客户端。
     * <p>
     * 构造时主动连接哨兵节点发现当前 Master，然后创建 JedisPool。
     * </p>
     *
     * @param masterName  Redis Master 名称
     * @param sentinelSet Sentinel 节点地址集合，格式：{@code host:port}
     * @param password    Redis 密码，可为 null 或空字符串
     * @param timeout     超时时间（毫秒）
     */
    public SentinelRedisClient(String masterName, Set<String> sentinelSet,
                               String password, int timeout) {
        this.masterName = masterName;
        this.sentinelSet = new HashSet<>(sentinelSet);
        this.password = password;
        this.timeout = timeout > 0 ? timeout : 2000;

        // 手动发现 Master 并创建连接池
        HostAndPort masterAddr = discoverMaster();
        this.masterPool = createMasterPool(masterAddr);
    }

    /** 简化构造 */
    public SentinelRedisClient(String masterName, Set<String> sentinelSet, String password) {
        this(masterName, sentinelSet, password, 2000);
    }

    // ======================== 哨兵发现逻辑 ========================

    /**
     * 遍历所有哨兵节点，通过 {@code SENTINEL get-master-addr-by-name} 命令发现当前 Master 地址。
     * <p>
     * 使用 {@link nl.melp.redis.Redis} 通过原始 Socket 连接哨兵，发送 Redis RESP 协议命令。
     * 这完全绕过了 {@link JedisSentinelPool#initSentinels} 的内部 bug。
     * </p>
     *
     * @return Master 的 HostAndPort
     * @throws RuntimeException 如果所有哨兵都不可用
     */
    @SuppressWarnings("unchecked")
    private HostAndPort discoverMaster() {
        for (String sentinelStr : sentinelSet) {
            String[] parts = sentinelStr.split(":");
            String host = parts[0].trim();
            int port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 26379;

            Socket socket = null;
            try {
                socket = new Socket(host, port);
                nl.melp.redis.Redis redis = new nl.melp.redis.Redis(socket);

                // 发送 SENTINEL get-master-addr-by-name <masterName>
                Object result = redis.call("SENTINEL", "get-master-addr-by-name", masterName);

                // 响应格式：["127.0.0.1", "6379"]
                if (result instanceof List) {
                    List<Object> addr = (List<Object>) result;
                    if (addr.size() >= 2) {
                        String masterHost = bytesToString(addr.get(0));
                        int masterPort = Integer.parseInt(bytesToString(addr.get(1)));
                        return new HostAndPort(masterHost, masterPort);
                    }
                }
            } catch (Exception e) {
                // 当前哨兵不可用，继续尝试下一个
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        }

        throw new RuntimeException("All sentinels down, cannot determine where is "
                + masterName + " master is running...");
    }

    /**
     * 将 bytes 或 String 转为 String。
     */
    private String bytesToString(Object obj) {
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, java.nio.charset.StandardCharsets.UTF_8);
        }
        return obj.toString();
    }

    /**
     * 为指定 Master 地址创建 JedisPool。
     */
    private JedisPool createMasterPool(HostAndPort masterAddr) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(8);
        config.setMaxIdle(4);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setBlockWhenExhausted(true);

        String pwd = (password != null && !password.isEmpty()) ? password : null;
        if (pwd != null) {
            return new JedisPool(config, masterAddr.getHost(), masterAddr.getPort(),
                    timeout, pwd);
        } else {
            return new JedisPool(config, masterAddr.getHost(), masterAddr.getPort(),
                    timeout);
        }
    }

    // ======================== IRedisClient 接口实现 ========================

    @Override
    public Object call(Object... args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Command arguments must not be empty");
        }

        try (Jedis jedis = masterPool.getResource()) {
            String cmdStr = args[0].toString().toUpperCase();
            ProtocolCommand cmd = resolveCommand(cmdStr);

            byte[][] bargs = new byte[args.length - 1][];
            for (int i = 1; i < args.length; i++) {
                bargs[i - 1] = SafeEncoder.encode(args[i].toString());
            }

            Object response = jedis.sendCommand(cmd, bargs);
            return normalizeResponse(response);

        } catch (Exception e) {
            // 如果连接失败，尝试重新发现 Master（故障转移场景）
            if (isPoolExhausted(e)) {
                try {
                    HostAndPort newMaster = discoverMaster();
                    JedisPool newPool = createMasterPool(newMaster);
                    JedisPool oldPool = this.masterPool;
                    this.masterPool = newPool;
                    if (oldPool != null && !oldPool.isClosed()) {
                        oldPool.close();
                    }
                    // 重试一次
                    try (Jedis jedis = masterPool.getResource()) {
                        // ... same as above
                        ProtocolCommand cmd = resolveCommand(args[0].toString().toUpperCase());
                        byte[][] bargs = new byte[args.length - 1][];
                        for (int i = 1; i < args.length; i++) {
                            bargs[i - 1] = SafeEncoder.encode(args[i].toString());
                        }
                        Object response = jedis.sendCommand(cmd, bargs);
                        return normalizeResponse(response);
                    }
                } catch (Exception retryEx) {
                    throw new IOException(
                            "Failed to execute Redis command (retry failed): " + args[0], retryEx);
                }
            }
            throw new IOException(
                    "Failed to execute Redis command on sentinel [master=" + masterName + "]: " + args[0], e);
        }
    }

    /**
     * 判断异常是否由连接池耗尽或连接断开引起，触发故障转移重试。
     */
    private boolean isPoolExhausted(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("Could not get a resource")
                || msg.contains("Connection refused")
                || msg.contains("timeout")
                || msg.contains("Broken pipe")
                || msg.contains("Connection reset"));
    }

    /**
     * 解析命令名。
     */
    private ProtocolCommand resolveCommand(String cmdStr) {
        try {
            return Protocol.Command.valueOf(cmdStr);
        } catch (IllegalArgumentException e) {
            // fallback
        }
        final byte[] rawBytes = SafeEncoder.encode(cmdStr);
        return () -> rawBytes;
    }

    /**
     * 标准化响应。
     */
    @SuppressWarnings("unchecked")
    private Object normalizeResponse(Object response) {
        if (response == null) return null;
        if (response instanceof byte[]) {
            return SafeEncoder.encode((byte[]) response);
        }
        if (response instanceof List) {
            List<Object> list = (List<Object>) response;
            Object[] arr = new Object[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = normalizeResponse(list.get(i));
            }
            return arr;
        }
        return response;
    }

    @Override
    public void close() {
        if (masterPool != null && !masterPool.isClosed()) {
            masterPool.close();
        }
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    // ======================== 访问器 ========================

    public String getMasterName() {
        return masterName;
    }

    public Set<String> getSentinels() {
        return new HashSet<>(sentinelSet);
    }

    public JedisPool getMasterPool() {
        return masterPool;
    }
}
