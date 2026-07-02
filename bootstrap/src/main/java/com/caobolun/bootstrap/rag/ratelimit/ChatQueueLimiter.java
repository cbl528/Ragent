package com.caobolun.bootstrap.rag.ratelimit;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.caobolun.bootstrap.core.enums.SSEEventType;
import com.caobolun.bootstrap.core.memory.ConversationMemoryService;
import com.caobolun.bootstrap.rag.config.MemoryProperties;
import com.caobolun.bootstrap.rag.config.RAGRateLimitProperties;
import com.caobolun.bootstrap.rag.dto.CompletionPayload;
import com.caobolun.bootstrap.rag.dto.MessageDelta;
import com.caobolun.bootstrap.rag.dto.MetaPayload;
import com.caobolun.bootstrap.rag.ratelimit.FairDistributedRateLimiter.AcquireRequest;
import com.caobolun.bootstrap.rag.service.ConversationGroupService;
import com.caobolun.framework.context.UserContext;
import com.caobolun.framework.convention.ChatMessage;
import com.caobolun.framework.web.SSEEmitterSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * SSE 全局并发限流入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatQueueLimiter {

    private static final String REJECT_MESSAGE = "系统繁忙，请稍后再试";
    private static final String RESPONSE_TYPE = "response";

    private final FairDistributedRateLimiter chatRateLimiter;
    private final Executor chatEntryExecutor;
    private final RAGRateLimitProperties rateLimitProperties;
    private final ConversationMemoryService memoryService;
    private final ConversationGroupService conversationGroupService;
    private final MemoryProperties memoryProperties;


    /**
     * 入队限流
     * @param question 提问的内容
     * @param conversationId 会话 ID
     * @param emitter SSE 发送器
     * @param onAcquire 获取令牌的回调
     */
    public void enqueue(String question, String conversationId, SseEmitter emitter, Runnable onAcquire) {
        // 全局限流开关关闭
        if (!Boolean.TRUE.equals(rateLimitProperties.getGlobalEnabled())) {
            try {
                // 限流关闭，直接在线程池中执行
                chatEntryExecutor.execute(onAcquire);
            } catch (RejectedExecutionException ex) {
                log.warn("直通分支线程池拒绝任务，转 reject 流程", ex);
                handleReject(question, conversationId, emitter);
            }
            return;
        }
        // 限流开启，通过Redis ZSET 公平限流器排队
        chatRateLimiter.acquire(AcquireRequest.builder()
                // 最大等待时间
                .maxWaitMillis(TimeUnit.SECONDS.toMillis(rateLimitProperties.getGlobalMaxWaitSeconds()))
                .onAcquired(onAcquire) // 获取令牌的回调
                .onTimeout(() -> handleReject(question, conversationId, emitter)) // 超时的回调
                .onAcquiredExecutor(chatEntryExecutor)
                .cancelBinder(cancel -> {
                    emitter.onCompletion(cancel);
                    emitter.onTimeout(cancel);
                    emitter.onError(e -> cancel.run());
                })
                .build());
    }

    // ==================== Reject 业务 ====================

    private void handleReject(String question, String conversationId, SseEmitter emitter) {
        RejectedContext context = null;
        try {
            context = recordRejectedConversation(question, conversationId, resolveUserId());
        } catch (Exception ex) {
            // 记录失败不能阻塞 emitter，否则前端永远收不到 DONE
            log.warn("记录 reject 会话失败，仍向前端发送 DONE", ex);
        }
        sendRejectEvents(emitter, context);
    }

    private RejectedContext recordRejectedConversation(String question, String conversationId, String userId) {
        if (StrUtil.isBlank(question) || StrUtil.isBlank(userId)) {
            return null;
        }

        String actualConversationId;
        boolean isNewConversation;
        if (StrUtil.isBlank(conversationId)) {
            // 入参未带 conversationId：刚生成的雪花 ID 不可能命中已有会话，跳过 existence 查询
            actualConversationId = IdUtil.getSnowflakeNextIdStr();
            isNewConversation = true;
        } else {
            actualConversationId = conversationId;
            isNewConversation = conversationGroupService.findConversation(actualConversationId, userId) == null;
        }

        memoryService.append(actualConversationId, userId, ChatMessage.user(question));
        String messageId = memoryService.append(actualConversationId, userId, ChatMessage.assistant(REJECT_MESSAGE));

        String title = Strings.EMPTY;
        if (isNewConversation) {
            // append(USER) 内部会触发 conversationService.createOrUpdate（含 LLM 生成标题），此处回查拿到生成结果
            var conversation = conversationGroupService.findConversation(actualConversationId, userId);
            title = conversation != null ? conversation.getTitle() : Strings.EMPTY;
            if (StrUtil.isBlank(title)) {
                title = buildFallbackTitle(question);
            }
        }
        String taskId = IdUtil.getSnowflakeNextIdStr();
        return new RejectedContext(actualConversationId, taskId, messageId, title);
    }

    private String buildFallbackTitle(String question) {
        if (StrUtil.isBlank(question)) {
            return Strings.EMPTY;
        }
        int maxLen = memoryProperties.getTitleMaxLength() != null ? memoryProperties.getTitleMaxLength() : 30;
        String cleaned = question.trim();
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);
    }

    private void sendRejectEvents(SseEmitter emitter, RejectedContext rejectedContext) {
        SSEEmitterSender sender = new SSEEmitterSender(emitter);
        if (rejectedContext != null) {
            sender.sendEvent(SSEEventType.META.value(), new MetaPayload(rejectedContext.conversationId, rejectedContext.taskId));
            sender.sendEvent(SSEEventType.REJECT.value(), new MessageDelta(RESPONSE_TYPE, REJECT_MESSAGE));
            sender.sendEvent(SSEEventType.FINISH.value(),
                    new CompletionPayload(String.valueOf(rejectedContext.messageId), rejectedContext.title));
        }
        sender.sendEvent(SSEEventType.DONE.value(), "[DONE]");
        sender.complete();
    }

    private String resolveUserId() {
        String userId = UserContext.getUserId();
        if (StrUtil.isNotBlank(userId)) {
            return userId;
        }
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private record RejectedContext(String conversationId, String taskId, String messageId, String title) {
    }
}
