package com.caobolun.infraai.model;

import com.caobolun.infraai.config.AIModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 模型健康状态存储器
 * 用于管理和跟踪各个 AI 模型的健康状况，实现断路器模式
 */
@Component
@RequiredArgsConstructor
public class ModelHealthStore {

    // AI模型属性
    private final AIModelProperties modelProperties;
    // 模型健康状态存储，使用ConcurrentHashMap实现线程安全
    private final Map<String, ModelHealth> healthMap = new ConcurrentHashMap<>();

    /**
     * 判断模型是否不可用
     * @param id 模型ID
     * @return 是否不可用
     */
    public boolean isUnavailable(String id) {
        ModelHealth health = healthMap.get(id);
        // 如果健康状态不存在，则认为模型可用
        if (health == null){
            return false;
        }
        // 如果健康状态为OPEN且熔断期未结束，则认为模型不可用
        if(health.state == State.OPEN && health.openUntil > System.currentTimeMillis()){
            return true;
        }
        // 如果健康状态为HALF_OPEN且有试探请求在执行，则认为模型不可用
        return health.state == State.HALF_OPEN && health.halfOpenInFlight;
    }

    /**
     * 是否允许发送本次模型调用
     * @param id
     * @return
     * 返回为True的情况
     * 1. 新模型首次调用
     * 2. 模型状态为CLOSED，即模型正常可用
     * 3. 模型状态OPEN且熔断期结束，放行一个试探请求
     * 4. 模型状态为HALF_OPEN并且没有试探请求在执行，放行一个试探请求
     * 返回为False的情况
     * 1. 模型状态为OPEN且熔断期未结束，即模型处于熔断状态
     * 2. id为空，即模型ID不存在
     * 3. 模型状态为HALF_OPEN且有试探请求在执行，即有试探请求正在执行
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // 取消布尔方法总是返回相反值的IDEA警告
    public boolean allowCall(String id){
        // 如果模型ID为空，则不允许发送模型调用
        if(id == null){
            return false;
        }
        // 获取当前时间
        long now = System.currentTimeMillis();
        // 创建一个原子布尔变量，初始值为false，用来表示是否允许发送模型调用
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        // HashMap的computer方法如果key存在的话函数返回值作为新值存入map，如果key存在，函数返回值作为新值更新到map中
        healthMap.compute(id, (key, value) -> {
            // 如果值为空，则创建一个新值
            if (value == null) {
                value = new ModelHealth();
            }
            if(value.state == State.OPEN){
                // 如果状态为OPEN且熔断期未结束，则返回当前值
                if(value.openUntil > now){
                    return value;
                }
                // 如果状态为OPEN且熔断期结束，则转换为HALF_OPEN状态，并允许当前请求通过
                value.state = State.HALF_OPEN;
                // 将试探请求标志设置为true，表示有试探请求在执行
                value.halfOpenInFlight = true;
                atomicBoolean.set(true);
                return value;
            }
            if(value.state == State.HALF_OPEN){
                // 如果状态为HALF_OPEN且有试探请求在执行，则返回当前值
                if(value.halfOpenInFlight){
                    return value;
                }
                // 设置试探请求标志为true，表示有试探请求在执行
                value.halfOpenInFlight = true;
                atomicBoolean.set(true);
                return value;
            }
            atomicBoolean.set(true);
            return value;
        });
        return atomicBoolean.get();
    }

    /**
     * 调用成功回调
     * @param id
     */
    public void markSuccess(String id) {
        // 如果模型ID为空，则直接返回
        if (id == null) {
            return;
        }
        // 更新模型健康状态
        healthMap.compute(id, (k, v) -> {
            // 如果值为空，则创建一个新值
            if (v == null) {
                return new ModelHealth();
            }
            v.state = State.CLOSED; // 将状态设置为CLOSED，表示模型正常可用
            v.consecutiveFailures = 0; // 重置连续失败次数
            v.openUntil = 0L; // 重置OPEN熔断到期时间戳
            v.halfOpenInFlight = false; // 重置试探请求标志
            return v;
        });
    }

    /**
     * 调用失败回调
     * @param id
     */
    public void markFailure(String id) {
        // 如果模型ID为空，则直接返回
        if (id == null) {
            return;
        }
        // 获取当前时间
        long now = System.currentTimeMillis();
        healthMap.compute(id, (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            // 如果状态为HALF_OPEN，则转换为OPEN状态，并设置OPEN熔断到期时间戳
            if (v.state == State.HALF_OPEN) {
                v.state = State.OPEN;
                v.openUntil = now + modelProperties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
                v.halfOpenInFlight = false;
                return v;
            }
            v.consecutiveFailures++; // 增加连续失败次数
            // 如果连续失败次数达到阈值，则转换为OPEN状态，并设置OPEN熔断到期时间戳，重置连续失败次数
            if (v.consecutiveFailures >= modelProperties.getSelection().getFailureThreshold()) {
                v.state = State.OPEN; // 转换为OPEN状态
                v.openUntil = now + modelProperties.getSelection().getOpenDurationMs(); // 设置OPEN熔断到期时间戳
                v.consecutiveFailures = 0; // 重置连续失败次数
            }
            return v;
        });
    }

    private static class ModelHealth {
        private int consecutiveFailures;
        private long openUntil;
        private boolean halfOpenInFlight;
        private State state;

        private ModelHealth() {
            this.consecutiveFailures = 0; // 连续失败次数
            this.openUntil = 0L; // OPEN熔断到期时间戳
            this.halfOpenInFlight = false; // 半开状态是否已有试探请求在执行
            this.state = State.CLOSED; // 当前状态
        }
    }
    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

}