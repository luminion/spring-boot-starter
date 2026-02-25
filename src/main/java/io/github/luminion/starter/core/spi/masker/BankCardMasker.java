package io.github.luminion.starter.core.spi.masker;

import io.github.luminion.starter.core.spi.StringMasker;
import org.springframework.stereotype.Component;

/**
 * 银行卡号脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class BankCardMasker implements StringMasker {

    @Override
    public String mask(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\w{4})\\w*(\\w{4})", "$1********$2");
    }

}
