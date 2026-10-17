package io.github.luminion.velo.log.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 入口采集的公共信息快照，不持有 Servlet、Feign 等协议对象。
 */
@Getter
@Builder
public final class InvocationLogContext {
    /**
     * 客户端 IP。
     */
    private final String clientIp;
    /**
     * HTTP 方法。
     */
    private final String httpMethod;
    /**
     * 请求路径或路由模板。
     */
    private final String path;
    /**
     * Feign 客户端名称。
     */
    private final String clientName;
    /**
     * Feign 配置上下文标识。
     */
    private final String contextId;
    /**
     * 当前已知的实际实例地址。
     */
    private final String instanceAddress;
}
