package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.rag.dto.request.MessageFeedbackRequest;
import com.caobolun.bootstrap.rag.mapper.ConversationMessageMapper;
import com.caobolun.bootstrap.rag.mapper.MessageFeedbackMapper;
import com.caobolun.bootstrap.rag.mq.MessageFeedbackEvent;
import com.caobolun.bootstrap.rag.service.MessageFeedbackService;
import com.caobolun.framework.mq.producer.MessageQueueProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageFeedbackServiceImpl implements MessageFeedbackService {

    private final MessageFeedbackMapper feedbackMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final MessageQueueProducer messageQueueProducer;

    @Override
    public void submitFeedback(String messageId, MessageFeedbackRequest request) {

    }

    @Override
    public void submitFeedbackAsync(String messageId, MessageFeedbackRequest request) {

    }

    @Override
    public void submitFeedbackByEvent(MessageFeedbackEvent event) {

    }

    @Override
    public Map<String, Integer> getUserVotes(String userId, List<String> messageIds) {
        return Map.of();
    }
}
