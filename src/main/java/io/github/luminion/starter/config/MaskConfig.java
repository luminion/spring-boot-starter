package io.github.luminion.starter.config;

import io.github.luminion.starter.mask.strategy.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
public class MaskConfig {

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
