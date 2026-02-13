package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.spi.KeyResolver;
import io.github.luminion.starter.core.spi.support.SpelKeyResolver;
import io.github.luminion.starter.core.mask.*;
import io.github.luminion.starter.core.spi.XssHandler;
import io.github.luminion.starter.core.xss.support.JsoupXssHandler;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
public class CoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeyResolver spelMethodFingerprinter() {
        return new SpelKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Jsoup.class)
    public XssHandler xssCleaner(Prop prop) {
        return new JsoupXssHandler(prop.getXssStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public BankCardNoEncoder bankCardNoEncoder() {
        return new BankCardNoEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailEncoder emailEncoder() {
        return new EmailEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdCardEncoder idCardEncoder() {
        return new IdCardEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public NameEncoder nameEncoder() {
        return new NameEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public PhoneEncoder phoneEncoder() {
        return new PhoneEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleEncoder simpleEncoder() {
        return new SimpleEncoder();
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jsoup.class)
    static class XssConfig {
        @Bean
        @ConditionalOnMissingBean
        public XssHandler xssCleaner(Prop prop) {
            return new JsoupXssHandler(prop.getXssStrategy());
        }
    }

}
