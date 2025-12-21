package io.github.luminion.autoconfigure.core;

import io.github.luminion.autoconfigure.core.spi.MethodFingerprinter;
import io.github.luminion.autoconfigure.core.support.SpelMethodFingerprinter;
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
    public MethodFingerprinter spelSignatureProvider() {
        log.debug("spelSignatureProvider Configured");
        return new SpelMethodFingerprinter("sp_e_l_method_fingerprinter");
    }
}
