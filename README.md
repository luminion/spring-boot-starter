# Velo Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.luminion/velo-spring-boot3-starter)](https://central.sonatype.com/artifact/io.github.luminion/velo-spring-boot3-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/luminion/spring-boot-starter?style=social)](https://github.com/luminion/spring-boot-starter)

Velo Spring Boot Starter 是一组低侵入的 Spring Boot 自动配置扩展。
项目以 `velo.*` 作为统一配置入口，围绕并发控制、缓存、Jackson、Redis、MyBatis-Plus、Excel、日志、XSS 和 Web MVC 常用增强提供开箱能力。


## 功能特性

- 统一的 `velo.*` 配置模型，集中管理各类自动配置开关
- 提供 `@Idempotent`、`@RateLimit`、`@Lock` 三类并发控制能力
- 支持 Redis / Redisson / Caffeine / JDK 多后端并自动降级
- 提供 Spring Cache + Redis Cache 的统一 TTL 和 key 前缀配置
- 提供 Jackson 日期时间、超大整数、枚举派生字段、字符串转换增强
- 提供 MyBatis-Plus 分页、乐观锁、防全表更新拦截器自动注册
- 提供 RedisTemplate 序列化风格统一能力
- 提供 Excel 扩展 converter 自动注册和 helper 工具类
- 提供注解日志、Controller 请求日志、XSS 清洗和 Web MVC 日期绑定增强

---

## 总览

Velo 默认使用 `velo.mode=OPINIONATED`，目标是引入 starter 后直接获得常用增强能力。

如果你希望 starter 只保留显式注解类能力，尽量不自动改变全局行为，可以切到保守模式：

```yaml
velo:
  mode: CONSERVATIVE
```

模式说明：

- `OPINIONATED`：默认模式，开启偏开箱即用的全局增强
- `CONSERVATIVE`：关闭容易自动影响应用行为的默认项，但 `@Idempotent`、`@RateLimit`、`@Lock`、`@InvokeLog`、`@SlowLog` 等显式注解仍可用
- `CONSERVATIVE` 只提供低优先级默认值，业务项目显式配置的属性优先级更高
- 保守模式下重新打开某类能力时，需要显式设置对应的 `enabled` 项，例如 `velo.jackson.enabled=true`
- 启用 `CONSERVATIVE` 后，starter 会在启动日志中输出一条 INFO，列出被默认关闭的能力，便于排查"为什么某全局增强没生效"

配置优先级（从高到低）：

| 优先级 | 来源 | 示例 |
| --- | --- | --- |
| 1 最高 | 命令行参数 | `--velo.log.trace.enabled=true` |
| 2 | application.yml / properties | `velo.log.trace.enabled: true` |
| 3 | 环境变量 | `VELO_LOG_TRACE_ENABLED=true` |
| 4 最低 | `velo.mode` 默认值 | `OPINIONATED` / `CONSERVATIVE` 注入的默认值 |

也就是说 `velo.mode` 注入的只是**最低优先级默认值**，业务项目任何显式配置都会覆盖它。

例如保守模式下重新打开 traceId：

```yaml
velo:
  mode: CONSERVATIVE
  log:
    trace:
      enabled: true
```

默认会自动影响全局行为的能力：

| 能力 | `OPINIONATED` 默认 | `CONSERVATIVE` 默认 | 说明 |
| --- | --- | --- | --- |
| traceId / MDC / 日志 pattern / 响应头 | 开启 | 关闭 | 影响用户自己的日志输出 |
| Controller 调用日志 | 开启 | 关闭 | Web 环境下自动记录请求调用 |
| Feign 调用日志 | 开启 | 关闭 | 存在 Feign 时自动记录远程调用 |
| Jackson 增强 | 开启 | 关闭 | 影响 JSON 序列化、反序列化扩展 |
| Spring Converter / Web MVC 日期绑定 | 开启 | 关闭 | 影响字符串到日期时间的全局转换 |
| MyBatis-Plus 拦截器 | 开启 | 关闭 | 自动补充分页、乐观锁、防全表更新 |
| RedisTemplate 自动补齐 | 开启 | 关闭 | 依赖 Redis classpath 与连接工厂 |
| Redis Cache 自动补齐 | 开启 | 关闭 | 依赖 Spring Cache / Redis 条件 |
| Excel converter 自动注册 | 开启 | 关闭 | helper 工具类不受影响 |


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

缓存雪崩防护（TTL 抖动）：

- `ttl-jitter-percentage` 默认 `0`（关闭），有效范围为 `0..100`。设为 `10` 表示每条缓存写入时 TTL 在原值 ±10% 内随机偏移；超出范围会导致应用启动失败
- 抖动在**每次写入时按 key 独立计算**，因此同一缓存名称下不同 key 也会获得不同过期时间，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类雪崩
- 抖动只影响实际写入 Redis 的过期时间，不改变 `default-ttl` / `ttl.<cacheName>` 的配置语义

### 2. Excel 自动配置

Velo 提供两层能力：

- Excel helper 工具类，随依赖引入即可直接使用
- 扩展 converter 自动注册，需显式开启 `velo.excel.converters.enabled`

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
    enabled: true
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

- `velo.excel.enabled` 默认开启
- `velo.excel.converters.enabled` 默认开启
- 一旦开启，starter 会根据 classpath 自动尝试向 EasyExcel、FastExcel、Fesod 注册扩展 converters
- 时间、日期、时区格式统一复用 `velo.date-time-format.*`
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

基础 starter 默认可用，无需额外依赖。

关键配置：

```yaml
velo:
  log:
    enabled: true
    level: INFO
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
      enabled: true
      max-payload-length: -1
      include-args: true
      include-result: true
      include-error-stack-trace: false
      controller:
        enabled: true
      feign:
        enabled: true
      method:
        enabled: true
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
```

说明：

- `traceId` 默认开启，会写入 MDC、响应头，并在 Feign 调用中透传
- 如果没有自定义 `logging.pattern.level`，会自动把 `%X{traceId}` 加到用户自己的日志中
- Controller、Feign 与 `@InvokeLog` 的进入日志格式为 `[target] ==> args=...`
- Controller、Feign 与 `@InvokeLog` 的退出日志格式为 `[target] <== cost=Xms result=...`；调用失败时输出异常摘要并使用 ERROR 级别
- `@SlowLog` 的阈值单位固定为毫秒，只在调用耗时超过阈值后输出一条独立慢日志，格式包含 `cost=Xms threshold=Yms`
- 慢日志级别由 `velo.log.slow.level` 控制，默认 WARN；调用异常且超过阈值时提升为 ERROR，设置为 `OFF` 时完全关闭独立慢日志
- 同时命中其他调用日志切面时，慢日志默认在 ENTRY、EXIT 日志之后最后输出
- 如果需要写入 MQ、数据库或审计系统，提供自定义 `InvocationLogWriter` Bean 即可
- `velo.log.level=OFF` 会关闭 Controller、Feign、`@InvokeLog` 的成功和异常输出；`velo.log.slow.level=OFF` 会关闭 `@SlowLog` 的独立慢日志；自定义 `InvocationLogWriter` 不受这些日志级别约束

敏感参数不打印（`@LogPayloadIgnore`）：

如果某些方法的入参或返回值包含密码、token 等敏感信息，可用 `@LogPayloadIgnore` 抑制其打印。被忽略的内容在日志中显示为 `-`，但调用本身（方法名、耗时、成功/异常状态）仍会记录。该注解对 Controller、Feign、`@InvokeLog`、`@SlowLog` 所有调用日志切面均生效，可标注在方法或类上。

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

XSS 清洗能力挂在 `velo.web.xss.*` 下，默认关闭。

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
  web:
    xss:
      enabled: true
      strategy: RELAXED
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

- `velo.web.xss.enabled` 默认 `false`
- `strategy` 可选 `NONE`、`ESCAPE`、`SIMPLE_TEXT`、`BASIC`、`BASIC_WITH_IMAGES`、`RELAXED`
- `ESCAPE` 不依赖 `jsoup`；其他 HTML 清洗策略必须引入 `jsoup`
- `ESCAPE` 且无 `jsoup` 时会走 Spring 转义；其他策略缺少 `jsoup` 时只打印 WARN，不注册 `XssCleaner`，也不会自动降级
- 清洗发生在 Web MVC 的字符串参数绑定阶段，包括 query/form/path 和普通对象参数中通过 MVC binder 绑定的 `String` 字段；不会接管 Jackson JSON 请求体反序列化，也不会全局处理所有字符串字段

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
    string-converter-enabled: true
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
- `string-converter-enabled=true` 时，`@JsonEncode` / `@JsonDecode` 会按函数类做字符串转换
- 日期时间格式依然复用 `velo.date-time-format.*`

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
- 若容器里已经存在同类 `InnerInterceptor` Bean，starter 不会覆盖
- 默认会把当前容器中的 `InnerInterceptor` 汇总进 `MybatisPlusInterceptor`

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
- Redis 连接地址、密码、数据库等仍然走标准 `spring.data.redis.*`
- starter 会尝试创建：
  - `redisTemplate`
  - `stringObjectRedisTemplate`
- 序列化器优先复用容器中的 `RedisSerializer<Object>`，通常会跟随 Velo 的 Jackson 配置保持一致

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
- `Date` -> `yyyy-MM-dd`

这些格式由 `velo.date-time-format.*` 统一控制。

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
    invocation:
      enabled: true
      max-payload-length: -1
      controller:
        enabled: true
    trace:
      enabled: true
```

说明：

- 默认开启
- 每次调用输出进入和退出两条日志；进入日志包含请求方法、controller 映射模板路径和入参，退出日志包含耗时、响应体或异常摘要
- 会过滤掉原始 query string，避免把敏感查询串直接打到日志中
- `max-payload-length` 为正数时，过长 payload 会按配置长度截断
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示 payload 记录为 `-`
- 如果不需要这层日志，关闭 `velo.log.invocation.controller.enabled`

### 4. Feign 调用日志

如果项目中存在 `@FeignClient`，Velo 会按统一调用日志格式记录 Feign 调用，并自动透传 `X-Trace-Id`。

配置项：

```yaml
velo:
  feign:
    enabled: true
  log:
    invocation:
      enabled: true
      max-payload-length: -1
      feign:
        enabled: true
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
- 当前默认 `max-payload-length=-1`，表示不限制长度；`0` 表示 payload 记录为 `-`
- 如果不需要这层日志，关闭 `velo.log.invocation.feign.enabled`

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

- 默认 `false`
- 开启后注册 `/**` 全局跨域规则
- 默认允许 `GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
- `allowedOriginPatterns("*")`，可通过 `velo.web.cors.allowed-origin-patterns` 覆盖
- `allowCredentials(true)`
- `maxAge(3600)`
- 旧配置 `velo.web.allow-cors` 已废弃，但仍向后兼容：与 `velo.web.cors.enabled` 任一为 `true` 即开启。新项目请使用 `velo.web.cors.enabled`

---

## 常用关闭开关

如果你希望尽量减少自动影响，优先使用：

```yaml
velo:
  mode: CONSERVATIVE
```

如果你只想保留部分能力，也可以按配置域逐项关闭：

```yaml
velo:
  feign:
    enabled: false
  web:
    enabled: false
    xss:
      enabled: false
  log:
    enabled: false
    invocation:
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
- 对应能力是否开启：确认未被 `velo.idempotent.enabled=false` 等关闭，也未处于 `velo.mode=CONSERVATIVE`（注意：这三类注解在 CONSERVATIVE 下仍可用，但需对应后端依赖存在）
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

可以。注解的 `message` 写成 `{i18n.key}` 形式时会从 Spring `MessageSource` 解析；普通文本则原样输出。未配置国际化的项目行为不变。详见「日志 / 异常提示」相关说明与 `velo/messages*.properties` 示例文件。

### Q6：缓存大量 key 同时过期（缓存雪崩）？

开启 TTL 抖动：`velo.cache.ttl-jitter-percentage=10`。抖动按 key 在写入时独立计算，可同时缓解「不同缓存类型同时过期」和「同一类型大量 key 同时过期」两类问题。
