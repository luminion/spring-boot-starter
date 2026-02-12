package io.github.luminion.starter.support.jackson.support;

import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
public class NameEncoder implements Function<String, String> {
    
    @Override
    public String apply(String s) {
        return s.replaceAll("(\\S)\\S(\\S*)", "$1*$2");
    }

}
