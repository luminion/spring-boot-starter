package io.github.luminion.autoconfigure.jackson.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * <a href="https://www.jianggujin.com/post/6">参考</a>
 * @author luminion
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SensitiveStrategy {

    /**
     * 姓名
     */
    CHINESE_NAME(s -> s.replaceAll("(\\S)\\S(\\S*)", "$1*$2")),
    /**
     * 身份证号
     */
    ID_CARD(s -> s.replaceAll("(\\d{6})\\d{9}(\\w{3})", "$1*********$2")),
    /**
     * 手机号
     */
    PHONE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),
    /**
     * 邮箱
     */
    EMAIL(s -> s.replaceAll("(\\w{2})\\w*(@[\\w|\\.]*)", "$1****$2")),
    /**
     * 银行卡
     */
    BANK_CARD_NO(s -> s.replaceAll("(\\w{4})\\w*(\\w{4})", "$1********$2")),

    /**
     * 自定义
     */
    CUSTOM(s -> s);

    private final Function<String, String> desensitizer;
    
}