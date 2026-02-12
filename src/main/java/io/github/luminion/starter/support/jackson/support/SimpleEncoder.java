package io.github.luminion.starter.support.jackson.support;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author luminion
 * @since 1.0.0
 */
public class SimpleEncoder implements Function<String, String> {
    
    @Override
    public String apply(String s) {
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
