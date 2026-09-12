# Velo Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.luminion/velo-spring-boot3-starter)](https://central.sonatype.com/artifact/io.github.luminion/velo-spring-boot3-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/luminion/spring-boot-starter?style=social)](https://github.com/luminion/spring-boot-starter)

Velo Spring Boot Starter 是一组低侵入的 Spring Boot 自动配置扩展。
项目以 `velo.*` 作为统一配置入口，围绕并发控制、缓存、Jackson、Redis、MyBatis-Plus、Excel、日志、XSS 以及 Web MVC/WebFlux 常用增强提供开箱能力。

> 修改记录：2026-09-02 17:46，明确各 Spring Boot Starter 的支持范围、JDK 要求及不保证的版本，避免将版本兼容范围理解过宽。
>
> 修改记录：2026-09-09 14:01，补充 XSS 对 Jackson JSON 请求体字符串的实际处理边界，避免文档与实现不一致。
>
> 修改记录：2026-09-09 15:15，补充 Redis 缓存与 RedisTemplate 的 Jackson 2/3 序列化器选择规则，避免 Boot 4 下将 Jackson 版本误认为由类路径自动唯一决定。

> 修改记录：2026-09-10 09:58，移除已废弃的旧 CORS 配置入口，统一使用 `velo.web.cors.enabled`，并明确该调整不再兼容旧配置。

> 修改记录：2026-09-10 10:10，将 CORS 凭证默认值调整为关闭；JWT 放在 Authorization 请求头时无需开启凭证，Cookie/Session 跨域时需显式开启并配置明确来源。

> 修改记录：2026-09-10 11:33，补充 Spring Boot 2/3/4 的 Redis JSON 序列化器兼容边界，明确 Boot 4 同时存在 Jackson 2/3 时默认使用 Jackson 3，Jackson 2 仅作为显式兼容与迁移路径。

> 修改记录：2026-09-10 13:26，补充默认调用日志异常摘要的转义规则，明确特殊字符不会破坏单行日志结构；原因是异常消息可能包含用户输入，需要避免日志注入和结构化解析歧义。

> 修改记录：2026-09-10 13:34，明确非法日期格式或时区会在转换器启动时失败，并补充启动横幅对空配置的保护行为；原因是避免“仅告警”与实际启动结果不一致，也避免诊断横幅因空配置阻止应用启动。

> 修改记录：2026-09-10 13:42，明确全局枚举映射为空时仍支持完整显式 `@JsonEnum` 字段映射；原因是全局默认映射关闭不应覆盖用户对单个字段的明确配置。
>
> 修改记录：2026-09-10 14:46，统一 Excel 两级开关的默认值和关闭语义；原因是“需显式开启”与配置默认值为 `true` 相互矛盾，容易误导使用方。
>
> 修改记录：2026-09-10 14:51，移除通用 Web 异常处理基类上的自动组件注册语义，并明确具体实现类需显式添加 `@RestControllerAdvice`；原因是宽范围组件扫描可能尝试实例化缺少响应函数依赖的泛型基类。
>
> 修改记录：2026-09-10 15:02，将 `velo.mode` 枚举配置简化为默认值为 `true` 的 `velo.opinionated` 布尔开关；原因是用单一正向开关表达开箱即用/无侵入两种行为，降低配置复杂度。
>
> 修改记录：2026-09-10 15:15，明确三个 Excel Helper 的 `createExtraConverters(...)` 返回可变列表，可在注册前追加自定义 converter；原因是公开工厂方法返回不可变列表容易造成扩展调用方无法组合自定义转换器的歧义。
>
> 修改记录：2026-09-11 09:40，补充 MyBatis-Plus 3.5.9 及以上分页和防全表更新功能所需的 JSQLParser 扩展依赖；原因是该依赖默认不再随 MyBatis-Plus 主 starter 携带，缺少时对应拦截器会按 classpath 条件跳过注册。
>
> 修改记录：2026-09-11 14:13，明确 Velo 内置 MyBatis-Plus 拦截器的低优先级顺序，并说明用户可通过 `@Order` 覆盖；原因是自动配置不应抢占用户自定义 SQL 拦截器的执行位置。
>
> 修改记录：2026-09-11 16:24，补充 `getRequestIp()` 在 Nginx/网关代理链下的信任边界和使用限制；原因是当前实现可以正确读取线上转发头，但转发头必须由可信入口代理清洗或覆盖，不能直接作为安全判断依据。

> 修改记录：2026-09-12 02:26，明确 Redis 和 MyBatis-Plus 为可选依赖，缺少对应类库时自动配置会安全跳过，不会因为 Velo 的自动配置导入导致应用启动失败。
>
> 修改记录：2026-09-11 17:33，补充 `velo.opinionated=false` 无侵入模式仍保留的能力及显式重新开启规则；原因是无侵入模式只注入全局增强的最低优先级关闭值，不等于停用全部 Velo Bean 或注解能力。

> 修改记录：2026-09-11 22:26，新增 WebFlux 版本的 trace、Controller 日志、请求工具、日期/XSS/CORS 配置和异常处理扩展，并明确 Boot 2/3/4 均需由应用按需引入 `spring-boot-starter-webflux`；原因是 WebFlux 与 Servlet MVC 使用不同请求模型，不能直接复用 Servlet 组件。

> 修改记录：2026-09-12 19:40，补齐 `@Idempotent`、`@RateLimit`、`@Lock`、`@InvokeLog` 和 `@SlowLog` 在 WebFlux `Mono/Flux` 生命周期中的响应式支持，并说明自定义响应式锁处理器的扩展契约；原因是同步切面只能覆盖 Publisher 组装阶段，不能安全承担订阅期间的加锁、幂等清理和耗时统计。


## 功能特性

- 统一的 `velo.*` 配置模型，集中管理各类自动配置开关
- 提供 `@Idempotent`、`@RateLimit`、`@Lock` 三类并发控制能力
- 支持 Redis / Redisson / Caffeine / JDK 多后端并自动降级
- 提供 Spring Cache + Redis Cache 的统一 TTL 和 key 前缀配置
- 提供 Jackson 日期时间、超大整数、枚举派生字段、字符串转换增强
- 提供 MyBatis-Plus 分页、乐观锁、防全表更新拦截器自动注册
- 提供 RedisTemplate 序列化风格统一能力
- 提供 Excel 扩展 converter 自动注册和 helper 工具类
- 提供注解日志、Controller 请求日志、XSS 清洗和 Web MVC/WebFlux 日期绑定增强
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
- `velo.opinionated=false`：无侵入模式，关闭容易自动影响应用行为的默认项，但 `@Idempotent`、`@RateLimit`、`@Lock`、`@InvokeLog`、`@SlowLog` 等显式注解仍可用
- `velo.opinionated=false` 只提供低优先级默认值，业务项目显式配置的属性优先级更高
- 无侵入模式下重新打开某类能力时，需要显式设置对应的 `enabled` 项，例如 `velo.jackson.enabled=true`
- 设置为 `false` 后，starter 会在启动日志中输出一条 INFO，列出被默认关闭的能力，便于排查“为什么某全局增强没生效”
- 旧配置 `velo.mode=OPINIONATED/CONSERVATIVE` 已移除，不再生效；请迁移为 `velo.opinionated=true/false`

配置优先级（从高到低）：

| 优先级 | 来源 | 示例 |
| --- | --- | --- |
| 1 最高 | 命令行参数 | `--velo.log.trace.enabled=true` |
| 2 | Java 系统属性 | `-Dvelo.log.trace.enabled=true` |
| 3 | 环境变量 | `VELO_LOG_TRACE_ENABLED=true` |
| 4 | application.yml / properties | `velo.log.trace.enabled: true` |
| 5 最低 | `velo.opinionated` 默认值 | `true` / `false` 注入的默认值 |

Spring Boot 还支持 `SPRING_APPLICATION_JSON`、测试属性等特殊配置源；上表列出本 starter 最常用的来源。也就是说 `velo.opinionated=false` 注入的只是**最低优先级默认值**，业务项目任何显式配置都会覆盖它。

例如无侵入模式下重新打开 traceId：

```yaml
velo:
  opinionated: false
  log:
    trace:
      enabled: true
```

默认会自动影响全局行为的能力：

| 能力 | `velo.opinionated=true` | `velo.opinionated=false` | 说明 |
| --- | --- | --- | --- |
| traceId / MDC / 日志 pattern / 响应头 | 开启 | 关闭 | 影响用户自己的日志输出 |
| Controller 调用日志 | 开启 | 关闭 | Web 环境下自动记录请求调用 |
| Feign 调用日志 | 开启 | 关闭 | 存在 Feign 时自动记录远程调用 |
| Jackson 增强 | 开启 | 关闭 | 影响 JSON 序列化、反序列化扩展 |
| Spring Converter / Web MVC 与 WebFlux 日期绑定 | 开启 | 关闭 | 影响字符串到日期时间的全局转换 |
| MyBatis-Plus 拦截器 | 开启 | 关闭 | 自动补充分页、乐观锁、防全表更新 |
| RedisTemplate 自动补齐 | 开启 | 关闭 | 依赖 Redis classpath 与连接工厂 |
| Redis Cache 自动补齐 | 开启 | 关闭 | 依赖 Spring Cache / Redis 条件 |
| Excel converter 自动注册 | 开启 | 关闭 | helper 工具类不受影响 |

无侵入模式仍保留或独立生效的能力：

| 能力 | 无侵入模式行为 | 说明 |
| --- | --- | --- |
| Velo Core 基础 Bean | 保留 | 提供指纹解析、消息解析、配置告警和可选 Banner 等基础支持 |
| `@Idempotent` / `@RateLimit` / `@Lock` | 保留 | 注解驱动能力不由 `velo.opinionated` 默认关闭，仍需对应后端依赖 |
| `@InvokeLog` / `@SlowLog` | 保留 | 方法级显式日志仍可用；Controller/Feign 自动日志默认关闭 |
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

---

## 各功能细览

### 1. 缓存自动配置

Velo 会在满足 Redis Cache 条件时补齐 `CacheManager`、`RedisCacheConfiguration` 和分 cache TTL 配置。

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
velo:
  cache:
    enabled: true
    prefix: app
    separator: ":"
    default-ttl: 5m
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
- 默认只在 `spring.cache.type=redis` 或未显式指定时接管 Redis Cache
- `default-ttl` 默认 `5m`
- `ttl.<cacheName>` 可按缓存名单独覆盖 TTL
- key 前缀格式为 `prefix + separator + cacheName + separator`
- 业务侧仍然需要自己开启 `@EnableCaching`
- 缓存值序列化器优先复用容器中的 `RedisSerializer<Object>`；没有显式 Bean 时使用 `RedisSerializer.json()`，跟随当前 Spring Data Redis 版本的原生 JSON 实现
- Boot 2 / 3 默认使用 Jackson 2 的 `GenericJackson2JsonRedisSerializer`（对应 Spring Data Redis 2.x / 3.x），写入 JSON 类型元数据，因此 `Object` / POJO 可以反序列化回原类型，而不是默认退化为 `LinkedHashMap`
- Boot 4 默认使用 `RedisSerializer.json()`，由 Spring Data Redis 4.x 选择 Jackson 3 的 `GenericJacksonJsonRedisSerializer`；即使 Jackson 2 / 3 同时存在，也不会按类路径猜测切换，默认仍使用 Jackson 3
- Boot 4 如需让 Redis 使用 Jackson 2，应引入官方 `spring-boot-jackson2` 及 Jackson 2 依赖，并显式注册一个 `RedisSerializer<Object>` Bean，例如 `GenericJackson2JsonRedisSerializer`；Spring Data Redis 4.x 仍保留该类用于兼容或迁移旧数据，但已标记为后续移除，不作为 Boot 4 默认实现
- Jackson 2 与 Jackson 3 的 Redis JSON 输出可能存在差异；从 Boot 2 / 3 切换到 Boot 4 时，应先规划旧数据读取、迁移或 key 空间隔离，不要默认认为历史值可以无缝混读

缓存雪崩防护（TTL 抖动）：

- `ttl-jitter-percentage` 默认 `0`（关闭），有效范围为 `0..100`。设为 `10` 表示每条缓存写入时 TTL 在原值 ±10% 内随机偏移；超出范围会导致应用启动失败
- 抖动在**每次写入时按 key 独立计算**，因此同一缓存名称下不同 key 也会获得不同过期时间，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类雪崩
- 抖动只影响实际写入 Redis 的过期时间，不改变 `default-ttl` / `ttl.<cacheName>` 的配置语义

### 2. Excel 自动配置

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

`@Idempotent` 用于防重复提交，默认即可使用。

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

- `backend` 可选 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`、`JDK`
- `AUTO` 模式下按自动配置顺序选择后端：`REDISSON -> REDIS -> CAFFEINE -> JDK`
- `prefix` 默认 `idempotent:`
- `ttl` 单位固定为毫秒，默认 `3000`（3 秒）
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
- 需要 Caffeine 后端时引入 `com.github.ben-manes.caffeine:caffeine`

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

@RateLimit(key = "#userId", permits = 10, window = 1000)
public Object query(Long userId) {
    return null;
}
```

说明：

- `permits` 表示一个时间窗口内允许通过的最大请求数
- `window` 定义窗口大小，单位固定为毫秒，默认 `1000`（1 秒）
- `backend` 与幂等一致，也支持 `AUTO/REDISSON/REDIS/CAFFEINE/JDK`
- `AUTO` 模式下默认选择顺序同幂等：`REDISSON -> REDIS -> CAFFEINE -> JDK`
- `REDIS` 后端使用 Boot 默认的 `stringRedisTemplate` 执行 Lua，脚本通过 Redis `TIME` 获取统一时钟，不受应用节点时间偏差影响

关于 `key` 的分桶语义（重要）：

- `key` 为空：方法级全局限流，该方法的**所有调用者共享同一个配额**
- `key` 非空：按 SpEL 表达式结果分桶，**每个桶独立计算配额**

| 写法 | 实际行为 |
| --- | --- |
| `@RateLimit(permits=10)` | 所有调用共享 10 次/窗口 |
| `@RateLimit(key="#userId", permits=10)` | 每个 userId 独立 10 次/窗口 |

> ⚠️ 如果你期望「每个用户 / 每个资源独立限流」，必须显式指定 `key`，否则会退化为全局共享配额。

关于小数 `permits`：

`permits` 支持小数，用于表达「低于 1 次 / 窗口」的限流需求。换算规则：

- 实际容量 `capacity = ceil(permits)`（向上取整）
- 实际窗口 `interval = window × (capacity / permits)`（拉长窗口以保持平均速率）

| 配置 | 含义 | 实际实现 |
| --- | --- | --- |
| `permits=10, window=1000` | 10 次/秒 | 容量=10，窗口=1000ms |
| `permits=0.5, window=1000` | 0.5 次/秒（即 2 秒 1 次） | 容量=1，窗口=2000ms |
| `permits=0.2, window=1000` | 0.2 次/秒（即 5 秒 1 次） | 容量=1，窗口=5000ms |

多数场景用整数更直观，例如「每 5 秒 1 次」可直接写 `@RateLimit(permits=1, window=5000)`，等价于 `permits=0.2, window=1000`。

### 5. 锁

`@Lock` 提供方法级互斥能力，适合支付、状态流转、扣减等需要串行化的业务场景。

可选额外依赖：

- 需要 Redisson 后端时引入 `org.redisson:redisson-spring-boot-starter`
- 需要 Redis 后端时引入 `spring-boot-starter-data-redis`
- 需要 Caffeine 后端时引入 `com.github.ben-manes.caffeine:caffeine`

关键配置：

```yaml
velo:
  lock:
    enabled: true
    backend: AUTO
    prefix: "lock:"
    retry-interval: 10ms
```

使用示例：

```java
import io.github.luminion.velo.lock.annotation.Lock;

@Lock(key = "#orderId", waitTimeout = 1000, lease = 30000)
public void pay(Long orderId) {
    // ...
}
```

说明：

- `backend` 也支持 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`、`JDK`
- `AUTO` 默认顺序为 `REDISSON -> REDIS -> CAFFEINE -> JDK`
- `waitTimeout` 单位固定为毫秒，默认 `0`，表示拿不到锁立即失败
- `retry-interval` 默认 `10ms`，仅用于简单 Redis 后端等待锁时的轮询；值越小获取越及时，但 Redis 请求频率越高
- `lease` 单位固定为毫秒，默认 `30000`（30 秒）
- `lease > 0` 在 Redis 简单实现和 Redisson 中都是固定 TTL；`lease = -1` 才请求看门狗续约。Redis 简单实现使用 30 秒初始 TTL、每 10 秒按 token 原子续约，Redisson 使用自身的原生看门狗
- 看门狗只能覆盖进程仍可执行续期任务的长调用；进程崩溃或 Redis 长时间不可用时，锁仍会在 TTL 到期后释放。耗时不确定的任务建议显式使用 `lease = -1`，有更高分布式锁要求时优先使用 Redisson
- `REDIS` / `REDISSON` 更适合分布式场景，`CAFFEINE` / `JDK` 只保证单 JVM 内互斥
- 本地锁只有一份实现：Caffeine 是纯缓存库、不提供互斥锁 API，因此 `CAFFEINE` 档位与 `JDK` 完全一致（复用同一实现），`backend=CAFFEINE` 仍可用，只是不再单独维护
- `REDIS` 后端支持同线程可重入（同一线程重复加同一把锁不会自锁死），最外层释放时才真正删除 Redis 锁
- `key` 为空时降级为方法级锁（基于 `全限定类名#方法名(参数类型...)`），表示「该方法全局串行执行」，是一个有意义的语义，因此安静降级、不打告警；需要按业务维度加锁时请显式指定，例如 `@Lock(key = "#orderId")`

### 6. 并发控制组合顺序

当 `@Idempotent`、`@RateLimit`、`@Lock` 同时作用于同一个方法时，starter 内置顺序为：

```text
@Idempotent -> @RateLimit -> @Lock -> 业务方法
```

这意味着重复提交会最先被拒绝，不消耗限流令牌，也不会尝试加锁；限流失败时不会进入锁等待；只有真正允许执行业务的方法调用才会获取锁。

这些切面使用接近 `Ordered.LOWEST_PRECEDENCE` 的低优先级顺序值，并在三者之间保留较大间隔，便于业务自定义切面通过 `@Order` 插入到合适位置。

### 7. 日志

Velo 提供一套统一调用日志能力。Controller、Feign 与 `@InvokeLog` 每次调用分别输出进入和退出两条记录：进入记录包含入参，退出记录包含耗时以及返回值或异常。

日志序列化器或 `InvocationLogWriter` 发生运行时异常时会记录内部 WARN 并丢弃本次日志，不会阻止业务执行或覆盖原始业务异常。

Controller、Feign 是默认调用日志来源，不需要业务方法添加注解；`@InvokeLog` 和 `@SlowLog` 是显式注解来源，没有命中注解的方法不会产生这两类日志。

基础 starter 默认可用，无需额外依赖。

关键配置：

```yaml
velo:
  log:
    enabled: true
    level: INFO
    controller:
      enabled: true
    feign:
      enabled: true
    slow:
      level: WARN
    trace:
      enabled: true
      header-name: X-Trace-Id
      mdc-key: traceId
      response-header-enabled: true
      feign-propagation-enabled: true
      logging-pattern-enabled: true
    invocation:
      max-payload-length: -1
      include-args: true
      include-result: true
      include-error-stack-trace: false
```

使用示例：

```java
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.SlowLog;

@InvokeLog
@SlowLog(300)
public Object createOrder(CreateOrderCmd cmd) {
    return null;
}

@InvokeLog(argsOnFinish = true)
public void enrichOrder(OrderDTO order) {
    order.setStatus("READY");
}
```

说明：

- `traceId` 默认开启，会写入 MDC、响应头，并在 Feign 调用中透传
- 如果没有自定义 `logging.pattern.level`，会自动把 `%X{traceId}` 加到用户自己的日志中
- Controller、Feign 与 `@InvokeLog` 的进入日志格式为 `[target] ==> args=...`
- Controller、Feign 与 `@InvokeLog` 的退出日志格式为 `[target] <== cost=Xms result=...`；无返回值时记录 `result=void`，返回 `null` 时记录 `result=null`，调用失败时输出异常摘要并使用 ERROR 级别
- 默认 SLF4J 调用日志 writer 会转义异常摘要中的双引号、反斜杠、换行和控制字符，避免破坏单行日志结构；开启异常堆栈时，完整堆栈仍按多行输出
- `@InvokeLog(argsOnFinish = true)` 会在正常返回和异常结束的退出日志中增加 `args=...`，记录方法结束时的参数状态
- payload 无法打印时会明确标记原因：`ignored`（注解忽略）、`disabled`（配置关闭）或 `serialization-failed`（序列化失败）
- `@SlowLog` 的阈值单位固定为毫秒，只在调用耗时超过阈值后输出一条独立慢日志，格式包含 `cost=Xms threshold=Yms`
- 慢日志级别由 `velo.log.slow.level` 控制，默认 WARN；调用异常且超过阈值时提升为 ERROR，设置为 `OFF` 时完全关闭独立慢日志
- 同时命中其他调用日志切面时，慢日志默认在 ENTRY、EXIT 日志之后最后输出
- 如果需要写入 MQ、数据库或审计系统，提供自定义 `InvocationLogWriter` Bean 即可
- `velo.log.controller.enabled` 和 `velo.log.feign.enabled` 分别控制默认 Controller/Feign 日志；关闭其中一项不会影响另一项，也不会影响注解日志
- `@InvokeLog` / `@SlowLog` 没有独立的 method 总开关：是否输出由方法或类上是否存在对应注解决定；`velo.log.enabled=false` 仍是所有 Velo 日志的总闸
- `velo.log.level=OFF` 会关闭 Controller、Feign、`@InvokeLog` 的成功和异常输出；`velo.log.slow.level=OFF` 会关闭 `@SlowLog` 的独立慢日志；自定义 `InvocationLogWriter` 不受这些日志级别约束

敏感参数不打印（`@LogPayloadIgnore`）：

如果某些方法的入参或返回值包含密码、token 等敏感信息，可用 `@LogPayloadIgnore` 抑制其打印。被忽略的内容在日志中显示为 `ignored`，但调用本身（方法名、耗时、成功/异常状态）仍会记录。该注解对 Controller、Feign、`@InvokeLog`、`@SlowLog` 所有调用日志切面均生效，可标注在方法或类上。

```java
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;

// 同时忽略入参与返回值
@LogPayloadIgnore
public void deleteUser(Long userId) { }

// 只忽略返回值，仍打印入参
@LogPayloadIgnore(args = false)
public UserDTO login(LoginRequest req) { }

// 只忽略入参，仍打印返回值
@LogPayloadIgnore(result = false)
public Token issueToken(Credential credential) { }
```

> 说明：starter 不内置基于字段名正则的自动脱敏，敏感信息控制统一通过 `@LogPayloadIgnore` 按方法显式声明，语义更明确、无误伤风险。

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
- `velo.xss.web-enabled` 默认 `true`，控制 Web MVC/WebFlux 字符串参数转换目标；设为 `false` 后即使已选择策略也不注册该目标转换器
- `velo.xss.jackson-enabled` 默认 `false`，控制 Jackson 普通 `String` 属性的全局清洗；这是独立开关，不影响 `@JsonEncode` / `@JsonDecode` 注解处理
- `strategy` 可选 `NONE`、`ESCAPE`、`SIMPLE_TEXT`、`BASIC`、`BASIC_WITH_IMAGES`、`RELAXED`
- `ESCAPE` 不依赖 `jsoup`；其他 HTML 清洗策略必须引入 `jsoup`
- `ESCAPE` 且无 `jsoup` 时会走 Spring 转义；其他策略缺少 `jsoup` 时只打印 WARN，不注册 `XssCleaner`，也不会自动降级
- 清洗发生在 Web MVC/WebFlux 的字符串参数绑定阶段，包括 query/form/path 和普通对象参数中通过对应 binder 绑定的 `String` 字段
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
- `serialize-long-as-string=true` 时，`Long` / `BigInteger` 在**序列化（写出给前端）**时统一转为字符串，避免 JS Number 超过 2^53 精度丢失
- 这些 `serialize-*` 开关**只影响序列化方向**；反序列化（前端传入）时数字和字符串都能正常绑定，无需前端特殊处理
- `serialize-big-decimal-as-string=true` 默认开启
- `enum-desc-enabled=true` 时，`@JsonEnum` 可为数值字段派生出描述字段，例如 `statusName`
- `enum-mappings` 为空时只关闭按全局约定进行的隐式匹配；`@JsonEnum` 同时指定 `codeField` 和 `nameField` 时仍独立生效
- `@JsonEncode` / `@JsonDecode` 是注解驱动能力：只要 Jackson 扩展与 `JsonProcessorProvider` 生效，带注解字段就会转换；未使用注解的字段不会执行转换，因此不再提供额外的 `string-converter-enabled` 总开关
- Jackson 的普通字符串 XSS 清洗由独立的 `velo.xss.jackson-enabled` 控制，默认关闭，避免把全局 Mapper 的所有字符串都意外改写
- 日期时间格式依然复用 `velo.date-time-format.*`；未标注的 `Date` 默认兼容日期-only 和完整日期时间，字段上的 `@JsonFormat` 优先

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
- Redis 连接地址、密码、数据库等仍然走标准 `spring.data.redis.*`
- starter 会尝试创建：
  - `redisTemplate`
  - `stringObjectRedisTemplate`
- 序列化器优先复用容器中的 `RedisSerializer<Object>`，通常会跟随 Velo 的 Jackson 配置保持一致
- 若容器没有 `RedisSerializer<Object>`，starter 使用 `RedisSerializer.json()` 作为回退；Boot 2/3 跟随对应 Spring Data Redis 的 Jackson 2 实现，Boot 4 使用 Jackson 3 实现
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

有 Web 环境时，Velo 默认开启 Controller 调用日志切面，并自动生成或接收 `X-Trace-Id`。

配置项：

```yaml
velo:
  log:
    controller:
      enabled: true
    invocation:
      max-payload-length: -1
    trace:
      enabled: true
```

说明：

- 默认开启
- 每次调用输出进入和退出两条日志；进入日志包含请求方法、controller 映射模板路径和入参，退出日志包含耗时、响应体或异常摘要
- 会过滤掉原始 query string，避免把敏感查询串直接打到日志中
- `max-payload-length` 为正数时，过长 payload 会按配置长度截断
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示不序列化 payload 并记录为 `disabled`
- 如果不需要这层默认日志，关闭 `velo.log.controller.enabled`；该配置不会关闭 `@InvokeLog` / `@SlowLog`

WebFlux 项目使用同一套配置，应用只需按需引入对应的 `spring-boot-starter-webflux`。Spring Boot 2.7、3.x 和 4.x 均支持 WebFlux，Velo 会根据当前 Starter 版本自动适配；不引入 WebFlux 时，相关自动配置不会生效。

WebFlux 版本的公开组件位于 `io.github.luminion.velo.webflux`：

- `TraceIdWebFluxFilter`：生成/接收 `X-Trace-Id`，写入响应头、Reactor Context 和当前线程 MDC；跨线程场景以 Reactor Context 为准
- `WebFluxControllerLogAspect`：Mono 在完成、异常或取消时记录退出日志；Flux 不缓存完整流，只记录完成时的元素数量或异常/取消状态
- `WebFluxIdempotentAspect`、`WebFluxRateLimitAspect`、`WebFluxLockAspect`：让 `@Idempotent`、`@RateLimit`、`@Lock` 在响应式订阅时生效；幂等成功后保留 TTL，异常/取消时清理本次记录，锁在完成/异常/取消时释放，限流检查不会在 Publisher 组装阶段提前执行
- `WebFluxInvokeLogAspect`、`WebFluxSlowLogAspect`：让方法日志绑定真实的 Mono/Flux 订阅生命周期；Mono 按完成、异常或取消记录，Flux 不缓存完整数据，只记录元素数量，慢日志按真实完成耗时判断
- `WebFluxUtils`：所有方法显式接收 `ServerWebExchange`；Session、Principal、请求体和响应写入使用 `Mono`/`Flux`，不提供 Servlet 风格的阻塞输入输出流
- `VeloWebFluxConfigurer`：复用 `velo.spring-converter`、`velo.xss` 和 `velo.web.cors` 配置，提供日期转换、XSS 字符串转换和 CORS
- `VeloWebFluxExceptionHandler`、`VeloValidationWebFluxExceptionHandler`：与 Servlet 异常基类一样只提供可继承逻辑，不自动注册，具体实现类仍需显式添加 `@RestControllerAdvice`

Controller 日志序列化复用 WebFlux 实际配置的 `HttpMessageWriter`，不直接绑定 Jackson 2 或 Jackson 3；因此 Boot 4 使用 Jackson 3、显式启用 Jackson 2 或用户自定义 WebFlux JSON 编解码器时都可以工作。

WebFlux 响应式注解切面说明：

- 响应式方法需要将返回类型声明为 `Mono`、`Flux` 或其他 Reactive Streams `Publisher`；如果方法声明为 `Object` 但运行时返回 Publisher，Velo 无法在同步切面与响应式切面之间可靠分流，按同步方法处理
- 响应式切面与同步切面共存，使用相同注解、配置项和 `velo.aspect-order.*` 顺序；同步切面会放行 Publisher，避免在组装阶段重复加锁、限流或写日志
- JDK、Redis、Redisson 内置锁处理器均支持跨 Reactor 线程的令牌式加锁与释放。用户自定义 `LockHandler` 如需用于 WebFlux `@Lock`，必须同时实现 `ReactiveLockHandler`；只有同步 `lock/unlock` 实现时，响应式 `@Lock` 会在订阅时明确报错，不会尝试使用不安全的线程绑定释放方式
- Redis/JDK 的响应式锁调用会放到 bounded-elastic 调度器，Redisson 使用显式 thread id 的异步 API；业务 Publisher 本身仍由应用的 Reactor 调度策略决定
- 响应式注解切面自动配置与 Web 层配置分离，因此 `velo.web.enabled=false` 不会关闭这些注解能力；`velo.idempotent.enabled`、`velo.rate-limit.enabled`、`velo.lock.enabled` 分别控制对应并发能力，`@InvokeLog` / `@SlowLog` 是否输出由注解决定，并受 `velo.log.enabled` 总闸控制

### 4. Feign 调用日志

如果项目中存在 `@FeignClient`，Velo 会按统一调用日志格式记录 Feign 调用，并自动透传 `X-Trace-Id`。

配置项：

```yaml
velo:
  feign:
    enabled: true
  log:
    feign:
      enabled: true
    invocation:
      max-payload-length: -1
    trace:
      enabled: true
      feign-propagation-enabled: true
```

说明：

- 默认开启
- 每次调用输出进入和退出两条日志，记录 client 名、HTTP 方法、映射路径、耗时、入参与响应体或异常摘要
- 日志格式和 Controller、`@InvokeLog` 保持一致，便于联调排查
- 暂不记录 header，只保留调试常用关键信息
- `max-payload-length` 为正数时，过长 payload 会按配置长度截断
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示不序列化 payload 并记录为 `disabled`
- 如果不需要这层默认日志，关闭 `velo.log.feign.enabled`；该配置不会关闭 `@InvokeLog` / `@SlowLog`

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
`VeloValidationWebExceptionHandler`；WebFlux 使用 `io.github.luminion.velo.webflux.exception` 下的
`VeloWebFluxExceptionHandler` 和 `VeloValidationWebFluxExceptionHandler`。两套都是可复用的异常处理基类，
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
  excel:
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
- 后端依赖是否就绪：`backend=AUTO` 会按 `REDISSON -> REDIS -> CAFFEINE -> JDK` 选择；若期望用 Redis 却走了本地实现，检查 classpath 与连接配置
- 开启调试日志观察：

```yaml
logging:
  level:
    io.github.luminion.velo: DEBUG
```

启动日志中也会有一条 INFO，显示各能力最终选用的后端，例如 `Idempotent enabled, backend handler: RedisIdempotentHandler`。

### Q2：Redis 连接失败时会怎样？

- `backend=AUTO`：Redis 不可用时会按顺序降级到 Caffeine 或 JDK 本地实现（仅单 JVM 有效，分布式场景下幂等/限流/锁会失去跨节点一致性）
- `backend=REDIS` / `REDISSON`：缺少对应依赖或连接 Bean 时应用启动失败（快速失败）

生产环境建议显式指定分布式后端，并配合健康检查确保 Redis 可用。

### Q3：如何调试 SpEL `key` 表达式？

- 确认已开启编译参数 `-parameters`，否则 `#userId` 这类按参数名引用无法解析，只能用 `#p0`、`#p1`
- 表达式解析为空字符串会抛出异常（幂等/限流的分桶 key 不允许解析为空白）
- 可单独用 `SpelExpressionParser` 写单测验证表达式取值是否符合预期

### Q4：限流被拒绝后多久可以重试？

令牌桶按固定速率恢复令牌，平均恢复一个令牌的间隔约为 `window / permits`。例如 `permits=10, window=1000` 约每 100ms 恢复 1 个令牌，被拒绝后立即重试可能仍失败，建议按该间隔退避重试。

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
