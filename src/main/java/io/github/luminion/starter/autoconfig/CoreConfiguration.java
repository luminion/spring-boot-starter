package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.spi.KeyResolver;
import io.github.luminion.starter.core.spi.support.SpelKeyResolver;
import io.github.luminion.starter.core.mask.*;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.support.JsoupXssCleaner;
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
    public XssCleaner xssCleaner(Prop prop) {
        return new JsoupXssCleaner(prop.getXssStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public BankCardMasker bankCardNoEncoder() {
        return new BankCardMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailMasker emailEncoder() {
        return new EmailMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdCardMasker idCardEncoder() {
        return new IdCardMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public NameMasker nameEncoder() {
        return new NameMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public PhoneMasker phoneEncoder() {
        return new PhoneMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleMasker simpleEncoder() {
        return new SimpleMasker();
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jsoup.class)
    static class XssConfig {
        @Bean
        @ConditionalOnMissingBean
        public XssCleaner xssCleaner(Prop prop) {
            return new JsoupXssCleaner(prop.getXssStrategy());
        }
    }

}
