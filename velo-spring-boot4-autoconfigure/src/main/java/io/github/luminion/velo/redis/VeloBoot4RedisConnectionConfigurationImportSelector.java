package io.github.luminion.velo.redis;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 提前导入 Spring Boot 4 的 Redis 连接工厂配置，使 Velo 模板配置能够在官方模板配置之前看到连接工厂。
 *
 * @author luminion
 * @since 1.3.1
 */
public class VeloBoot4RedisConnectionConfigurationImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        String[] candidates = new String[]{
                "org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration"
        };
        List<String> imports = new ArrayList<>(candidates.length);
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        for (String candidate : candidates) {
            if (ClassUtils.isPresent(candidate, classLoader)) {
                imports.add(candidate);
            }
        }
        return imports.toArray(new String[0]);
    }
}
