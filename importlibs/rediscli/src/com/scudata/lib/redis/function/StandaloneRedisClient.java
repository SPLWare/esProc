package com.scudata.lib.redis.function;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.util.List;

/**
 * 单机模式（Standalone）Redis 客户端实现。
 * <p>
 * 基于 {@link JedisPool} 实现连接池管理，
 * 通过 {@link Jedis#sendCommand(ProtocolCommand, byte[][])} 发送任意 Redis 命令，
 * 兼容现有的 {@code redis.call("COMMAND", arg1, arg2, ...)} 通用调用风格。
 * </p>
 *
 * <b>核心流程：</b>
 * <ol>
 *   <li>从连接池借用一个 {@link Jedis} 实例</li>
 *   <li>将命令名解析为 {@link Protocol.Command} 枚举</li>
 *   <li>调用 {@code sendCommand()} 发送命令和参数</li>
 *   <li>直接获取返回的解析结果（sendCommand 已内置响应解析）</li>
 *   <li>将 Jedis 实例归还连接池</li>
 * </ol>
 *
 * @author RedisCli
 */
public class StandaloneRedisClient implements IRedisClient {

    /** Jedis 连接池 */
    private final JedisPool pool;

    /** 连接超时（毫秒） */
    private static final int DEFAULT_TIMEOUT = 2000;

    /** 最大连接数 */
    private static final int DEFAULT_MAX_TOTAL = 8;

    /** 最大空闲连接数 */
    private static final int DEFAULT_MAX_IDLE = 4;

    /**
     * 构造单机模式客户端。
     *
     * @param host     Redis 主机地址
     * @param port     Redis 端口
     * @param password 密码，可为 null 或空字符串表示无需认证
     * @param timeout  超时时间（毫秒），&le;0 时使用默认值
     */
    public StandaloneRedisClient(String host, int port, String password, int timeout) {
        JedisPoolConfig config = createDefaultPoolConfig();
        int connTimeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
        if (password != null && !password.isEmpty()) {
            this.pool = new JedisPool(config, host, port, connTimeout, password);
        } else {
            this.pool = new JedisPool(config, host, port, connTimeout);
        }
    }

    /** 简化构造：使用默认超时 */
    public StandaloneRedisClient(String host, int port, String password) {
        this(host, port, password, DEFAULT_TIMEOUT);
    }

    /** 简化构造：无密码 */
    public StandaloneRedisClient(String host, int port) {
        this(host, port, null, DEFAULT_TIMEOUT);
    }

    /** 创建默认连接池配置 */
    private static JedisPoolConfig createDefaultPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(DEFAULT_MAX_TOTAL);
        config.setMaxIdle(DEFAULT_MAX_IDLE);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setBlockWhenExhausted(true);
        return config;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>实现说明：</b>
     * <ol>
     *   <li>从连接池借用一个 {@link Jedis} 实例（try-with-resource 自动归还）</li>
     *   <li>将第一个参数解析为 {@link Protocol.Command} 枚举</li>
     *   <li>剩余参数编码为 byte[] 数组</li>
     *   <li>调用 {@link Jedis#sendCommand(ProtocolCommand, byte[][])} 执行命令</li>
     *   <li>直接返回解析后的结果（sendCommand 内部已调用 getOne）</li>
     * </ol>
     * </p>
     */
    @Override
    public Object call(Object... args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Command arguments must not be empty");
        }

        try (Jedis jedis = pool.getResource()) {
            // 将 Redis 命令名（args[0]）解析为 ProtocolCommand
            // 优先使用 Protocol.Command 枚举（标准命令），
            // 如果枚举中不存在（如 COMMAND、模块命令等），则创建自定义实现
            String cmdStr = args[0].toString().toUpperCase();
            ProtocolCommand cmd = resolveCommand(cmdStr);

            // 将参数（args[1..n]）编码为 byte[]
            byte[][] bargs = new byte[args.length - 1][];
            for (int i = 1; i < args.length; i++) {
                bargs[i - 1] = SafeEncoder.encode(args[i].toString());
            }

            // 发送命令并获取解析后的响应
            // BinaryJedis.sendCommand(ProtocolCommand, byte[]...) 已内置响应解析
            Object response = jedis.sendCommand(cmd, bargs);
            return normalizeResponse(response);

        } catch (Exception e) {
            throw new IOException("Failed to execute Redis command: " + args[0], e);
        }
    }

    /**
     * 将命令名字符串解析为 {@link ProtocolCommand}。
     * <p>
     * 优先尝试 {@link Protocol.Command#valueOf(String)} 匹配标准枚举，
     * 如果命令不在枚举中（如 {@code COMMAND}、模块命令 {@code FT.SEARCH} 等），
     * 则返回一个自定义的 {@link ProtocolCommand} 实现，直接使用原始命令名字节。
     * </p>
     *
     * <b>为何需要 fallback：</b>
     * Redis 有很多有效命令（如 COMMAND、MODULE LIST 等），
     * 这些命令可能不在 Jedis 的 {@link Protocol.Command} 枚举中。
     * 通过自定义实现 {@link ProtocolCommand#getRaw()}，可以支持
     * 任意 Redis 命令（包括 Redis Modules 的扩展命令）。
     */
    private ProtocolCommand resolveCommand(String cmdStr) {
        // 1. 尝试标准枚举匹配
        try {
            return Protocol.Command.valueOf(cmdStr);
        } catch (IllegalArgumentException e) {
            // fall through to custom implementation
        }

        // 2. 枚举不匹配时，返回自定义实现
        final byte[] rawBytes = SafeEncoder.encode(cmdStr);
        return () -> rawBytes;
    }

    /**
     * 将 Jedis 原生响应标准化为上层可识别的格式。
     * <ul>
     *   <li>{@code byte[]} → {@link String}（UTF-8 解码）</li>
     *   <li>{@code List&lt;?&gt;} → 递归解析为 {@code Object[]}</li>
     *   <li>其他类型（{@code Long}, {@code String}）→ 直接返回</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private Object normalizeResponse(Object response) {
        if (response == null) {
            return null;
        }
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
        // Long, String 等类型直接返回
        return response;
    }

    @Override
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    /** 获取底层连接池（用于监控或扩展） */
    public JedisPool getPool() {
        return pool;
    }
}
