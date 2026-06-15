package com.scudata.lib.redis.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * Redis 连接关闭函数。
 * <p>
 * 关闭由 {@link SocketRedisOpen}、{@link SocketRedisOpenSentinel} 或
 * {@link SocketRedisOpenCluster} 打开的 Redis 连接，释放所有底层资源。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 *   handle = redis_open("127.0.0.1:6379", "password")
 *   redis_close(handle)
 * }</pre>
 *
 * @author RedisCli
 */
public class SocketRedisClose extends Function {

    @Override
    public Node optimize(Context ctx) {
        return this;
    }

    @Override
    public Object calculate(Context ctx) {
        if (param == null) {
            MessageManager mm = EngineMessage.get();
            throw new RQException("redis_close" + mm.getMessage("function.missingParam"));
        }

        try {
            // 获取第一个参数（即 redis_open 返回的连接句柄）
            Object handle = param.getLeafExpression().calculate(ctx);

            if (handle == null) {
                // 句柄为 null，视为已关闭，直接返回成功
                return "success";
            }

            if (handle instanceof SocketRedisOpen) {
                // 统一关闭：SocketRedisOpen / SocketRedisOpenSentinel / SocketRedisOpenCluster
                // 后两者继承了 SocketRedisOpen，因此 instanceof 检查覆盖所有三种情况
                ((SocketRedisOpen) handle).close();
            } else if (handle instanceof IRedisClient) {
                // 如果直接持有 IRedisClient 实例
                ((IRedisClient) handle).close();
            } else {
                throw new RQException("redis_close: invalid handle type: "
                        + handle.getClass().getName());
            }

            return "success";

        } catch (RQException e) {
            throw e;
        } catch (Exception e) {
            throw new RQException("redis_close error: " + e.getMessage(), e);
        }
    }
}
