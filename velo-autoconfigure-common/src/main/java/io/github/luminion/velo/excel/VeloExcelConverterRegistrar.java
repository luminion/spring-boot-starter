package io.github.luminion.velo.excel;

import io.github.luminion.velo.core.VeloProperties;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * Excel 扩展转换器注册器。
 */
public class VeloExcelConverterRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanClassLoaderAware {

    private static final String EASY_EXCEL_LOADER = "com.alibaba.excel.converters.DefaultConverterLoader";
    private static final String FAST_EXCEL_LOADER = "cn.idev.excel.converters.DefaultConverterLoader";
    private static final String FESOD_EXCEL_LOADER = "org.apache.fesod.sheet.converters.DefaultConverterLoader";

    private Environment environment;

    private ClassLoader beanClassLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
            org.springframework.beans.factory.support.BeanDefinitionRegistry registry) {
        VeloProperties properties = Binder.get(environment)
                .bind("velo", VeloProperties.class)
                .orElseGet(VeloProperties::new);
        VeloProperties.ExcelProperties excelProperties = properties.getExcel();
        if (!excelProperties.isEnabled() || !excelProperties.getConverters().isEnabled()) {
            return;
        }

        registerIfPresent(EASY_EXCEL_LOADER, () -> EasyExcelHelper.registerConverters(EasyExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters())));

        registerIfPresent(FAST_EXCEL_LOADER, () -> FastExcelHelper.registerConverters(FastExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters())));

        registerIfPresent(FESOD_EXCEL_LOADER, () -> FesodExcelHelper.registerConverters(FesodExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters())));
    }

    private void registerIfPresent(String className, Runnable action) {
        if (beanClassLoader == null || !ClassUtils.isPresent(className, beanClassLoader)) {
            return;
        }
        action.run();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
}
