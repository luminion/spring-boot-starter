package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.core.fingerprint.SpelMethodFingerprinter;
import io.github.luminion.starter.mask.strategy.*;
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
    public MethodFingerprinter spelMethodFingerprinter() {
        return new SpelMethodFingerprinter();
    }

}
