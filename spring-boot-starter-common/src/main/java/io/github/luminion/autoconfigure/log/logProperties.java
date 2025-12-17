package io.github.luminion.autoconfigure.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luminion
 */
@Data
@ConfigurationProperties("turbo.log")
public class logProperties {
    private final Email email = new Email();
    @Data
    public static class Email {
        private String[] to;
    }
    
}
