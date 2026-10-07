package io.github.luminion.velo.log.config;



import java.util.Collections;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * 为 Spring Boot 默认文本日志补充低优先级日期格式，保留用户自定义输出。
 */
public class VeloLogEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DATE_FORMAT_PROPERTY = "logging.pattern.dateformat";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String[] CUSTOM_LOGGING_PROPERTIES = {
            DATE_FORMAT_PROPERTY,
            "LOG_DATEFORMAT_PATTERN",
            "logging.config",
            "logback.configurationFile",
            "log4j.configurationFile",
            "log4j2.configurationFile",
            "logging.pattern.console",
            "logging.pattern.file",
            "CONSOLE_LOG_PATTERN",
            "FILE_LOG_PATTERN",
            "logging.structured.format.console",
            "logging.structured.format.file",
            "CONSOLE_LOG_STRUCTURED_FORMAT",
            "FILE_LOG_STRUCTURED_FORMAT"
    };
    private static final String[] LOGBACK_CONFIGURATIONS = {
            "logback-test.xml",
            "logback-test.groovy",
            "logback.xml",
            "logback.groovy",
            "logback-spring.xml",
            "logback-spring.groovy"
    };
    private static final String[] LOG4J_CONFIG_PREFIXES = {"log4j2-test", "log4j2", "log4j2-spring"};
    private static final String[] LOG4J_CONFIG_EXTENSIONS = {
            ".properties", ".xml", ".json", ".jsn", ".yaml", ".yml"
    };

    // 在 ConfigData 和无侵入模式默认值之后、日志系统初始化之前补充格式。
    @Getter
    private final int order = ConfigDataEnvironmentPostProcessor.ORDER + 2;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("velo.opinionated", Boolean.class, true)
                || !environment.getProperty("velo.log.enabled", Boolean.class, true)) {
            return;
        }
        for (String property : CUSTOM_LOGGING_PROPERTIES) {
            // 显式空值也属于用户配置，不替用户纠正或覆盖。
            if (environment.containsProperty(property)) {
                return;
            }
        }
        if (hasCustomLoggingConfiguration(application)) {
            return;
        }
        environment
                .getPropertySources()
                .addLast(
                        new MapPropertySource(
                                "veloLogPatternDefaults",
                                Collections.singletonMap(DATE_FORMAT_PROPERTY, DEFAULT_DATE_FORMAT)));
    }

    private boolean hasCustomLoggingConfiguration(SpringApplication application) {
        ResourceLoader resources = application.getResourceLoader();
        if (resources == null) {
            resources = new DefaultResourceLoader(application.getClassLoader());
        }
        for (String configuration : LOGBACK_CONFIGURATIONS) {
            if (resources.getResource("classpath:" + configuration).exists()) {
                return true;
            }
        }
        for (String prefix : LOG4J_CONFIG_PREFIXES) {
            for (String extension : LOG4J_CONFIG_EXTENSIONS) {
                if (resources.getResource("classpath:" + prefix + extension).exists()) {
                    return true;
                }
            }
        }
        return resources.getResource("classpath:logging.properties").exists();
    }
}
