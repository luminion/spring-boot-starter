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
public class VeloCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Fingerprinter fingerprinter() {
        return new SpelFingerprinter();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingSuffixStrategy namingSuffixStrategy() {
        return () -> "Name";
    }

    @Bean
    @ConditionalOnMissingBean
    public EnumFieldConvention enumFieldConvention(VeloProperties properties) {
        return new EnumFieldConvention() {
            @Override
            public List<String> codeFieldNames() {
                return properties.getCore().getEnumCodeFields();
            }

            @Override
            public List<String> descFieldNames() {
                return properties.getCore().getEnumDescFields();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonProcessorProvider jsonProcessorProvider(BeanFactory beanFactory) {
        return new DefaultJsonProcessorProvider(beanFactory);
    }
}
