package com.caobolun.bootstrap.rag.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.caobolun.bootstrap.rag.config.MemoryProperties;
import com.caobolun.bootstrap.rag.dto.request.ConversationUpdateRequest;
import com.caobolun.bootstrap.rag.dto.vo.ConversationVO;
import com.caobolun.bootstrap.rag.entity.ConversationDO;
import com.caobolun.bootstrap.rag.entity.ConversationMessageDO;
import com.caobolun.bootstrap.rag.entity.ConversationSummaryDO;
import com.caobolun.bootstrap.rag.mapper.ConversationMapper;
import com.caobolun.bootstrap.rag.mapper.ConversationMessageMapper;
import com.caobolun.bootstrap.rag.mapper.ConversationSummaryMapper;
import com.caobolun.bootstrap.rag.service.ConversationService;
import com.caobolun.bootstrap.rag.service.bo.ConversationCreateBO;
import com.caobolun.framework.context.UserContext;
import com.caobolun.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 * 处理会话的创建、更新、重命名和删除等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final MemoryProperties memoryProperties;
    private final ConversationTitleGenerator conversationTitleGenerator;

    @Override
    public List<ConversationVO> listByUserId(String userId) {
        if(StrUtil.isBlank(userId)){
            return List.of();
        }
        // 查询会话列表
        List<ConversationDO> records = conversationMapper.selectList(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getDeleted, 0)
                        .orderByDesc(ConversationDO::getLastTime)
        );
        // 如果没有会话，则返回空列表
        if(records == null || records.isEmpty()){
            return List.of();
        }
        // 转换会话列表为会话VO列表
        return records.stream()
                .map(item -> ConversationVO.builder()
                        .conversationId(item.getConversationId())
                        .title(item.getTitle())
                        .lastTime(item.getLastTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void createOrUpdate(ConversationCreateBO request) {
        String userId = request.getUserId();
        String conversationId = request.getConversationId();
        String question = request.getQuestion();
        if(StrUtil.isBlank(userId)){
            throw new ClientException("用户信息缺失");
        }

        ConversationDO conversationDO = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getDeleted, 0)
        );

        if(conversationDO == null){
            conversationDO = ConversationDO.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .title(question)
                    .lastTime(request.getLastTime())
                    .build();
            conversationMapper.insert(conversationDO);
            return;
        }

        conversationDO.setLastTime(request.getLastTime());
        conversationMapper.updateById(conversationDO);
    }

    @Override
    public void rename(String conversationId, ConversationUpdateRequest request) {
        String userId = UserContext.getUserId();
        if(StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)){
            throw new ClientException("会话信息缺失");
        }
        // 获取会话标题
        String title = request.getTitle();
        if(StrUtil.isBlank(title)){
            throw new ClientException("会话标题不能为空");
        }
        // 验证会话标题长度
        Integer titleMaxLength = memoryProperties.getTitleMaxLength();
        if(title.length() > titleMaxLength){
            throw new ClientException("会话标题不能超过" + titleMaxLength + "个字");
        }
        // 查询会话
        ConversationDO conversationDO = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getDeleted, 0)
        );
        // 验证会话是否存在
        if(conversationDO == null){
            throw new ClientException("会话不存在");
        }
        // 更新会话标题
        conversationDO.setTitle(title);
        conversationMapper.updateById(conversationDO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String conversationId) {
        String userId = UserContext.getUserId();
        if(StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)){
            throw new ClientException("会话信息缺失");
        }
        ConversationDO conversationDO = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getDeleted, 0)
        );
        if(conversationDO == null){
            throw new ClientException("会话不存在");
        }
        conversationMapper.deleteById(conversationDO.getId());
        conversationMessageMapper.delete(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getDeleted, 0)
        );
        conversationSummaryMapper.delete(
                Wrappers.lambdaQuery(ConversationSummaryDO.class)
                        .eq(ConversationSummaryDO::getConversationId, conversationId)
                        .eq(ConversationSummaryDO::getUserId, userId)
                        .eq(ConversationSummaryDO::getDeleted, 0)
        );
    }
}
