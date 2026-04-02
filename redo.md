

```markdown
# 任务：项目重命名 lumos → velo，包名迁移  →  io.github.luminion.velo

## 背景

这是一个多模块 Spring Boot Starter 项目，支持 Boot 2/3/4，按 common / javax / jakarta / jackson2 / jackson3 拆分自动配置。现在需要完命名从 lumos 迁移到 velo。

## 1. 目录与模块重命名

| 旧名称 | 新名称 |
|---------|--------|
| 根目录 `spring-boot-starter/` | `velo-spring-boot/` |
| `lumos-autoconfigure-common` | `velo-autoconfigure-common` |
| `lumos-autoconfigure-javax` | `velo-autoconfigure-javax` |
| `lumos-autoconfigure-jakarta` | `velo-autoconfigure-jakarta` |
| `lumos-autoconfigure-jackson2` | `velo-autoconfigure-jackson2` |
| `lumos-autoconfigure-jackson3` | `velo-autoconfigure-jackson3` |
| `lumos-spring-boot2-autoconfigure` | `velo-spring-boot2-autoconfigure` |
| `lumos-spring-boot2-starter` | `velo-spring-boot2-starter` |
| `lumos-spring-boot3-autoconfigure` | `velo-spring-boot3-autoconfigure` |
| `lumos-spring-boot3-starter` | `velo-spring-boot3-starter` |
| `lumos-spring-boot4-autoconfigure` | `velo-spring-boot4-autoconfigure` |
| `lumos-spring-boot4-starter` | `velo-spring-boot4-starter` |
| `lumos-bom-boot2` | `velo-spring-boot2-dependencies` |
| `lumos-bom-boot3` | `velo-spring-boot3-dependencies` |
| `lumos-bom-boot4` | `velo-spring-boot4-dependencies` |

## 2. groupId 变更

```
旧: io.github.luminion
新: io.velo
```

所有 pom.xml 中的 `<groupId>`、依赖引用中的 `<groupId>` 全部替换。

## 3. Java 包名变更

```
旧: io.github.luminion.**
新: io.velo.**
```

- 所有 `src/main/java/io/github/luminion/` 目录重命名为 `src/main/java/io/velo/`
- 所有 `src/test/java/io/github/luminion/` 目录重命名为 `src/test/java/io/velo/`
- 所有 `.java` 文件中的 `package io.github.luminion` → `package io.velo`
- 所有 `.java` 文件中的 `import io.github.luminion` → `import io.velo`
- 保持包内子路径结构不变，例如：
  - `io.github.luminion.autoconfigure.lock` → `io.velo.autoconfigure.lock`
  - `io.github.luminion.autoconfigure.jackson` → `io.velo.autoconfigure.jackson`

## 4. 配置属性前缀变更

```
旧: lumos.*
新: velo.*
```

涉及位置：
- `@ConfigurationProperties(prefix = "lumos.xxx")` 中的 prefix 改为 `velo.xxx`
- `application.yml` / `application.properties` 中的 key
- `additional-spring-configuration-metadata.json` 中的 name
- `spring-configuration-metadata.json`（如果有手写的）
- README 文档中的配置示例

## 5. 自动配置注册文件

更新以下文件中的全限定类名（`io.github.luminion` → `io.velo`）：
- `META-INF/spring.factories`（Boot 2）
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Boot 3+）

## 6. POM 文件全量更新

对所有 `pom.xml` 执行：
- `<groupId>` 改为 `io.velo`
- `<artifactId>` 按照第 1 节的映射表替换
- `<module>` 标签中的模块名替换
- `<parent>` 中的 groupId 和 artifactId 同步替换
- `<name>`、`<description>` 中的 lumos / luminion 替换为 velo
- 内部依赖引用（dependency 中的 artifactId 和 groupId）同步替换
- 属性变量名如 `<lumos.version>` 改为 `<velo.version>`
- `<url>`、`<scm>` 等地址中的 luminion / lumos 替换为 velo（如果有）

## 7. 其他文件

- `README.md`：所有 lumos / luminion 替换为 velo
- `.flattened-pom.xml`：直接删除（构建时会重新生成）
- `aiplans/` 目录下的文档：保持原样不动
- `LICENSE`：不修改
- `.gitignore`：不修改
- `.vscode/`、`.codex/`：不修改

## 8. 注意事项

- 不要修改任何业务逻辑代码，只做重命名
- 不要改变模块之间的依赖关系
- 不要改变目录的层级结构（除了上面列出的重命名）
- 替换必须精确：`io.github.luminion` 整体替换，不要误伤其他 `io.github.*` 的内容
- 字符串 `lumos` 的替换需确认上下文是本项目相关的，不要误伤第三方依赖或注释中无关的内容
- 所有替换完成后，列出完整的变更文件清单供我确认
```