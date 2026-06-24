package com.caobolun.framework.idempotent;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 防止用户重复提交表单信息切面控制器
 */
@Aspect
@Component
@RequiredArgsConstructor
public final class IdempotentSubmitAspect {

    private final RedissonClient redissonClient;
    private final Gson gson = new Gson();

    @Value("${app.eval.enabled:false}")
    private boolean evalEnabled;


}