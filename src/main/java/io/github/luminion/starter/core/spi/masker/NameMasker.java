package io.github.luminion.starter.core.spi.masker;

import io.github.luminion.starter.core.spi.StringMasker;
import org.springframework.stereotype.Component;
import java.util.function.Function;

/**
 * 姓名脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class NameMasker implements StringMasker {

    @Override
    public String mask(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\S)\\S(\\S*)", "$1*$2");
    }

}
