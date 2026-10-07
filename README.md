# Velo Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.luminion/velo-spring-boot3-starter)](https://central.sonatype.com/artifact/io.github.luminion/velo-spring-boot3-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/luminion/spring-boot-starter?style=social)](https://github.com/luminion/spring-boot-starter)

Velo Spring Boot Starter 是一组低侵入的 Spring Boot 自动配置扩展。
项目通过 `velo.*` 管理扩展能力，原生缓存通用设置沿用 `spring.cache.*`，围绕并发控制、缓存、Jackson、Redis、MyBatis-Plus、Excel、日志、XSS 以及 Web MVC 常用增强提供开箱能力。


首次接入可先查看 [版本与兼容性](#版本与兼容性) 和 [Maven 依赖](#maven-依赖)。

## 功能特性

- 统一的 `velo.*` 配置模型，集中管理各类自动配置开关
- 提供 `@Idempotent`、`@RateLimit`、`@Lock` 三类并发控制能力
- 按功能适配 Redis / Redisson / Guava / Caffeine / JDK，并在启动时按依赖和 Bean 条件自动选择
- 复用 Spring Cache + Redis Cache 通用配置，补充分缓存 TTL、单冒号键格式和 TTL 抖动
- 提供 Jackson 日期时间、超大整数、枚举派生字段、字符串转换增强
- 提供 MyBatis-Plus 分页、乐观锁、防全表更新拦截器自动注册
- 提供 RedisTemplate 序列化风格统一能力
- 提供 Excel 扩展 converter 自动注册和 helper 工具类
- 提供注解日志、Controller 请求日志、XSS 清洗和 Web MVC 日期绑定增强
- 提供可选启动横幅，展示各能力开关及实际后端状态

---

## 总览

Velo 默认使用 `velo.opinionated=true`，目标是引入 starter 后直接获得常用增强能力。

如果你希望 starter 尽量不自动改变全局行为，可以设置为无侵入模式：

```yaml
velo:
  opinionated: false
```

开关说明：

- `velo.opinionated=true`：默认开箱即用，启用偏全局增强的默认行为
- `velo.opinionated=false`：无侵入模式，关闭容易自动影响应用行为的默认项，但 `@Idempotent`、`@RateLimit`、`@Lock`、`@InvokeLog` 等显式注解仍可用
- `velo.opinionated=false` 只提供低优先级默认值，业务项目显式配置的属性优先级更高
- 无侵入模式下重新打开某类能力时，需要显式设置对应的 `enabled` 项，例如 `velo.jackson.enabled=true`
- 设置为 `false` 后，starter 会在启动日志中输出一条 INFO，列出被默认关闭的能力，便于排查“为什么某全局增强没生效”
- 旧配置 `velo.mode=OPINIONATED/CONSERVATIVE` 已移除，不再生效；请迁移为 `velo.opinionated=true/false`

配置优先级（从高到低）：

| 优先级 | 来源 | 示例 |
| --- | --- | --- |
| 1 最高 | 命令行参数 | `--velo.trace.enabled=true` |
| 2 | Java 系统属性 | `-Dvelo.trace.enabled=true` |
| 3 | 环境变量 | `VELO_TRACE_ENABLED=true` |
| 4 | application.yml / properties | `velo.trace.enabled: true` |
| 5 最低 | `velo.opinionated` 默认值 | `true` / `false` 注入的默认值 |

Spring Boot 还支持 `SPRING_APPLICATION_JSON`、测试属性等特殊配置源；上表列出本 starter 最常用的来源。也就是说 `velo.opinionated=false` 注入的只是**最低优先级默认值**，业务项目任何显式配置都会覆盖它。

例如无侵入模式下重新打开 traceId：

```yaml
velo:
  opinionated: false
  trace:
    enabled: true
```

默认会自动影响全局行为的能力：

| 能力 | `velo.opinionated=true` | `velo.opinionated=false` | 说明 |
| --- | --- | --- | --- |
| traceId / MDC / 日志 pattern | 开启 | 关闭 | 影响用户自己的日志输出 |
| 文本日志默认日期格式 | `yyyy-MM-dd HH:mm:ss.SSS` | 保持 Spring Boot 默认 | 用户日期配置、完整模板及自定义日志配置优先 |
| Controller 调用日志 | 开启 | 关闭 | Web 环境下自动记录请求调用 |
| Feign 调用日志 | 开启 | 关闭 | 存在 Feign 时自动记录远程调用 |
| Jackson 增强 | 开启 | 关闭 | 影响 JSON 序列化、反序列化扩展 |
| Spring Converter / Web MVC 日期绑定 | 开启 | 关闭 | 影响字符串到日期时间的全局转换 |
| MyBatis-Plus 拦截器 | 开启 | 关闭 | 自动补充分页、乐观锁、防全表更新 |
| RedisTemplate 自动补齐 | 开启 | 关闭 | 依赖 Redis classpath 与连接工厂 |
| Redis Cache 自动补齐 | 开启 | 关闭 | 依赖 Spring Cache / Redis 条件 |
| Excel converter 自动注册 | 开启 | 关闭 | helper 工具类不受影响 |

无侵入模式仍保留或独立生效的能力：

| 能力 | 无侵入模式行为 | 说明 |
| --- | --- | --- |
| Velo Core 基础 Bean | 保留 | 提供指纹解析、消息解析、配置告警和可选 Banner 等基础支持 |
| `@Idempotent` / `@RateLimit` / `@Lock` | 保留 | 注解驱动能力不由 `velo.opinionated` 默认关闭，仍需对应后端依赖 |
| `@InvokeLog` | 保留 | 方法级显式日志仍可用；Controller/Feign 自动日志默认关闭 |
| XSS / CORS | 默认关闭，显式配置可开启 | 两者本身默认关闭，不依赖无侵入模式额外处理 |
| Excel Helper | 保留 | 手工调用 Helper 不受全局 converter 注册开关影响 |
| Spring Boot 官方自动配置 | 不受影响 | 无侵入模式只注入 `velo.*` 的最低优先级默认值 |

无侵入模式下，如果需要重新启用某项全局增强，显式配置对应开关即可覆盖默认关闭值，例如：

```yaml
velo:
  opinionated: false
  jackson:
    enabled: true
  cache:
    enabled: true
  excel:
    converters:
      enabled: true
```


## Maven 依赖

最新版本

[![Maven Central](https://img.shields.io/maven-central/v/io.github.luminion/velo-spring-boot3-starter)](https://central.sonatype.com/artifact/io.github.luminion/velo-spring-boot3-starter)



Velo 组件可直接从 Maven Central 获取，无需额外仓库配置。示例中的 `${velo.version}` 请在业务 POM 的 `properties` 中定义；使用本地安装版本时，与源码的 `revision` 保持一致。

### Spring Boot 2

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot2-starter</artifactId>
    <version>${velo.version}</version>
</dependency>
```

### Spring Boot 3

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot3-starter</artifactId>
    <version>${velo.version}</version>
</dependency>
```

### Spring Boot 4

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot4-starter</artifactId>
    <version>${velo.version}</version>
</dependency>
```

### 版本与兼容性

| Starter | 当前依赖线 | 支持的 Spring Boot 范围 | 编译 / 运行 JDK | 说明 |
| --- | --- | --- | --- | --- |
| `velo-spring-boot2-starter` | 2.7.18 | Boot 2.7.x | Java 8 及以上 | Boot 2.6.x 及以下版本不在当前支持范围内 |
| `velo-spring-boot3-starter` | 3.5.9 | Boot 3.2.x 及以上的 3.x 版本 | Java 17 及以上 | Boot 3.0.x、3.1.x 不在当前支持范围内 |
| `velo-spring-boot4-starter` | 4.0.5 | Boot 4.0.x | Java 17 及以上 | 使用 Boot 4 适配模块时不能使用 Java 8 |

以上范围是当前项目的构建和 API 适配边界；未列出的 Spring Boot 小版本不承诺兼容。Boot 2、Boot 3、Boot 4 Starter 分别依赖对应版本的适配模块，引入 Boot 2 Starter 不会因为根 POM 的 `<modules>` 配置而自动引入 Boot 3 或 Boot 4 模块。

Spring Boot 的 `spring.threads.virtual.enabled` 虚拟线程自动配置从 Boot 3.2.0 开始提供，并且运行时还需要 Java 21 及以上；本 Starter 不会因引入自身而自动开启该配置。详见 [Spring Boot 3.2 系统要求](https://docs.spring.io/spring-boot/docs/3.2.10/reference/htmlsingle/) 和 [Spring Boot 虚拟线程配置说明](https://docs.spring.io/spring-boot/reference/features/spring-application.html)。


### 编译参数建议

建议业务项目开启 Java 编译参数 `-parameters`,以支持 SpEL key 引用方法参数名，例如 `#userId`，未开启时只能为 `#p0`、`#p1`等：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

### 本地安装

需要使用源码版本时，在 Java 17 环境下执行 `bash ./mvnw -T 4 install -DskipTests -Dmaven.javadoc.skip=true`（Windows 使用 `mvnw.cmd`），随后按上面的 Maven 坐标引用本地仓库中的版本。

### 可选 Redisson 依赖

导入与 Starter 相同主版本的 `velo-spring-boot2/3/4-dependencies` BOM，统一管理可选组件版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.luminion</groupId>
            <artifactId>velo-spring-boot3-dependencies</artifactId>
            <version>${velo.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

| Spring Boot | Redisson | Spring Data 适配组件 | Redisson 自动配置入口 |
| --- | --- | --- | --- |
| 2.7 | 3.40.0 | `redisson-spring-data-27` | `RedissonAutoConfigurationV2` |
| 3.5（BOM 默认） | 3.50.0 | `redisson-spring-data-35` | `RedissonAutoConfigurationV2` |
| 4.0 | 4.3.1 | `redisson-spring-data-40` | `RedissonAutoConfigurationV4` |

Boot 3.2 / 3.3 / 3.4 应排除 Redisson starter 默认的 data-35，分别补充同版本的 `redisson-spring-data-32` / `33` / `34`；Boot 3 BOM 也管理这些组件的版本。

Boot 3.5 / 4 在导入对应 BOM 后，添加 `org.redisson:redisson-spring-boot-starter` 即可。Boot 2 的 BOM 会排除 starter 默认携带的 data-34，还必须添加以下 data-27 依赖；BOM 管理版本，不能自行新增传递依赖。

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
<!-- 仅 Spring Boot 2 需要补充 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-data-27</artifactId>
</dependency>
```

适配组件的选择参照 [Redisson 官方 Spring 集成说明](https://redisson.pro/docs/integration-with-spring/)。

---

## 各功能细览

### 1. 缓存自动配置

Velo 在 Spring Boot 原生 Redis Cache 创建流程中提供默认 JSON 序列化、单冒号键格式、分缓存 TTL 和 TTL 抖动；CacheManager 创建及用户自定义器执行由 Boot 负责。

额外依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

关键配置：

```yaml
spring:
  cache:
    type: redis
    cache-names: [user, order]
    redis:
      time-to-live: 5m
      key-prefix: "app:"
      cache-null-values: true
      use-key-prefix: true
      enable-statistics: false

velo:
  cache:
    enabled: true
    separator: ":"
    transaction-aware: false
    ttl-jitter-percentage: 0
    ttl:
      user: 10m
      order: 30m
```

使用示例：

```java
@EnableCaching
@SpringBootApplication
public class DemoApplication {
}
```

```java
@Cacheable(cacheNames = "user", key = "#id")
public UserDTO getById(Long id) {
    return null;
}
```

说明：

- `velo.cache.enabled` 默认开启
- 默认只在 `spring.cache.type=redis` 或未显式指定时增强 Redis Cache；缓存类型选择遵循 Boot 原生流程
- 默认 TTL 为 `5m`，通过 `spring.cache.redis.time-to-live` 覆盖；`velo.cache.ttl.<cacheName>` 可按缓存名单独覆盖
- 通用配置已统一到 Spring：原 `velo.cache.default-ttl`、`velo.cache.prefix`、`velo.cache.null-caching-enabled` 已移除，分别改用 `spring.cache.redis.time-to-live`、`key-prefix`、`cache-null-values`。原业务前缀 `app` 需改为 `key-prefix: "app:"`，新入口不会自动追加业务前缀分隔符
- TTL 只允许 `0`（不过期）或至少 `1ms` 的正值；负值、非零但不足 `1ms` 的值，以及超出 long 毫秒范围的值会在缓存初始化时导致启动失败
- 空值缓存、前缀开关及统计使用 `spring.cache.redis.cache-null-values`、`use-key-prefix`、`enable-statistics`；`spring.cache.cache-names` 用于预创建缓存，默认仍允许按名称动态创建
- key 前缀格式为 `spring.cache.redis.key-prefix + cacheName + velo.cache.separator`。业务前缀原样拼接；例如 `key-prefix: "app:"` 和默认分隔符生成 `app:user:123`
- 建议保留默认 `use-key-prefix=true`。关闭前缀后，原生整缓存清空会使用不带缓存名前缀的匹配模式，可能删除同库其他条目；只有独占 Redis 键空间时才适合关闭
- `cacheNames` 应使用固定名称，例如 `user`、`order`；业务 ID 和动态查询条件放在 `key` 中，避免持续创建不同名称的 Cache 实例
- 单冒号分隔便于 Redis 工具按层级展示。缓存名称末尾不带分隔符，由框架追加；名称内部可以用冒号分组，例如 `center-sys:shop-info:id`。避免同时定义 `user` 与 `user:detail` 这种存在键空间重叠的缓存名称；重叠时可能发生值覆盖，整缓存清空也可能一并删除另一缓存的条目。缓存前缀和缓存名还应避免 Redis 匹配通配符（`*`、`?`、`[`、`]`）
- 业务侧仍然需要自己开启 `@EnableCaching`
- Velo 默认增强先于普通用户 `RedisCacheManagerBuilderCustomizer` 执行，用户回调可以覆盖默认设置；`CacheManagerCustomizer` 由 Boot 在管理器构建后执行。Boot 2/3 的接口位于 `org.springframework.boot.autoconfigure.cache`，Boot 4 位于 `org.springframework.boot.cache.autoconfigure`
- 用户提供 `RedisCacheConfiguration` Bean 时，Velo 保留其默认 TTL、键格式和序列化设置；与 Boot 原生规则一致，`spring.cache.redis.time-to-live`、`key-prefix`、`cache-null-values`、`use-key-prefix` 等默认项和 `velo.cache.separator` 不再合并进该 Bean。按名称设置的 `velo.cache.ttl` 仍会基于该配置覆盖指定缓存的 TTL，用户自定义器可以继续覆盖它；用户提供 CacheManager 时，Velo 缓存增强退让
- 事务感知默认关闭，跟随原生行为；需要时设置 `velo.cache.transaction-aware=true`，也可以通过标准自定义器调整
- Velo 优先使用用户提供的 `RedisCacheWriter` Bean；没有时才创建默认 writer。多个候选建议通过 `@Primary` 明确选择，Spring 无法确定候选时直接启动报错。普通用户 `RedisCacheManagerBuilderCustomizer` 仍可以通过 `cacheWriter(...)` 设置最终 writer
- Velo 默认 writer 在 Lettuce 连接工厂下使用原生 `BatchStrategies.scan(1000)` 清空缓存；其他连接工厂保留原生 KEYS 策略，避免对未知驱动或 Jedis 集群作兼容假设。SCAN 分批查找并删除，会增加命令往返，也不提供并发写入时的原子清空保证；它不能解决缓存名前缀重叠。用户提供 writer 时，其连接及清空策略由用户决定
- Velo 默认 writer 的普通缓存写入、删除及清空采用同步行为；Boot 4 使用原生 `immediateWrites()` 保持与 Boot 2/3 一致。用户提供 writer 时保留其原生设置，包括同步或异步行为。事务感知开启时，事务内的相关操作仍按 Spring 原生规则延迟到提交后执行；显式异步缓存接口遵循其原生语义
- Velo 不维护按业务 key 累积的本地缓存或定时清理任务；缓存存储和过期由 Spring Data Redis 与 Redis 提供，CacheManager 仍会按缓存名称持有 Cache 实例
- Velo 创建默认缓存配置时，缓存值序列化器优先复用容器中的 `RedisSerializer<Object>`；没有候选时使用 `RedisSerializer.json()`，跟随当前 Spring Data Redis 版本的原生 JSON 实现。存在多个候选且 Spring 无法选出唯一主候选时，直接启动报错，避免悄然切换缓存读写格式。用户提供完整缓存配置时，序列化器由该配置明确指定，不再自动选取
- 默认 Redis 值序列化使用独立的映射器，HTTP JSON 的数字格式、日期格式、`JsonEncode` 等配置不会自动应用到缓存；需要自定义缓存值格式时，可提供自己的 `RedisSerializer<Object>` Bean，Velo 默认缓存和默认 RedisTemplate 可以共同复用它。用户自定义 RedisTemplate 的序列化设置不会自动传递给 CacheManager
- Boot 2 / 3 默认使用 Jackson 2 的 `GenericJackson2JsonRedisSerializer`（对应 Spring Data Redis 2.x / 3.x），写入 JSON 类型元数据，因此 `Object` / POJO 可以反序列化回原类型，而不是默认退化为 `LinkedHashMap`
- Boot 4 默认使用 `RedisSerializer.json()`，由 Spring Data Redis 4.x 选择 Jackson 3 的 `GenericJacksonJsonRedisSerializer`；即使 Jackson 2 / 3 同时存在，也不会按类路径猜测切换，默认仍使用 Jackson 3
- Boot 4 如需让 Redis 使用 Jackson 2，应引入官方 `spring-boot-jackson2` 及 Jackson 2 依赖，并显式注册一个 `RedisSerializer<Object>` Bean，例如 `GenericJackson2JsonRedisSerializer`；Spring Data Redis 4.x 仍保留该类用于兼容或迁移旧数据，但已标记为后续移除，不作为 Boot 4 默认实现
- Jackson 2 与 Jackson 3 的 Redis JSON 输出可能存在差异；从 Boot 2 / 3 切换到 Boot 4 时，应先规划旧数据读取、迁移或 key 空间隔离，不要默认认为历史值可以无缝混读
- **安全提示（多态反序列化）**：上述 JSON 序列化器为支持 `Object` / POJO 回读会写入并按类型元数据（`@class`）反序列化，且使用宽松的类型校验（与 Spring Data Redis 原生行为一致）。若 Redis 未鉴权或被写入恶意 `@class` 载荷，存在反序列化攻击面。建议 Redis 启用鉴权与网络隔离；如需收敛，可自行注册基于 `BasicPolymorphicTypeValidator` 白名单的 `RedisSerializer<Object>` Bean，Velo 会复用该 Bean

扩展组件的接入方式：

| 组件 | 接入方式 |
|---|---|
| `RedisConnectionFactory` | Boot 原生使用它创建缓存管理器；多个候选需明确主候选 |
| `RedisCacheConfiguration` | Boot 原生采用其默认配置；Velo 的默认配置 Bean 退让 |
| `RedisCacheManagerBuilderCustomizer` | Boot 按顺序调用，可设置按名称 TTL、writer、统计等 |
| `CacheManagerCustomizer<RedisCacheManager>` | Boot 在构建后、初始化前调用，可调整事务感知等 |
| `RedisCacheTimeMapProvider` | Velo 优先采用用户 Bean；按名称 TTL 在管理器创建时应用，运行时修改原 Map 不会刷新已有缓存配置 |
| `RedisCacheWriter` | Velo 优先采用用户 Bean；默认 writer 退让 |
| `RedisSerializer<Object>` | Velo 默认缓存配置选择唯一或主候选，也可以在完整缓存配置中显式指定 |
| `CacheKeyPrefix` | 在缓存配置中显式传给 `computePrefixWith(...)`，单独注册 Bean 不会自动接入 |
| `RedisCacheWriter.TtlFunction` | 在缓存配置中显式传给 `entryTtl(...)`；原生能力要求 Spring Data Redis 3.2+，适用于 Boot 3.2+ / 4，Boot 2 不支持 |
| `BatchStrategy` | 创建 writer 时传入；单独注册 Bean 不会自动接入 |
| `ObjectMapper` | 用它构造 Redis 序列化器，再通过缓存配置接入；单独注册 Bean 不会自动改变缓存格式 |
| `RedisTemplate` / `StringRedisTemplate` | 与 CacheManager 独立，缓存直接通过连接工厂和 writer 操作 |

需要只修改某个缓存的 TTL 时，使用现有默认配置派生，保留其键格式和序列化器。Boot 2/3 的 `RedisCacheManagerBuilderCustomizer` 导入路径为 `org.springframework.boot.autoconfigure.cache`，Boot 4 为 `org.springframework.boot.cache.autoconfigure`：

```java
@Bean
RedisCacheManagerBuilderCustomizer orderCacheTtl(RedisCacheConfiguration defaults) {
    return builder -> builder.withCacheConfiguration("order", defaults.entryTtl(Duration.ofMinutes(30)));
}
```

需要完全控制默认键格式和序列化时，提供自己的配置 Bean；序列化器同时负责写入和读取：

```java
@Bean
RedisCacheConfiguration cacheConfiguration(RedisSerializer<Object> valueSerializer) {
    return RedisCacheConfiguration.defaultCacheConfig()
            .computePrefixWith(name -> "app:" + name + ":")
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
}
```

Boot 3.2+ / 4 如需根据每条数据动态计算 TTL，可在上述配置中把固定 `entryTtl(Duration...)` 替换为 `entryTtl(ttlFunction)`，并通过配置方法参数注入自己定义的 `RedisCacheWriter.TtlFunction`。同样，`CacheKeyPrefix` 可通过方法参数注入后传给 `computePrefixWith(...)`。用户原生 TTL 函数的返回值遵循 Spring Data Redis 规则，Velo 不拦截其结果。自定义 writer 可通过标准 `RedisCacheManagerBuilderCustomizer` 继续覆盖，连接与客户端参数沿用对应连接工厂的配置。

缓存雪崩防护（TTL 抖动）：

- `ttl-jitter-percentage` 默认 `0`（关闭），有效范围为 `0..100`。设为 `10` 表示每条缓存写入时 TTL 在原值 ±10% 内随机偏移；超出范围会导致应用启动失败
- 抖动在**每次写入时按 key 独立计算**，因此同一缓存名称下不同 key 也会获得不同过期时间，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类雪崩
- 抖动只影响实际写入 Redis 的过期时间，不改变 `spring.cache.redis.time-to-live` / `velo.cache.ttl.<cacheName>` 的配置语义；不过期的条目不会应用抖动，开启统计不会关闭抖动。显式开启抖动时也会包装用户提供的 writer；后续自定义器替换 writer 时，以最终 writer 的行为为准
- TTL 抖动用于分散大量条目的过期时间，不能保证同一个热点 key 在并发缓存未命中时只回源一次；默认非锁定 RedisCacheWriter 不提供跨应用实例的防击穿保证

### 2. Excel 自动配置

默认写出类型与 Velo Jackson 默认策略一致：Long / BigInteger / BigDecimal 使用文本，Float / Double 使用数值，Boolean 使用布尔单元格。读取仍支持字符串形式的数字及“是/否”等布尔输入。日期默认拒绝非法日历日期，显式格式配置仍优先。

Velo 提供两层能力：

- Excel helper 工具类，随依赖引入即可直接使用
- 扩展 converter 自动注册默认开启，可通过 `velo.excel.converters.enabled=false` 显式关闭

可选额外依赖，按实际使用的库引入：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>cn.idev.excel</groupId>
    <artifactId>fastexcel</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.apache.fesod</groupId>
    <artifactId>fesod-sheet</artifactId>
</dependency>
```

关键配置：

```yaml
velo:
  excel:
    converters:
      enabled: true
      boolean-enabled: true
      long-enabled: true
      float-enabled: true
      double-enabled: true
      big-integer-enabled: true
      big-decimal-enabled: true
      date-enabled: true
      local-date-time-enabled: true
      local-date-enabled: true
      local-time-enabled: true
```

使用示例：

```java
List<Converter<?>> converters = EasyExcelHelper.createExtraConverters(
        "yyyy-MM-dd",
        "HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "GMT+8"
);

EasyExcelHelper.registerConverters(converters);
```

说明：

- `velo.excel.converters.enabled` 默认开启；设为 `false` 时关闭 starter 的 converter 自动注册，不影响 helper 手工调用
- Excel 自动配置本身没有重复的总开关；starter 会根据 classpath 自动尝试向 EasyExcel、FastExcel、Fesod 注册扩展 converters
- 时间、日期、时区格式统一复用 `velo.date-time-format.*`
- `createExtraConverters(...)` 返回独立的可变列表；需要追加自定义 converter 时，可在调用 `registerConverters(...)` 前直接使用 `converters.add(...)`
- 如果你只想手工控制注册时机，也可以直接使用 `EasyExcelHelper`、`FastExcelHelper`、`FesodExcelHelper`

### 3. 幂等

`@Idempotent` 用于防重复提交，需要 Caffeine、Redis 或 Redisson 后端。

可选额外依赖：

- 需要 Redisson 后端时引入 `org.redisson:redisson-spring-boot-starter`
- 需要 Redis 后端时引入 `spring-boot-starter-data-redis`
- 需要 Caffeine 后端时引入 `com.github.ben-manes.caffeine:caffeine`

关键配置：

```yaml
velo:
  idempotent:
    enabled: true
    backend: AUTO
    prefix: "idempotent:"
```

使用示例：

```java
import io.github.luminion.velo.idempotent.annotation.Idempotent;

@Idempotent(key = "#userId", ttl = 3000)
public void submitOrder(Long userId) {
    // ...
}
```

说明：

- `backend` 可选 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`
- `AUTO` 模式下按自动配置顺序选择后端：`REDISSON -> REDIS -> CAFFEINE`
- Caffeine 使用原生缓存过期；Redis 使用 `SET NX` + TTL 和失败时的 token 比对删除；Redisson 使用原生 `RBucket`，Velo 不额外维护本地幂等缓存或清理任务
- 没有可用实现时，应用仍可启动，调用带注解的方法会在业务执行前报错；显式指定不可用或不支持的后端时启动报错。自定义 `IdempotentHandler` 优先
- `prefix` 默认 `idempotent:`
- `ttl` 单位固定为毫秒，默认 `3000`（3 秒）
- TTL 从首次进入时开始计算，成功结束不重置；默认 3 秒主要用于防连点，长任务应配置更长窗口。TTL 到期后同 key 可再次进入，不保证业务永久只执行一次
- `value` 与 `key` 互为别名，`@Idempotent("#userId")` 等价于 `@Idempotent(key = "#userId")`；同时设置时必须一致
- 业务失败（抛异常）时会清除本次幂等记录以允许重试；清除采用 token 比对，只删除本次请求写入的记录，不会误删并发请求在窗口内刚写入的新记录
- 幂等 key 始终以方法（`全限定类名#方法名(参数类型...)`）为前缀，再拼接 SpEL 结果，与限流分桶语义一致：**不同方法或同名重载方法即使用相同的 SpEL key（如都用 `#orderId`）也不会互相碰撞、共享同一幂等窗口**

> ⚠️ `key` 必须显式指定。`key` 为空时会退化成 `全限定类名#方法名(参数类型...)`，意味着**该方法的所有调用（不分参数、不分调用者）共享同一个幂等窗口**，这通常不是期望行为。此时 starter 会打印一条 WARN 提醒。
>
> ```java
> // ❌ 危险：userId=1 提交后，3 秒内 userId=2 也会被拦截
> @Idempotent(ttl = 3000)
> public void submitOrder(Long userId) { }
>
> // ✅ 正确：每个用户独立幂等
> @Idempotent(key = "#userId", ttl = 3000)
> public void submitOrder(Long userId) { }
> ```
>
> 若确实需要「全局同一时刻只能执行一次」的语义（如系统级初始化），更推荐用 `@Lock`。

### 4. 限流

`@RateLimit` 用于方法级限流，适合接口限流、用户维度限流和资源维度限流。

可选额外依赖：

- 需要 Redisson 后端时引入 `org.redisson:redisson-spring-boot-starter`
- 需要 Redis 后端时引入 `spring-boot-starter-data-redis`
- 需要 Guava 后端时引入 `com.google.guava:guava`，版本由各 Boot BOM 管理

关键配置：

```yaml
velo:
  rate-limit:
    enabled: true
    backend: AUTO
    prefix: "rateLimit:"
```

使用示例：

```java
import io.github.luminion.velo.ratelimit.annotation.RateLimit;

@RateLimit(qps = 10, key = "#userId")
public Object query(Long userId) {
    return null;
}
```

说明：

- `qps` 表示每秒请求速率，默认 `50`，必须为正整数
- `value` 与 `qps` 互为别名，`@RateLimit(10)` 等价于 `@RateLimit(qps = 10)`；同时设置时必须一致，`key` 单独配置
- `backend` 支持 `AUTO/REDISSON/REDIS/GUAVA`，`AUTO` 默认顺序为 `REDISSON -> REDIS -> GUAVA`
- Guava 使用原生 `RateLimiter`，同 key 共享限流器，空闲五分钟后由 Guava 原生缓存过期；额度补充和空闲突发遵循 Guava 行为，仅保证单 JVM 内有效
- Redis 使用 Lua 原子计数，首次请求开始固定一秒窗口，后续请求不延长 TTL；过期由 Redis 处理，无本地缓存或清理任务
- **Redis 允许窗口边界突发**：上一窗口末尾和下一窗口开头可连续消耗各自额度，短时间内最多接近两倍 `qps`；不能当作“任意滚动一秒最多 `qps` 次”的严格限制
- Redisson 直接使用 `RRateLimiter`，配置为每秒 `qps` 次，额度按 Redisson 原生规则恢复；只保证各后端声明的速率语义，不统一额度归还节奏
- 多实例共享配额请使用 Redis 或 Redisson；Velo 不统一不同后端的额度归还节奏
- 没有可用实现时，应用仍可启动，调用带注解的方法会在业务执行前报错；显式指定不可用或不支持的后端时启动报错。自定义 `RateLimitHandler` 优先

关于 `key` 的分桶语义（重要）：

- `key` 为空：方法级全局限流，该方法的**所有调用者共享同一个配额**
- `key` 非空：按 SpEL 表达式结果分桶，**每个桶独立计算配额**

| 写法 | 实际行为 |
| --- | --- |
| `@RateLimit(10)` | 该方法所有调用共享每秒 10 次的速率 |
| `@RateLimit(qps=10, key="#userId")` | 该方法每个 userId 独立按每秒 10 次限流 |

> ⚠️ 如果你期望「每个用户 / 每个资源独立限流」，必须显式指定 `key`，否则会退化为全局共享配额。

### 5. 锁

`@Lock` 提供方法级互斥能力，适合支付、状态流转、扣减等需要串行化的业务场景。

可选额外依赖：

- 需要 Redisson 后端时引入 `org.redisson:redisson-spring-boot-starter`
- 需要 Redis 后端时引入 `spring-boot-starter-data-redis`

关键配置：

```yaml
velo:
  lock:
    enabled: true
    backend: AUTO
    prefix: "lock:"
    redis-ttl-seconds: 60
```

使用示例：

```java
import io.github.luminion.velo.lock.annotation.Lock;

@Lock(key = "#orderId")
public void pay(Long orderId) {
    // ...
}
```

说明：

- `backend` 支持 `AUTO`、`REDISSON`、`REDIS`、`JDK`
- `AUTO` 默认顺序为 `REDISSON -> REDIS -> JDK`；JDK 无需额外依赖，自定义 `LockHandler` 优先
- 三种后端均只尝试一次，拿不到锁立即失败；不提供等待参数或自动重试
- `value` 与 `key` 互为别名，`@Lock("#orderId")` 等价于 `@Lock(key = "#orderId")`；同时设置时必须一致
- 自定义 `LockHandler` 实现 `tryLock(String key)` 和 `unlock(String key)`；成功获取后必须在原线程配对释放。Redis / Redisson 获取过程的连接异常向上抛出，与锁被占用返回 `false` 区分
- 注解和 `LockHandler` 不提供租期参数；锁的生命周期由后端管理
- Redis 简单锁的固定 TTL 由构造函数指定秒数，自动配置使用 `velo.lock.redis-ttl-seconds`，默认 `60`，必须为正数；重入不重置 TTL，不启动任何自写续期线程
- Redis TTL 到期后自动释放，不中断正在执行的业务，也不保证到期后的互斥；业务必须在 TTL 内完成。耗时不确定的调用优先使用 Redisson
- Redisson 使用不指定租期的原生获取接口，由自身看门狗续期；超时时间使用 Redisson 的 `lockWatchdogTimeout` 配置
- Redis / Redisson 用于分布式场景；JDK 使用原生 `ReentrantLock`，只保证单 JVM 内互斥，不超时释放正在使用的锁
- JDK 只登记正在获取或持有的锁，获取失败撤销引用，最后一个使用者离开后立即删除；不能每次创建独立锁，也不能按缓存 TTL 淘汰活跃锁，否则会破坏互斥
- `REDIS` 后端支持同线程可重入（同一线程重复加同一把锁不会自锁死），最外层释放时才真正删除 Redis 锁
- Redis 锁只在当前线程持有期间记录 token 和重入次数，最外层释放后立即清理；Redis 幂等和 Redisson 三种后端都不额外维护本地键缓存
- `key` 为空时降级为方法级锁（基于 `全限定类名#方法名(参数类型...)`），表示「该方法全局串行执行」，是一个有意义的语义，因此安静降级、不打告警；需要按业务维度加锁时请显式指定，例如 `@Lock(key = "#orderId")`
- 显式 `key` 也带完整方法标识，因此不同方法即使使用相同业务值，也会使用不同的锁

### 6. 并发控制组合顺序

这三项功能不创建 Velo 自己的定时清理任务、续期任务、轮询或后台线程。Guava / Caffeine 的过期和维护交给原生缓存，Redis 的过期交给服务端。JDK / Redisson 锁调用原生无等待的 `tryLock()`，Redis 锁只发送一次 `SET NX`。Redisson 看门狗和 Redis 客户端网络线程仍由第三方管理；Caffeine 原生维护可能使用公共线程池，因此不代表整个应用没有后台线程。

JDK 锁和 Redis 锁的本地记录只保存活跃调用，不随历史业务 key 累积；切面在业务成功或抛出异常时均释放锁。Guava 限流与 Caffeine 幂等缓存依赖原生过期机制，过期记录不再生效，但物理清理不保证发生在到期瞬间。两者未设置容量淘汰上限，避免淘汰仍有效的记录而放过限流或重复提交；大量不同 key 在有效期内仍会占用相应内存，需要按业务控制 key 数量和 TTL。

注解请标在具体实现方法上；`@RateLimit` 也可以标在具体实现类上，作用于该类声明的方法，方法上的注解优先。Starter 不额外搜索父接口或父类上的注解；继承方法是否被拦截沿用 Spring AOP 原生行为，不作为额外继承能力承诺。同类内部直接调用绕过代理时，切面不会生效。

三种注解使用同一套键规则：`功能前缀 + 实际用户类全名#方法名(完整参数类型...) + 可选的 :SpEL结果`。保留完整类名和重载签名，避免简称或同名方法碰撞。表达式结果支持字符串、数字、布尔值、字符、UUID 和枚举（使用 `name()`）；空值、空白或数组、集合、对象等复杂结果会报错，请明确取业务 ID。字符串原样保留，不裁剪首尾空格。解析器只缓存固定声明的表达式和方法标识，每次调用创建独立求值上下文。

当 `@Idempotent`、`@RateLimit`、`@Lock` 同时作用于同一个方法时，starter 内置顺序为：

```text
@Idempotent -> @RateLimit -> @Lock -> 业务方法
```

这意味着重复提交会最先被拒绝，不消耗限流令牌，也不会尝试加锁；限流失败时不会尝试加锁；只有真正允许执行业务的方法调用才会获取锁。

这些切面使用接近 `Ordered.LOWEST_PRECEDENCE` 的低优先级顺序值，并在三者之间保留较大间隔，便于业务自定义切面通过 `@Order` 插入到合适位置。

### 7. 日志

Controller、Feign、`@InvokeLog` 和任务入口共用一个同步 `InvocationLogEngine`。入口适配负责提供调用信息并管理独立的 trace 作用域；引擎解析日志策略、计时和记录方法返回/抛出异常，`LogValueFormatter` 转换内容，`InvocationLogWriter` 只拼接固定格式并输出。每个功能单独一行，功能决定 `==>` 或 `<==` 方向。

```text
[controller] [127.0.0.1 GET /users/{id}] ==> entryArgs={"id":1}
[controller] [127.0.0.1 GET /users/{id}] ==> requestHeaders={"X-Demo":["visible"]}
[controller] [127.0.0.1 GET /users/{id}] <== exitArgs={"id":1}
[controller] [127.0.0.1 GET /users/{id}] <== exitResult={"name":"Tom"}
[controller] [127.0.0.1 GET /users/{id}] <== responseHeaders={"X-Result":["ok"]}
[controller] [127.0.0.1 GET /users/{id}] <== slow={"costMs":1200,"thresholdMs":1000}
[invoke] [find()] <== error={"type":"java.lang.IllegalArgumentException","message":"参数无效"}
[feign] [remote() GET /log/remote] <== exitResult={"message":"hello"}
[scheduled] [run()] <== slow={"costMs":5,"thresholdMs":0}
[xxl-job] [run()] <== slow={"costMs":5,"thresholdMs":0}
```

固定结构为 `[入口类型] [调用目标] 箭头 功能名=内容`，各块之间保留一个空格，等号两侧不留空格。入口类型为 `controller/invoke/feign/scheduled/xxl-job`；功能名为 `entryArgs/exitArgs/exitResult/requestHeaders/responseHeaders/slow/error`，慢调用和异常分别使用独立对象，异常摘要不附带堆栈。类名由日志框架输出，Invoke、Scheduled 和 XXL-Job 的调用目标只显示方法名。

正文不重复打印 `traceId`、`source`、`event`、`invocationId`。默认日志格式通过 MDC 在级别位置显示 traceId。`InvocationLogRecord` 只包含 `source/target/feature/loggerName/level/content` 六个字段，`content` 为已经转换的字符串；不再生成 invocationId，也不复制 traceId。自定义输出器在 `write` 时可读取 MDC；延迟或异步输出时应自行捕获上下文快照。

| 功能注解 | Controller 默认 | Feign 默认 | Invoke 默认 | 默认级别 |
| --- | --- | --- | --- | --- |
| `EntryArgs` | 开 | 开 | 开 | INFO |
| `ExitArgs` | 关 | 关 | 关 | INFO |
| `ExitResult` | 开 | 开 | 开 | INFO |
| `SlowLog` | 开，1000ms | 开，1000ms | 开，1000ms | WARN |
| `RequestHeadersLog` | 关 | 关 | 不适用 | INFO |
| `ResponseHeadersLog` | 关 | 关 | 不适用 | INFO |
| `ErrorLog` | 开 | 开 | 开 | WARN |

任务入口包括 `@Scheduled` 和 XXL-Job，默认只开慢调用、异常摘要；参数和返回值默认关闭。普通方法需要 `@InvokeLog` 作为切入点；Controller、Feign、任务入口由自己的切面自动接入，重复标注 `@InvokeLog` 不会重复输出。只有功能注解的普通方法不会自动切入。切面基于 Spring AOP，同类内部直接调用、未经过代理的对象、private/final 方法不保证被拦截。

配置优先级：**方法注解 > 类注解 > 来源配置 > 全局配置 > 内置默认值**。注解为完整配置，例如 `@EntryArgs` 自带 `enabled=true, level=INFO`，会覆盖类或 properties 对该功能的配置。没有方法注解才继承类配置。所有注解只支持类和方法，不支持参数、字段、返回类型；不再提供 `CONFIGURED` 或按慢调用/失败联动打印参数的触发策略。

`@LogIgnore` 排除其所在类或方法的全部调用日志，其他功能注解不能重新开启。它不屏蔽独立的下游调用日志，也不阻断 traceId 传播。`velo.log.enabled=false` 或 `sources.<source>.enabled=false` 是来源总开关，功能注解不能越过总开关。

```java
@Service
@InvokeLog
@EntryArgs(enabled = false)
public class UserService {
    @EntryArgs
    @ExitResult
    @SlowLog(thresholdMs = 200, level = LogLevel.WARN)
    public User find(Long id) { /* 业务实现 */ }

    @LogIgnore
    public Token credentials() { /* 不记录该方法的任何调用日志 */ }
}
```

每个功能都有独立 `enabled` 和 `level`，支持 Spring Boot `LogLevel`，`OFF` 关闭该功能。没有普通耗时 `CostLog`；将 SlowLog 阈值设为 `0`、级别设为 `INFO` 即可记录所有耗时。阈值单位毫秒，应大于等于 0。耗时从入口日志输出后计到当前方法返回或抛出异常，结束日志本身不计入耗时。返回 `CompletionStage/Future` 也立即记录退出，不等待、不监听完成，不改变原返回对象或取消行为。ExitResult 表示返回对象而非异步最终结果；此类方法可关闭 ExitResult，或在自定义 formatter 中输出类型摘要。ErrorLog 只记录当前方法抛出的异常，不记录返回 Future 的后续失败。

```yaml
velo:
  log:
    enabled: true
    defaults:
      max-payload-length: -1
      entry-args:
        level: INFO
      exit-args:
        enabled: false
      slow-log:
        enabled: true
        threshold-ms: 1000
        level: WARN
      error-log:
        enabled: true
        level: WARN
    sources:
      controller:
        exit-result:
          enabled: true
          level: INFO
      feign:
        request-headers:
          enabled: true
          allowlist: [traceparent, tracestate, Content-Type]
        response-headers:
          enabled: true
          allowlist: [X-Trace-Id, Content-Type]
      invoke:
        slow-log:
          threshold-ms: 0
          level: INFO
  trace:
    enabled: true
    mdc-key: traceId
    feign-propagation-enabled: true
    logging-pattern-enabled: true
```

`ErrorLog` 只在异常结束时输出异常类型和提示信息，默认 WARN，不打印堆栈，不区分业务异常。异常继续原样抛出，由应用异常处理器决定响应、堆栈、ERROR 和告警；框架不注册业务异常分类规则。换行和控制字符会转义，便于 ELK/SLS 按单行采集。同一次失败在不同调用层可能各输出一条摘要。

对象转换只有一个扩展方法：

```java
@Bean
public LogValueFormatter logValueFormatter() {
    return value -> myFormatter.format(value);
}
```

默认使用应用已有的 Jackson 2/3 Mapper 生成完整字符串，复用字段忽略、日期格式、命名规则和自定义模块，非 Web 应用也可用；没有 Mapper 时使用 `String.valueOf`。可使用 `@JsonIgnore` 或 MixIn 隐藏 DTO 字段，这些规则也会影响使用同一 Mapper 的业务序列化。Jackson 转换失败输出 `serialization-failed`，不会回退到可能暴露敏感字段的 toString。HttpEntity/ResponseEntity 返回值只转换 body，原响应对象保持不变。流、Servlet 等技术对象省略内容；容器保留循环和深度保护，不限制元素数量。DTO 原样交给 Mapper，不自行反射拆解。自定义 formatter 返回完整文本，框架统一转义换行和控制字符。

参数、结果、慢日志及异常摘要都使用同一个 formatter；慢日志中的 Long 数值也沿用应用的序列化规则，例如将耗时和阈值输出为字符串。

`max-payload-length` 默认 `-1`，完整输出，不做字符截断。仅支持 `-1` 和 `0`，其他值会在日志引擎启动时报错；`0` 关闭参数、结果及协议头载荷日志的输出和序列化，慢调用/异常摘要仍可完整输出。

对于预期的超大字符串、大集合或大对象，应在应用中忽略对应载荷日志，避免无用的序列化和日志输出成本。可通过方法或类上的注解关闭参数、返回值日志，保留慢调用和异常摘要：

```java
@EntryArgs(enabled = false)
@ExitArgs(enabled = false)
@ExitResult(enabled = false)
public ExportResult export(ExportRequest request) { /* 业务实现 */ }
```

需要忽略该方法的全部调用日志时使用 `@LogIgnore`；也可通过来源或全局配置关闭对应功能。框架不按字段名称自动脱敏；协议头的空 allowlist 表示全部头，启用采集时应设置需要的白名单。Feign 在底层客户端构建请求后捕获最终一次尝试的真实请求/响应头，因此请求头日志可能出现在完成阶段；逻辑请求目标使用方法名、HTTP 方法和映射路径，入口和完成阶段保持一致。Controller 响应头取当前方法返回或抛出异常时已设置的头，不代表 Servlet 最终提交后的完整响应。

trace 模块通过独立 `VeloTraceAutoConfiguration`、HTTP Filter、Feign 传播配置及入口作用域管理启用。`velo.log.enabled=false` 不影响 trace；`velo.trace.enabled=false` 不影响日志功能。HTTP Filter 保留异步/错误派发复用，TaskDecorator 保留常见执行器传播，两者与 Future 的日志完成监听无关。原公共 Resolver/TraceContext 类型和包名保持不变。

推荐使用 `velo.trace.enabled/mdc-key/feign-propagation-enabled/logging-pattern-enabled`。旧 `velo.log.trace.*` 中这四项仍作为逐项回退值；同一项同时配置时新名称优先。

traceId 获取和生成只有一个无参扩展方法，默认注入 `W3cTraceContextResolver`。它从当前 Spring 请求上下文读取 `traceparent` 和 `tracestate`：合法时沿用 traceId 并保留完整协议字段；缺失、重复或非法时生成新的非零 32 位小写十六进制 traceId 和合法的 `traceparent`，无效的 `tracestate` 只会被丢弃。已有 `X-Trace-Id` 请求头不会参与默认解析。规则参考 [W3C Trace Context](https://www.w3.org/TR/trace-context/)。

保留原来的指定头协议，只需注册一个 Bean：

```java
@Bean
public TraceContextResolver traceContextResolver() {
    return new HeaderTraceContextResolver("X-Trace-Id");
}
```

此实现从指定头读取纯 traceId 并向下游发送同名头；缺失、重复或非法时生成新值。接受 1–128 位 ASCII 字母、数字、`-`、`_`、`.`。构造器只需提供头名称。

更复杂的协议也可自行实现：

```java
@Bean
public TraceContextResolver traceContextResolver() {
    return () -> {
        String id = myContext.currentTraceIdOrGenerate();
        Map<String, String> headers = Collections.singletonMap("X-Custom-Trace", id);
        return new TraceData(id, headers);
    };
}
```

`TraceContextResolver.resolve()` 自己读取当前上下文，可以用 `RequestContextHolder.getRequestAttributes()`，也可以读应用的 ThreadLocal；必须兼容没有 HTTP 请求的调用并保证线程安全。HTTP Filter 在解析期间临时提供包含请求和响应的 `ServletRequestAttributes`，解析后恢复已有 Spring 上下文。解析器返回不可变 `TraceData`，包含 traceId 和下游传播头，不保存 Servlet 对象；框架负责复用、MDC 和请求传播，自定义 ThreadLocal 由应用清理。传播头的空字符串表示移除已有同名头，不发送该值。自定义解析器返回 null 或抛出 RuntimeException 时，框架生成 W3C 兜底上下文，异常不会打断业务。

traceId 生命周期：

- HTTP：每次新请求解析一次，不沿用工作线程残留值。请求属性保存完整快照，异步/错误派发复用；结束后恢复先前 MDC 和框架上下文。
- Invoke：已有框架上下文直接复用，没有时调用解析器；只有 MDC 标识时由解析器判断是否能沿用。最外层结束后恢复，嵌套调用复用同一快照。自定义多步流程可在外层加 `@InvokeLog`，或用 `try (TraceContext.Scope scope = TraceContext.open("traceId", true, resolver)) { ... }` 包住流程，其中 resolver 为注入的 Bean。两参 `open` 固定使用内置 W3C 实现。
- Feign：发送快照中的传播头，下游 HTTP 入口使用相同协议即可沿用，实现 A→B 一致。默认透传 `traceparent/tracestate`，保留上游 parent-id 和采样标志，不生成每次调用的 span。没有上层上下文时由 Feign 入口适配建立作用域；仅使用裸拦截器时生成出站头，不给调用线程永久写 MDC。
- 常见 Spring 执行器：提供 `MdcTaskDecorator` Bean，Boot 支持 TaskDecorator 的自动配置执行器会采用它。提交时复制 MDC 和完整链路快照，任务内复用或调用解析器生成，执行后恢复工作线程。不会复制 Servlet 请求、响应或应用自己的 ThreadLocal。用户已有 TaskDecorator 时保留用户 Bean；自定义线程池需要自行安装该装饰器。
- Scheduled/XXL-Job：每次执行调用同一个解析器，不沿用 HTTP 请求或调用方 MDC，结束后恢复。内置实现为每次任务生成新值；自定义 ThreadLocal 的独立性由自定义实现保证。

MDC 只存 traceId，不存 spanId；协议中的 parent-id 只作为传播字段保存。这里是 W3C 上下文透传和日志关联，不采集 span，不改变上游采样决定，完整分布式追踪可另行接入。若同时使用其他追踪组件，应选择一个组件负责出站追踪头，避免多个拦截器相互覆盖。前端发送 `traceparent` 时需自行生成合法字段，并在跨域场景允许该请求头及需要读取的响应头。

框架不在 HTTP 响应中自动返回 traceId。链路关联通过请求头向下游传播；响应回传属于应用可选的诊断约定，W3C 处理模型没有要求该步骤。需要向前端提供排查编号时，由网关、应用 Filter 或异常处理器读取 MDC 后返回响应头或响应体。`ResponseHeadersLog` 仍按配置采集业务实际设置的响应头。

`velo.log.trace.header-name`、`velo.log.trace.response-header-enabled` 和 `TraceData.responseHeaders` 已移除；指定请求头通过 `HeaderTraceContextResolver` Bean 选择，构造 `TraceData` 时只传 traceId 和传播头。

自定义 Logback/Log4j2 格式需自行加入 MDC 键；默认键对应 `%X{traceId}`，例如 Spring Boot 的 `logging.pattern.level: "%5p [%X{traceId}]"`。自定义 `mdc-key` 后同步更改格式。功能级别仍受对应类 logger 的有效级别限制，例如注解选 DEBUG 而该 logger 为 INFO 时不会输出。

默认文本日志的行首时间格式为 `yyyy-MM-dd HH:mm:ss.SSS`。在日志初始化前通过最低优先级属性源补充 `logging.pattern.dateformat`，用户在配置文件、环境变量或 JVM 参数中指定的格式优先，包括直接指定 `LOG_DATEFORMAT_PATTERN`。用户已有完整 console/file pattern、结构化日志配置、`logging.config` 或 classpath 日志配置文件时不补充日期默认值。`velo.opinionated=false` 或 `velo.log.enabled=false` 时保持原日期格式；关闭 traceId 不影响日期默认值。此设置只影响日志行首，不修改 Jackson 日期格式、文件滚动策略或用户自定义模板。

```yaml
logging:
  pattern:
    dateformat: "yyyy-MM-dd HH:mm:ss.SSS" # 可按需覆盖，无需使用 Velo 专用属性
```

本次将 `InvokeArgs`、`ReturnResult` 统一更名为 `EntryArgs`、`ExitResult`，配置 `invoke-args`、`return-result` 同步更名为 `entry-args`、`exit-result`；`ExitArgs` 不变，不保留旧命名别名。

此前移除的旧 `InvokeEntryArgs/InvokeExitArgs/InvokeExitResult/InvokeLogIgnore`、旧请求/响应头注解、`RuntimeJsonSerializer`、异常分类规则、`slow-threshold-ms` 和统一日志级别配置仍不支持。旧枚举值配置（例如 `entry-args: ALWAYS`、`exit-result: ON_SLOW`）需迁移为带 `enabled/level` 的功能配置对象，慢调用阈值使用 `slow-log.threshold-ms`。

### 7. XSS

XSS 配置位于顶层 `velo.xss.*`，与 `velo.web.enabled` 解耦。默认策略为 `NONE`，不会创建 Velo 内置清洗器；选择具体策略后，Web 参数清洗和 Jackson 普通字符串清洗仍由两个目标开关分别控制。

额外依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
</dependency>
```

关键配置：

```yaml
velo:
  xss:
    strategy: RELAXED
    web-enabled: true
    jackson-enabled: false
```

使用示例：

```java
public class UserQuery {

    private String keyword;

    @XssIgnore
    private String rawHtml;
}
```

说明：

- `velo.xss.strategy=NONE` 默认不创建 Velo 内置 `XssCleaner`；选择其他策略才会按依赖情况创建内置清洗器
- `velo.xss.web-enabled` 默认 `true`，控制 Web MVC 字符串参数转换目标；设为 `false` 后即使已选择策略也不注册该目标转换器
- `velo.xss.jackson-enabled` 默认 `false`，控制 Jackson 普通 `String` 属性的全局清洗；这是独立开关，不影响 `@JsonEncode` / `@JsonDecode` 注解处理
- `strategy` 可选 `NONE`、`ESCAPE`、`SIMPLE_TEXT`、`BASIC`、`BASIC_WITH_IMAGES`、`RELAXED`
- `ESCAPE` 不依赖 `jsoup`；其他 HTML 清洗策略必须引入 `jsoup`
- `ESCAPE` 且无 `jsoup` 时会走 Spring 转义；其他策略缺少 `jsoup` 时只打印 WARN，不注册 `XssCleaner`，也不会自动降级
- 清洗发生在 Web MVC 的字符串参数绑定阶段，包括 query/form/path 和普通对象参数中通过对应 binder 绑定的 `String` 字段
- 同时启用 XSS 与 `velo.xss.jackson-enabled` 时，Jackson JSON 请求体中的普通 `String` 字段也会进行清洗；字段上的 `@XssIgnore` 可以跳过清洗，`@JsonDecode` 会在解码后继续执行清洗
- 用户可以直接提供自己的 `XssCleaner` Bean；`strategy=NONE` 只表示不创建 Velo 内置清洗器，不会阻止用户清洗器在目标开关开启时生效

### 8. Jackson

Jackson 增强是基础 starter 的核心能力之一，会统一处理日期时间、超大整数、枚举派生字段和字符串转换。

基础 starter 默认可用，无需额外依赖。

关键配置：

```yaml
velo:
  jackson:
    enabled: true
    date-time-enabled: true
    serialize-long-as-string: true
    serialize-big-decimal-as-string: true
    serialize-floating-as-string: false
    enum-desc-enabled: true
    enum-name-suffix: name
    enum-mappings:
      code: name
      key: value
```

使用示例：

```java
public class OrderVO {

    @JsonEnum(OrderStatusEnum.class)
    private Integer status;

    @JsonEncode(PhoneMasker.class)
    private String mobile;

    @JsonDecode(EmailMasker.class)
    private String email;
}
```

说明：

- Boot 2 / 3 使用 Jackson 2 自动配置，Boot 4 使用 Jackson 3 自动配置
- `serialize-long-as-string=true` 时，`long` / `Long` / `BigInteger` 默认按字符串写出，包括基本类型数组、包装类型数组及嵌套数组，避免 JS Number 超过 2^53 精度丢失
- 数字转换是默认规则，字段上的 `@JsonFormat(shape = JsonFormat.Shape.NUMBER)` 可恢复数值输出；关闭默认转换后，显式 `Shape.STRING` 仍沿用 Jackson 原生行为
- 这些 `serialize-*` 开关**只影响序列化方向**；反序列化（前端传入）时数字和字符串都能正常绑定，无需前端特殊处理
- `serialize-big-decimal-as-string=true` 默认开启，`BigDecimal[]` 与单值使用同样的字符串输出及尾零处理规则；开启 `serialize-floating-as-string` 后，`float[]` / `double[]` 与包装类型数组同样生效
- `enum-desc-enabled=true` 时，`@JsonEnum` 可为数值字段派生出描述字段，例如 `statusName`；派生字段遵循原字段的包含策略和视图，并支持类级命名策略及数组输出形态
- `enum-mappings` 为空时只关闭按全局约定进行的隐式匹配；`@JsonEnum` 同时指定 `codeField` 和 `nameField` 时仍独立生效
- `@JsonEncode` / `@JsonDecode` 是注解驱动能力：只要 Jackson 扩展与 `JsonProcessorProvider` 生效，带注解字段就会转换；未使用注解的字段不会执行转换，因此不再提供额外的 `string-converter-enabled` 总开关
- Jackson 的普通字符串 XSS 清洗由独立的 `velo.xss.jackson-enabled` 控制，默认关闭，避免把全局 Mapper 的所有字符串都意外改写
- 日期时间格式依然复用 `velo.date-time-format.*`；未标注的 `Date` 默认兼容日期-only 和完整日期时间，字段上的 `@JsonFormat` 优先
- `velo.date-time-format.time-zone` 默认 `GMT+8`，支持 `+08:00`、`GMT+08:00`、`Asia/Shanghai` 等 `ZoneId` 写法；日期增强启用时，非法时区使初始化失败，不会静默退回 GMT
- 字符串编码先执行 `@JsonEncode` 转换，再交给 Jackson 原生字符串序列化器写出，保留自然字符串的多态处理及空字符串包含规则
- Boot 4 的模块自动发现由 `spring.jackson.find-and-add-modules` 控制，关闭后可通过 Module Bean 或显式注册提供扩展；Velo 不额外扫描模块

### 9. MyBatis-Plus 自动配置

Velo 会在满足条件时自动创建 `MybatisPlusInterceptor`，并按开关补充分页、乐观锁、防全表更新拦截器。

额外依赖：

Spring Boot 2：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

Spring Boot 3：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
```

Spring Boot 4：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
</dependency>
```

MyBatis-Plus 3.5.9 及以上已将 SQL Parser 相关拦截器拆分为可选模块。如果需要分页或防全表更新/删除能力，还需引入与 MyBatis-Plus 版本一致的 JSQLParser 扩展；当前项目依赖管理使用 `3.5.14`，示例为：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser-4.9</artifactId>
    <version>3.5.14</version>
</dependency>
```

未引入该模块时，`pagination-enabled` 和 `block-attack-enabled` 虽然默认值为 `true`，但对应拦截器类不在 classpath 中，Velo 会安全跳过相应 Bean；乐观锁拦截器不依赖该模块。

关键配置：

```yaml
velo:
  mybatis-plus:
    enabled: true
    pagination-enabled: true
    optimistic-locker-enabled: true
    block-attack-enabled: true
```

说明：

- `velo.mybatis-plus.enabled` 默认开启
- 未引入 MyBatis-Plus 时，相关自动配置会安全跳过，不会因为可选依赖缺失导致应用启动失败
- 若容器里已经存在同类 `InnerInterceptor` Bean，starter 不会覆盖
- 默认会把当前容器中的 `InnerInterceptor` 汇总进 `MybatisPlusInterceptor`
- 用户分页拦截器即使将 `@Bean` 返回类型声明为 `InnerInterceptor`，Velo 也会按实际类型退让，只保留用户的分页 Bean 和唯一执行实例；建议声明具体返回类型，让 Spring 在注册阶段就能识别。
- Velo 内置拦截器默认按 `OptimisticLocker → BlockAttack → Pagination` 排列，使用低优先级值 `LOWEST_PRECEDENCE - 300/-200/-100`
- 用户可以在自定义 `InnerInterceptor` Bean 方法上使用 `@Order` 控制执行位置；通常值越小越先执行，未声明 `@Order` 时不承诺与 Velo 的严格相对顺序

### 10. Redis 自动配置

Velo 会基于已有 `RedisConnectionFactory` 和 `RedisSerializer<Object>` 补齐常用 `RedisTemplate`。

额外依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

关键配置：

```yaml
velo:
  redis:
    enabled: true
```

说明：

- `velo.redis.enabled` 默认开启
- 未引入 Spring Data Redis 时，Redis 自动配置会安全跳过，不会触发 Redis 连接配置或导致应用启动失败
- Boot 2 的 Redis 连接地址、密码、数据库使用 `spring.redis.*`；Boot 3 / 4 使用 `spring.data.redis.*`
- starter 会尝试创建：
  - `redisTemplate`
  - `stringObjectRedisTemplate`
- 序列化器优先复用容器中的 `RedisSerializer<Object>`，与 Velo 缓存共用该 Bean；默认 Redis 映射器独立于 HTTP JSON 映射器，Web 的全局 Jackson 增强不会自动影响 Redis 值格式
- 若容器没有 `RedisSerializer<Object>`，starter 使用 `RedisSerializer.json()` 作为回退；Boot 2/3 跟随对应 Spring Data Redis 的 Jackson 2 实现，Boot 4 使用 Jackson 3 实现。RedisTemplate 的可选依赖在多个候选且无法确定主候选时仍会回退默认并打 WARN；Velo 默认缓存配置对序列化器歧义直接报错，详见缓存说明
- Boot 4 项目若选择 Jackson 2，请自行提供 Jackson 2 的 `RedisSerializer<Object>` Bean，Velo 的缓存和 RedisTemplate 会共同复用该 Bean；Jackson 2/3 同时存在时不会仅依据类路径猜测用户意图，详细兼容边界见上面的缓存说明

---

## 开箱即用能力

这一节只放“无需你显式加注解即可生效”的能力。

### 1. Web MVC 日期时间绑定

只要项目引入了 `spring-boot-starter-web`，且 `velo.web.enabled=true`，Velo 就会自动注册 `WebMvcConfigurer`。

Controller 示例：

```java
@GetMapping("/orders")
public Object list(LocalDate date, LocalDateTime createTime, Date paidAt) {
    return null;
}
```

默认格式：

- `LocalDate` -> `yyyy-MM-dd`
- `LocalTime` -> `HH:mm:ss`
- `LocalDateTime` -> `yyyy-MM-dd HH:mm:ss`
- `Date` 默认输入兼容 `yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd`，输出使用 `yyyy-MM-dd HH:mm:ss`

这些格式由 `velo.date-time-format.*` 统一控制。

未显式指定格式时，`Date` 会先按 `velo.date-time-format.date-time` 解析，失败后再按
`velo.date-time-format.date` 解析；日期-only 输入会按配置时区转换为当天 `00:00:00`。
字段或参数上的 Spring `@DateTimeFormat`、Jackson `@JsonFormat` 等显式格式优先于 Starter 默认格式。
配置了非法日期模式、空日期模式或非法时区时，启动告警会提前提示，但对应转换器仍会在启动阶段失败；Starter 不会静默替换用户配置。

如果开启 `velo.banner.enabled=true`，横幅仅用于诊断，不应成为启动失败原因。配置对象被显式置空时，横幅会跳过自身输出或将对应能力显示为 `unavailable (config missing)`。

### 2. 全局 Spring Converter

除 Web MVC 参数绑定外，starter 还会默认注册全局 `String -> Date/Time` Converter Bean。

配置项：

```yaml
velo:
  spring-converter:
    date-time-enabled: true
```

说明：

- 默认值就是 `true`
- 关闭后，会同时影响全局 Converter 注册以及 Web MVC 日期时间 formatter 逻辑

### 3. Controller 调用日志

有 Web 环境时，Velo 默认开启 Controller 调用日志切面，并从 W3C `traceparent` 获取 traceId，缺失或非法时自动生成。指定纯 traceId 请求头的用法见上面的 Resolver Bean 示例。

配置项：

```yaml
velo:
  log:
    sources:
      controller:
        enabled: true
    defaults:
      max-payload-length: -1
  trace:
    enabled: true
```

说明：

- 默认开启
- 默认打印 Spring 绑定后的入参和返回结果；达到阈值打印独立慢调用日志，异常时打印独立 WARN 摘要
- 会过滤掉原始 query string，避免把敏感查询串直接打到日志中
- 默认 `max-payload-length=-1`，完整输出；`0` 不序列化或输出载荷日志，其他值启动时报错
- 预期的超大对象应通过注解或配置关闭对应载荷日志
- 如需关闭 Controller 自动日志，设置 `velo.log.sources.controller.enabled=false`

### 4. Feign 调用日志

如果项目中存在 `@FeignClient`，Velo 会按统一调用日志格式记录 Feign 调用，并自动透传 Resolver 返回的协议字段；默认发送 W3C `traceparent/tracestate`。

配置项：

```yaml
velo:
  feign:
    enabled: true
  log:
    sources:
      feign:
        enabled: true
    defaults:
      max-payload-length: -1
  trace:
    enabled: true
    feign-propagation-enabled: true
```

说明：

- 默认开启
- 前缀为 `[feign] [方法名() HTTP方法 接口路径]`，例如 `[feign] [remote() GET /log/remote]`；类名由日志框架输出，不重复打印 client 名或 contextId
- 接口路径取 Spring MVC 映射模板，例如 `/users/{id}`，不展开路径变量或拼接查询参数；无法解析映射时省略缺失部分，保留方法名
- 默认打印入参和返回结果，慢调用和异常各输出独立日志
- 日志格式和 Controller、`@InvokeLog` 保持一致，便于联调排查
- 请求头和响应头默认不采集，可通过独立配置或注解启用
- 默认 `max-payload-length=-1`，完整输出；`0` 不序列化或输出载荷日志，其他值启动时报错
- 预期的超大对象应通过注解或配置关闭对应载荷日志
- 如需关闭 Feign 自动日志，设置 `velo.log.sources.feign.enabled=false`

### 5. CORS

Velo 提供一个偏“快速放开”的 CORS 开关，默认关闭。

配置项：

```yaml
velo:
  web:
    cors:
      enabled: true
      allowed-origin-patterns: "*"
```

说明：

- `velo.web.cors.enabled` 默认 `false`
- 开启后注册 `/**` 全局跨域规则
- 默认允许 `GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
- `allowedOriginPatterns("*")`，可通过 `velo.web.cors.allowed-origin-patterns` 覆盖
- `velo.web.cors.allow-credentials` 默认 `false`；需要 Cookie/Session 跨域时显式设为 `true`，并配置明确的允许来源
- `maxAge(3600)`
- 旧配置 `velo.web.allow-cors` 已移除，不再兼容；请统一使用 `velo.web.cors.enabled`

### 6. 客户端 IP 解析

`WebUtils.getRequestIp()` 会按 `X-Forwarded-For`、`Proxy-Client-IP`、`WL-Proxy-Client-IP`、`X-Real-IP` 的顺序读取第一个有效的非空值；没有代理头时回退到请求的直连地址。

该行为适用于“客户端 → Nginx → 网关 → 服务”的常见部署链路，但前提是公网入口的 Nginx 和网关已经清洗或覆盖客户端自行携带的 `Forwarded`、`X-Forwarded-*` 等转发头，只把受控代理生成的值传给下游。入口代理如果直接使用会保留外部值的追加配置，客户端伪造的第一个地址可能继续留在链路中。

该方法当前主要用于 Controller 调用日志和排查，不应在未配置可信代理边界时用于认证、授权、限流、黑名单或其他安全判断。若业务需要安全可信的来源 IP，应由网关或 Web 容器先按可信代理配置解析，再使用其标准化后的远端地址。

---

## Web 异常处理扩展

Servlet MVC 使用 `io.github.luminion.velo.web.exception` 下的 `VeloWebExceptionHandler` 和
`VeloValidationWebExceptionHandler`。这些都是可复用的异常处理基类，
不会自动注册为 Spring 组件。

应用继承或实现具体异常处理类时，需要在具体类上显式添加 `@RestControllerAdvice`，并通过构造函数提供失败响应和系统异常响应的转换函数。这样可以避免用户扫描 `io.github.luminion` 等宽范围包时，Spring 误尝试实例化缺少构造函数依赖的泛型基类。

---

## 常用关闭开关

如果你希望尽量减少自动影响，优先使用：

```yaml
velo:
  opinionated: false
```

如果你只想保留部分能力，也可以按配置域逐项关闭：

```yaml
velo:
  feign:
    enabled: false
  web:
    enabled: false
  xss:
    web-enabled: false
    jackson-enabled: false
  log:
    enabled: false
    sources:
      controller:
        enabled: false
      feign:
        enabled: false
  trace:
    enabled: false
  jackson:
    enabled: false
  redis:
    enabled: false
  cache:
    enabled: false
  mybatis-plus:
    enabled: false
  idempotent:
    enabled: false
  rate-limit:
    enabled: false
  lock:
    enabled: false
  spring-converter:
    date-time-enabled: false
```

---

## 故障排查 FAQ

### Q1：`@Idempotent` / `@RateLimit` / `@Lock` 不生效？

按以下顺序排查：

- 方法是否被 Spring 代理：`private`、`final`、`static` 方法以及类内部自调用（`this.method()`）都无法被 AOP 拦截，需通过注入的代理对象调用
- 对应能力是否开启：确认未被 `velo.idempotent.enabled=false` 等关闭；若使用 `velo.opinionated=false` 无侵入模式，需按需显式开启对应能力（注意：这三类注解仍可用，但需对应后端依赖存在）
- 后端依赖是否就绪：幂等的 `AUTO` 顺序为 `REDISSON -> REDIS -> CAFFEINE`，锁为 `REDISSON -> REDIS -> JDK`，限流为 `REDISSON -> REDIS -> GUAVA`；若期望用分布式后端却走了本地实现，检查 classpath 与对应 Bean 配置
- 开启调试日志观察：

```yaml
logging:
  level:
    io.github.luminion.velo: DEBUG
```

启动日志中也会有一条 INFO，显示各能力最终选用的后端，例如 `Idempotent enabled, backend handler: RedisIdempotentHandler`。

### Q2：Redis 连接失败时会怎样？

- `backend=AUTO` 只在启动配置阶段按类路径和 Bean 条件选择后端。Redis/Redisson 条件不满足时，锁使用 JDK，幂等尝试 Caffeine，限流尝试 Guava；后两者没有可用实现时调用报错。不会测试 Redis 连接，也不会在运行期间自动切换后端。
- 已选中 Redis/Redisson 后，连接失败可能阻止启动或使相应调用失败、抛异常，不会转为本地幂等、限流或锁。Redis Bean 已存在但服务不可达时，`AUTO` 也可能选中该后端。
- 显式配置的后端缺少依赖或 Bean，或者不属于该功能支持的实现时，启动报错；提供自定义处理器可以覆盖选择。

生产环境建议显式指定分布式后端，并配合健康检查确保 Redis 可用；业务侧应决定连接故障时如何处理请求。

### Q3：如何调试 SpEL `key` 表达式？

- 确认已开启编译参数 `-parameters`，否则 `#userId` 这类按参数名引用无法解析，只能用 `#p0`、`#p1`
- 表达式解析为空值、空白字符串或复杂对象会抛出异常；三种注解行为一致
- 可单独用 `SpelExpressionParser` 写单测验证表达式取值是否符合预期

### Q4：限流被拒绝后多久可以重试？

由具体后端决定。Guava 使用原生速率补充机制，Redis 在当前一秒固定窗口到期后重置，Redisson 使用原生的一秒额度恢复规则。被拒绝后立即重试可能仍失败，业务侧应按所选后端退避，Starter 不承诺统一的重试时间。Redis 固定窗口允许边界两侧的额度连续使用。

### Q5：异常信息能否做国际化？

可以。国际化是可选能力，不会改变注解默认的中文提示。注解的 `message` 写成 `{i18n.key}` 形式时，Velo 会通过 Spring 应用上下文的主 `MessageSource` 解析；普通文本则原样输出。

Starter 内置了 `velo/messages.properties` 和 `velo/messages_en.properties`。如需使用内置资源，需要显式配置消息资源路径：

```yaml
spring:
  messages:
    basename: velo/messages
```

然后在注解中引用消息 key：

```java
@Idempotent(message = "{velo.idempotent.rejected}")
```

如果应用自定义了消息源，应将其作为名为 `messageSource` 的主消息源；存在多个不同名称的 `MessageSource` 时，Starter 不会自动选择其中一个。未配置消息资源或找不到 key 时，会回退为 key 文本，不会抛出异常。未使用 `{key}` 形式的项目不受影响。

### Q6：缓存大量 key 同时过期（缓存雪崩）？

开启 TTL 抖动：`velo.cache.ttl-jitter-percentage=10`。抖动按 key 在写入时独立计算，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类问题。
