package io.github.luminion.velo.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 Spring Boot 2 真实 MVC 上下文中的参数格式化和跨域配置。
 *
 * @author luminion
 */
class Boot2MockMvcIntegrationTest {

    @Test
    void shouldBindDateTimeFormatsAndCorsInRealMvcContext() throws Exception {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setRegisterShutdownHook(false);

        ConfigurableApplicationContext context = application.run(
                "--spring.main.banner-mode=off",
                "--server.port=18082",
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
        } finally {
            context.close();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestController.class)
    static class TestApplication {
    }

    @RestController
    static class TestController {

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
