package io.github.luminion.starter.feature.xss;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.feature.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.starter.feature.xss.converter.XssStringConverter;
import org.jsoup.Jsoup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
