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

@Idempotent(key = "#userId", ttl = 3)
public void submitOrder(Long userId) {
    // ...
}
```

说明：

- `backend` 可选 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`、`JDK`
- `AUTO` 模式下按自动配置顺序选择后端：`REDISSON -> REDIS -> CAFFEINE -> JDK`
- `prefix` 默认 `idempotent:`
- `key` 为空时会退化成 `类名#方法名`，但业务通常仍建议显式给出 SpEL key

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

@RateLimit(key = "#userId", permits = 10, ttl = 1)
public Object query(Long userId) {
    return null;
}
```

说明：

- `permits` 表示一个时间窗口内允许通过的最大请求数
- `ttl + unit` 定义窗口大小
- `backend` 与幂等一致，也支持 `AUTO/REDISSON/REDIS/CAFFEINE/JDK`
- `AUTO` 模式下默认选择顺序同幂等：`REDISSON -> REDIS -> CAFFEINE -> JDK`

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
```

使用示例：

```java
import io.github.luminion.velo.lock.annotation.Lock;

@Lock(key = "#orderId", waitTimeout = 1, lease = 30)
public void pay(Long orderId) {
    // ...
}
```

说明：

- `backend` 也支持 `AUTO`、`REDISSON`、`REDIS`、`CAFFEINE`、`JDK`
- `AUTO` 默认顺序为 `REDISSON -> REDIS -> CAFFEINE -> JDK`
- `waitTimeout` 默认 `0`，表示拿不到锁立即失败
- `lease` 默认 `30s`
- `REDIS` / `REDISSON` 更适合分布式场景，`CAFFEINE` / `JDK` 只保证单 JVM 内互斥

### 6. 日志

Velo 提供一套注解式方法日志能力，默认输出到 `Slf4J`。

基础 starter 默认可用，无需额外依赖。

关键配置：

```yaml
velo:
  log:
    enabled: true
    level: INFO
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

- `@InvokeLog` 是组合注解，等价于 `@ArgsLog + @ResultLog + @ErrorLog`
- `@SlowLog` 用于慢调用日志，默认时间单位为毫秒
- `velo.log.level` 控制 starter 默认 `Slf4JLogWriter` 的输出级别
- 如果你自己提供 `InvokeArgsWriter`、`InvokeResultWriter`、`ErrorLogWriter`、`SlowLogWriter` Bean，starter 会优先使用自定义实现

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
- 开启后会注册基于 Jsoup 的 `XssCleaner`
- 清洗发生在 Web MVC 的字符串参数绑定阶段，不会全局处理所有字符串字段

### 8. Jackson

Jackson 增强是基础 starter 的核心能力之一，会统一处理日期时间、超大整数、枚举派生字段和字符串转换。

基础 starter 默认可用，无需额外依赖。

关键配置：

```yaml
velo:
  jackson:
    enabled: true
    date-time-enabled: true
    unsafe-integer-as-string: true
    big-decimal-as-string: true
    floating-as-string: false
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
- `unsafe-integer-as-string=true` 时，仅对超出 JavaScript 安全整数范围的整数转成字符串
- `big-decimal-as-string=true` 默认开启
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

### 3. Controller 请求日志

有 Web 环境时，Velo 默认开启 Controller 请求日志切面。

配置项：

```yaml
velo:
  web:
    request-logging-enabled: true
```

说明：

- 默认开启
- 会记录请求方法、路径、入参与响应体
- 会过滤掉原始 query string，避免把敏感查询串直接打到日志中
- 过长 payload 会自动截断
- 如果不需要这层日志，直接关闭 `velo.web.request-logging-enabled`

### 4. CORS

Velo 提供一个偏“快速放开”的 CORS 开关，默认关闭。

配置项：

```yaml
velo:
  web:
    allow-cors: true
```

说明：

- 默认 `false`
- 开启后注册 `/**` 全局跨域规则
- 默认允许 `GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
- `allowedOriginPatterns("*")`
- `allowCredentials(true)`
- `maxAge(3600)`

---

## 常用关闭开关

如果你只想保留部分能力，可以按配置域逐项关闭：

```yaml
velo:
  web:
    enabled: false
    request-logging-enabled: false
    xss:
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
  log:
    enabled: false
  spring-converter:
    date-time-enabled: false
```

---

## 构建验证

```bash
mvn -q -DskipTests compile
mvn test
```
