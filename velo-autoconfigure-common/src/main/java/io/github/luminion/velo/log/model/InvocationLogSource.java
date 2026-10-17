package io.github.luminion.velo.log.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 调用日志来源及输出标识。
 */
@Getter
@RequiredArgsConstructor
public enum InvocationLogSource {
    CONTROLLER("controller"),
    FEIGN("feign"),
    ANNOTATION("annotation"),
    XXL_JOB("xxl-job"),
    SCHEDULED("scheduled");

    /**
     * 日志前缀使用的入口标识。
     */
    private final String value;
}
