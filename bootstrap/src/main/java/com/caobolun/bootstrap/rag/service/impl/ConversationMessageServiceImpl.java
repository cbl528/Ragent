package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.core.enums.ConversationMessageOrder;
import com.caobolun.bootstrap.rag.dto.vo.ConversationMessageVO;
import com.caobolun.bootstrap.rag.mapper.ConversationMapper;
import com.caobolun.bootstrap.rag.mapper.ConversationMessageMapper;
import com.caobolun.bootstrap.rag.mapper.ConversationSummaryMapper;
import com.caobolun.bootstrap.rag.service.ConversationMessageService;
import com.caobolun.bootstrap.rag.service.MessageFeedbackService;
import com.caobolun.bootstrap.rag.service.bo.ConversationMessageBO;
import com.caobolun.bootstrap.rag.service.bo.ConversationSummaryBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {

    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final ConversationMapper conversationMapper;
    private final MessageFeedbackService feedbackService;

    @Override
    public String addMessage(ConversationMessageBO conversationMessage) {
        return "";
    }

    @Override
    public List<ConversationMessageVO> listMessages(String conversationId, String userId, Integer limit, ConversationMessageOrder order) {
        return List.of();
    }

    @Override
    public void addMessageSummary(ConversationSummaryBO conversationSummary) {

    }
}
