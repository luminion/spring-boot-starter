package io.github.luminion.starter.core;

import io.github.luminion.starter.core.spi.EnumFieldConvention;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.core.spi.JsonProcessorProvider;
import io.github.luminion.starter.core.spi.NamingSuffixStrategy;
import io.github.luminion.starter.core.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.starter.core.spi.func.*;
import io.github.luminion.starter.core.spi.provider.DefaultJsonProcessorProvider;
import io.github.luminion.starter.core.util.AspectUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author luminion
 * @since 1.0.0
 */
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


    @Bean
    @ConditionalOnMissingBean
    public JsonProcessorProvider jacksonProcessorProvider(BeanFactory beanFactory) {
        return new DefaultJsonProcessorProvider(beanFactory);
    }

}
