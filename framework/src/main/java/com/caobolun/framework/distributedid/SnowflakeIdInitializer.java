package com.caobolun.framework.distributedid;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnowflakeIdInitializer {

    private final StringRedisTemplate stringRedisTemplate;

    public void init(){
        // 加载Lua脚本
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/snowflake_init.lua")));
        script.setResultType(List.class);

        try{
            // 执行lua脚本
            List<Long> result = stringRedisTemplate.execute(script, Collections.emptyList());
            if(CollUtil.isEmpty(result) || result.size() != 2){
                throw new RuntimeException("从Redis中获取WorkerID和DataCenterID失败");
            }

            Long workerId = result.get(0);
            Long dataCenterId = result.get(1);

            // 注册Snowflake实例到Hutool
            Snowflake snowflake = new Snowflake(workerId, dataCenterId);
            Singleton.put(snowflake);

            log.info("Snowflake ID初始化完成，WorkerID：{}，DataCenterID：{}", workerId, dataCenterId);
        } catch (Exception e){
            throw new RuntimeException("分布式Snowflake初始化失败");
        }
    }
}
