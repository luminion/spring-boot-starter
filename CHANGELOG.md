# 更新记录

## 1.3.0

### 不兼容变更
- Jackson 配置项改名：`velo.jackson.long-as-string` → `serialize-long-as-string`、`big-decimal-as-string` → `serialize-big-decimal-as-string`、`floating-as-string` → `serialize-floating-as-string`，并移除已弃用的 `unsafe-integer-as-string`（需更新配置文件）
- `IdempotentHandler` SPI 方法签名变更：`tryRecord` 增加 `token` 参数，`remove(key)` 改为 `removeIfMatch(key, token)`（仅影响自定义实现者）

### 新增
- 新增 `@LogPayloadIgnore` 注解，可标注在方法或类上控制调用日志中参数和返回值的可见性，适用于 Controller、Feign、`@InvokeLog`、`@SlowLog` 所有日志切面
- 异常结构化：`IdempotentException` / `RateLimitException` / `LockException` 携带 key、窗口、限额等字段，便于上层做友好提示
- 异常消息国际化：注解 `message` 写成 `{i18n.key}` 形式时从 `MessageSource` 解析，普通文本原样输出，未配置国际化的项目行为不变；附带 `velo/messages*.properties` 中英示例
- 新增 `velo.banner.enabled`（默认 `true`），启动时打印各能力开关概览横幅，可设为 `false` 关闭
- 幂等、限流、锁启用时打印 INFO 日志，显示实际选用的后端实现
- `velo.mode=CONSERVATIVE` 启动时输出 INFO 日志，列出被默认关闭的全局增强能力
- 配置元数据 hints：`velo.mode`、各 `backend`、`web.xss.strategy` 等枚举项在 IDE 中显示候选值下拉与中文说明
- CORS 新增 `velo.web.cors.allowed-origin-patterns`（默认 `*`），支持灵活的跨域源匹配
- 新增 `velo.aspect-order.*` 配置项，可自定义幂等、限流、锁、InvokeLog、SlowLog、ControllerLog、FeignLog 各切面的执行顺序
- 新增 `velo.cache.null-caching-enabled`（默认 `true`），设为 `false` 时关闭 null 值缓存
- 新增 `velo.cache.ttl-jitter-percentage`（默认 `0`），对缓存 TTL 添加 `±n%` 随机抖动，防止缓存雪崩

### 修复
- 幂等竞态修复：业务失败清除幂等记录时采用 token 比对，只删除本次请求写入的记录，不会误删并发请求在窗口内刚写入的新记录
- 缓存雪崩防护增强：TTL 抖动改为写入时按 key 独立计算，可同时缓解「不同缓存类型同时过期」和「同一缓存类型大量 key 同时过期」两类问题

### 调整
- `@Lock` 空 key 行为：移除强制非空校验，安静降级为方法级锁（基于 `类名#方法名`）
- `@Idempotent` 空 key 行为：降级为方法级幂等，并打印 WARN 日志提醒（通常不是期望行为）
- `WebUtils` 提取 servlet 无关逻辑到 common 模块，减少 jakarta/javax 重复代码

### 文档
- 新增配置优先级表格（命令行参数 > 配置文件 > 环境变量 > `velo.mode` 默认值）
- 新增限流 `key` 分桶语义对比表，以及小数 `permits` 换算公式和示例
- 补充幂等、限流、锁的 `key` 语义说明，以及 `@LogPayloadIgnore` 用法
- 新增故障排查 FAQ
- 删除文档中不存在的 `velo.log.invocation.sensitive-pattern` 配置说明

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
