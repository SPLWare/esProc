package com.scudata.lib.redis.function;

import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群模式（Cluster）Redis 客户端实现。
 * <p>
 * 实现 Redis Cluster 协议的命令路由机制：
 * <ul>
 *   <li>根据 key 的 CRC16 哈希值计算槽位（slot）</li>
 *   <li>维护 slot → 节点池 的映射关系</li>
 *   <li>自动处理 {@code MOVED} 重定向（刷新 slot 映射并重试）</li>
 *   <li>自动处理 {@code ASK} 重定向（临时路由到目标节点）</li>
 * </ul>
 * </p>
 *
 * <b>核心流程：</b>
 * <ol>
 *   <li>构造时连接任意集群节点，执行 {@code CLUSTER SLOTS} 获取全量拓扑</li>
 *   <li>为每个节点创建 {@link JedisPool}</li>
 *   <li>命令到达时，提取 key 并计算 CRC16 槽位</li>
 *   <li>查找该槽位所属的节点池，借用连接执行命令</li>
 *   <li>遇到 {@code MOVED} 错误时刷新拓扑并重试（最多 3 次）</li>
 * </ol>
 *
 * <b>连接字符串格式（通过 SocketRedisOpen 统一入口）：</b>
 * <pre>{@code cluster://host1:port1,host2:port2,host3:port3}</pre>
 *
 * @author RedisCli
 */
public class ClusterRedisClient implements IRedisClient {

    /** 最大重定向重试次数 */
    private static final int MAX_REDIRECT_RETRIES = 3;

    /** 节点池映射：HostAndPort → JedisPool */
    private final Map<HostAndPort, JedisPool> nodePools = new ConcurrentHashMap<>();

    /** 槽位映射：slot → 节点 HostAndPort */
    private volatile SlotCache slotCache;

    /** 初始集群节点地址列表 */
    private final Set<HostAndPort> initialNodes;

    /** 密码 */
    private final String password;

    /** 超时时间 */
    private final int timeout;

    /**
     * 构造集群模式客户端。
     *
     * @param clusterNodes 集群节点列表
     * @param password     Redis 密码，可为 null 或空字符串
     * @param timeout      超时时间（毫秒），&le;0 使用默认值 2000ms
     */
    public ClusterRedisClient(Set<HostAndPort> clusterNodes, String password, int timeout) {
        this.initialNodes = new HashSet<>(clusterNodes);
        this.password = password;
        this.timeout = timeout > 0 ? timeout : 2000;

        // 为每个节点创建连接池
        for (HostAndPort node : clusterNodes) {
            nodePools.computeIfAbsent(node, this::createPool);
        }

        // 初始化 slot 映射
        refreshSlotMapping();
    }

    /** 简化构造 */
    public ClusterRedisClient(Set<HostAndPort> clusterNodes, String password) {
        this(clusterNodes, password, 2000);
    }

    /** 为指定节点创建连接池 */
    private JedisPool createPool(HostAndPort node) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(4);
        config.setMaxIdle(2);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);

        if (password != null && !password.isEmpty()) {
            return new JedisPool(config, node.getHost(), node.getPort(), timeout, password);
        } else {
            return new JedisPool(config, node.getHost(), node.getPort(), timeout);
        }
    }

    /**
     * 刷新 slot 映射表。
     * <p>
     * 使用 {@link nl.melp.redis.Redis} 通过原始 Socket 连接任意集群节点，
     * 执行 {@code CLUSTER SLOTS} 命令（绕过 {@code Jedis.sendCommand()} 的内部 bug），
     * 解析返回的拓扑信息，构建 slot → HostAndPort 映射。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void refreshSlotMapping() {
        for (Map.Entry<HostAndPort, JedisPool> entry : nodePools.entrySet()) {
            HostAndPort node = entry.getKey();

            Socket socket = null;
            try {
                socket = new Socket(node.getHost(), node.getPort());
                nl.melp.redis.Redis redis = new nl.melp.redis.Redis(socket);

                // 先认证（requirepass 配置下 CLUSTER SLOTS 需要 AUTH）
                if (password != null && !password.isEmpty()) {
                    redis.call("AUTH", password);
                }

                // 执行 CLUSTER SLOTS 获取全量槽位信息
                Object result = redis.call("CLUSTER", "SLOTS");
                List<Object> slotsInfo = (List<Object>) result;

                Map<Integer, HostAndPort> newSlotMap = new HashMap<>();

                for (Object slotObj : slotsInfo) {
                    List<Object> slotRange = (List<Object>) slotObj;
                    if (slotRange.size() < 3) continue;

                    long startSlot = toLong(slotRange.get(0));
                    long endSlot = toLong(slotRange.get(1));

                    List<Object> masterInfo = (List<Object>) slotRange.get(2);
                    String host = bytesToString(masterInfo.get(0));
                    int port = (int) toLong(masterInfo.get(1));
                    HostAndPort masterNode = new HostAndPort(host, port);

                    // 确保 master 节点也有连接池
                    nodePools.computeIfAbsent(masterNode, this::createPool);

                    // 更新 slot 映射
                    for (long slot = startSlot; slot <= endSlot; slot++) {
                        newSlotMap.put((int) slot, masterNode);
                    }
                }

                this.slotCache = new SlotCache(newSlotMap, System.currentTimeMillis());
                return; // 成功获取拓扑，退出
            } catch (Exception e) {
                // 当前节点不可用，尝试下一个节点
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        }
        throw new RuntimeException("Failed to refresh cluster slot mapping: no reachable nodes");
    }

    /**
     * 将 Object 安全转为 long（兼容 byte[] 和 Number 类型）。
     */
    private long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(bytesToString(obj));
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

    /** 根据 key 计算 CRC16 槽位 */
    private int getSlot(String key) {
        return JedisClusterCRC16.getSlot(key);
    }

    /**
     * 获取 key 在命令参数中的位置。
     * <p>
     * 大多数 Redis 命令的 key 在 args[1]（紧跟命令名之后）。
     * 部分特殊命令（如 EVAL/EVALSHA）的 key 从 args[2] 开始。
     * </p>
     */
    private int getKeyIndex(String cmd) {
        switch (cmd) {
            case "EVAL":
            case "EVALSHA":
                return 2;
            case "SORT":
                return 1;
            case "PUBLISH":
                return 1;
            case "PING":
            case "INFO":
            case "TIME":
            case "DBSIZE":
            case "CLUSTER":
            case "RANDOMKEY":
            case "WAIT":
            case "COMMAND":
            case "AUTH":
            case "ECHO":
            case "FLUSHALL":
            case "FLUSHDB":
            case "SAVE":
            case "BGSAVE":
            case "LASTSAVE":
            case "SHUTDOWN":
            case "SLAVEOF":
            case "REPLICAOF":
            case "ROLE":
            case "CONFIG":
            case "CLIENT":
            case "SLOWLOG":
            case "SCRIPT":
            case "SELECT":
            case "SWAPDB":
            case "QUIT":
            case "HELLO":
            case "MEMORY":
            case "ACL":
            case "MODULE":
            case "DEBUG":
            case "OBJECT":
            case "LATENCY":
                return -1;
            default:
                return 1;
        }
    }

    /**
     * 为指定 key 获取目标节点连接。
     *
     * @param key 用于计算槽位的 key
     * @return 目标节点的 Jedis 连接
     */
    private Jedis getConnectionForKey(String key) {
        int slot = getSlot(key);
        HostAndPort targetNode = slotCache.getSlotNode(slot);
        if (targetNode == null) {
            refreshSlotMapping();
            targetNode = slotCache.getSlotNode(slot);
            if (targetNode == null) {
                throw new RuntimeException("No node found for slot " + slot);
            }
        }
        JedisPool pool = nodePools.get(targetNode);
        if (pool == null || pool.isClosed()) {
            refreshSlotMapping();
            pool = nodePools.get(slotCache.getSlotNode(slot));
            if (pool == null) {
                throw new RuntimeException("No pool found for node " + targetNode);
            }
        }
        return pool.getResource();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 集群模式命令执行流程：
     * <ul>
     *   <li>确定 key 在参数中的位置</li>
     *   <li>计算 CRC16 槽位，路由到目标节点</li>
     *   <li>调用 {@link Jedis#sendCommand(ProtocolCommand, byte[][])} 执行命令</li>
     *   <li>如果收到 {@code MOVED} 或 {@code ASK} 错误，刷新拓扑并重试</li>
     * </ul>
     * </p>
     */
    @Override
    public Object call(Object... args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Command arguments must not be empty");
        }

        String cmdStr = args[0].toString().toUpperCase();
        ProtocolCommand cmd = resolveCommand(cmdStr);

        int keyIdx = getKeyIndex(cmdStr);

        // 编码所有参数为 byte[]
        byte[][] byteArgs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            byteArgs[i] = SafeEncoder.encode(args[i].toString());
        }

        // 不需要 key 路由的命令（PING, INFO 等），选择任意节点
        if (keyIdx < 0) {
            return executeOnRandomNode(cmd, byteArgs);
        }

        // 需要 key 路由：使用第一个 key 计算槽位
        if (keyIdx >= args.length) {
            // 命令声明了 key 索引但参数不够，视为无 key 命令
            return executeOnRandomNode(cmd, byteArgs);
        }
        String key = args[keyIdx].toString();
        int retries = 0;

        while (retries <= MAX_REDIRECT_RETRIES) {
            try (Jedis jedis = getConnectionForKey(key)) {
                // sendCommand 已内置响应解析
                Object response = jedis.sendCommand(cmd,
                        Arrays.copyOfRange(byteArgs, 1, byteArgs.length));
                return normalizeResponse(response);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("MOVED") || msg.contains("ASK"))) {
                    retries++;
                    refreshSlotMapping();
                    continue;
                }
                throw new IOException(
                        "Failed to execute cluster command: " + cmdStr, e);
            }
        }
        throw new IOException(
                "Failed to execute cluster command after " + MAX_REDIRECT_RETRIES + " retries: " + cmdStr);
    }

    /** 在随机节点上执行不需要 key 路由的命令 */
    private Object executeOnRandomNode(ProtocolCommand cmd, byte[][] byteArgs) throws IOException {
        List<HostAndPort> nodes = new ArrayList<>(nodePools.keySet());
        if (nodes.isEmpty()) {
            throw new IOException("No available cluster nodes");
        }
        Collections.shuffle(nodes, new Random());

        for (HostAndPort node : nodes) {
            JedisPool pool = nodePools.get(node);
            if (pool == null || pool.isClosed()) continue;

            try (Jedis jedis = pool.getResource()) {
                Object response = jedis.sendCommand(cmd,
                        Arrays.copyOfRange(byteArgs, 1, byteArgs.length));
                return normalizeResponse(response);
            } catch (Exception e) {
                continue; // 当前节点失败，尝试下一个
            }
        }
        throw new IOException("Failed to execute command on any cluster node");
    }

    /**
     * 将命令名字符串解析为 {@link ProtocolCommand}。
     * <p>
     * 优先尝试 {@link Protocol.Command#valueOf(String)}；
     * 若枚举不匹配（如 COMMAND、模块命令等），返回自定义实现。
     * </p>
     */
    private ProtocolCommand resolveCommand(String cmdStr) {
        try {
            return Protocol.Command.valueOf(cmdStr);
        } catch (IllegalArgumentException e) {
            // fallback: 自定义实现以支持任意命令
        }
        final byte[] rawBytes = SafeEncoder.encode(cmdStr);
        return () -> rawBytes;
    }

    /** 标准化响应格式 */
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
        for (Map.Entry<HostAndPort, JedisPool> entry : nodePools.entrySet()) {
            JedisPool pool = entry.getValue();
            if (pool != null && !pool.isClosed()) {
                pool.close();
            }
        }
        nodePools.clear();
        slotCache = null;
    }

    @Override
    public boolean isCluster() {
        return true;
    }

    // ======================== 内部类 ========================

    /** 槽位缓存 */
    private static class SlotCache {
        private final Map<Integer, HostAndPort> slotMap;
        @SuppressWarnings("unused")
        private final long refreshTime;

        SlotCache(Map<Integer, HostAndPort> slotMap, long refreshTime) {
            this.slotMap = Collections.unmodifiableMap(new HashMap<>(slotMap));
            this.refreshTime = refreshTime;
        }

        HostAndPort getSlotNode(int slot) {
            return slotMap.get(slot);
        }
    }

    // ======================== 访问器 ========================

    public Set<HostAndPort> getInitialNodes() {
        return new HashSet<>(initialNodes);
    }

    public Map<HostAndPort, JedisPool> getNodePools() {
        return Collections.unmodifiableMap(nodePools);
    }

    public int getSlotCount() {
        SlotCache cache = slotCache;
        return cache != null ? cache.slotMap.size() : 0;
    }
}
