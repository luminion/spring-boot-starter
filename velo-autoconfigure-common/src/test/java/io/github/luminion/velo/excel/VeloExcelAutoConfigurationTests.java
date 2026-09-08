package io.github.luminion.velo.excel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloExcelAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloExcelAutoConfiguration.class));

    @Test
    void shouldStartWhenExcelAutoRegistrationIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "velo.date-time-format.date=dd/MM/yyyy",
                        "velo.date-time-format.time=HH:mm",
                        "velo.date-time-format.date-time=dd/MM/yyyy HH:mm",
                        "velo.date-time-format.time-zone=UTC",
                        "velo.excel.converters.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void shouldSkipExcelRegistrationWhenExcelLibrariesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        "com.alibaba.excel",
                        "cn.idev.excel",
                        "org.apache.fesod.sheet"))
                .withPropertyValues("velo.excel.converters.enabled=true")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void booleanConvertersShouldDeclareStringCellType() {
        assertThat(new EasyExcelHelper.BooleanConverter().supportExcelTypeKey())
                .isEqualTo(com.alibaba.excel.enums.CellDataTypeEnum.STRING);
        assertThat(new FastExcelHelper.BooleanConverter().supportExcelTypeKey())
                .isEqualTo(cn.idev.excel.enums.CellDataTypeEnum.STRING);
        assertThat(new FesodExcelHelper.BooleanConverter().supportExcelTypeKey())
                .isEqualTo(org.apache.fesod.sheet.enums.CellDataTypeEnum.STRING);
    }

    @Test
    void easyExcelDateConverterShouldMatchSpringEmptyStringBehavior() {
        EasyExcelHelper.DateConverter converter = new EasyExcelHelper.DateConverter(
                "yyyy-MM-dd HH:mm:ss", "UTC");

        assertThat(converter.convertToJavaData(
                new com.alibaba.excel.metadata.data.ReadCellData<String>(
                        com.alibaba.excel.enums.CellDataTypeEnum.STRING, ""), null, null)).isNull();
        assertThatThrownBy(() -> converter.convertToJavaData(
                new com.alibaba.excel.metadata.data.ReadCellData<String>(
                        com.alibaba.excel.enums.CellDataTypeEnum.STRING, "   "), null, null))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void fastExcelDateConverterShouldMatchSpringEmptyStringBehavior() {
        FastExcelHelper.DateConverter converter = new FastExcelHelper.DateConverter(
                "yyyy-MM-dd HH:mm:ss", "UTC");

        assertThat(converter.convertToJavaData(
                new cn.idev.excel.metadata.data.ReadCellData<String>(
                        cn.idev.excel.enums.CellDataTypeEnum.STRING, ""), null, null)).isNull();
        assertThatThrownBy(() -> converter.convertToJavaData(
                new cn.idev.excel.metadata.data.ReadCellData<String>(
                        cn.idev.excel.enums.CellDataTypeEnum.STRING, "   "), null, null))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void fesodDateConverterShouldMatchSpringEmptyStringBehavior() {
        FesodExcelHelper.DateConverter converter = new FesodExcelHelper.DateConverter(
                "yyyy-MM-dd HH:mm:ss", "UTC");

        assertThat(converter.convertToJavaData(
                new org.apache.fesod.sheet.metadata.data.ReadCellData<String>(
                        org.apache.fesod.sheet.enums.CellDataTypeEnum.STRING, ""), null, null)).isNull();
        assertThatThrownBy(() -> converter.convertToJavaData(
                new org.apache.fesod.sheet.metadata.data.ReadCellData<String>(
                        org.apache.fesod.sheet.enums.CellDataTypeEnum.STRING, "   "), null, null))
                .isInstanceOf(DateTimeParseException.class);
    }
}
