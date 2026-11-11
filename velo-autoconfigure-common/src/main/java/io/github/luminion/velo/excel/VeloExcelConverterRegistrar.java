package io.github.luminion.velo.excel;

import io.github.luminion.velo.VeloProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Excel 扩展转换器注册器。
 */
public class VeloExcelConverterRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanClassLoaderAware {
    private static final Logger log = LoggerFactory.getLogger(VeloExcelConverterRegistrar.class);

    /*
     * 三个 Excel 库公开的 registerConverter 只能绑定当前 Reader/Writer Builder，
     * 无法通过自动配置覆盖业务代码创建的所有 Builder。Starter 需要让用户配置的
     * Date、LocalDate、LocalTime、LocalDateTime 格式自动生效，因此仍采用各库的
     * DefaultConverterLoader 做全局注册；具体反射桥接和版本边界见各 Helper。
     */
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
        if (excelProperties == null || excelProperties.getConverters() == null
                || !excelProperties.getConverters().isEnabled()) {
            return;
        }

        registerIfPresent(EASY_EXCEL_LOADER, () -> EasyExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters()), converters -> EasyExcelHelper.registerConverters(converters));

        registerIfPresent(FAST_EXCEL_LOADER, () -> FastExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters()), converters -> FastExcelHelper.registerConverters(converters));

        registerIfPresent(FESOD_EXCEL_LOADER, () -> FesodExcelHelper.createExtraConverters(
                properties.getDateTimeFormat().getDate(),
                properties.getDateTimeFormat().getTime(),
                properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone(),
                excelProperties.getConverters()), converters -> FesodExcelHelper.registerConverters(converters));
    }

    private <T> void registerIfPresent(String className, Supplier<T> converters, Consumer<T> registration) {
        if (beanClassLoader == null || !ClassUtils.isPresent(className, beanClassLoader)) {
            return;
        }
        // 转换器创建失败（如日期格式非法）属于配置错误，照常在启动期报错。
        T created = converters.get();
        try {
            registration.accept(created);
        } catch (Exception | LinkageError error) {
            // 转换器通过反射调用 Excel 库的内部方法注册；库版本变化导致注册失败时只影响扩展转换器，不阻断启动。
            log.warn("[Velo Starter] Excel 扩展转换器未注册：{} 的注册方法不可用，日期等格式将使用 Excel 库默认转换。原因：{}",
                    className, error.toString());
        }
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
