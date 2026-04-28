package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.EnumFieldConvention;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.spi.NamingSuffixStrategy;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.spi.provider.DefaultJsonProcessorProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 核心基础能力自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(VeloProperties.class)
@ConditionalOnProperty(prefix = "velo.core", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.core", name = "fingerprinter-enabled", havingValue = "true", matchIfMissing = true)
    public Fingerprinter fingerprinter() {
        return new SpelFingerprinter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.core", name = "naming-suffix-strategy-enabled", havingValue = "true", matchIfMissing = true)
    public NamingSuffixStrategy namingSuffixStrategy() {
        return () -> "Name";
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.core", name = "enum-field-convention-enabled", havingValue = "true", matchIfMissing = true)
    public EnumFieldConvention enumFieldConvention(VeloProperties properties) {
        return new EnumFieldConvention() {
            @Override
            public List<String> codeFieldNames() {
                return properties.getEnumCodeFields();
            }

            @Override
            public List<String> descFieldNames() {
                return properties.getEnumDescFields();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.core", name = "json-processor-provider-enabled", havingValue = "true", matchIfMissing = true)
    public JsonProcessorProvider jsonProcessorProvider(BeanFactory beanFactory) {
        return new DefaultJsonProcessorProvider(beanFactory);
    }
}
