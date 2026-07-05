# 更新记录

## 1.3.0

### 不兼容变更
- Jackson 配置项改名：`velo.jackson.long-as-string` → `serialize-long-as-string`、`big-decimal-as-string` → `serialize-big-decimal-as-string`、`floating-as-string` → `serialize-floating-as-string`，并移除已弃用的 `unsafe-integer-as-string`（需更新配置文件）
- `IdempotentHandler` SPI 方法签名变更：`tryRecord` 增加 `token` 参数，`remove(key)` 改为 `removeIfMatch(key, token)`（仅影响自定义实现者）

### 新增
- 新增 `@LogPayloadIgnore` 注解，可标注在方法或类上控制调用日志中参数和返回值的可见性，适用于 Controller、Feign、`@InvokeLog`、`@SlowLog` 所有日志切面
- 异常结构化：`IdempotentException` / `RateLimitException` / `LockException` 携带 key、窗口、限额等字段，便于上层做友好提示
- 异常消息国际化：注解 `message` 写成 `{i18n.key}` 形式时从 `MessageSource` 解析，普通文本原样输出，未配置国际化的项目行为不变；附带 `velo/messages*.properties` 中英示例
- 新增 `velo.banner.enabled`（默认 `false`），开启后启动时在控制台打印各能力开关概览横幅（直接输出到 `System.out`，不进入日志框架）；幂等/限流/锁展示实际生效的 handler 实现类，便于确认 `AUTO` 模式下的真实后端
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
- 限流令牌桶补充计算修复：高速率叠加长时间空闲时，令牌补充量因整型溢出变负导致限流永久卡死，现已修正（影响 JDK 与 Caffeine 两种本地限流器）
- Excel 读取修复：`Float` 类型单元格因类型声明错误始终被读成 `null`，现可正常解析（影响 EasyExcel / FastExcel / Fesod 三种实现）
- 幂等切面异常修复：业务失败回滚幂等记录时若清理动作自身抛异常（如 Redis 超时），不再覆盖原始业务异常，改为附加到 suppressed 保留现场
- 本地锁实现合并：Caffeine 不提供互斥锁 API，锁的本地实现只能基于 `ConcurrentHashMap + ReentrantLock`，与 JDK 完全一致；移除单独的 `CaffeineLockHandler`，`CAFFEINE` 档位复用 `JdkLockHandler`（`backend=CAFFEINE` 仍可用），本地锁只保留一份实现，用引用计数管理锁对象生命周期
- Redis 锁可重入修复：同线程重复获取同一把锁时按本地计数重入、不再自锁死，仅最外层释放才删除 Redis 锁；解锁仍用 Lua 脚本比对 owner token 保证原子
- 幂等 key 隔离修复：幂等 key 现以 `全限定类名#方法名` 为前缀再拼接 SpEL 结果，与限流分桶语义一致，避免不同方法使用相同 SpEL key（如都用 `#orderId`）时误共享同一幂等窗口
- 客户端 IP 解析修复：多级代理头全部为 `unknown` 时正确回退到直连地址；IPv6 环回地址（全展开 `0:0:0:0:0:0:0:1` 与压缩形 `::1`）在代理链场景下也能正确转换为 `127.0.0.1`
- 本地限流/幂等清理修复：容器关闭后并发请求触发的清理任务提交被拒不再向调用方抛出异常
- traceId 日志格式修复：移除失效的日期格式注入（属性名 `logging.pattern.date-format` 拼写错误，Spring Boot 实际读取 `logging.pattern.dateformat` 且不走 relaxed binding，从未生效）；traceId 仅作为增强追加到 level pattern，日期格式保持 Spring Boot 默认、不改变全局行为
- Jackson 大整数序列化统一：开启 `serialize-long-as-string` 时 `Long` 与 `BigInteger` 均无条件转字符串（此前 `BigInteger` 仅在超过 2^53 时才转，导致同类型字段时而 number 时而 string、契约不稳定）
- Jackson 3 String 序列化 null 处理对齐 Jackson 2：`null` 直接输出 JSON null、不再传入 `@JsonEncode` 转换函数，避免用户函数未防 null 时 NPE

### 调整
- `@Lock` 空 key 行为：移除强制非空校验，安静降级为方法级锁（基于 `全限定类名#方法名`）
- `@Idempotent` 空 key 行为：降级为方法级幂等，并打印 WARN 日志提醒（通常不是期望行为）
- `WebUtils` 提取 servlet 无关逻辑到 common 模块，减少 jakarta/javax 重复代码
- `@Lock` 支持看门狗：`lease = -1` 请求自动续约，锁随业务执行自动延长、结束时释放，适合耗时不确定的长任务；仅 Redisson 后端真正支持，Redis 简单实现会降级为固定默认租约并打印告警，本地实现忽略该值靠方法结束释放
- Web 异常提示健壮性：异常消息或 `Content-Type` 为 `null` 时返回兜底文案，不再出现 `null` 字样或潜在 NPE（影响 jakarta 与 javax 两个 Web 模块）
- 分布式锁/幂等误用提示：检测到 `setIfAbsent` 被 Redis 事务或 pipeline 延迟执行（返回 `null`）时打印 WARN，提示不应将锁/幂等操作包裹在 Redis 事务内
- Feign 调用日志：未配置 traceId 的 MDC key 时跳过 trace 处理，避免链路标识静默丢失

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
