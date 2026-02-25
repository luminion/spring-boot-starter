package io.github.luminion.starter.core.spi.masker;

import io.github.luminion.starter.core.spi.StringMasker;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 通用脱敏处理器（按比例脱敏中间字符）
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class SimpleMasker implements StringMasker {

    @Override
    public String mask(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        int length = s.length();
        if (length == 1) {
            return "*";
        }
        if (length == 2) {
            return s.charAt(0) + "*";
        }

        int prefixSuffixLength = length / 3;
        int middleLength = length - 2 * prefixSuffixLength;

        String prefix = s.substring(0, prefixSuffixLength);
        String suffix = s.substring(length - prefixSuffixLength);

        String asterisks = Stream.generate(() -> "*").limit(middleLength).collect(Collectors.joining());

        return prefix + asterisks + suffix;
    }

}
