package com.caobolun.bootstrap.rag.handler;

import com.caobolun.bootstrap.core.memory.ConversationMemoryService;
import com.caobolun.bootstrap.rag.service.ConversationGroupService;
import com.caobolun.infraai.config.AIModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreamCallbackFactory {

    private final AIModelProperties modelProperties;
    private final ConversationMemoryService memoryService;
    private final ConversationGroupService groupService;
    private final StreamTaskManager streamTaskManager;

}
