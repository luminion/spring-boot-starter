package io.github.luminion.starter.support.jackson.support;

import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
public class BankCardNoEncoder implements Function<String, String> {

    @Override
    public String apply(String s) {
        return s.replaceAll("(\\w{4})\\w*(\\w{4})", "$1********$2");
    }

}