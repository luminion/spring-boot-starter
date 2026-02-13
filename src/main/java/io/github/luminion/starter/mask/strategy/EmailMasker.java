package io.github.luminion.starter.mask.strategy;

import org.springframework.stereotype.Component;
import java.util.function.Function;

/**
 * 邮箱脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class EmailMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\w?)(\\w+)(@\\w+\\.[a-z]+(\\.[a-z]+)?)", "$1****$3");
    }

}
