# 更新记录

## 1.1.0

- 新增 `velo.feign.request-logging-enabled` 与 `velo.feign.request-logging-max-payload-length`，支持按 Controller 日志风格输出 Feign 调试日志
- Controller 请求日志最大截取长度改为可配置，新增 `velo.web.request-logging-max-payload-length`，默认值为 `2000`
- `velo.web.xss.strategy=ESCAPE` 时，即使没有引入 `jsoup` 也可以生效

## 1.0.0

- 首个发版基线。
