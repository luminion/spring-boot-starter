package io.github.luminion.velo.core.spi;

/**
 * Serializes runtime values with the application's JSON infrastructure.
 */
@FunctionalInterface
public interface RuntimeJsonSerializer {

    /**
     * Serializes a value to a single-line JSON fragment.
     *
     * @param value value to serialize
     * @return JSON text or a safe JSON fallback
     */
    String toJson(Object value);
}
