package io.github.luminion.velo.excel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Excel 扩展转换器自动配置。
 */
@AutoConfiguration
@Import(VeloExcelConverterRegistrar.class)
public class VeloExcelAutoConfiguration {
}
