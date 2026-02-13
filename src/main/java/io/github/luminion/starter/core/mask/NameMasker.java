package io.github.luminion.starter.core.mask;

import org.springframework.stereotype.Component;
import java.util.function.Function;

/**
 * 姓名脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class NameMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\S)\\S(\\S*)", "$1*$2");
    }

}
