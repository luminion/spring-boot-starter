package io.github.luminion.velo.core;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.NoSuchMessageException;

/**
 * 解析注解上的提示信息，支持可选的国际化。
 * <p>
 * 约定：当 message 形如 <code>{some.key}</code> 时，按 i18n key 从 {@link MessageSource} 解析；
 * 否则原样返回。这样未配置国际化的项目（message 为普通文本）行为完全不变，向后兼容。
 * <p>
 * 即使是 <code>{key}</code> 形式，若没有任何 {@link MessageSource} 或找不到对应文案，
 * 也会回退为 key 本身的字面文本，不会抛异常。
 *
 * @author luminion
 */
public class VeloMessageResolver implements MessageSourceAware {

    private MessageSource messageSource;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 解析提示信息。
     *
     * @param message 注解上配置的原始信息（普通文本或 {@code {i18n.key}}）
     * @return 解析后的最终文本
     */
    public String resolve(String message) {
        if (message == null) {
            return null;
        }
        String key = extractKey(message);
        if (key == null) {
            // 普通文本，原样返回
            return message;
        }
        if (messageSource == null) {
            // 没有 MessageSource，回退为 key 字面量
            return key;
        }
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException ex) {
            // 找不到文案时回退为 key 字面量，保持健壮
            return key;
        }
    }

    /**
     * 当且仅当 message 形如 {@code {xxx}} 时返回去掉花括号的 key，否则返回 {@code null}。
     */
    private static String extractKey(String message) {
        int length = message.length();
        if (length >= 3 && message.charAt(0) == '{' && message.charAt(length - 1) == '}') {
            String inner = message.substring(1, length - 1).trim();
            if (!inner.isEmpty() && inner.indexOf('{') < 0 && inner.indexOf('}') < 0) {
                return inner;
            }
        }
        return null;
    }
}
