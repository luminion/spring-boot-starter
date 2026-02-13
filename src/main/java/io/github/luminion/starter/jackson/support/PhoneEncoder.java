package io.github.luminion.starter.jackson.support;

import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
public class PhoneEncoder implements Function<String, String> {

    @Override
    public String apply(String s) {
        return s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

}