# 日志重构执行说明

## 执行内容

本次把 Controller 日志、Feign 日志、`@InvokeLog`、`@SlowLog` 收敛成统一调用日志模型。

统一新增核心类型：

- `InvocationLogRecord`
- `InvocationLogSource`
- `InvocationLogWriter`
- `InvocationLogSupport`
- `Slf4JInvocationLogWriter`

默认日志输出从“入参一条、返回一条、异常一条”改为“一次调用一条完成态日志”：

```text
traceId=xxx source=controller target="127.0.0.1 GET /users/{id}" cost=12ms status=success args={...} result={...}
traceId=xxx source=feign target="user-service GET /users/{id}" cost=35ms status=error args={...} error="FeignException: timeout"
```

同时新增并默认开启 traceId 能力：

- 入站请求优先读取 `X-Trace-Id`
- 没有 `X-Trace-Id` 时自动生成
- 写入 SLF4J MDC，默认 key 为 `traceId`
- 响应头回写 `X-Trace-Id`
- Feign 出站请求自动透传 `X-Trace-Id`
- 未自定义 `logging.pattern.level` 时，自动追加 `%X{traceId}`

`velo.log.enabled=false` 作为日志能力总开关，会同时关闭统一调用日志、traceId 过滤器、Feign trace 透传和日志 pattern 注入。

配置收敛到：

```yaml
velo:
  log:
    enabled: true
    trace:
      enabled: true
    invocation:
      enabled: true
      max-payload-length: 2000
      include-args: true
      include-result: true
      include-error-stack-trace: false
      sensitive-fields:
        - password
        - token
        - authorization
      controller:
        enabled: true
      feign:
        enabled: true
      method:
        enabled: true
```

## 为什么这样执行

你的目标是开箱体验优先，并且项目主要自用。基于这个前提，我没有继续维护旧的三段式日志模型。

保留旧模型会导致：

- Controller、Feign、`InvokeLog` 三套相似逻辑长期重复
- `ArgsLog`、`ResultLog`、`ErrorLog` 与统一日志目标冲突
- 旧 writer SPI 和新 writer SPI 并存，扩展点变复杂
- 配置分散在 `velo.web`、`velo.feign`、`velo.log`，使用成本更高

所以我选择直接做 breaking change，把日志能力收敛成一套模型和一套扩展点。

## 本应确认但我自行选择的点

1. traceId 是否默认开启

选择：默认开启。

原因：你明确更看重开箱体验。traceId 默认开启能让用户自己的日志、Controller 日志、Feign 日志和方法日志天然串起来。

2. 是否兼容旧配置

选择：不兼容旧的 `velo.web.request-logging-*` 和 `velo.feign.request-logging-*`。

原因：旧配置本身是旧日志模型的一部分，继续兼容会让配置结构长期分裂。

3. 是否保留 `@ArgsLog`、`@ResultLog`、`@ErrorLog`

选择：删除。

原因：统一日志已经覆盖入参、结果和异常摘要，拆分注解会把模型重新拉回三段式。

4. 是否保留旧 writer SPI

选择：删除旧的 `InvokeArgsWriter`、`InvokeResultWriter`、`ErrorLogWriter`、`SlowLogWriter`，改用 `InvocationLogWriter`。

原因：统一 writer 更适合接入 Slf4J、MQ、数据库或审计系统。

5. 异常是否默认打印堆栈

选择：默认不打印堆栈，只打印异常类名和 message；可通过 `velo.log.invocation.include-error-stack-trace=true` 开启。

原因：Controller/Feign/方法日志统一只负责记录调用摘要，完整堆栈通常由上层异常处理、容器日志或 APM 记录，默认减少重复刷屏。

6. `@SlowLog` 是否删除

选择：保留。

原因：`@SlowLog` 是慢调用告警语义，不是旧三段式日志模型。保留后接入统一 record；如果和 `@InvokeLog` 同时存在，只打一条日志并标记 `slow=true`。

7. 敏感字段是否只处理顶层参数

选择：递归处理 `Map`、集合和数组里的敏感字段。

原因：既然配置叫 `sensitive-fields`，用户会预期 `password`、`token` 这类字段在嵌套请求对象中也不会泄漏。实现里限制了递归深度并处理循环引用，避免日志序列化阶段引入额外风险。

8. `velo.log.enabled=false` 是否只关闭 `VeloLogAutoConfiguration`

选择：作为日志总开关处理。

原因：traceId 会自动影响用户日志，属于日志功能的一部分。用户关闭 `velo.log.enabled` 时，不应继续注入 MDC、响应头、Feign header 或日志 pattern。

## 验证

已执行：

```powershell
mvn -T 8 -q -DskipTests compile
mvn -T 8 -q -pl velo-autoconfigure-common,velo-autoconfigure-jakarta -am test
mvn -T 8 -q test
```
