package com.caobolun.infraai.model;

import cn.dev33.satoken.fun.SaFunction;
import cn.hutool.core.util.StrUtil;
import com.caobolun.infraai.config.AIModelProperties;
import com.caobolun.infraai.enums.ModelProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 模型选择器
 * 负责根据配置和当前需求选择合适的模型
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {
    // 模型配置
    private final AIModelProperties modelProperties;
    // 模型健康状态存储
    private final ModelHealthStore healthStore;

    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(modelProperties.getEmbedding());
    }

    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(modelProperties.getRerank());
    }

    public List<ModelTarget> selectVlmCandidates() {
        return selectCandidates(modelProperties.getVlm());
    }


    /**
     * 对话场景专用，传入是否开启深度思考，返回有序可用对话模型列表
     * @param deepThinking
     * @return
     */
    public List<ModelTarget> selectChatCandidates(boolean deepThinking) {
        // 获取可用对话模型列表
        AIModelProperties.ModelGroup chatModelGroup = modelProperties.getChat();
        if(chatModelGroup == null){
            return List.of(); // 没有可用对话模型，返回空列表
        }
        String firstChoiceModel = resolveFirstChoiceModel(chatModelGroup, deepThinking);
        return selectCandidates(chatModelGroup,firstChoiceModel,deepThinking);

    }

    /**
     * 根据模型组、首选模型和是否开启深度思考，返回有序可用模型列表
     * @param group
     * @param firstChoiceModelId
     * @param deepThinking
     * @return
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group, String firstChoiceModelId, boolean deepThinking) {
        if (group == null || group.getCandidates() == null) {
            return List.of();
        }
        List<AIModelProperties.ModelCandidate> modelCandidates = filterAndSortCandidates(group.getCandidates(), firstChoiceModelId, deepThinking);
        return buildAvailableTargets(modelCandidates);
    }

    /**
     * 根据模型组返回有序可用模型列表
     * @param group
     * @return
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group) {
        if (group == null) {
            return List.of();
        }
        return selectCandidates(group, group.getDefaultModel(), false);
    }

    /**
     * 根据候选模型列表，返回有序可用模型目标列表
     * @param candidates
     * @return
     */
    private List<ModelTarget> buildAvailableTargets(List<AIModelProperties.ModelCandidate> candidates) {
        Map<String, AIModelProperties.ProviderConfig> providers = modelProperties.getProviders();
        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据候选模型和提供者列表，返回模型目标
     * @param candidate
     * @param providers
     * @return
     */
    private ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, Map<String, AIModelProperties.ProviderConfig> providers) {
        // 解析模型 ID(供应商::模型)
        String modelId = resolveId(candidate);
        // 检查模型是否可用
        if(healthStore.isUnavailable(modelId)){
            return null;
        }
        // 获取供应商配置
        AIModelProperties.ProviderConfig providerConfig = providers.get(candidate.getProvider());
        if(providerConfig == null && !ModelProvider.NOOP.matches(candidate.getProvider())){
            log.warn("Provider配置缺失: provider={}, modelId={}", candidate.getProvider(), modelId);
            return null;
        }
        return new ModelTarget(modelId, candidate, providerConfig);

    }
    /**
     * 过滤并排序候选模型列表
     */
    private List<AIModelProperties.ModelCandidate> filterAndSortCandidates(List<AIModelProperties.ModelCandidate> candidates,
                                                                           String firstChoiceModelId,
                                                                           boolean deepThinking) {
        List<AIModelProperties.ModelCandidate> enabled = candidates.stream()
                .filter(c -> c != null && !Boolean.FALSE.equals(c.getEnabled()))
                .filter(c -> !deepThinking || Boolean.TRUE.equals(c.getSupportsThinking()))
                .sorted(Comparator
                        .comparing((AIModelProperties.ModelCandidate c) ->
                                !Objects.equals(resolveId(c), firstChoiceModelId))
                        .thenComparing(AIModelProperties.ModelCandidate::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AIModelProperties.ModelCandidate::getId,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        if(deepThinking && enabled.isEmpty()){
            log.warn("深度思考模式没有可用候选模型");
        }
        return enabled;
    }

    /**
     * 生成全局唯一模型标识
     * 统一规则生成 modelId，用于熔断器、路由匹配：
     * 如果候选配置显式配置了 id，直接使用；
     * 无自定义 id：拼接 provider::model 作为唯一标识，防止重复。
     * @param candidate
     * @return
     */
    private String resolveId(AIModelProperties.ModelCandidate candidate) {
        if(StrUtil.isNotBlank(candidate.getId())){
            return candidate.getId();
        }
        return String.format("%s::%s",
                Objects.toString(candidate.getProvider(), "unknown"),
                Objects.toString(candidate.getModel(), "unknown"));
    }

    /**
     * 根据模型组和是否开启深度思考，返回首选可用模型
     * @param group
     * @param deepThinking
     * @return
     */
    private String resolveFirstChoiceModel(AIModelProperties.ModelGroup group, boolean deepThinking) {
        if(deepThinking){
            String model = group.getDeepThinkingModel();
            if(StrUtil.isNotBlank(model)){
                return model;
            }
        }
        return group.getDefaultModel();
    }
}
