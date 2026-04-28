package io.github.luminion.velo.xss;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.velo.xss.converter.XssStringConverter;
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
 * XSS 自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "velo.web.xss", name = "enabled", havingValue = "true")
public class VeloXssAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jsoup.class)
    static class JsoupXssConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.web.xss", name = "cleaner-enabled", havingValue = "true", matchIfMissing = true)
        public XssCleaner xssCleaner(VeloProperties properties) {
            return new JsoupXssCleaner(properties.getXssStrategy());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XssCleaner.class)
    @ConditionalOnProperty(prefix = "velo.web.xss", name = "string-converter-enabled", havingValue = "true", matchIfMissing = true)
    public XssStringConverter xssStringConverter(XssCleaner xssCleaner) {
        return new XssStringConverter(xssCleaner);
    }
}
