package io.github.luminion.velo.autoconfigure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 Spring Boot 3 真实 MVC 上下文中的参数格式化和跨域配置。
 *
 * @author luminion
 */
class Boot3MockMvcIntegrationTest {

    @Test
    void shouldBindDateTimeFormatsAndCorsInRealMvcContext() throws Exception {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setRegisterShutdownHook(false);

        ConfigurableApplicationContext context = application.run(
                "--spring.main.banner-mode=off",
                "--server.port=0",
                "--velo.banner.enabled=false",
                "--velo.web.cors.enabled=true",
                "--velo.web.cors.allowed-origin-patterns=https://client.example",
                "--velo.web.cors.allow-credentials=false"
        );
        try {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();

            mockMvc.perform(get("/integration/date-time")
                            .param("date", "2024-01-02 03:04:05")
                            .param("dateOnly", "2024-01-02")
                            .param("formattedDate", "2024/01/02")
                            .param("localDate", "2024-01-02")
                            .param("localTime", "03:04:05")
                            .param("localDateTime", "2024-01-02 03:04:05")
                            .header("Origin", "https://client.example"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("ok"))
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://client.example"));

            mockMvc.perform(get("/integration/date-time")
                            .param("date", "2024-01-02 03:04:05")
                            .param("dateOnly", "2024-01-02")
                            .param("formattedDate", "2024/01/02")
                            .param("localDate", "2026-02-30")
                            .param("localTime", "03:04:05")
                            .param("localDateTime", "2024-01-02 03:04:05"))
                    .andExpect(status().isBadRequest());
        } finally {
            context.close();
        }
    }

    /** 注册 MVC 集成验证所需的最小 Spring Boot 应用。 */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV2")
    @Import(TestController.class)
    static class TestApplication {
    }

    /** 暴露多种日期时间参数，供 MVC 转换和跨域行为验证使用。 */
    @RestController
    static class TestController {

        /**
         * 接收日期时间参数并返回固定响应。
         *
         * @param date 日期时间参数
         * @param dateOnly 日期参数
         * @param formattedDate 使用自定义格式的日期参数
         * @param localDate 本地日期参数
         * @param localTime 本地时间参数
         * @param localDateTime 本地日期时间参数
         * @return 固定的成功响应
         */
        @GetMapping("/integration/date-time")
        String dateTime(@RequestParam("date") Date date,
                        @RequestParam("dateOnly") Date dateOnly,
                        @RequestParam("formattedDate") @DateTimeFormat(pattern = "yyyy/MM/dd") Date formattedDate,
                        @RequestParam("localDate") LocalDate localDate,
                        @RequestParam("localTime") LocalTime localTime,
                        @RequestParam("localDateTime") LocalDateTime localDateTime) {
            return "ok";
        }
    }
}
