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
 * Redis 集群模式连接打开函数（独立入口，方案 D）。
 * <p>
 * 与 {@link SocketRedisOpen} 的区别在于，此函数以独立参数形式接收集群节点列表，
 * 更适合在脚本中显式指定各参数。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   redis_open_cluster("127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382", "password")
 * }</pre>
 *
 * <b>参数说明：</b>
 * <ol>
 *   <li>nodeList — 集群节点列表，逗号分隔，格式：host:port,host:port,...</li>
 *   <li>password（可选）— Redis 密码</li>
 * </ol>
 *
 * @author RedisCli
 */
/**
 * Redis 集群模式连接打开函数（独立入口，方案 D）。
 * <p>
 * 继承自 {@link SocketRedisOpen}，因此可与 {@link SocketRedis} 无缝协作。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   redis_open_cluster("127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382", "password")
 * }</pre>
 *
 * <b>参数说明：</b>
 * <ol>
 *   <li>nodeList — 集群节点列表，逗号分隔，格式：host:port,host:port,...</li>
 *   <li>password（可选）— Redis 密码</li>
 * </ol>
 *
 * @author RedisCli
 */
public class SocketRedisOpenCluster extends SocketRedisOpen {

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
                throw new RQException("redis_open_cluster" +
                        mm.getMessage("function.missingParam"));
            }

            // 参数 1: nodeList (逗号分隔)
            String nodeList = objs[0].toString();
            Set<HostAndPort> nodes = new HashSet<>();
            for (String nodeStr : nodeList.split(",")) {
                String trimmed = nodeStr.trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split(":");
                if (parts.length < 2) {
                    throw new RQException(
                            "redis_open_cluster: invalid node format, expected host:port, got: " + trimmed);
                }
                String host = parts[0].trim();
                int port;
                try {
                    port = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    throw new RQException(
                            "redis_open_cluster: invalid port in node: " + trimmed);
                }
                nodes.add(new HostAndPort(host, port));
            }

            if (nodes.isEmpty()) {
                throw new RQException("redis_open_cluster: at least one cluster node is required");
            }

            // 参数 2: password（可选）
            String password = (objs.length >= 2 && objs[1] instanceof String)
                    ? objs[1].toString()
                    : null;

            redis = new ClusterRedisClient(nodes, password);

        } catch (RQException e) {
            throw e;
        } catch (Exception e) {
            throw new RQException("redis_open_cluster failed: " + e.getMessage(), e);
        }

        return this;
    }
}
