package com.caobolun.infraai.rerank;

import com.caobolun.framework.convention.RetrievedChunk;
import com.caobolun.infraai.enums.ModelProvider;
import com.caobolun.infraai.model.ModelTarget;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 无调用外部服务的重排客户端
 * 不调用任何外部模型，原样保留检索结果的顺序，仅做数量截断，是重排功能的"保底方案"。
 */
@Service
public class NoopRerankClient implements RerankClient{
    @Override
    public String provider() {
        return ModelProvider.NOOP.getId();
    }


    /**
     * 对候选文档片段列表进行排序，返回前N个最相关的结果(无调用外部服务)
     * @param query      用户查询文本
     * @param candidates 待排序的候选文档片段列表
     * @param topN       返回前N个最相关的结果
     * @param target     目标模型配置信息
     * @return 排序后的候选文档片段列表
     */
    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target) {
        // 查询文本为空或候选列表为空，则返回空列表
        if(candidates == null || candidates.isEmpty()){
            return List.of();
        }
        // topN小于等于0或候选列表长度小于等于topN，则返回候选列表
        if(topN <= 0 || candidates.size() <= topN){
            return candidates;
        }
        // 对候选列表进行排序，并返回前N个最相关的结果
        return candidates.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }
}
