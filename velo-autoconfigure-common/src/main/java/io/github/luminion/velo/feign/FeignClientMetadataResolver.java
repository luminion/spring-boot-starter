package io.github.luminion.velo.feign;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Feign 元数据解析器。
 */
final class FeignClientMetadataResolver {

    private static final String FEIGN_CLIENT_ANNOTATION = "org.springframework.cloud.openfeign.FeignClient";

    private FeignClientMetadataResolver() {
    }

    static boolean isFeignClientType(Class<?> beanType) {
        return findFeignClientAnnotation(beanType) != null;
    }

    static String resolveClientAddress(Class<?> beanType) {
        Annotation annotation = findFeignClientAnnotation(beanType);
        if (annotation == null) {
            return beanType.getSimpleName();
        }
        String value = readStringAttribute(annotation, "value");
        String name = readStringAttribute(annotation, "name");
        String contextId = readStringAttribute(annotation, "contextId");
        String clientValue = StringUtils.hasText(value) ? value : name;

        if (StringUtils.hasText(clientValue) && StringUtils.hasText(contextId) && !clientValue.equals(contextId)) {
            return clientValue + "(" + contextId + ")";
        }
        if (StringUtils.hasText(clientValue)) {
            return clientValue;
        }
        if (StringUtils.hasText(contextId)) {
            return contextId;
        }
        return beanType.getSimpleName();
    }

    static FeignRequestMetadata resolveRequestMetadata(Method method) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RequestMapping.class);
        RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        String path = joinPath(extractFirstPath(classMapping), extractFirstPath(methodMapping));
        String httpMethod = extractHttpMethod(method);
        return new FeignRequestMetadata(httpMethod, path);
    }

    private static String extractHttpMethod(Method method) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping != null && requestMapping.method().length > 0) {
            return requestMapping.method()[0].name();
        }
        return "";
    }

    private static String extractFirstPath(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return "";
        }
        if (requestMapping.path().length > 0) {
            return requestMapping.path()[0];
        }
        if (requestMapping.value().length > 0) {
            return requestMapping.value()[0];
        }
        return "";
    }

    private static String joinPath(String classPath, String methodPath) {
        if (!StringUtils.hasText(classPath) && !StringUtils.hasText(methodPath)) {
            return "";
        }
        String normalizedClassPath = normalizePath(classPath);
        String normalizedMethodPath = normalizePath(methodPath);
        if (!StringUtils.hasText(normalizedClassPath)) {
            return normalizedMethodPath;
        }
        if (!StringUtils.hasText(normalizedMethodPath)) {
            return normalizedClassPath;
        }
        if ("/".equals(normalizedClassPath)) {
            return normalizedMethodPath;
        }
        if ("/".equals(normalizedMethodPath)) {
            return normalizedClassPath;
        }
        return normalizedClassPath + normalizedMethodPath;
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Annotation findFeignClientAnnotation(Class<?> beanType) {
        Class<?> userClass = ClassUtils.getUserClass(beanType);
        Annotation annotation = findFeignClientAnnotationOnType(userClass);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> interfaceType : userClass.getInterfaces()) {
            annotation = findFeignClientAnnotationOnType(interfaceType);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private static Annotation findFeignClientAnnotationOnType(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if (FEIGN_CLIENT_ANNOTATION.equals(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    private static String readStringAttribute(Annotation annotation, String attributeName) {
        try {
            Object value = annotation.annotationType().getMethod(attributeName).invoke(annotation);
            return value instanceof String ? (String) value : "";
        } catch (Exception ex) {
            return "";
        }
    }
}
