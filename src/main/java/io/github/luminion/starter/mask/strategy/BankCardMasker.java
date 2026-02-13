package io.github.luminion.starter.mask.strategy;

import org.springframework.stereotype.Component;
import java.util.function.Function;

/**
 * 银行卡号脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@Component
public class BankCardMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\w{4})\\w*(\\w{4})", "$1********$2");
    }

}
