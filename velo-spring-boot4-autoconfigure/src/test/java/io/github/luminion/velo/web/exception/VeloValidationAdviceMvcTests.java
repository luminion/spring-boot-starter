package io.github.luminion.velo.web.exception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证真实 MVC 校验、显式 Bean 注册，以及返回值失败的分类。 */
class VeloValidationAdviceMvcTests {
    @Test
    void plainBeanUsesValidationMessageForInvalidInput() throws Exception {
        try (AnnotationConfigWebApplicationContext context = context(LegacyAdvice.class)) {
            mvc(context).perform(get("/input").param("name", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failed:名称不能为空"));
            mvc(context).perform(get("/missing"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failed:缺少必要参数: name"));
        }
    }

    @Test
    void returnValueValidationUsesErrorConverter() throws Exception {
        try (AnnotationConfigWebApplicationContext context = context(LegacyAdvice.class)) {
            mvc(context).perform(get("/output"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("error:HandlerMethodValidationException"));
        }
    }

    @Test
    void statusAdvicePreserves400ForInputAnd500ForOutput() throws Exception {
        try (AnnotationConfigWebApplicationContext context = context(StatusAdvice.class)) {
            mvc(context).perform(get("/input").param("name", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("failed:名称不能为空"));
            mvc(context).perform(get("/output"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("error:HandlerMethodValidationException"));
        }
    }

    private AnnotationConfigWebApplicationContext context(Class<?> advice) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(WebConfig.class, advice);
        context.refresh();
        return context;
    }

    private MockMvc mvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebConfig implements WebMvcConfigurer {
        @Override
        public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
            for (HttpMessageConverter<?> converter : converters) {
                if (converter instanceof StringHttpMessageConverter) {
                    ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
                }
            }
        }

        @Bean
        Controller controller() {
            return new Controller();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LegacyAdvice {
        @Bean
        VeloValidationWebExceptionHandler<String> advice() {
            return new VeloValidationWebExceptionHandler<>(message -> "failed:" + message,
                    error -> "error:" + error.getClass().getSimpleName());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StatusAdvice {
        @Bean
        VeloStatusWebExceptionHandler<String> advice() {
            return new VeloStatusWebExceptionHandler<>(message -> "failed:" + message,
                    error -> "error:" + error.getClass().getSimpleName());
        }
    }

    @RestController
    static class Controller {
        @GetMapping("/input")
        String input(@RequestParam("name") @NotBlank(message = "名称不能为空") String name) {
            return name;
        }

        @GetMapping("/missing")
        String missing(@RequestParam("name") String name) {
            return name;
        }

        @GetMapping("/output")
        @NotNull(message = "返回值不能为空")
        String output() {
            return null;
        }
    }
}

