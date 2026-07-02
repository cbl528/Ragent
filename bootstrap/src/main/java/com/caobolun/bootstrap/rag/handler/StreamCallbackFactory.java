package com.caobolun.bootstrap.rag.handler;

import com.caobolun.bootstrap.core.memory.ConversationMemoryService;
import com.caobolun.bootstrap.rag.service.ConversationGroupService;
import com.caobolun.infraai.chat.StreamCallback;
import com.caobolun.infraai.config.AIModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * StreamCallback工厂类
 * 负责创建和管理StreamCallback实例
 */
@Component
@RequiredArgsConstructor
public class StreamCallbackFactory {

    private final AIModelProperties modelProperties;
    private final ConversationMemoryService memoryService;
    private final ConversationGroupService groupService;
    private final StreamTaskManager taskManager;

    /**
     * 创建一个聊天信息时间处理实例
     * @param emitter SSE发射器
     * @param conversationId 会话ID
     * @param taskId 任务ID
     * @return  StreamCallback实例
     */
    public StreamCallback createChatEventHandler(SseEmitter emitter,
                                                 String conversationId,
                                                 String taskId){
        StreamChatHandlerParams build = StreamChatHandlerParams.builder()
                .emitter(emitter)
                .conversationId(conversationId)
                .taskId(taskId)
                .modelProperties(modelProperties)
                .memoryService(memoryService)
                .conversationGroupService(groupService)
                .taskManager(taskManager)
                .build();
        return new StreamChatEventHandler(build);
    }

}
