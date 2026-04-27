# velo-spring-boot-starter

一个按 Spring Boot 官方 `starter` / `autoconfigure` 模式重构的工具箱工程，定位为低侵入、按需增强，提供幂等、限流、分布式锁、Jackson、Redis、缓存、MyBatis-Plus、Web 增强、XSS 和日志等能力。

## 模块结构

### 聚合与版本管理

- `velo-spring-boot-project`：父工程，统一插件与模块管理。
- `velo-spring-boot2-dependencies` / `velo-spring-boot3-dependencies` / `velo-spring-boot4-dependencies`：分别管理 Spring Boot 2/3/4 依赖版本。

### 公共与差异层

- `velo-autoconfigure-common`：公共注解、SPI、切面、异常、配置属性、工具类，以及核心、缓存、Redis、日期时间转换、限流、防重、锁等跨 Boot 版本自动配置。
- `velo-autoconfigure-javax`：`javax.*` / Servlet API 相关自动配置，供 Boot 2 使用。
- `velo-autoconfigure-jakarta`：`jakarta.*` / Servlet API 相关自动配置，供 Boot 3/4 使用。
- `velo-autoconfigure-jackson2`：Jackson 2 自动配置，供 Boot 2/3 使用。
- `velo-autoconfigure-jackson3`：Jackson 3 自动配置，供 Boot 4 使用。

### 对外发布模块

- `velo-spring-boot2-starter` / `velo-spring-boot2-autoconfigure`
- `velo-spring-boot3-starter` / `velo-spring-boot3-autoconfigure`
- `velo-spring-boot4-starter` / `velo-spring-boot4-autoconfigure`

## 功能特性

- `@Idempotent`：防重复提交，支持 SpEL，支持 Redis / Redisson / Caffeine / JDK 回退。
- `@RateLimit`：方法或类级限流，支持 SpEL；未指定 `key` 时按方法限流，指定 `key` 时按“方法 + SpEL 结果”分桶，支持 Redis / Redisson / Caffeine / JDK 回退。
- `@Lock`：分布式锁，支持 SpEL，支持 Redis / Redisson / JDK 回退。
- Jackson 增强：字符串处理、枚举展开、数字转字符串、日期时间格式统一。
- Redis 与 Cache 自动配置：统一 JSON 序列化。
- MyBatis-Plus 自动配置：分页、乐观锁、防全表更新。
- Web 增强：日期时间转换、可选 CORS、可选请求日志。
- XSS：提供清洗器与可选字符串转换能力。

## 快速开始

### 1. 本地安装

```bash
mvn -q -DskipTests install
```

### 2. 选择对应版本基础 starter

Spring Boot 2:

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot2-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

基础 starter 仅默认引入：

- `velo-spring-boot2-autoconfigure`
- `spring-boot-starter-aop`

Spring Boot 3:

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot3-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

基础 starter 仅默认引入：

- `velo-spring-boot3-autoconfigure`
- `spring-boot-starter-aop`

Spring Boot 4:

```xml
<dependency>
    <groupId>io.github.luminion</groupId>
    <artifactId>velo-spring-boot4-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

基础 starter 仅默认引入：

- `velo-spring-boot4-autoconfigure`
- `spring-boot-starter-aspectj`

### 3. 按需添加能力依赖

基础 starter 不再传递引入 Web、Validation、Redis、Cache、Mail、Redisson、MyBatis-Plus、Caffeine、Jsoup 等技术栈依赖；只有在项目确实需要对应能力时，才显式添加。

例如，Spring Boot 3 项目如果需要 Redis、Cache 和 Redisson：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

如果需要 Web / XSS / 请求日志能力：

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

如果需要 MyBatis-Plus：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
```

## 配置示例

```yaml
velo:
  rate-limit-prefix: rateLimit:
  idempotent-prefix: idempotent:
  lock-prefix: lock:
  log-level: INFO

  date-time-format:
    enabled: true
    date: yyyy-MM-dd
    time: HH:mm:ss
    date-time: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

  cache:
    enabled: true
    key-prefix: ""
    key-separator: ":"
    default-ttl-seconds: 3000
    ttl-map: {}

  jackson:
    enabled: true
    write-unsafe-integer-as-string: true
    write-big-decimal-as-string: true
    write-floating-point-as-string: false
    enum-description-enabled: false

  redis:
    enabled: true

  mybatis-plus:
    enabled: true

  web:
    allow-cors: false
    request-logging:
      enabled: false
    xss:
      string-converter-enabled: false
```

## 设计说明

- `starter` 模块不放业务代码，只聚合依赖。
- 基础 `starter` 只保留低侵入、普适性强的依赖；技术栈型依赖全部交给业务项目按需声明。
- 自动配置放在 `autoconfigure` 模块，按 `javax` / `jakarta` 和 Jackson 版本拆分。
- `core` / `cache` / `redis` / `converter` 等无 `javax` / `jakarta` 差异的自动配置统一收敛到 `velo-autoconfigure-common`。
- Redisson / Redis / Caffeine / JDK 等后端按自动装配顺序显式兜底，默认优先级为 `Redisson > Redis > Caffeine > JDK`。
- 请求日志和全局字符串 XSS 清洗默认关闭，改为显式开关。

## 构建与验证

```bash
mvn -q -DskipTests compile
mvn test
```
