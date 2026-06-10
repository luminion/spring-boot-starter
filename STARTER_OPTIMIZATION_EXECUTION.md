# Starter 优化执行说明

## 执行内容

本轮在统一日志重构基础上，继续围绕“引入即可开箱、功能可选、维护成本低”做了收口：

- 新增 `velo.mode`，默认 `OPINIONATED`，保留开箱即用体验
- 新增 `CONSERVATIVE` 模式，用低优先级默认值关闭会自动影响全局行为的能力
- 保守模式下仍保留 `@Idempotent`、`@RateLimit`、`@Lock`、`@InvokeLog`、`@SlowLog` 等显式注解能力
- 将 `IdempotentHandler` SPI 从锁语义改为记录语义：`tryLock(...)` 改为 `tryRecord(...)`，删除 `unlock(...)`
- 统一 `max-payload-length=0` 的 record 层语义，payload 直接记录为 `-`
- 修正默认 Slf4J 调用日志 writer 的 `velo.log.level=OFF` 行为，OFF 时成功和异常都不输出
- 更新 README / CHANGELOG，补齐模式、traceId、统一日志、XSS 边界、幂等 key、限流小数和关闭开关说明
- 清理根 `pom.xml` 中重复的 `project.build.sourceEncoding`

## 为什么这样执行

你的核心目标是个人项目里获得更好的开箱体验，同时不要让 starter 增加复杂度。

因此我选择默认保持积极增强：traceId、Controller/Feign 调用日志、Jackson、日期转换、缓存、Redis、MyBatis-Plus、Excel converter 这些常用能力默认可用。这样新项目引入后马上能看到效果。

同时新增 `velo.mode=CONSERVATIVE` 作为一键保守入口。它不是硬开关，而是低优先级默认值，业务项目显式写的配置仍然优先。这样既能给重视开箱体验的项目默认效果，也能给谨慎接入的项目一个简单退路。

幂等 SPI 我没有继续沿用 `tryLock/unlock` 命名，因为幂等不是临界区互斥，成功记录后应该等 TTL 过期，而不是方法执行完释放。继续叫锁会误导自定义实现，所以这次直接做 breaking change。

## 本应确认但我自行选择的点

1. `velo.mode` 默认值

选择：默认 `OPINIONATED`。

原因：你明确更看重开箱体验。默认保守虽然更稳，但会让 starter 引入后“没感觉”，不符合这个项目的目标。

2. 保守模式关闭哪些能力

选择：只关闭会自动影响全局行为的默认项，包括 traceId、Controller/Feign 自动调用日志、Jackson、日期转换、MyBatis-Plus、Redis、Cache、Excel converter 自动注册。

原因：显式注解能力本身需要用户主动标注，不属于无感全局影响，保留可用更符合“功能可选”。

3. 旧日志注解和旧配置是否兼容

选择：不兼容，直接移除旧三段式日志注解、旧 writer SPI 和旧配置。

原因：这是个人项目，旧能力使用少。继续兼容会把日志系统重新拆成两套，后续维护成本不值得。

4. 幂等 SPI 是否保留旧方法名

选择：不保留，改成 `tryRecord(...)` 并删除 `unlock(...)`。

原因：幂等语义应由 TTL 控制重复提交窗口，不应在方法结束后释放。

5. `max-payload-length=0` 的表现

选择：record 层直接记录为 `-`。

原因：这样默认 Slf4J writer 和自定义 `InvocationLogWriter` 看到的语义一致，不需要每个 writer 自己处理空字符串。

## 验证

已执行：

```powershell
mvn -T 8 -q -pl velo-autoconfigure-common test
mvn -T 8 -q test
```

两个命令退出码均为 0。
