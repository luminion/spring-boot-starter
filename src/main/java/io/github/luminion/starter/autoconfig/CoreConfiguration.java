package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.aop.KeyResolver;
import io.github.luminion.starter.core.aop.SpelKeyResolver;
import io.github.luminion.starter.core.xss.XssHandler;
import io.github.luminion.starter.core.xss.support.JsoupXssHandler;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    public KeyResolver spelMethodFingerprinter() {
        log.debug("SpelKeyResolver Configured");
        return new SpelKeyResolver("spelFingerprinter");
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Jsoup.class)
    public XssHandler xssCleaner(Prop prop) {
        log.debug("JsoupXssHandler Configured with strategy: {}", prop.getXssStrategy());
        return new JsoupXssHandler(prop.getXssStrategy());
    }
}
