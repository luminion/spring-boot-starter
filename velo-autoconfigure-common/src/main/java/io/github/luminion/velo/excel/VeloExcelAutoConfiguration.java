package io.github.luminion.velo.excel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Excel 扩展转换器自动配置。
 */
@AutoConfiguration
@Import(VeloExcelConverterRegistrar.class)
@ConditionalOnProperty(prefix = "velo.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloExcelAutoConfiguration {
}
