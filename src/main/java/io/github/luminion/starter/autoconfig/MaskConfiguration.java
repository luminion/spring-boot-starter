package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.mask.strategy.*;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.support.JsoupXssCleaner;
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
@AutoConfiguration
public class MaskConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BankCardMasker bankCardMasker() {
        return new BankCardMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailMasker emailMasker() {
        return new EmailMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdCardMasker idCardMasker() {
        return new IdCardMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public NameMasker nameMasker() {
        return new NameMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public PhoneMasker phoneMasker() {
        return new PhoneMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleMasker simpleMasker() {
        return new SimpleMasker();
    }

    
}
