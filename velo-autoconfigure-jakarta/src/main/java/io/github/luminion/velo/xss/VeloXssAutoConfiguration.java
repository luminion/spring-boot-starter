package io.github.luminion.velo.xss;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.velo.xss.cleaner.SpringHtmlEscapeXssCleaner;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XSS 自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "velo.web.xss", name = "enabled", havingValue = "true")
public class VeloXssAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VeloXssAutoConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.jsoup.Jsoup")
    static class JsoupXssConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public XssCleaner xssCleaner(VeloProperties properties) {
            return new JsoupXssCleaner(properties.getWeb().getXss().getStrategy());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.jsoup.Jsoup")
    static class SpringXssConfiguration {

        SpringXssConfiguration(VeloProperties properties) {
            XssStrategy strategy = properties.getWeb().getXss().getStrategy();
            if (strategy != XssStrategy.NONE && strategy != XssStrategy.ESCAPE) {
                log.warn("[Velo Starter] XSS protection is enabled with strategy {}, but jsoup is not present. " +
                        "No XssCleaner will be registered. Add org.jsoup:jsoup or use strategy=ESCAPE.", strategy);
            }
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.web.xss", name = "strategy", havingValue = "ESCAPE")
        public XssCleaner xssCleaner() {
            return new SpringHtmlEscapeXssCleaner();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XssCleaner.class)
    public XssStringConverter xssStringConverter(XssCleaner xssCleaner) {
        return new XssStringConverter(xssCleaner);
    }
}
