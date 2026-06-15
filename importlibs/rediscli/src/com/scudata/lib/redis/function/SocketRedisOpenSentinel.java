package com.scudata.lib.redis.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis 哨兵模式连接打开函数（独立入口，方案 D）。
 * <p>
 * 与 {@link SocketRedisOpen} 的区别在于，此函数以独立参数形式接收哨兵配置，
 * 而非通过 URL 格式的字符串解析，更适合在脚本中显式指定各参数。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   redis_open_sentinel("mymaster", "127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381", "password")
 * }</pre>
 *
 * <b>参数说明：</b>
 * <ol>
 *   <li>masterName — Redis Sentinel 中配置的 Master 名称</li>
 *   <li>sentinelList — 哨兵节点列表，逗号分隔，格式：host:port,host:port,...</li>
 *   <li>password（可选）— Redis 密码</li>
 * </ol>
 *
 * @author RedisCli
 */
/**
 * Redis 哨兵模式连接打开函数（独立入口，方案 D）。
 * <p>
 * 继承自 {@link SocketRedisOpen}，因此可与 {@link SocketRedis} 无缝协作。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   redis_open_sentinel("mymaster", "127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381", "password")
 * }</pre>
 *
 * <b>参数说明：</b>
 * <ol>
 *   <li>masterName — Redis Sentinel 中配置的 Master 名称</li>
 *   <li>sentinelList — 哨兵节点列表，逗号分隔，格式：host:port,host:port,...</li>
 *   <li>password（可选）— Redis 密码</li>
 * </ol>
 *
 * @author RedisCli
 */
public class SocketRedisOpenSentinel extends SocketRedisOpen {

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
            if (objs == null || objs.length < 2) {
                MessageManager mm = EngineMessage.get();
                throw new RQException("redis_open_sentinel" +
                        mm.getMessage("function.missingParam"));
            }

            // 参数 1: masterName
            String masterName = objs[0].toString();
            if (masterName.isEmpty()) {
                throw new RQException("redis_open_sentinel: masterName must not be empty");
            }

            // 参数 2: sentinelList (逗号分隔)
            String sentinelList = objs[1].toString();
            Set<String> sentinelSet = new HashSet<>();
            for (String node : sentinelList.split(",")) {
                String trimmed = node.trim();
                if (!trimmed.isEmpty()) {
                    sentinelSet.add(trimmed);
                }
            }
            if (sentinelSet.isEmpty()) {
                throw new RQException("redis_open_sentinel: at least one sentinel node is required");
            }

            // 参数 3: password（可选）
            String password = (objs.length >= 3 && objs[2] instanceof String)
                    ? objs[2].toString()
                    : null;

            redis = new SentinelRedisClient(masterName, sentinelSet, password);

        } catch (RQException e) {
            throw e;
        } catch (Exception e) {
            throw new RQException("redis_open_sentinel failed: " + e.getMessage(), e);
        }

        return this;
    }
}
