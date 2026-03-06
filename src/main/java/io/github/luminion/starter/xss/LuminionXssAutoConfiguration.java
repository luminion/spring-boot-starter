package io.github.luminion.starter.xss;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.web.filter.XssFilter;
import io.github.luminion.starter.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.starter.xss.converter.XssStringConverter;
import org.jsoup.Jsoup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSS配置
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class LuminionXssAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jsoup.class)
    static class JsoupXssConfig {
        @Bean
        @ConditionalOnMissingBean
        public XssCleaner xssCleaner(Prop prop) {
            return new JsoupXssCleaner(prop.getXssStrategy());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XssCleaner.class)
    public XssStringConverter stringToHtmlConverter(XssCleaner xssCleaner) {
        return new XssStringConverter(xssCleaner);
    }
}
