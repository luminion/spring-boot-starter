package io.github.luminion.autoconfigure.servlet;

import io.github.luminion.autoconfigure.ConditionalOnListProperty;
import io.github.luminion.autoconfigure.servlet.filter.RefererFilter;
import io.github.luminion.autoconfigure.servlet.filter.RepeatableFilter;
import io.github.luminion.autoconfigure.servlet.filter.XssFilter;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;


/**
 * servlet自动配置
 *
 * @author luminion
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({ServletFilterProperties.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(value = "turbo.servlet.enabled", havingValue = "true", matchIfMissing = true)
public class ServletAutoConfiguration {
    
    @ConditionalOnClass(Jsoup.class)
    @Configuration(proxyBeanMethods = false)
    static class XssFilterRegistrationConfiguration {
        @Bean
        @ConditionalOnListProperty(value = "turbo.servlet.filter.xss-includes")
        @ConditionalOnMissingBean(name = "xssFilterRegistration")
        public FilterRegistrationBean<XssFilter> xssFilterRegistration(ServletFilterProperties filterProperties) {
            FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
            registration.setDispatcherTypes(DispatcherType.REQUEST);
            Function<String, String> sanitizer;
            switch (filterProperties.getXssSanitizer()) {
                case NONE:
                    sanitizer = s -> Jsoup.clean(s, Safelist.none());
                    break;
                case SIMPLE_TEXT:
                    sanitizer = s -> Jsoup.clean(s, Safelist.simpleText());
                    break;
                case BASIC:
                    sanitizer = s -> Jsoup.clean(s, Safelist.basic());
                    break;
                case BASIC_WITH_IMAGES:
                    sanitizer = s -> Jsoup.clean(s, Safelist.basicWithImages());
                    break;
                case RELAXED:
                    sanitizer = s -> Jsoup.clean(s, Safelist.relaxed());
                    break;
                default:
                    sanitizer = s -> Jsoup.clean(s, Safelist.relaxed());
                    log.warn("Unsupported value '{}' for property 'luminion.servlet.filter.xss-sanitizer', " +
                                    "using 'RELAXED' instead. Supported values: [NONE, SIMPLE_TEXT, BASIC, BASIC_WITH_IMAGES, RELAXED]",
                            filterProperties.getXssSanitizer());
                    
            }
            XssFilter xssFilter = new XssFilter(filterProperties.getXssIncludes(),
                    filterProperties.getXssExcludes(),
                    sanitizer
            );
            registration.setFilter(xssFilter);
            registration.setName("xssFilter");
            // 设置为拦截所有路径，由过滤器内部进行路径匹配
            registration.addUrlPatterns("/*");
            registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
            log.debug("xssFilterRegistration Configured");
            return registration;
        }
    }

    @Bean
    @ConditionalOnListProperty(value = "turbo.servlet.filter.referer-allow-domains")
    @ConditionalOnMissingBean(name = "refererFilterRegistration")
    public FilterRegistrationBean<RefererFilter> refererFilterRegistration(ServletFilterProperties properties) {
        FilterRegistrationBean<RefererFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        RefererFilter refererFilter = new RefererFilter(properties.getRefererAllowDomains());
        registration.setFilter(refererFilter);
        registration.setName("refererFilter");
        // 设置为拦截所有路径，由过滤器内部进行路径匹配
        registration.addUrlPatterns("/*");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        log.debug("refererFilterRegistration Configured");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.servlet.filter.repeatable", havingValue = "true")
    @ConditionalOnMissingBean(name = "repeatableFilterRegistration")
    public FilterRegistrationBean<RepeatableFilter> repeatableFilterRegistration() {
        FilterRegistrationBean<RepeatableFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        log.debug("repeatableFilterRegistration Configured");
        return registration;
    }

}
