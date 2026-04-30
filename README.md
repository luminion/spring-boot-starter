# velo-spring-boot-starter

低侵入的 Spring Boot 工具 starter。基础 starter 只引入自动配置和 AOP，Web、Redis、Redisson、Cache、MyBatis-Plus、Excel、XSS 等技术栈依赖由业务项目按需添加。

## 快速开始

本地安装：

```bash
mvn -q -DskipTests install
```

Spring Boot 2：

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot2-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot 3：

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot3-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot 4：

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot4-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 功能与依赖

基础 starter 默认可用：

- `@Idempotent` 防重复提交：默认使用 JDK 本地兜底实现。
- `@RateLimit` 限流：默认使用 JDK 本地兜底实现。
- `@Lock` 锁：默认使用 JDK 本地兜底实现。
- Jackson 增强：随基础 starter 自动配置，Boot 2/3 使用 Jackson 2，Boot 4 使用 Jackson 3。
- 日志增强：随基础 starter 自动配置。

需要 Web MVC、Controller 请求日志、Web 参数日期格式化、CORS：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

需要 Redis 支持：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

需要 Spring Cache + Redis Cache 配置：

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

需要 Redisson 作为幂等、限流、锁后端：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

需要 Caffeine 作为幂等、限流、锁本地后端：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

需要 XSS 清洗：

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

需要 MyBatis-Plus 自动注册分页、乐观锁、防全表更新拦截器。

Spring Boot 2：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

Spring Boot 3 / 4：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
```

需要 Excel helper 或内置 Excel converter 时，按实际使用的库添加一个或多个依赖：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>4.0.3</version>
</dependency>
```

```xml
<dependency>
    <groupId>cn.idev.excel</groupId>
    <artifactId>fastexcel</artifactId>
    <version>1.3.0</version>
</dependency>
```

```xml
<dependency>
    <groupId>org.apache.fesod</groupId>
    <artifactId>fesod-sheet</artifactId>
    <version>2.0.1-incubating</version>
</dependency>
```

## 使用方式

防重复提交：

```java
import io.github.luminion.velo.idempotent.annotation.Idempotent;

@Idempotent(key = "#userId", ttl = 3)
public void submitOrder(Long userId) {
    // ...
}
```

限流：

```java
import io.github.luminion.velo.ratelimit.annotation.RateLimit;

@RateLimit(key = "#userId", permits = 10, ttl = 1)
public Object query(Long userId) {
    return null;
}
```

锁：

```java
import io.github.luminion.velo.lock.annotation.Lock;

@Lock(key = "#orderId", waitTimeout = 1, lease = 30)
public void pay(Long orderId) {
    // ...
}
```

Web MVC 日期参数格式化默认开箱即用。引入 `spring-boot-starter-web` 后，Controller 参数可以按统一格式绑定：

```java
@GetMapping("/orders")
public Object list(LocalDate date, LocalDateTime createTime) {
    return null;
}
```

默认格式：

- `LocalDate`：`yyyy-MM-dd`
- `LocalTime`：`HH:mm:ss`
- `LocalDateTime`：`yyyy-MM-dd HH:mm:ss`
- `Date`：`yyyy-MM-dd`

如果需要全局 Spring Converter Bean，而不仅是 Web MVC 参数绑定，显式开启：

```yaml
velo:
  spring-converter:
    date-time-enabled: true
```

XSS 清洗默认关闭。开启后会注册 Jsoup 清洗器，并在 Web MVC 字符串参数绑定时进行清洗：

```yaml
velo:
  web:
    xss:
      enabled: true
      strategy: RELAXED
```

Controller 请求日志默认开启；如果不需要：

```yaml
velo:
  web:
    request-logging-enabled: false
```

CORS 默认关闭；如果需要快速放开跨域：

```yaml
velo:
  web:
    allow-cors: true
```

幂等、限流、锁默认按 classpath 和 Bean 情况自动选择后端。也可以显式指定：

```yaml
velo:
  idempotent:
    backend: REDIS
    prefix: "idempotent:"
  rate-limit:
    backend: REDIS
    prefix: "rateLimit:"
  lock:
    backend: REDIS
    prefix: "lock:"
```

可选值：

- `AUTO`
- `REDISSON`
- `REDIS`
- `CAFFEINE`
- `JDK`

Jackson 常用配置：

```yaml
velo:
  date-time-format:
    date: yyyy-MM-dd
    time: HH:mm:ss
    date-time: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  jackson:
    enabled: true
    date-time-enabled: true
    unsafe-integer-as-string: true
    big-decimal-as-string: true
    floating-as-string: false
    enum-desc-enabled: true
    enum-name-suffix: name
    string-converter-enabled: true
```

Redis 与 Cache 常用配置：

```yaml
velo:
  redis:
    enabled: true
  cache:
    enabled: true
    prefix: ""
    separator: ":"
    default-ttl: 5m
    ttl:
      users: 10m
```

MyBatis-Plus 常用配置：

```yaml
velo:
  mybatis-plus:
    enabled: true
    pagination-enabled: true
    optimistic-locker-enabled: true
    block-attack-enabled: true
```

Excel converter 默认不自动注册；需要时开启：

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

## 常用开关

关闭某类自动配置：

```yaml
velo:
  web:
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
```

## 构建验证

```bash
mvn -q -DskipTests compile
mvn test
```
