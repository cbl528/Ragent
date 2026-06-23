package com.caobolun.infraai.chat;

import com.caobolun.framework.trace.RagStreamTraceSupport.StreamSpan;

public final class StreamSpanCallback extends ForwardingStreamCallback {

    private final StreamSpan span;

    public StreamSpanCallback(StreamCallback delegate, StreamSpan span) {
        super(delegate);
        this.span = span;
    }

    @Override
    protected void onFinish(boolean success, Throwable error) {
        if (success) {
            span.finishSuccess();
        } else {
            span.finishError(error);
        }
    }

    /**
     * 取消时由调用方触发：若 span 仍 RUNNING，按取消语义结束，避免 trace 行悬挂
     */
    public void onCancel() {
        span.finishCancelledIfRunning();
        finishExternally(false, null);
    }
}
