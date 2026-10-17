package io.github.luminion.velo.web.exception;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证通过 {@code @Bean} 显式注册的 Advice 能处理 MVC 异常。 */
class VeloAdviceBeanMvcTests {
    @Test
    void explicitlyCreatedBeanHandlesMvcExceptionsWithoutSubclassAnnotation() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(WebConfig.class);
            context.refresh();
            MockMvcBuilders.webAppContextSetup(context)
                    .build()
                    .perform(get("/missing"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("failed:缺少必要参数: name"));
        }
    }

    /** 为 MVC 异常处理测试提供最小 Web 配置。 */
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
        VeloValidationWebExceptionHandler<String> advice() {
            return new VeloValidationWebExceptionHandler<>(
                    message -> "failed:" + message,
                    error -> "error:" + error.getClass().getSimpleName());
        }

        @Bean
        Controller controller() {
            return new Controller();
        }
    }

    /** 提供一个缺少请求参数时触发异常的测试端点。 */
    @RestController
    static class Controller {
        @GetMapping("/missing")
        String missing(@RequestParam("name") String name) {
            return name;
        }
    }
}
