package io.github.luminion.velo.log.spi;

import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;

/**
 * 按入口替换公共块格式；不修改功能内容或直接写入日志。
 */
public interface InvocationLogContextFormatter {
    /**
     * 指定替换默认公共块的入口类别，同一入口只允许一个业务实现。
     */
    InvocationLogSource getSource();

    /**
     * 使用当前记录的公共信息快照生成公共块，不包含外层方括号。
     * 无协议上下文时 context 可能为 null；返回 null 或空字符串时，默认输出器省略公共块，功能正文仍正常输出。
     * 同一次调用的多条功能日志会分别执行本方法，不应依赖其他处理器的执行结果。
     */
    String format(LogInvocation invocation, InvocationLogContext context);
}
