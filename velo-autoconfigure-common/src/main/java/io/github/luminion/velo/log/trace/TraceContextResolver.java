package io.github.luminion.velo.log.trace;

/**
 * 从当前执行上下文解析链路数据；没有有效来源时生成新值。
 *
 * <p>实现可以读取 Spring 请求上下文或用户自己的 ThreadLocal。实现应线程安全，返回值不能携带请求、响应对象。 框架在入口调用一次，随后负责复用、传播和恢复，不管理实现自身的
 * ThreadLocal。
 */
@FunctionalInterface
public interface TraceContextResolver {
    TraceData resolve();
}
