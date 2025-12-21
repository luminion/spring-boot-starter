package io.github.luminion.autoconfigure.core;

import io.github.luminion.autoconfigure.core.spi.KeyResolver;
import io.github.luminion.autoconfigure.core.support.SpelKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
public class CoreAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public KeyResolver spelMethodFingerprinter() {
        log.debug("SpelKeyResolver Configured");
        return new SpelKeyResolver("spelFingerprinter");
    }
}
