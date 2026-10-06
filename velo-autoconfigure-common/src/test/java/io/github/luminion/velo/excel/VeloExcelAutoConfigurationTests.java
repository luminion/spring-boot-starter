package io.github.luminion.velo.excel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloExcelAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloExcelAutoConfiguration.class));

    @Test
    void basicCellTypesShouldFollowVeloJacksonDefaults() {
        assertThat(new EasyExcelHelper.BooleanConverter().convertToExcelData(true, null, null).getType())
                .isEqualTo(com.alibaba.excel.enums.CellDataTypeEnum.BOOLEAN);
        assertThat(new FastExcelHelper.BooleanConverter().convertToExcelData(true, null, null).getType())
                .isEqualTo(cn.idev.excel.enums.CellDataTypeEnum.BOOLEAN);
        assertThat(new FesodExcelHelper.BooleanConverter().convertToExcelData(true, null, null).getType())
                .isEqualTo(org.apache.fesod.sheet.enums.CellDataTypeEnum.BOOLEAN);
        assertThat(new EasyExcelHelper.FloatConverter().convertToExcelData(0.25F, null, null).getType())
                .isEqualTo(com.alibaba.excel.enums.CellDataTypeEnum.NUMBER);
        assertThat(new FastExcelHelper.DoubleConverter().convertToExcelData(0.125D, null, null).getType())
                .isEqualTo(cn.idev.excel.enums.CellDataTypeEnum.NUMBER);
        assertThat(new FesodExcelHelper.DoubleConverter().convertToExcelData(0.125D, null, null).getType())
                .isEqualTo(org.apache.fesod.sheet.enums.CellDataTypeEnum.NUMBER);
    }

    @Test
    void integerConvertersShouldAlwaysExportTextLikeVeloJacksonDefaults() {
        for (long value : new long[]{1, -1, 1234567890123456L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertThat(new EasyExcelHelper.LongConverter().convertToExcelData(value, null, null).getStringValue())
                    .isEqualTo(Long.toString(value));
            assertThat(new FastExcelHelper.LongConverter().convertToExcelData(value, null, null).getStringValue())
                    .isEqualTo(Long.toString(value));
            assertThat(new FesodExcelHelper.LongConverter().convertToExcelData(value, null, null).getStringValue())
                    .isEqualTo(Long.toString(value));
            java.math.BigInteger integer = java.math.BigInteger.valueOf(value);
            assertThat(new EasyExcelHelper.BigIntergerConverter().convertToExcelData(integer, null, null).getStringValue())
                    .isEqualTo(integer.toString());
            assertThat(new FastExcelHelper.BigIntergerConverter().convertToExcelData(integer, null, null).getStringValue())
                    .isEqualTo(integer.toString());
            assertThat(new FesodExcelHelper.BigIntergerConverter().convertToExcelData(integer, null, null).getStringValue())
                    .isEqualTo(integer.toString());
        }
    }

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
    void extraConverterListsShouldAllowCustomConverters() {
        List<com.alibaba.excel.converters.Converter<?>> easyConverters = EasyExcelHelper.createExtraConverters();
        int easySize = easyConverters.size();
        easyConverters.add(easyConverters.get(0));
        assertThat(easyConverters).hasSize(easySize + 1);

        List<cn.idev.excel.converters.Converter<?>> fastConverters = FastExcelHelper.createExtraConverters();
        int fastSize = fastConverters.size();
        fastConverters.add(fastConverters.get(0));
        assertThat(fastConverters).hasSize(fastSize + 1);

        List<org.apache.fesod.sheet.converters.Converter<?>> fesodConverters = FesodExcelHelper.createExtraConverters();
        int fesodSize = fesodConverters.size();
        fesodConverters.add(fesodConverters.get(0));
        assertThat(fesodConverters).hasSize(fesodSize + 1);
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
