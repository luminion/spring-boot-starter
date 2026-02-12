package io.github.luminion.starter.support.jackson.support;

import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
public class EmailEncoder implements Function<String, String> {

    @Override
    public String apply(String s) {
        return s.replaceAll("(\\w{2})\\w*(@[\\w|\\.]*)", "$1****$2");
    }

}