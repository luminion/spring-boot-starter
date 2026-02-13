package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.core.fingerprint.SpelMethodFingerprinter;
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
public class CoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodFingerprinter spelMethodFingerprinter() {
        return new SpelMethodFingerprinter();
    }

}
