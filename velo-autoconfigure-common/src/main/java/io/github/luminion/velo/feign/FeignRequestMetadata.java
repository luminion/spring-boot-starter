package io.github.luminion.velo.feign;

/**
 * Feign 请求元数据。
 */
final class FeignRequestMetadata {

    private final String httpMethod;

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
