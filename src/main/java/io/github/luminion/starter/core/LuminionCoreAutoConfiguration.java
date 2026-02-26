package io.github.luminion.starter.core;

import io.github.luminion.starter.core.spi.EnumFieldConvention;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.core.spi.NamingSuffixStrategy;
import io.github.luminion.starter.core.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.starter.core.spi.masker.*;
import io.github.luminion.starter.core.util.AspectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(Prop.class)
public class LuminionCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Fingerprinter spelMethodFingerprinter() {
        return new SpelFingerprinter(AspectUtils::getArgsSimpleString);
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingSuffixStrategy suffixProvider() {
        return () -> "Name";
    }
    

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
    
    
    @Bean
    @ConditionalOnMissingBean
    public EnumFieldConvention enumFieldConventionProvider(Prop prop) {
        return new EnumFieldConvention() {
            @Override
            public List<String> codeFieldNames() {
                return prop.getEnumCodeFields();
            }

            @Override
            public List<String> descFieldNames() {
                return prop.getEnumDescFields();
            }
        };
    }

}
