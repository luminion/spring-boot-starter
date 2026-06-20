package io.github.luminion.velo.log;

/**
 * Writes unified invocation logs.
 * <p>
 * The default implementation ({@link io.github.luminion.velo.log.support.Slf4JInvocationLogWriter})
 * writes logs synchronously. This is a deliberate design choice:
 * <ul>
 *   <li>The dominant cost is JSON serialization of args/result, which occurs in the
 *       aspect before {@code write()} is called. Making {@code write()} itself async
 *       would only offload the relatively cheap {@code Logger.debug/info} call.</li>
 *   <li>Serializing object references asynchronously is unsafe: the business thread may
 *       mutate or garbage-collect the objects before the async thread reads them.</li>
 *   <li>Logback's built-in {@code AsyncAppender} already provides async log writing at
 *       the framework level when needed.</li>
 * </ul>
 * For high-throughput or large-payload endpoints, prefer {@code @LogPayloadIgnore} or set
 * {@code velo.log.invocation.include-args=false} / {@code include-result=false}.
 */
public interface InvocationLogWriter {

    void write(InvocationLogRecord record);
}
