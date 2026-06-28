package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.rag.dto.request.ConversationUpdateRequest;
import com.caobolun.bootstrap.rag.dto.vo.ConversationVO;
import com.caobolun.bootstrap.rag.service.ConversationService;
import com.caobolun.bootstrap.rag.service.bo.ConversationCreateBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话服务实现类
 * 处理会话的创建、更新、重命名和删除等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

    @Override
    public List<ConversationVO> listByUserId(String userId) {
        return List.of();
    }

    @Override
    public void createOrUpdate(ConversationCreateBO request) {

    }

    @Override
    public void rename(String conversationId, ConversationUpdateRequest request) {

    }

    @Override
    public void delete(String conversationId) {

    }
}
