# Velo Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.luminion/velo-spring-boot3-starter)](https://central.sonatype.com/artifact/io.github.luminion/velo-spring-boot3-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/luminion/spring-boot-starter?style=social)](https://github.com/luminion/spring-boot-starter)

Velo Spring Boot Starter 是一组低侵入的 Spring Boot 自动配置扩展。
项目以 `velo.*` 作为统一配置入口，围绕并发控制、缓存、Jackson、Redis、MyBatis-Plus、Excel、日志、XSS 以及 Web MVC 常用增强提供开箱能力。


首次接入可先查看 [版本与兼容性](#版本与兼容性) 和 [Maven 依赖](#maven-依赖)。

## 功能特性

- 统一的 `velo.*` 配置模型，集中管理各类自动配置开关
- 提供 `@Idempotent`、`@RateLimit`、`@Lock` 三类并发控制能力
- 支持 Redis / Redisson / Caffeine / JDK 多后端，并在启动时按依赖和 Bean 条件自动选择
- 提供 Spring Cache + Redis Cache 的统一 TTL 和 key 前缀配置
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

### 验证与发布

- 在 Java 17 下运行 `mvn -T 4 verify` 可执行完整构建；CI 还分别验证 Boot 2.7 / Java 8、Boot 3.2 与当前 3.x / Java 17、Boot 4.0 / Java 17。
- 真实 Redis 集成测试在设置 `VELO_TEST_REDIS_URL=redis://localhost:6379` 后执行；CI 会启动独立 Redis 服务。未设置时该组测试跳过，不影响本地单元测试。
- Maven Central 发布由 GitHub Actions 的 **Verify and publish** 工作流手动触发，仅允许 `master` 分支且 `publish=true`。兼容性矩阵全部通过后才执行发布。发布环境需配置 `CENTRAL_USERNAME`、`CENTRAL_PASSWORD`、`MAVEN_GPG_KEY`、`MAVEN_GPG_PASSPHRASE` 四项密钥。
- 本地 `mvn deploy` 默认不会自动公开发布包；确认已完成验证并需要公开发布时显式使用 `mvn -Ppublish deploy`。

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
- 缓存值序列化器优先复用容器中的 `RedisSerializer<Object>`；没有显式 Bean 时使用 `RedisSerializer.json()`，跟随当前 Spring Data Redis 版本的原生 JSON 实现。容器中存在多个候选且未标 `@Primary` 时，同样回退 `RedisSerializer.json()` 并打 WARN（不会导致启动失败）
- Boot 2 / 3 默认使用 Jackson 2 的 `GenericJackson2JsonRedisSerializer`（对应 Spring Data Redis 2.x / 3.x），写入 JSON 类型元数据，因此 `Object` / POJO 可以反序列化回原类型，而不是默认退化为 `LinkedHashMap`
- Boot 4 默认使用 `RedisSerializer.json()`，由 Spring Data Redis 4.x 选择 Jackson 3 的 `GenericJacksonJsonRedisSerializer`；即使 Jackson 2 / 3 同时存在，也不会按类路径猜测切换，默认仍使用 Jackson 3
- Boot 4 如需让 Redis 使用 Jackson 2，应引入官方 `spring-boot-jackson2` 及 Jackson 2 依赖，并显式注册一个 `RedisSerializer<Object>` Bean，例如 `GenericJackson2JsonRedisSerializer`；Spring Data Redis 4.x 仍保留该类用于兼容或迁移旧数据，但已标记为后续移除，不作为 Boot 4 默认实现
- Jackson 2 与 Jackson 3 的 Redis JSON 输出可能存在差异；从 Boot 2 / 3 切换到 Boot 4 时，应先规划旧数据读取、迁移或 key 空间隔离，不要默认认为历史值可以无缝混读
- **安全提示（多态反序列化）**：上述 JSON 序列化器为支持 `Object` / POJO 回读会写入并按类型元数据（`@class`）反序列化，且使用宽松的类型校验（与 Spring Data Redis 原生行为一致）。若 Redis 未鉴权或被写入恶意 `@class` 载荷，存在反序列化攻击面。建议 Redis 启用鉴权与网络隔离；如需收敛，可自行注册基于 `BasicPolymorphicTypeValidator` 白名单的 `RedisSerializer<Object>` Bean，Velo 会复用该 Bean

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

Controller、Feign、`@InvokeLog` 和任务入口共用一个 `InvocationLogEngine`。切面只提供调用目标、方法、参数和协议头；引擎解析功能策略并完成调用生命周期，`LogValueFormatter` 转换对象，`InvocationLogWriter` 输出记录。每个功能单独一行，`==>` 表示调用前，`<==` 表示调用完成。

```text
[controller][127.0.0.1 GET /users/{id}] ==> entryArgs = {"id":1}
[controller][127.0.0.1 GET /users/{id}] ==> requestHeaders = {"X-Demo":["visible"]}
[controller][127.0.0.1 GET /users/{id}] <== exitArgs = {"id":1}
[controller][127.0.0.1 GET /users/{id}] <== exitResult = {"name":"Tom"}
[controller][127.0.0.1 GET /users/{id}] <== responseHeaders = {"X-Result":["ok"]}
[controller][127.0.0.1 GET /users/{id}] <== slow = {"costMs":1200,"thresholdMs":1000}
[invoke][find()] <== error = {"type":"java.lang.IllegalArgumentException","message":"参数无效"}
[feign][remote() GET /log/remote] <== exitResult = {"message":"hello"}
[scheduled][run()] <== slow = {"costMs":5,"thresholdMs":0}
[xxl-job][run()] <== slow = {"costMs":5,"thresholdMs":0}
```

固定结构为 `[入口类型][调用目标] 箭头 功能名 = 内容`。入口类型为 `controller/invoke/feign/scheduled/xxl-job`；功能名为 `entryArgs/exitArgs/exitResult/requestHeaders/responseHeaders/slow/error`，慢调用和异常分别使用独立对象，异常摘要不附带堆栈。类名由日志框架输出，Invoke、Scheduled 和 XXL-Job 的调用目标只显示方法名。

正文不重复打印 `traceId`、`source`、`event`、`invocationId`。默认日志格式通过 MDC 在级别位置显示 traceId。自定义 `InvocationLogWriter` 仍可读取记录中的来源、traceId 和 invocationId。

| 功能注解 | Controller 默认 | Feign 默认 | Invoke 默认 | 默认级别 |
| --- | --- | --- | --- | --- |
| `EntryArgs` | 开 | 开 | 开 | INFO |
| `ExitArgs` | 关 | 关 | 关 | INFO |
| `ExitResult` | 关 | 关 | 开 | INFO |
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

每个功能都有独立 `enabled` 和 `level`，支持 Spring Boot `LogLevel`，`OFF` 关闭该功能。没有普通耗时 `CostLog`；将 SlowLog 阈值设为 `0`、级别设为 `INFO` 即可记录所有耗时。阈值单位毫秒，应大于等于 0。耗时从入口载荷输出后计到业务方法或 Future 完成，结束日志本身不计入耗时。

```yaml
velo:
  log:
    enabled: true
    defaults:
      max-payload-length: 1024
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

默认使用应用已有的 Jackson 2/3 Mapper，复用字段忽略、日期格式、命名规则和自定义模块，非 Web 应用也可用；没有 Mapper 时使用 `String.valueOf`。可使用 `@JsonIgnore` 或 MixIn 隐藏 DTO 字段。Jackson 转换失败输出 `serialization-failed`，不会回退到可能暴露敏感字段的 toString。HttpEntity/ResponseEntity 返回值只转换 body，原响应对象保持不变。流、Servlet 等技术对象省略内容；循环容器和异常深度不会无限遍历。

`max-payload-length=-1` 默认不限长，正数截断最终载荷字符串，`0` 关闭参数、结果及协议头载荷日志的输出和序列化，慢调用/异常摘要仍可输出。限长不限制对象遍历或序列化的工作量。框架不按字段名称自动脱敏；协议头的空 allowlist 表示全部头，启用采集时应设置需要的白名单。Feign 在底层客户端构建请求后捕获最终一次尝试的真实请求/响应头，因此请求头日志可能出现在完成阶段；逻辑请求目标使用方法名、HTTP 方法和映射路径，入口和完成阶段保持一致。Controller 响应头取方法或 Future 完成时已设置的头，不代表 Servlet 最终提交后的完整响应。

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
- Feign：发送快照中的传播头，下游 HTTP 入口使用相同协议即可沿用，实现 A→B 一致。默认透传 `traceparent/tracestate`，保留上游 parent-id 和采样标志，不生成每次调用的 span。没有上层上下文时由引擎建立作用域；仅使用裸拦截器时生成出站头，不给调用线程永久写 MDC。
- 常见 Spring 执行器：提供 `MdcTaskDecorator` Bean，Boot 支持 TaskDecorator 的自动配置执行器会采用它。提交时复制 MDC 和完整链路快照，任务内复用或调用解析器生成，执行后恢复工作线程。不会复制 Servlet 请求、响应或应用自己的 ThreadLocal。用户已有 TaskDecorator 时保留用户 Bean；自定义线程池需要自行安装该装饰器。
- `CompletionStage`：日志仅观察原 Future，返回同一对象，保留取消语义。输出完成日志时临时安装完整快照并恢复回调线程上下文；不传播到用户自定义公共池、线程或任意异步回调。取消是否中断实际工作由原 Future 决定。
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
- 若容器没有 `RedisSerializer<Object>`，starter 使用 `RedisSerializer.json()` 作为回退；Boot 2/3 跟随对应 Spring Data Redis 的 Jackson 2 实现，Boot 4 使用 Jackson 3 实现。多个候选且未标 `@Primary` 时同样回退默认并打 WARN
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
- 默认打印 Spring 绑定后的入参；结果载荷默认关闭，达到阈值打印独立慢调用日志，异常时打印独立 WARN 摘要
- 会过滤掉原始 query string，避免把敏感查询串直接打到日志中
- `max-payload-length` 为正数时，过长 payload 会按配置长度截断
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示不序列化或输出载荷日志
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
- 前缀为 `[feign][方法名() HTTP方法 接口路径]`，例如 `[feign][remote() GET /log/remote]`；类名由日志框架输出，不重复打印 client 名或 contextId
- 接口路径取 Spring MVC 映射模板，例如 `/users/{id}`，不展开路径变量或拼接查询参数；无法解析映射时省略缺失部分，保留方法名
- 默认打印入参，结果默认关闭，慢调用和异常各输出独立日志
- 日志格式和 Controller、`@InvokeLog` 保持一致，便于联调排查
- 请求头和响应头默认不采集，可通过独立配置或注解启用
- `max-payload-length` 为正数时，过长 payload 会按配置长度截断
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示不序列化或输出载荷日志
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
- 后端依赖是否就绪：`backend=AUTO` 会在启动时按 `REDISSON -> REDIS -> CAFFEINE -> JDK` 选择；若期望用 Redis 却走了本地实现，检查 classpath 与 Redis Bean 配置
- 开启调试日志观察：

```yaml
logging:
  level:
    io.github.luminion.velo: DEBUG
```

启动日志中也会有一条 INFO，显示各能力最终选用的后端，例如 `Idempotent enabled, backend handler: RedisIdempotentHandler`。

### Q2：Redis 连接失败时会怎样？

- `backend=AUTO` 只在启动配置阶段按类路径和 Bean 条件选择后端。Redis/Redisson 的相应条件都不满足时才会选用 Caffeine/JDK 本地后端；不会测试 Redis 连接，也不会在运行期间自动切换后端。
- 已选中 Redis/Redisson 后，连接失败可能阻止启动或使相应调用失败、抛异常，不会转为本地幂等、限流或锁。Redis Bean 已存在但服务不可达时，`AUTO` 也可能选中该后端。
- `backend=REDIS` / `REDISSON` 缺少对应依赖或 Bean 时按显式配置的条件快速失败。

生产环境建议显式指定分布式后端，并配合健康检查确保 Redis 可用；业务侧应决定连接故障时如何处理请求。

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
