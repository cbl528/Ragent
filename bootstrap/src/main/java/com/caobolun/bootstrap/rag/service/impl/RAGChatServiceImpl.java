package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.rag.service.RAGChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class RAGChatServiceImpl implements RAGChatService {

    @Override
    public void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter) {

    }

    @Override
    public void stopTask(String taskId) {

    }
}
