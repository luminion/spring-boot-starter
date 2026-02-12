package io.github.luminion.starter.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnMissingBean
public class LogConfiguration {



}
