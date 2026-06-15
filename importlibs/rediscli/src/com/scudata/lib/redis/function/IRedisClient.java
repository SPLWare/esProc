package com.scudata.lib.redis.function;

import java.io.IOException;

/**
 * Redis 客户端统一接口。
 * <p>
 * 为三种部署模式（Standalone / Sentinel / Cluster）提供统一的 call() 和 close() 契约，
 * 使得上层的 SocketRedis / SocketRedisOpen 可以以多态方式操作，无需关心底层连接差异。
 * </p>
 * 
 * <b>设计原则：</b>
 * <ul>
 *   <li>单一职责：每个实现类只负责一种模式的连接管理和命令执行</li>
 *   <li>开闭原则：新增部署模式只需添加新的实现类，无需修改上层调用方</li>
 *   <li>里氏替换：所有实现类均可透明替换</li>
 * </ul>
 * 
 * @author RedisCli
 */
public interface IRedisClient extends AutoCloseable {

    /**
     * 调用一个 Redis 命令。
     * <p>
     * 参数格式与 Redis 协议一致：第一个参数为命令名（如 "GET", "SET"），
     * 后续参数为该命令的参数。
     * </p>
     *
     * <b>示例：</b>
     * <pre>{@code
     *   client.call("GET", "mykey");
     *   client.call("SET", "mykey", "myvalue");
     *   client.call("HGETALL", "myhash");
     * }</pre>
     *
     * @param args 命令名 + 参数列表
     * @return 执行结果，可能为 byte[] / Long / List&lt;Object&gt; / String / null
     * @throws IOException 网络异常或 Redis 服务端错误
     */
    Object call(Object... args) throws IOException;

    /**
     * 关闭客户端，释放所有底层资源（连接池、Socket 等）。
     * <p>
     * 实现 {@link AutoCloseable} 接口以支持 try-with-resources 语法。
     * </p>
     */
    @Override
    void close();

    /**
     * 判断当前客户端是否为集群模式。
     * <p>
     * 集群模式下的命令路由方式与单机/哨兵不同，
     * 上层可根据此标识决定是否可以执行某些特殊操作（如 multi-key 命令）。
     * </p>
     *
     * @return true 表示集群模式
     */
    boolean isCluster();
}
