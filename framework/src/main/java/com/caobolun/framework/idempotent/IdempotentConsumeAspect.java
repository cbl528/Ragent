package com.caobolun.framework.idempotent;

import com.caobolun.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 防止消息队列消费者重复消费消息切面控制器
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public final class IdempotentConsumeAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local value = ARGV[1]
            local expire_time_ms = ARGV[2]
            return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
            """;

    @Around("@annotation(com.caobolun.framework.idempotent.IdempotentConsume)")
    public Object idempotentConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取对应的方法的注解
        IdempotentConsume idempotentConsume = getIdempotentConsumeAnnotation(joinPoint);
        // 根据注解的keyPrefix和key生成唯一key
        String uniqueKey = idempotentConsume.keyPrefix() + SpELUtil.parseKey(idempotentConsume.key(),
                ((MethodSignature) joinPoint.getSignature()).getMethod(),
                joinPoint.getArgs());
        long keyTimeout = idempotentConsume.keyTimeout();

        //执行lua脚本
        String absentAndGet = stringRedisTemplate.execute(
                RedisScript.of(LUA_SCRIPT, String.class),
                List.of(uniqueKey), // 设置KEY[1]
                IdempotentConsumeStatusEnum.CONSUMING.getCode(), // 设置ARGV[1]
                String.valueOf(TimeUnit.SECONDS.toMillis(keyTimeout)) // 设置ARGV[2]
        );

        boolean error = IdempotentConsumeStatusEnum.isError(absentAndGet);
        if(error){
            log.warn("[{}] MQ 重复使用，等待延迟重试", uniqueKey);
            throw new ServiceException(String.format("消息消费者幂等异常，幂等标识：%s", uniqueKey));
        }
        if(IdempotentConsumeStatusEnum.CONSUMED.getCode().equals(absentAndGet)){
            log.warn("[{}] MQ 已消费，跳过", uniqueKey);
            return null;
        }
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            // 设置Redis，标记已消费
            stringRedisTemplate.opsForValue().set(
                    uniqueKey,
                    IdempotentConsumeStatusEnum.CONSUMED.getCode(),
                    keyTimeout,
                    TimeUnit.SECONDS
            );
            return result;
        } catch (Throwable ex) {
            // 删除Redis，标记消费失败
            stringRedisTemplate.delete(uniqueKey);
            // 抛出异常，让MQ重新消费
            throw ex;
        }
    }

    public static IdempotentConsume getIdempotentConsumeAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(IdempotentConsume.class);
    }

}