package io.github.luminion.velo.trace;

import io.github.luminion.velo.VeloProperties.TraceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 入口作用域管理；不依赖日志引擎、日志功能或日志开关。
 */
public class TraceScopeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceScopeManager.class);
    private final TraceProperties properties;
    private final TraceContextResolver resolver;

    public TraceScopeManager(TraceProperties properties, TraceContextResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
        if (properties.isEnabled() && !StringUtils.hasText(properties.getMdcKey())) {
            LOGGER.warn("[Velo Starter] 配置告警：velo.trace.mdc-key 为空，链路 ID 无法正常写入日志上下文。");
        }
    }

    /**
     * 嵌套调用复用上下文；无上下文时建立局部作用域。
     */
    public TraceContext.Scope open() {
        return TraceContext.open(properties.getMdcKey(), properties.isEnabled(), resolver);
    }

    /**
     * 每次任务建立新的根作用域，结束后恢复工作线程上下文。
     */
    public TraceContext.Scope root() {
        return TraceContext.root(properties.getMdcKey(), properties.isEnabled(), resolver);
    }
}
