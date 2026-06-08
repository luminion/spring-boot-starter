package io.github.luminion.velo.log;

/**
 * Writes unified invocation logs.
 */
public interface InvocationLogWriter {

    void write(InvocationLogRecord record);
}
