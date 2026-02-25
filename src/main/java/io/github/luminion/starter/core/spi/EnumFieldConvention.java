package io.github.luminion.starter.core.spi;

import java.util.List;

/**
 * 枚举字段转化约定提供者
 * <p>
 * 用户可以通过实现此接口并注入 Spring 容器来覆盖默认约定。
 *
 * @author luminion
 */
public interface EnumFieldConvention {

    /**
     * 代码字段名
     */
    List<String> codeFieldNames();
    
    
    /**
     * 描述字段名
     */
    List<String> descFieldNames();

}
