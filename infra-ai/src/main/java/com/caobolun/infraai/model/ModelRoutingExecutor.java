package com.caobolun.infraai.model;

import com.caobolun.framework.errorcode.BaseErrorCode;
import com.caobolun.framework.exception.RemoteException;
import com.caobolun.infraai.enums.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 模型路由执行器
 * 负责在多个模型候选者之间进行调度执行，并提供故障转移（Fallback）和健康检查机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    // 模型健康状态存储
    private final ModelHealthStore healthStore;

    /**
     * 执行模型调用，并提供故障转移和健康检查
     * @param modelCapability  模型能力
     * @param targets 模型目标列表
     * @param clientResolver 客户端解析器
     * @param caller 模型调用器
     * @return 模型调用结果
     * @param <C> 客户端类型
     * @param <T> 模型调用结果类型
     */
    public <C, T> T executeWithFallback(
            ModelCapability modelCapability,
            List<ModelTarget> targets,
            Function<ModelTarget, C> clientResolver,
            ModelCaller<C, T> caller){
        // 获取模型能力的显示名称
        String capability = modelCapability.getDisplayName();
        if(targets == null || targets.isEmpty()){
            // 如果能力名称为空，则抛出异常
            throw new RemoteException("不存在" + capability + "能力的模型可以使用");
        }

        Throwable last = null;
        for(ModelTarget target : targets){
            // 获取模型目标的客户端
            C apply = clientResolver.apply(target);
            if(apply == null){
                // 如果客户端解析失败，则记录警告日志
                log.warn("{} 模型客户端解析失败: 供应商：{}, 模型ID：{}", capability, target.candidate().getProvider(), target.id());
            }
            // 如果模型目标不可用，则跳过
            if(!healthStore.allowCall(target.id())){
                continue;
            }
            try {
                T response = caller.call(apply, target); // 调用模型并获取结果
                healthStore.markSuccess(target.id()); // 标记模型目标调用成功
                return response;
            } catch (Exception e) {
                last = e;
                healthStore.markFailure(target.id()); // 如果抛出异常，标记模型调用失败
                log.warn("{} 模型调用失败, 回退下一个: 供应商：{}, 模型ID：{}", capability, target.candidate().getProvider(), target.id(), e);
            }
        }
        // 如果所有模型调用都失败了，则抛出异常
        throw new RemoteException(
                "所有" + capability + "能力的模型调用失败:" + (last == null ? "unknown" : last.getMessage()),
                last,
                BaseErrorCode.REMOTE_ERROR
        );
    }

}