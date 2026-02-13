package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.web.filter.RefererFilter;
import io.github.luminion.starter.web.filter.RepeatableFilter;
import io.github.luminion.starter.web.filter.XssFilter;
import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.util.function.Function;


/**
 * servlet自动配置（jakarta版本）
 *
 * @author luminion
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Deprecated
public class ServletConfigurationBack {

    /**
     * 请求日志记录筛选器
     *
     * @return commons请求日志记录筛选器
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(2000);
        return loggingFilter;
    }


    @ConditionalOnClass(Jsoup.class)
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(Prop.class)
    static class XssFilterRegistrationConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "xssFilterRegistration")
        public FilterRegistrationBean<XssFilter> xssFilterRegistration(Prop prop) {
            FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
            registration.setDispatcherTypes(DispatcherType.REQUEST);
            Function<String, String> sanitizer;
            switch (prop.getXssStrategy()) {
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
                            prop.getXssStrategy());

            }
            XssFilter xssFilter = new XssFilter(prop.getServletFilter().getXssIncludes(),
                    prop.getServletFilter().getXssExcludes(),
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
    @ConditionalOnMissingBean(name = "refererFilterRegistration")
    @ConditionalOnBean(Prop.class)
    public FilterRegistrationBean<RefererFilter> refererFilterRegistration(Prop prop) {
        FilterRegistrationBean<RefererFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        RefererFilter refererFilter = new RefererFilter(prop.getServletFilter().getRefererAllowDomains());
        registration.setFilter(refererFilter);
        registration.setName("refererFilter");
        // 设置为拦截所有路径，由过滤器内部进行路径匹配
        registration.addUrlPatterns("/*");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        log.debug("refererFilterRegistration Configured");
        return registration;
    }

    @Bean
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

