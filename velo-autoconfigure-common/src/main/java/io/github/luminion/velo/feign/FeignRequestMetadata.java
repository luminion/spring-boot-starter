package io.github.luminion.velo.feign;

/**
 * Feign 请求元数据。
 */
final class FeignRequestMetadata {

    /** Feign 请求使用的 HTTP 方法。 */
    private final String httpMethod;

    /** Feign 请求映射的路径。 */
    private final String path;

    FeignRequestMetadata(String httpMethod, String path) {
        this.httpMethod = httpMethod;
        this.path = path;
    }

    String getHttpMethod() {
        return httpMethod;
    }

    String getPath() {
        return path;
    }
}
