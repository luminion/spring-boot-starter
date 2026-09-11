package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloWebFluxConfigurerTests {

    private static ObjectProvider<XssStringConverter> emptyConverterProvider() {
        return new StaticListableBeanFactory().getBeanProvider(XssStringConverter.class);
    }

    @Test
    void shouldNotRegisterCorsMappingsByDefault() {
        VeloProperties properties = new VeloProperties();
        ExposingCorsRegistry registry = new ExposingCorsRegistry();

        new VeloWebFluxConfigurer(emptyConverterProvider(), properties).addCorsMappings(registry);

        assertThat(registry.getConfigurations()).isEmpty();
    }

    @Test
    void shouldRegisterCorsMappingsWhenEnabled() {
        VeloProperties properties = new VeloProperties();
        properties.getWeb().getCors().setEnabled(true);
        properties.getWeb().getCors().setAllowedOriginPatterns(new String[] {"https://client.example"});
        properties.getWeb().getCors().setAllowCredentials(true);
        ExposingCorsRegistry registry = new ExposingCorsRegistry();

        new VeloWebFluxConfigurer(emptyConverterProvider(), properties).addCorsMappings(registry);

        CorsConfiguration configuration = registry.getConfigurations().get("/**");
        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns()).containsExactly("https://client.example");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void shouldRegisterDateFormattersWhenEnabled() {
        VeloProperties properties = new VeloProperties();
        properties.getSpringConverter().setDateTimeEnabled(true);
        properties.getDateTimeFormat().setDate("yyyy|MM|dd");
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebFluxConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThat(conversionService.convert("2024|03|31", java.time.LocalDate.class))
                .isEqualTo(java.time.LocalDate.of(2024, 3, 31));
    }

    @Test
    void shouldSkipDateFormattersWhenDisabled() {
        VeloProperties properties = new VeloProperties();
        properties.getSpringConverter().setDateTimeEnabled(false);
        properties.getDateTimeFormat().setDate("yyyy|MM|dd");
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebFluxConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThatThrownBy(() -> conversionService.convert("2024|03|31", java.time.LocalDate.class))
                .isInstanceOf(ConversionFailedException.class);
    }

    @Test
    void shouldRegisterXssConverterOnlyWhenXssIsEnabled() {
        XssStringConverter converter = new XssStringConverter(value -> value.replace("<", ""));
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("xssStringConverter", converter);
        VeloProperties properties = new VeloProperties();
        properties.getWeb().getXss().setEnabled(true);
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebFluxConfigurer(beanFactory.getBeanProvider(XssStringConverter.class), properties)
                .addFormatters(conversionService);

        assertThat(conversionService.convert("<script>", String.class)).isEqualTo("script>");
    }

    private static final class ExposingCorsRegistry extends CorsRegistry {

        Map<String, CorsConfiguration> getConfigurations() {
            return getCorsConfigurations();
        }
    }
}
