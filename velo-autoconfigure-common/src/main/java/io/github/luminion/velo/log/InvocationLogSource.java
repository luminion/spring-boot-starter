package io.github.luminion.velo.log;

/**
 * Invocation log source.
 */
public enum InvocationLogSource {

    CONTROLLER("controller"),

    FEIGN("feign"),

    INVOKE("invoke");

    private final String value;

    InvocationLogSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
