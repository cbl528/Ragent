package com.caobolun.bootstrap.rag.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.caobolun.bootstrap.rag.handler.StreamCallbackFactory;
import com.caobolun.bootstrap.rag.handler.StreamTaskManager;
import com.caobolun.bootstrap.rag.pipeline.StreamChatPipeline;
import com.caobolun.bootstrap.rag.ratelimit.ChatQueueLimiter;
import com.caobolun.bootstrap.rag.service.RAGChatService;
import com.caobolun.bootstrap.rag.trace.StreamChatTraceRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class RAGChatServiceImpl implements RAGChatService {

    private final StreamChatPipeline streamChatPipeline;
    private final ChatQueueLimiter chatQueueLimiter;
    private final StreamCallbackFactory callbackFactory;
    private final StreamChatTraceRunner chatTraceRunner;
    private final StreamTaskManager taskManager;

    @Override
    public void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter) {
        String actualConversationId = StrUtil.isBlank(conversationId) ? IdUtil.getSnowflakeNextIdStr() : conversationId;
        String taskId = IdUtil.getSnowflakeNextIdStr();

    }

    @Override
    public void stopTask(String taskId) {

    }
}
