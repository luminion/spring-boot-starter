package io.github.luminion.velo.log;

/**
 * Unified invocation log payload.
 */
public class InvocationLogRecord {

    private String traceId;

    private InvocationLogSource source;

    private String target;

    private long costMs;

    private boolean success;

    private String args;

    private String result;

    private String errorClass;

    private String errorMessage;

    private Throwable error;

    private boolean slow;

    /**
     * Logger name used by the log writer. When set, the writer logs under this class name
     * instead of its own, so the log output shows the actual intercepted class.
     */
    private String loggerName;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public InvocationLogSource getSource() {
        return source;
    }

    public void setSource(InvocationLogSource source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getCostMs() {
        return costMs;
    }

    public void setCostMs(long costMs) {
        this.costMs = costMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(String errorClass) {
        this.errorClass = errorClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean isSlow() {
        return slow;
    }

    public void setSlow(boolean slow) {
        this.slow = slow;
    }

    /**
     * 日志阶段。{@link InvocationPhase#ENTRY} 为进入记录，{@link InvocationPhase#EXIT}
     * 为退出记录。为 null 时以旧单行格式输出（慢日志等兼容场景）。
     */
    private InvocationPhase phase;

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public InvocationPhase getPhase() {
        return phase;
    }

    public void setPhase(InvocationPhase phase) {
        this.phase = phase;
    }
}
