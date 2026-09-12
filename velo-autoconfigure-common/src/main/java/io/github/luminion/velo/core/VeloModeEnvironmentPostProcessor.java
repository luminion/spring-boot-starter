package io.github.luminion.velo.core;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用无侵入模式的默认值，且不覆盖用户显式配置。
 */
public class VeloModeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "veloOpinionatedDefaults";

    private final Log log;

    /**
     * Spring Boot 在引导阶段通过该构造器注入 {@link DeferredLogFactory}，
     * 使得环境后处理阶段产生的日志能延迟到日志系统就绪后再输出。
     */
    public VeloModeEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(VeloModeEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean opinionated = environment.getProperty("velo.opinionated", Boolean.class, true);
        if (opinionated) {
            return;
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("velo.log.trace.enabled", "false");
        defaults.put("velo.log.invocation.controller.enabled", "false");
        defaults.put("velo.log.invocation.feign.enabled", "false");
        defaults.put("velo.jackson.enabled", "false");
        defaults.put("velo.spring-converter.date-time-enabled", "false");
        defaults.put("velo.mybatis-plus.enabled", "false");
        defaults.put("velo.cache.enabled", "false");
        defaults.put("velo.redis.enabled", "false");
        defaults.put("velo.excel.converters.enabled", "false");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));

        log.info("[Velo Starter] 无侵入模式已启用，以下全局增强默认关闭（可通过对应 enabled 配置单独覆盖）："
                + "trace/MDC 日志、Controller 与 Feign 调用日志、Jackson 扩展、Spring 日期转换器、"
                + "MyBatis-Plus 拦截器、缓存、RedisTemplate、Excel converter。");
    }

    @Override
    public int getOrder() {
        // 必须在 ConfigData 加载 application 配置后执行，才能识别用户显式设置的 velo.opinionated。
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
