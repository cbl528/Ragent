package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.core.enums.ConversationMessageOrder;
import com.caobolun.bootstrap.rag.dto.vo.ConversationMessageVO;
import com.caobolun.bootstrap.rag.service.ConversationMessageService;
import com.caobolun.bootstrap.rag.service.bo.ConversationMessageBO;
import com.caobolun.bootstrap.rag.service.bo.ConversationSummaryBO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationMessageServiceImpl implements ConversationMessageService {

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
