package io.github.luminion.velo.cache;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 按缓存名称声明纯 JSON 的目标类型；一个缓存名称对应一种值类型。
 * Class 可直接登记，泛型集合可用 ParameterizedTypeReference 的 getType() 登记。
 * 配置只在构建 CacheManager 时使用，不维护业务对象缓存。
 */
public class RedisCacheTypeMapProvider {

    private final Map<String, Type> cacheTypes;

    public RedisCacheTypeMapProvider(Map<String, ? extends Type> cacheTypes) {
        Assert.notNull(cacheTypes, "Redis 缓存类型映射不能为 null");
        this.cacheTypes = new LinkedHashMap<>();
        cacheTypes.forEach((name, type) -> {
            Assert.hasText(name, "Redis 缓存类型映射的缓存名称不能为空");
            Assert.notNull(type, "Redis 缓存 " + name + " 的目标类型不能为 null");
            Assert.isTrue(!ResolvableType.forType(type).hasUnresolvableGenerics(),
                    "Redis 缓存 " + name + " 必须登记完整泛型，例如 List<Dto>，不能只登记 List.class");
            this.cacheTypes.put(name, type);
        });
    }

    public Map<String, Type> getCacheTypes() {
        return new LinkedHashMap<>(cacheTypes);
    }
}
