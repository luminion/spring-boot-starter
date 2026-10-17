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
- 空值缓存、前缀开关及统计使用 `spring.cache.redis.cache-null-values`、`use-key-prefix`、`enable-statistics`；`spring.cache.cache-names` 用于预创建缓存；默认允许按 Spring 原生规则动态创建缓存
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
- Velo 默认缓存配置优先复用容器中的 `RedisSerializer<Object>`；没有用户候选时自动注册 `veloRedisSerializer`（`RedisSerializer<Object>`）Bean，提供带类型信息的原生通用 JSON 序列化器。存在多个候选且 Spring 无法选出唯一主候选时，直接启动报错，避免悄然切换缓存读写格式。用户提供完整缓存配置时，序列化器由该配置明确指定，不再自动选取
- Redis JSON 使用独立映射器，HTTP 数字、脱敏、`JsonEncode` 等输出规则不会改变存储数据；关闭 `velo.jackson.enabled` 也不影响 Redis JSON 的日期支持。普通 RedisTemplate 与 Spring Cache 默认共用带类型信息的 Generic Jackson JSON，支持 DTO 和集合自动恢复。两者都可复用用户 `RedisSerializer<Object>`；用户自定义 RedisTemplate 的设置不会自动传给 CacheManager
- Boot 2 / 3 使用 Jackson 2：普通 RedisTemplate 与 Spring Cache 默认均使用 `GenericJackson2JsonRedisSerializer`，保留类型信息
- Boot 4 使用 Jackson 3：普通 RedisTemplate 与 Spring Cache 默认均使用 `GenericJacksonJsonRedisSerializer`。即使同时存在 Jackson 2，也不会自行切换默认实现
- Boot 4 如需让 Redis 使用 Jackson 2，应引入官方 `spring-boot-jackson2` 及 Jackson 2 依赖，并显式注册一个 `RedisSerializer<Object>` Bean，例如 `GenericJackson2JsonRedisSerializer`；Spring Data Redis 4.x 仍保留该类用于兼容或迁移旧数据，但已标记为后续移除，不作为 Boot 4 默认实现
- Jackson 2 与 Jackson 3 的 Redis JSON 输出可能存在差异；从 Boot 2 / 3 切换到 Boot 4 时，应先规划旧数据读取、迁移或 key 空间隔离，不要默认认为历史值可以无缝混读
- **Redis 信任约定**：默认将 Redis 及其写入内容视为可信。普通 RedisTemplate 与 Spring Cache 为支持 `Object` / POJO 回读，会写入并按类型元数据（`@class`）反序列化；Velo 不额外维护业务类型白名单。业务如需限制可读取的类型，可通过原生缓存配置提供定向 serializer，或自行注册基于 `BasicPolymorphicTypeValidator` 的 `RedisSerializer<Object>` Bean，Velo 会复用该 Bean

默认缓存格式与类型恢复：

- 开启 Spring Cache 并满足 Redis 自动配置条件后，无需按缓存名称登记 Class 或泛型；DTO、集合等常见值通过 Generic Jackson 的类型元数据自动恢复
- 默认缓存值使用 JSON，允许包含 `@class` 或类型包装；不会扫描方法返回类型，也不会对缓存名称建立业务类型白名单
- `spring.cache.cache-names`、`velo.cache.ttl` 和运行时新缓存名称均遵循 Spring 原生流程，不要求附加类型配置
- null 继续走 Spring 原生空值缓存机制，原始空值标记并非 JSON
- 用户提供 `RedisSerializer<Object>`、完整 `RedisCacheConfiguration` 或原生 customizer 时，沿用用户指定的序列化协议
- 升级自原定向 JSON 缓存时，应清理对应旧条目或更换缓存业务前缀，再由应用回填；没有类型元数据的旧 DTO JSON 不能直接按新的通用格式还原

扩展组件的接入方式：

| 组件 | 接入方式 |
|---|---|
| `RedisConnectionFactory` | Boot 原生使用它创建缓存管理器；多个候选需明确主候选 |
| `RedisCacheConfiguration` | Boot 原生采用其默认配置；Velo 的默认配置 Bean 退让 |
| `RedisCacheManagerBuilderCustomizer` | Boot 按顺序调用，可设置按名称 TTL、writer、统计等 |
| `CacheManagerCustomizer<RedisCacheManager>` | Boot 在构建后、初始化前调用，可调整事务感知等 |
| `RedisCacheTimeMapProvider` | Velo 优先采用用户 Bean；按名称 TTL 在管理器创建时应用，运行时修改原 Map 不会刷新已有缓存配置 |
| `RedisSerializer<Object>` | 默认注册 `veloRedisSerializer` Bean，供 RedisTemplate 和 Spring Cache 共用；用户通用 Bean 存在时退让，多个候选须指定唯一 `@Primary` |
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
- 内置日期转换器用于全局替换 Excel 库的默认转换器，导出时直接使用上述全局格式，不合并 Excel 字段上的 `@DateTimeFormat`。需要沿用 Excel 库的字段格式行为时，请自行关闭相应的 `velo.excel.converters.date-enabled`、`local-date-time-enabled`、`local-date-enabled`、`local-time-enabled`，或设置 `velo.excel.converters.enabled=false` 关闭全部自动注册；也可为业务 Reader/Writer 显式提供自定义 converter
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
    message: "您的请求已提交，请勿重复操作"
```

使用示例：

```java
import io.github.luminion.velo.idempotent.annotation.Idempotent;

@Idempotent(value = "#userId", ttl = 3000)
public void submitOrder(Long userId) {
    // ...
}
```

说明：

- `backend` 可选 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`
- `AUTO` 模式下按自动配置顺序选择后端：`REDISSON -> REDIS -> CAFFEINE`
- Caffeine 使用原生缓存过期；Redis 使用 `SET NX` + TTL 和失败时的 token 比对删除；Redisson 使用原生 `RBucket`，Velo 不额外维护本地幂等缓存或清理任务
- 没有可用实现时，应用仍可启动，调用带注解的方法会在业务执行前报错；显式指定不可用或不支持的后端时启动报错。自定义 `IdempotentHandler` 优先
- 配置项 `velo.idempotent.prefix` 默认 `idempotent:`，是功能的全局键前缀；注解的 `prefix` 是资源范围，默认空，使用方法指纹
- `ttl` 单位固定为毫秒，默认 `3000`（3 秒）
- TTL 从首次进入时开始计算，成功结束不重置；默认 3 秒主要用于防连点，长任务应配置更长窗口。TTL 到期后同 key 可再次进入，不保证业务永久只执行一次
- `value` 专门表示 SpEL 表达式，`@Idempotent("#userId")` 等价于 `@Idempotent(value = "#userId")`；不再提供 `key` 属性
- 当前方法调用同步抛出异常时，会清除本次幂等记录以允许重试；清除采用 token 比对，只删除本次请求写入的记录，不会误删并发请求在窗口内刚写入的新记录
- 幂等检查作用于注解调用点的 TTL 窗口，不管理异步任务生命周期。方法返回 `Future` / `CompletionStage`，或在方法内启动异步任务时，不等待、不监听后续完成或失败；返回异步对象视为当前方法正常返回，幂等记录仍保留至 TTL 到期。异步任务后续失败不会触发记录清理
- 未指定注解 `prefix` 时，不同方法或重载方法按方法指纹隔离；显式指定相同 `prefix` 后，相同 SpEL 结果可在不同方法、不同类之间共享防重复窗口，例如 `@Idempotent(prefix = "order-submit", value = "#requestId")`
- 共享防重复窗口适合多个入口代表同一次提交的场景，应使用相同 TTL、请求标识和业务含义。不同操作仅操作同一订单时，应使用不同防重复范围；需要互斥时使用共享资源锁。TTL 由首次成功写入记录的调用决定，后续重复调用不重置

> `value` 为空时不拼接参数，所有调用者共享该范围的防重复窗口。`prefix` 也为空时使用方法指纹，starter 会打印 WARN 提醒；显式 `prefix` 已声明共享资源意图，不再告警。若要按用户或请求分别防重复，请指定 `value`。
>
> ```java
> // 方法级窗口：userId=1 提交后，3 秒内 userId=2 也会被拦截
> @Idempotent(ttl = 3000)
> public void submitOrder(Long userId) { }
>
> // 每个用户独立防重复
> @Idempotent(value = "#userId", ttl = 3000)
> public void submitOrder(Long userId) { }
> ```
>
> 若需要「同一时刻只能执行一次」的互斥语义（如系统级初始化），使用 `@Lock`；防重复窗口限制的是一段时间内再次进入。

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
    message: "当前访问人数较多，请稍后再试"
```

使用示例：

```java
import io.github.luminion.velo.ratelimit.annotation.RateLimit;

@RateLimit(qps = 10, value = "#userId")
public Object query(Long userId) {
    return null;
}
```

说明：

- `qps` 表示每秒请求速率，默认 `50`，必须为正整数
- `value` 专门表示 SpEL 表达式，不再与 `qps` 互为别名，也不再提供 `key` 属性；仅指定速率时使用 `@RateLimit(qps = 10)`
- `backend` 支持 `AUTO/REDISSON/REDIS/GUAVA`，`AUTO` 默认顺序为 `REDISSON -> REDIS -> GUAVA`
- Guava 使用原生 `RateLimiter`，同 key 共享限流器，空闲五分钟后由 Guava 原生缓存过期；额度补充和空闲突发遵循 Guava 行为，仅保证单 JVM 内有效
- Redis 使用 Lua 原子计数，首次请求开始固定一秒窗口，后续请求不延长 TTL；过期由 Redis 处理，无本地缓存或清理任务
- **Redis 允许窗口边界突发**：上一窗口末尾和下一窗口开头可连续消耗各自额度，短时间内最多接近两倍 `qps`；不能当作“任意滚动一秒最多 `qps` 次”的严格限制
- Redisson 直接使用 `RRateLimiter`，配置为每秒 `qps` 次，额度按 Redisson 原生规则恢复；只保证各后端声明的速率语义，不统一额度归还节奏
- 多实例共享配额请使用 Redis 或 Redisson；Velo 不统一不同后端的额度归还节奏
- 没有可用实现时，应用仍可启动，调用带注解的方法会在业务执行前报错；显式指定不可用或不支持的后端时启动报错。自定义 `RateLimitHandler` 优先

关于分桶范围：

- 注解 `prefix` 为空：按方法指纹隔离；显式 `prefix` 替代方法指纹，可在不同方法、不同类之间共享额度
- `value` 为空：不拼接参数，范围内的所有调用者共享配额；`value` 非空：按 SpEL 结果进一步分桶，每桶独立计算配额
- 共享桶必须使用相同 `qps`，避免不同入口交替改变 Guava / Redisson 的速率配置；这是业务声明约束，starter 不跨方法注册或校验配额配置
- 配置项 `velo.rate-limit.prefix` 默认 `rateLimit:`，是功能的全局键前缀，仍保留在最终键中

| 写法 | 实际行为 |
| --- | --- |
| `@RateLimit(qps = 10)` | 该方法所有调用共享每秒 10 次的速率 |
| `@RateLimit(value = "#userId", qps = 10)` | 该方法每个 userId 独立按每秒 10 次限流 |
| `@RateLimit(prefix = "report", value = "#userId", qps = 10)` | 所有使用该范围的入口，每个 userId 共享每秒 10 次的速率 |
| `@RateLimit(prefix = "report", qps = 10)` | 所有使用该范围的入口和调用者共享每秒 10 次的速率 |

> 如果期望每个用户 / 每个资源独立限流，请通过 `value` 指定对应业务标识。限流限制的是调用速率，不提供防重复提交或互斥。

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
    message: "系统繁忙，请稍后再试"
    redis-ttl-seconds: 60
```

使用示例：

```java
import io.github.luminion.velo.lock.annotation.Lock;

@Lock(value = "#orderId")
public void pay(Long orderId) {
    // ...
}
```

说明：

- `backend` 支持 `AUTO`、`REDISSON`、`REDIS`、`JDK`
- `AUTO` 默认顺序为 `REDISSON -> REDIS -> JDK`；JDK 无需额外依赖，自定义 `LockHandler` 优先
- 三种后端均只尝试一次，拿不到锁立即失败；不提供等待参数或自动重试
- `value` 专门表示 SpEL 表达式，`@Lock("#orderId")` 等价于 `@Lock(value = "#orderId")`；不再提供 `key` 属性
- 自定义 `LockHandler` 实现 `tryLock(String key)` 和 `unlock(String key)`；成功获取后必须在原线程配对释放。Redis / Redisson 获取过程的连接异常向上抛出，与锁被占用返回 `false` 区分
- 加锁范围是经过 AOP 代理的当前方法调用：方法正常返回或同步抛出异常时，释放本次获取的锁。返回 `Future` / `CompletionStage`、方法内启动的异步任务均不等待、不监听，不延长持锁时间，也不转移锁的线程归属
- 当前调用已经参与外层事务时，仍在当前方法返回时释放锁，不额外等待外层事务提交或回滚。需要互斥覆盖完整业务过程时，应将锁放在实际包围该过程的代理调用点，并安排好事务边界
- 注解和 `LockHandler` 不提供租期参数；锁的生命周期由后端管理
- Redis 简单锁的固定 TTL 由构造函数指定秒数，自动配置使用 `velo.lock.redis-ttl-seconds`，默认 `60`，必须为正数；重入不重置 TTL，不启动任何自写续期线程
- Redis TTL 到期后自动释放，不中断正在执行的业务，也不保证到期后的互斥；业务必须在 TTL 内完成。耗时不确定的调用优先使用 Redisson
- Redisson 使用不指定租期的原生获取接口，由自身看门狗续期；超时时间使用 Redisson 的 `lockWatchdogTimeout` 配置
- Redis / Redisson 用于分布式场景；JDK 使用原生 `ReentrantLock`，只保证单 JVM 内互斥，不超时释放正在使用的锁
- JDK 只登记正在获取或持有的锁，获取失败撤销引用，最后一个使用者离开后立即删除；不能每次创建独立锁，也不能按缓存 TTL 淘汰活跃锁，否则会破坏互斥
- `REDIS` 后端支持同线程可重入（同一线程重复加同一把锁不会自锁死），最外层释放时才真正删除 Redis 锁
- Redis 锁只在当前线程持有期间记录 token 和重入次数，最外层释放后立即清理；Redis 幂等和 Redisson 三种后端都不额外维护本地键缓存
- 注解 `prefix` 与 `value` 都为空时使用方法级锁（`全限定类名#方法名(参数类型...)`），所有参数、调用者共享一把锁，不告警；只指定 `value` 时仍按方法隔离，再按表达式结果区分资源
- 显式注解 `prefix` 替代方法指纹：支付与取消等不同方法使用 `@Lock(prefix = "order", value = "#orderId")` 时，相同订单共享 `lock:order:123` 这把锁；`@Lock(prefix = "order")` 则使所有这些调用共享 `lock:order`，不拼接参数
- 配置项 `velo.lock.prefix` 默认 `lock:`，是功能的全局键前缀，注解的资源前缀不替代它。跨应用共享需要使用相同键配置与同一分布式后端；JDK 后端的共享范围仅限同一处理器所在 JVM

### 6. 并发控制组合顺序

这三项功能不创建 Velo 自己的定时清理任务、续期任务、轮询或后台线程。Guava / Caffeine 的过期和维护交给原生缓存，Redis 的过期交给服务端。JDK / Redisson 锁调用原生无等待的 `tryLock()`，Redis 锁只发送一次 `SET NX`。Redisson 看门狗和 Redis 客户端网络线程仍由第三方管理；Caffeine 原生维护可能使用公共线程池，因此不代表整个应用没有后台线程。

JDK 锁和 Redis 锁的本地记录只保存活跃调用，不随历史业务 key 累积；切面在业务成功或抛出异常时均释放锁。Guava 限流与 Caffeine 幂等缓存依赖原生过期机制，过期记录不再生效，但物理清理不保证发生在到期瞬间。两者未设置容量淘汰上限，避免淘汰仍有效的记录而放过限流或重复提交；大量不同 key 在有效期内仍会占用相应内存，需要按业务控制 key 数量和 TTL。

注解请标在具体实现方法上；`@RateLimit` 也可以标在具体实现类上，作用于该类声明的方法。方法上的注解整份覆盖类上声明，包括 `prefix`、`value` 和 `qps`，不逐项合并；方法注解未指定 `prefix` 时恢复方法指纹范围。Starter 不额外搜索父接口或父类上的注解；继承方法是否被拦截沿用 Spring AOP 原生行为，不作为额外继承能力承诺。同类内部直接调用绕过代理时，切面不会生效。

三种注解统一使用 `prefix`（固定资源前缀）与 `value`（SpEL 后缀），两者不互为别名，也不保留 `key` 属性。选择 `prefix` 这个名称，是因为它只指定键的固定部分，最终键仍包含功能前缀和可选的动态后缀。

| 属性 | 统一含义 |
| --- | --- |
| `prefix` | 固定资源范围，不解析 SpEL；默认空，使用实际用户类全名及完整方法签名 |
| `value` | SpEL 表达式；默认空，不拼接参数；非空时追加 `:表达式结果` |
| `message` | 被拒绝时的提示信息；默认空，继承对应功能的全局消息配置；非空白时覆盖配置 |
| `ttl`（仅 Idempotent） | 防重复窗口，单位毫秒，默认 3000 |
| `qps`（仅 RateLimit） | 每秒请求速率，默认 50 |

最终键规则为：`功能全局前缀:资源前缀或方法指纹[:SpEL结果]`。默认保留完整类名和重载签名，避免不同方法碰撞；显式资源前缀用于主动共享，需由业务统一命名。各功能仍按默认 `lock:`、`idempotent:`、`rateLimit:` 前缀隔离，跨功能的相同注解 prefix 不会共用记录或额度。资源范围命名应体现用途：锁可用 `order` 覆盖支付和取消，防重复可用 `order-submit` 标识同一次提交，限流可用 `order-api` 统一多个入口的预算。

三个注解的 `message` 默认都是空串；空串或纯空白表示继承 `velo.lock.message`、`velo.idempotent.message`、`velo.rate-limit.message`，非空白文案覆盖相应全局配置。配置未指定时沿用上方 YAML 中的中文默认文案；配置显式写成空串或纯空白时原样保留，不校验、不再回退到框架默认文案。

默认文案仅在配置属性中维护，不在注解或切面中重复定义。普通文本原样保留，注解与全局配置都支持 `{i18n.key}`，只在请求被拒绝时按当前语言解析。

旧代码中显式填写的中文文案仍作为局部覆盖；读取注解默认值的自定义切面需同步处理空串继承规则。手动构造切面时，原构造函数沿用框架默认文案，新增构造函数可传入自定义默认文案。

注解资源前缀裁剪首尾空白和末尾冒号，例如 `" order: "` 规范为 `"order"`；空白前缀等同未指定，只有冒号的非空前缀会报错。`value` 总是 SpEL，固定字符串写成 `value = "'all'"`，固定资源范围直接写 `prefix = "order"`。表达式结果支持字符串、数字、布尔值、字符、UUID 和枚举（使用 `name()`）；空值、空白或数组、集合、对象等复杂结果会报错，请明确取业务 ID。表达式结果字符串原样保留，不裁剪首尾空格。解析器只缓存固定声明的表达式和方法标识，每次调用创建独立求值上下文。

升级迁移：旧 `@Lock(key = "#id")` / `@Idempotent(key = "#id")` 改为 `value = "#id"`；旧 `@RateLimit(10)` 改为 `@RateLimit(qps = 10)`，旧 `@RateLimit(value = 10, key = "#id")` 改为 `@RateLimit(qps = 10, value = "#id")`。这是注解属性的不兼容调整，应重新编译业务代码。未指定注解 `prefix` 时，迁移后的默认键格式保持不变。

自定义 `Fingerprinter` 的实现方法增加了资源前缀参数：`resolveMethodFingerprint(target, method, args, prefix, expression)`，须遵守相同范围规则。原四参数调用仍可使用，等价于空资源前缀；原四参数 lambda 实现需迁移为五参数。

当 `@Idempotent`、`@RateLimit`、`@Lock` 同时作用于同一个方法时，starter 内置顺序为：

```text
@Idempotent -> @RateLimit -> @Lock -> 业务方法
```

这意味着重复提交会最先被拒绝，不消耗限流令牌，也不会尝试加锁；限流失败时不会尝试加锁；只有真正允许执行业务的方法调用才会获取锁。

这些切面使用接近 `Ordered.LOWEST_PRECEDENCE` 的低优先级顺序值，并在三者之间保留较大间隔，便于业务自定义切面通过 `@Order` 插入到合适位置。

### 7. 日志

功能注解可独立接入普通方法日志，也可组成业务自定义注解。功能注解位于 `io.github.luminion.velo.log.annotation`；唯一内置组合 `InvokeLog` 位于 `io.github.luminion.velo.log.annotation.composed`。Controller、Feign 默认都使用这个组合，用户可以通过配置替换为自己的组合，无需再提供内容相同的 ControllerLog / FeignLog。

日志包按职责组织，扩展接口集中在 `log.spi`，数据模型与核心协调逻辑分别维护：

```text
log
├── annotation          功能注解与排除注解
│   └── composed        内置组合注解
├── aspect              普通方法与任务入口
├── config              自动配置
│   └── condition       入口启用条件
├── core                调用协调、注解检索与策略解析
├── model               调用、事件、策略、记录和枚举
├── spi                 功能处理、格式化与输出扩展接口
├── handler             默认功能处理器
├── formatter           默认公共块格式化器
├── writer              默认日志输出器
└── support             调用适配、载荷处理和安全输出工具
```

例如，等级枚举为 `log.model.LogPolicyLevel`，处理器扩展接口为 `log.spi.InvocationLogHandler`，默认耗时处理器为 `log.handler.CostLogHandler`。Controller、Feign 的协议适配器继续位于各自的 Web、Feign 包中，共用这些日志接口。

| 功能注解 | 主要默认值 |
| --- | --- |
| `EntryArgs` / `ExitArgs` / `ExitResult` | `level=LogPolicyLevel.INFO`、`maxLength=-1` |
| `CostLog` | `level=LogPolicyLevel.INFO` |
| `SlowLog` | `level=LogPolicyLevel.WARN`、`threshold=1000` |
| `ErrorLog` | `level=LogPolicyLevel.WARN`、`maxLength=-1` |
| `RequestHeadersLog` / `ResponseHeadersLog` | `level=LogPolicyLevel.INFO`、`allowlist={}`、`maxLength=-1` |

等级使用独立的 `io.github.luminion.velo.log.model.LogPolicyLevel`，只包含 TRACE、DEBUG、INFO、WARN、ERROR、OFF，不依赖第三方等级类型。`level` 同时控制等级和启闭，不再提供 `enabled` 属性；例如 `@CostLog(level = LogPolicyLevel.OFF)` 显式关闭耗时日志，直接标注 `@CostLog` 使用 INFO 开启。注解提供完整默认值，没有 INHERIT 状态：选中的同项声明整体生效，包含省略属性的默认值。

按功能选择声明：**方法 > 类 > 自动入口默认组合**；同一声明位置直接标注优先于组合中的声明，其次选元注解距离较近者。同距离保留 Spring 遍历遇到的首项，应避免冲突；不逐属性拼接多个候选。例如类或组合指定 `@SlowLog(threshold=200)`，方法直接标注 `@SlowLog(level=LogPolicyLevel.DEBUG)`，最终使用 DEBUG / 1000ms。方法只标注 CostLog 不会清空类上的其他功能。

单个普通功能注解只选择自己；`@InvokeLog` 则组合全部八项功能：EntryArgs、ExitResult、CostLog、SlowLog、ErrorLog 默认开，ExitArgs、请求头、响应头默认关。InvokeLog 按普通组合解析，无特殊配置覆盖规则。

```java
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.SlowLog;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import io.github.luminion.velo.log.model.LogPolicyLevel;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@EntryArgs(maxLength = 2048)
@ExitResult(maxLength = 4096)
@CostLog(level = LogPolicyLevel.DEBUG)
@SlowLog(threshold = 200)
public @interface BusinessLog {
}
```

类或方法标注 `@BusinessLog` 即生效，无需额外添加 InvokeLog。支持多层组合及 Spring `@AliasFor` 属性映射。通过配置选择的组合必须是运行时注解、包含日志功能且所有自有属性都有默认值；框架合成其默认实例并解析，无法通过配置传递自定义属性值。业务可定义另一个组合并切换注解类。

```yaml
velo:
  log:
    enabled: true
    sources:
      controller:
        enabled: true
        default-annotation: com.example.BusinessLog
      feign:
        enabled: true
        default-annotation: io.github.luminion.velo.log.annotation.composed.InvokeLog
      annotation:
        enabled: true
  trace:
    enabled: true
    mdc-key: traceId
    response-header-enabled: true
    response-header-name: X-Trace-Id
    feign-propagation-enabled: true
    logging-pattern-enabled: true
```

配置只保留日志总开关、来源开关和自动入口默认组合类型，不再配置具体功能的等级、启闭、阈值、长度或白名单。Scheduled、XXL-Job 继续自动接入，默认选择 CostLog / SlowLog / ErrorLog，也支持 `sources.scheduled.default-annotation` 和 `sources.xxl-job.default-annotation`。普通 annotation 入口仅使用显式声明，不提供默认组合配置。

Controller、Feign、普通注解和任务入口共用同步 `InvocationLogEngine`。入口识别调用并提供公共信息；协调器负责统一计时和输出阶段；功能处理器生成各自内容；输出器拼接公共格式并写入日志。各项记录仍单独一行：

```text
[controller] [find()] [ip=127.0.0.1 method=GET route=/users/{id}] ==> entryArgs={"id":1}
[controller] [find()] [ip=127.0.0.1 method=GET route=/users/{id}] <== exitResult={"name":"Tom"}
[controller] [find()] [ip=127.0.0.1 method=GET route=/users/{id}] <== cost=12ms
[feign] [remote()] [client=users contextId=usersRead method=GET route=/users/{id}] <== cost=24ms
[annotation] [calculate()] <== slow={cost=1200ms, threshold=1000ms}
[scheduled] [run()] <== cost=5ms
```

固定结构是 `[来源] [方法名] [可选公共块] 方向 功能名=内容`。功能标签为 entryArgs、exitArgs、exitResult、requestHeaders、responseHeaders、cost、slow、error，不重复打印注解类名。来源固定表达入口类别，自定义组合仍归 annotation；Controller / Feign 上的功能声明调整策略，来源及协议信息归属不变，不重复接入。traceId 由 MDC 和日志框架的日志格式显示，正文不重复打印。

IP、HTTP 方法、路径、Feign 客户端及实例信息不提供对应注解，由各入口采集并提供。公共块按入口统一定制。Feign contextId 是客户端配置身份；实际实例地址属于运行时数据，只有底层提供且能够解析时才显示，入口日志可能尚无该字段。框架不为补齐公共块延迟入口日志。请求头、响应头仅由 Controller / Feign 提供；供应器缺失或返回 null 时静默跳过，空集合正常输出，采集失败输出 serialization-failed。

自定义单项处理器：声明 `InvocationLogHandler` Bean，`getFeature()` 指定替换的功能，`render(event, policy, formatter)` 返回功能内容，返回 null 跳过。也可以继承默认实现，例如 `CostLogHandler`。每项最多一个业务处理器，重复注册会在启动时报错；其余默认处理器继续使用。处理器不直接输出日志。CostLog 和 SlowLog 共用协调器的一次计时，默认输出固定毫秒摘要，不经过对象 formatter。

自定义公共块：声明 `InvocationLogContextFormatter` Bean，`getSource()` 选择入口，`format(invocation, context)` 返回不含外层方括号的公共块。每个入口最多一个业务实现；格式化器只处理公共块，使用当前阶段的不可变上下文快照。整体布局和输出目的地通过替换 `InvocationLogWriter` Bean 定制；对象转换通过 `LogValueFormatter` Bean 定制。扩展点的运行时异常被隔离，业务返回值或异常保持原样。

例如，在业务的 `@Configuration` 类中保留 Controller 默认的 IP、HTTP 方法和路由字段，再追加应用标识：

```java
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.formatter.DefaultInvocationLogContextFormatter;
import org.springframework.context.annotation.Bean;

@Bean
InvocationLogContextFormatter controllerLogContextFormatter() {
    DefaultInvocationLogContextFormatter defaults =
            new DefaultInvocationLogContextFormatter(InvocationLogSource.CONTROLLER);
    return new InvocationLogContextFormatter() {
        @Override
        public InvocationLogSource getSource() {
            return InvocationLogSource.CONTROLLER;
        }

        @Override
        public String format(LogInvocation invocation, InvocationLogContext context) {
            String common = defaults.format(invocation, context);
            return common.isEmpty() ? "app=orders" : common + " app=orders";
        }
    };
}
```

Controller 的每条功能日志都会带上这段公共块，Feign 和 annotation 来源继续使用各自的默认格式。定制其他入口时，将示例中两处来源都改为 `FEIGN` 或 `ANNOTATION`。格式化器返回 null 或空字符串时，默认输出器省略公共块，功能正文仍输出；普通注解入口可能没有协议上下文，需允许 `context` 为 null。自定义 writer 的 `isEnabled` 是正文和公共块生成前的等级预检；需要完整内容的过滤放在 `write` 中。

正文长度在 EntryArgs、ExitArgs、ExitResult、ErrorLog 和两个头注解上配置：`maxLength=-1` 完整输出，`0` 跳过本项并避免格式化，正数限制最终单行正文长度，不改变其他功能或公共块。阈值 `0` 表示所有调用满足慢调用条件，负数无效；CostLog 不受阈值影响，关闭两者中的一个不影响另一个。

`@LogIgnore` 排除所在类或方法的全部调用日志，方法功能注解不能越过类级 Ignore。`velo.log.enabled=false` 或来源 `enabled=false` 是总开关。日志和 trace 独立：已接入入口仍可生成、传播并恢复 trace；Ignore 单独出现不创建入口。入口基于 Spring AOP，需经过 Spring 代理。

返回 Future / CompletionStage 时立即记录返回对象和耗时，不等待、不监听异步完成；ErrorLog 只记录当前同步抛出的异常。默认 Jackson formatter 复用应用 Mapper 和字段规则；无 Mapper 使用 String.valueOf；转换失败使用 serialization-failed，不回退 DTO 的 toString。HttpEntity / ResponseEntity 只格式化 body，技术对象、循环与深度保护保持原规则。

升级迁移：删除旧 `velo.log.defaults.*` 和 `sources.<source>.<feature>.*` 配置，改用组合注解类；功能注解移除 enabled，改用 level=LogPolicyLevel.OFF 关闭；LogPolicyLevel 只保留五个常用等级及 OFF，InvokeLog 的导入改为 `log.annotation.composed.InvokeLog`。原阈值 -1、白名单单空字符串的继承标记均不再支持。完整解析规则与实现链接见[日志注解检索说明](docs/logging-annotation-resolution.md)。

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
- 派生字段按自身名称独立参与 Jackson 动态属性过滤：通过 `@JsonFilter` 排除 `status` 时，`statusName` 仍可输出，支持只返回枚举描述的用法；需要同时隐藏时应同时过滤这两个名称。通过 `@JsonIgnore` 等方式从序列化属性中移除源字段时，不会生成对应派生字段
- `enum-mappings` 为空时只关闭按全局约定进行的隐式匹配；`@JsonEnum` 同时指定 `codeField` 和 `nameField` 时仍独立生效
- `@JsonEncode` / `@JsonDecode` 是注解驱动能力：只要 Jackson 扩展与 `JsonProcessorProvider` 生效，带注解字段就会转换；未使用注解的字段不会执行转换，因此不再提供额外的 `string-converter-enabled` 总开关
- Jackson 的普通字符串 XSS 清洗由独立的 `velo.xss.jackson-enabled` 控制，默认关闭，避免把全局 Mapper 的所有字符串都意外改写
- 日期时间格式依然复用 `velo.date-time-format.*`；未标注的 `Date` 默认兼容日期-only 和完整日期时间，字段上的 `@JsonFormat` 优先
- `velo.date-time-format.time-zone` 默认 `Asia/Shanghai`（中国时间），支持 `+08:00`、`GMT+08:00` 等 `ZoneId` 写法；日期增强启用时，非法时区使初始化失败，不会静默退回 GMT。固定偏移量与地区时区对历史日期的处理遵循 JDK 原生规则
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

Velo 基于已有 `RedisConnectionFactory` 补齐常用 `RedisTemplate`，优先使用用户的 `RedisSerializer<Object>`，没有时使用自动注册的 Generic Jackson JSON 序列化器 Bean，通过类型元数据自动恢复常见业务对象。

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

- `velo.redis.enabled` 默认开启，只控制 RedisTemplate 增强；共用的 JSON 序列化器 Bean 仍可供 Spring Cache 使用
- 未引入 Spring Data Redis 时，Redis 自动配置会安全跳过，不会触发 Redis 连接配置或导致应用启动失败
- Boot 2 的 Redis 连接地址、密码、数据库使用 `spring.redis.*`；Boot 3 / 4 使用 `spring.data.redis.*`
- starter 会尝试创建：
  - `redisTemplate`
  - `stringObjectRedisTemplate`
- 普通模板与 Spring Cache 均优先使用用户的 `RedisSerializer<Object>`；没有用户候选时共用自动注册的通用 JSON 序列化器 Bean。默认 Redis mapper 独立于 HTTP JSON，Web 全局增强不会自动影响 Redis 值格式
- 默认注册标准 `RedisSerializer<Object>` Bean（名称为 `veloRedisSerializer`，不标记 `@Primary`），无需通过工厂获取；局部 `RedisSerializer<Dto>` / `RedisSerializer<String>` 不会关闭默认 Bean，也不会作为通用 Object 值序列化器被选择
- 用户提供一个 `RedisSerializer<Object>` 时直接采用，Bean 名无需固定；多个通用候选须使用 `@Primary` 明确选择，否则按 Spring 原生规则启动报错，不再静默回退另一套格式；也不会以空序列化器触发 RedisTemplate 的 JDK 默认序列化。`@Order` 不决定单个 Bean 的注入优先级
- 默认普通 Redis 值以 JSON 储存，允许包含 `@class` 或类型包装；通用 Object 模板读回 DTO 时恢复实际类型，集合元素也恢复类型，业务本身写入的 Map 仍读为 Map。无需登记目标类型，遵循 1.3.0 的通用 JSON 使用方式；若明确需要无类型信息的 JSON，可自行提供定向 serializer 或使用 StringRedisTemplate 写入 JSON 字符串
- 通用 JSON 的集合兼容性遵循当前 Jackson / Spring Data Redis 版本。已验证普通 DTO、final DTO、record、ArrayList 和 LinkedHashMap；Jackson 2 的类型判定与 Generic JSON 保持一致，业务 final 类型也保留回读信息，日期时间使用 ISO 表示并保留纳秒精度。不可变集合及其他 JDK 容器不承诺自动恢复，需要稳定恢复内容时建议写入 ArrayList / LinkedHashMap，或自行提供适合业务协议的 serializer；Velo 不在写入前转换业务集合，也不在读取后过滤业务字段
- HTTP Jackson mapper 不用于默认 Redis 存储；默认序列化器保留日期支持，不增加业务 key 缓存、轮询或定时线程
- 从此前无类型信息的 Object JSON 切换到默认通用 JSON 时，应清理对应旧条目或更换业务 key 前缀；默认序列化器不会猜测旧 JSON 的 DTO 类型。用户自己注册的模板、局部定向 serializer 和 StringRedisTemplate 保持原有协议
- 1.3.0 的默认 redisTemplate 可能先由 Spring Boot 创建并使用 JDK 序列化，Velo 的 JSON 配置只作用于其实际创建的模板。当前版本在官方模板配置前创建 Velo 模板；从旧 JDK 数据切换到 JSON 时也应更换业务 key 前缀或重建对应缓存。Boot 3 / 4 对上述常见对象提供相同的使用约定；共享 Redis 数据时仍需验证实际业务类型在两种运行时之间的双向读取，不承诺任意历史 JSON 或全部 Jackson 类型的跨版本兼容
- Boot 4 项目若选择 Jackson 2，请自行提供 Jackson 2 的 `RedisSerializer<Object>` Bean，普通 RedisTemplate 和 Spring Cache 默认配置会复用该 Bean；也可用用户 builder customizer 显式覆盖。Jackson 2/3 同时存在时不会仅依据类路径猜测用户意图，详细兼容边界见上面的缓存说明

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
上述显式格式优先规则适用于 Spring / Jackson；Excel 全局转换器的行为与禁用方式见 Excel 章节。
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
        default-annotation: io.github.luminion.velo.log.annotation.composed.InvokeLog
  trace:
    enabled: true
```

说明：

- 默认开启
- 默认打印 Spring 绑定后的入参、返回结果和独立 INFO 耗时；达到阈值打印独立慢调用日志，异常时打印独立 WARN 摘要
- 每条日志使用 `[controller] [方法名()] [ip=客户端IP method=HTTP方法 route=请求路径]` 前缀；公共块优先使用路由模板，缺失时使用请求 URI，不拼接原始 query string
- 通过组合注解或具体功能注解设置 `maxLength`：`-1` 完整输出，`0` 跳过本项，正数截断本项最终正文
- 功能参数在注解上定义，配置只选择 `default-annotation`；可用 `level=LogPolicyLevel.OFF` 关闭对应功能
- 公共块通过 Controller 来源的 `InvocationLogContextFormatter` Bean 定制，整体格式通过 `InvocationLogWriter` Bean 定制，示例见前面的日志章节
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
        default-annotation: io.github.luminion.velo.log.annotation.composed.InvokeLog
  trace:
    enabled: true
    feign-propagation-enabled: true
```

说明：

- 默认开启
- 前缀为 `[feign] [方法名()] [client=客户端名称 contextId=配置标识 method=HTTP方法 route=接口路径]`；已知实际实例地址时追加 `instance`，暂不可用的字段省略
- 接口路径取 Spring MVC 映射模板，例如 `/users/{id}`，不展开路径变量或拼接查询参数；无法解析映射时省略缺失部分，保留方法名
- 默认打印入参、返回结果和独立 INFO 耗时，慢调用和异常各输出独立日志
- 日志格式和 Controller、`@InvokeLog` 保持一致，便于联调排查
- 请求头和响应头默认不输出，可通过具体功能注解或配置选择的自定义组合启用
- 通过组合注解或具体功能注解设置 `maxLength`：`-1` 完整输出，`0` 跳过本项，正数截断本项最终正文；`level=LogPolicyLevel.OFF` 关闭对应功能
- 公共块通过 Feign 来源的 `InvocationLogContextFormatter` Bean 定制，实例信息使用当前阶段快照，不延迟入口日志等待采集
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

通过 `@Bean` 注册并提供转换函数即可生效，无需另写子类或添加 `@RestControllerAdvice`：

```java
@Bean
public VeloValidationWebExceptionHandler<Result<?>> exceptionHandler() {
    return new VeloValidationWebExceptionHandler<>(Result::failed, error -> Result.error("系统异常"));
}
```

Advice 元数据由抽象父类提供，具体处理器不会被宽范围组件扫描自动实例化。已有带 `@RestControllerAdvice` 的子类也可继续使用。每个应用选择一个兜底处理器，避免同时注册多个相同优先级的全局处理器。

Boot 2 使用 `javax.validation`；Boot 3.2+、Boot 4 使用 `jakarta.validation`，并处理 Spring 6.1+ 的 `HandlerMethodValidationException`。参数校验失败使用失败响应转换函数；返回值校验失败使用系统异常转换函数。`VeloStatusWebExceptionHandler`（Jakarta）保留原生 400 / 500 状态码；`VeloValidationWebExceptionHandler` 保留已有的响应状态行为。

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

### Q3：如何调试 SpEL `value` 表达式？

- 确认已开启编译参数 `-parameters`，否则 `#userId` 这类按参数名引用无法解析，只能用 `#p0`、`#p1`
- 表达式解析为空值、空白字符串或复杂对象会抛出异常；三种注解行为一致
- 可单独用 `SpelExpressionParser` 写单测验证表达式取值是否符合预期

### Q4：限流被拒绝后多久可以重试？

由具体后端决定。Guava 使用原生速率补充机制，Redis 在当前一秒固定窗口到期后重置，Redisson 使用原生的一秒额度恢复规则。被拒绝后立即重试可能仍失败，业务侧应按所选后端退避，Starter 不承诺统一的重试时间。Redis 固定窗口允许边界两侧的额度连续使用。

### Q5：异常信息能否做国际化？

可以。国际化是可选能力，不会改变未配置时的中文提示。注解的 `message` 默认空，继承对应功能的全局消息配置；注解或配置文案写成 `{i18n.key}` 形式时，Velo 会通过 Spring 应用上下文的主 `MessageSource` 按当前语言解析；普通文本则原样输出。

Starter 内置了 `velo/messages.properties` 和 `velo/messages_en.properties`。如需使用内置资源，需要显式配置消息资源路径：

```yaml
spring:
  messages:
    basename: velo/messages
```

然后在注解或全局配置中引用消息 key：

```java
@Idempotent(message = "{velo.idempotent.rejected}")
```

```yaml
velo:
  idempotent:
    message: "{velo.idempotent.rejected}"
```

如果应用自定义了消息源，应将其作为名为 `messageSource` 的主消息源；存在多个不同名称的 `MessageSource` 时，Starter 不会自动选择其中一个。未配置消息资源或找不到 key 时，会回退为 key 文本，不会抛出异常。未使用 `{key}` 形式的项目不受影响。

### Q6：缓存大量 key 同时过期（缓存雪崩）？

开启 TTL 抖动：`velo.cache.ttl-jitter-percentage=10`。抖动按 key 在写入时独立计算，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类问题。
