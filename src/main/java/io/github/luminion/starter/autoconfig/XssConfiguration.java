package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.support.JsoupXssCleaner;
import org.jsoup.Jsoup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨域配置
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
public class XssConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jsoup.class)
    static class JsoupXssConfig {
        @Bean
        @ConditionalOnMissingBean
        public XssCleaner xssCleaner(Prop prop) {
            return new JsoupXssCleaner(prop.getXssStrategy());
        }
    }
}
