package com.scudata.lib.redis.function;

import redis.clients.jedis.HostAndPort;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis 连接打开函数（统一入口）。
 * <p>
 * 支持三种部署模式，通过连接字符串前缀自动识别：
 * <ul>
 *   <li><b>单机模式（Standalone）</b> — {@code host:port}（向后兼容原有格式）</li>
 *   <li><b>哨兵模式（Sentinel）</b> — {@code sentinel://masterName@host1:port1,host2:port2,...}</li>
 *   <li><b>集群模式（Cluster）</b> — {@code cluster://host1:port1,host2:port2,...}</li>
 * </ul>
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   // 单机（原有格式，完全兼容）
 *   redis_open("127.0.0.1:6379", "password")
 *
 *   // 哨兵
 *   redis_open("sentinel://mymaster@127.0.0.1:26379,127.0.0.1:26380", "password")
 *
 *   // 集群
 *   redis_open("cluster://127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382", "password")
 * }</pre>
 *
 * @author RedisCli
 */
public class SocketRedisOpen extends ImFunction {

    /**
     * Redis 客户端实例。
     * <p>
     * 通过 {@link IRedisClient} 接口统一多态访问，
     * 实际类型为 {@link StandaloneRedisClient} / {@link SentinelRedisClient} / {@link ClusterRedisClient} 之一。
     * </p>
     */
    protected IRedisClient redis = null;

    /**
     * 连接模式枚举。
     */
    private enum Mode {
        /** 单机模式 */
        STANDALONE,
        /** 哨兵模式 */
        SENTINEL,
        /** 集群模式 */
        CLUSTER
    }

    @Override
    public Node optimize(Context ctx) {
        if (param != null) {
            param.optimize(ctx);
        }
        return this;
    }

    @Override
    protected Object doQuery(Object[] objs) {
        try {
            if (objs == null || objs.length < 1) {
                MessageManager mm = EngineMessage.get();
                throw new RQException("redis_open" + mm.getMessage("function.paramTypeError"));
            }

            String connStr = objs[0].toString();
            String password = (objs.length >= 2 && objs[1] instanceof String)
                    ? objs[1].toString()
                    : null;

            // 1. 解析连接模式
            Mode mode = detectMode(connStr);

            // 2. 根据模式创建对应的客户端
            switch (mode) {
                case SENTINEL:
                    redis = createSentinelClient(connStr, password);
                    break;
                case CLUSTER:
                    redis = createClusterClient(connStr, password);
                    break;
                default:
                    redis = createStandaloneClient(connStr, password);
                    break;
            }

        } catch (RQException e) {
            throw e;
        } catch (Exception e) {
            throw new RQException("redis_open failed: " + e.getMessage(), e);
        }

        return this;
    }

    /**
     * 从连接字符串中检测部署模式。
     *
     * @param connStr 连接字符串
     * @return 检测到的模式
     */
    private Mode detectMode(String connStr) {
        if (connStr == null || connStr.isEmpty()) {
            return Mode.STANDALONE;
        }

        String trimmed = connStr.trim().toLowerCase();

        if (trimmed.startsWith("sentinel://")) {
            return Mode.SENTINEL;
        }
        if (trimmed.startsWith("cluster://")) {
            return Mode.CLUSTER;
        }

        return Mode.STANDALONE;
    }

    /**
     * 创建单机模式客户端。
     * <p>
     * 连接字符串格式：{@code host:port} 或 {@code host}
     * （兼容旧版格式，port 默认为 6379）
     * </p>
     */
    private IRedisClient createStandaloneClient(String connStr, String password) {
        String[] parts = connStr.split(":");
        String host = parts[0].trim();
        int port = 6379;
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new RQException("redis_open: invalid port in connection string: " + connStr);
            }
        }
        return new StandaloneRedisClient(host, port, password);
    }

    /**
     * 创建哨兵模式客户端。
     * <p>
     * 连接字符串格式：
     * {@code sentinel://masterName@host1:port1,host2:port2,...}
     * </p>
     */
    private IRedisClient createSentinelClient(String connStr, String password) {
        // 去掉 "sentinel://" 前缀
        String body = connStr.substring("sentinel://".length()).trim();

        // 提取 masterName: sentinel://masterName@sentinels
        int atIndex = body.indexOf('@');
        if (atIndex < 0) {
            throw new RQException(
                    "redis_open: sentinel connection string must contain '@', " +
                    "format: sentinel://masterName@host1:port1,host2:port2");
        }

        String masterName = body.substring(0, atIndex).trim();
        String sentinelPart = body.substring(atIndex + 1).trim();

        if (masterName.isEmpty()) {
            throw new RQException("redis_open: sentinel master name must not be empty");
        }
        if (sentinelPart.isEmpty()) {
            throw new RQException("redis_open: sentinel node list must not be empty");
        }

        // 解析哨兵节点列表
        Set<String> sentinels = new HashSet<>();
        for (String node : sentinelPart.split(",")) {
            sentinels.add(node.trim());
        }

        if (sentinels.isEmpty()) {
            throw new RQException("redis_open: at least one sentinel node is required");
        }

        return new SentinelRedisClient(masterName, sentinels, password);
    }

    /**
     * 创建集群模式客户端。
     * <p>
     * 连接字符串格式：
     * {@code cluster://host1:port1,host2:port2,...}
     * </p>
     */
    private IRedisClient createClusterClient(String connStr, String password) {
        // 去掉 "cluster://" 前缀
        String body = connStr.substring("cluster://".length()).trim();

        if (body.isEmpty()) {
            throw new RQException("redis_open: cluster node list must not be empty");
        }

        // 解析集群节点列表
        Set<HostAndPort> nodes = new HashSet<>();
        for (String node : body.split(",")) {
            node = node.trim();
            String[] parts = node.split(":");
            if (parts.length < 2) {
                throw new RQException(
                        "redis_open: invalid cluster node format, expected host:port, got: " + node);
            }
            String host = parts[0].trim();
            int port;
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new RQException(
                        "redis_open: invalid port in cluster node: " + node);
            }
            nodes.add(new HostAndPort(host, port));
        }

        if (nodes.size() < 1) {
            throw new RQException("redis_open: at least one cluster node is required");
        }

        return new ClusterRedisClient(nodes, password);
    }

    /**
     * 获取底层 Redis 客户端。
     * <p>
     * 供 {@link SocketRedis} 调用命令时使用。
     * </p>
     *
     * @return IRedisClient 实例
     */
    public IRedisClient getRedisClient() {
        return redis;
    }

    /**
     * 关闭连接。
     * <p>
     * 供 {@link SocketRedisClose} 调用。
     * </p>
     */
    public void close() {
        if (redis != null) {
            redis.close();
            redis = null;
        }
    }
}
