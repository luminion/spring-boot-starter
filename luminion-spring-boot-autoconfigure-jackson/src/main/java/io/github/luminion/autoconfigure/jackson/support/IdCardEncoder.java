package io.github.luminion.autoconfigure.jackson.support;

import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
public class IdCardEncoder implements Function<String, String> {

    @Override
    public String apply(String s) {
        return s.replaceAll("(\\d{6})\\d{9}(\\w{3})", "$1*********$2");
    }

}