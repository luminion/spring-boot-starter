# 更新记录

## 1.3.0
- 新增 `@LogPayloadIgnore` 注解，可标注在方法或类上控制调用日志中参数和返回值的可见性，适用于 Controller、Feign、`@InvokeLog`、`@SlowLog` 所有日志切面
- CORS 新增 `velo.web.cors.allowed-origin-patterns`（默认 `*`），支持灵活的跨域源匹配
- 新增 `velo.aspect-order.*` 配置项，可自定义幂等、限流、锁、InvokeLog、SlowLog、ControllerLog、FeignLog 各切面的执行顺序
- 新增 `velo.cache.null-caching-enabled`（默认 `true`），设为 `false` 时关闭 null 值缓存
- 新增 `velo.cache.ttl-jitter-percentage`（默认 `0`），对缓存 TTL 添加 `±n%` 随机抖动，防止缓存雪崩
- `WebUtils` 提取 servlet 无关逻辑到 common 模块，减少 jakarta/javax 重复代码

## 1.2.0
- 新增 `velo.mode`，默认 `OPINIONATED` 保持开箱体验；可设置 `CONSERVATIVE` 关闭全局行为类默认项，显式注解能力仍可用
- 重构日志能力为统一调用日志：Controller、Feign、`@InvokeLog`、`@SlowLog` 统一输出 `InvocationLogRecord`
- 默认开启 traceId：入站请求自动生成或接收 `X-Trace-Id`，写入 MDC，响应头回写，并在 Feign 调用中透传
- 新增 `InvocationLogWriter`，替代旧的 `InvokeArgsWriter`、`InvokeResultWriter`、`ErrorLogWriter`、`SlowLogWriter`
- 删除 `@ArgsLog`、`@ResultLog`、`@ErrorLog` 及三段式日志切面
- 日志配置迁移到 `velo.log.invocation.*` 与 `velo.log.trace.*`
- 移除旧的 `velo.web.request-logging-*` 与 `velo.feign.request-logging-*`
- `IdempotentHandler` SPI 语义调整：`tryLock(...)` 重命名为 `tryRecord(...)`，删除 `unlock(...)`
- `velo.log.invocation.max-payload-length=0` 时，调用日志 payload 统一记录为 `-`
- `velo.log.level=OFF` 时，默认 Slf4J 调用日志 writer 不再输出异常摘要

## 1.1.0

- 新增 `velo.feign.request-logging-enabled` 与 `velo.feign.request-logging-max-payload-length`，支持按 Controller 日志风格输出 Feign 调试日志
- Controller 请求日志最大截取长度改为可配置，新增 `velo.web.request-logging-max-payload-length`，默认值为 `2000`
- `velo.web.xss.strategy=ESCAPE` 时，即使没有引入 `jsoup` 也可以生效

## 1.0.0

- 首个发版基线。
