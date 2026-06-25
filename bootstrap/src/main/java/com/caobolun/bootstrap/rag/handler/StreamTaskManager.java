package com.caobolun.bootstrap.rag.handler;

import cn.hutool.core.util.StrUtil;
import com.caobolun.bootstrap.rag.dto.CompletionPayload;
import com.caobolun.framework.web.SSEEmitterSender;
import com.caobolun.infraai.chat.StreamCancellationHandle;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
@Component
public class StreamTaskManager {

    private static final String CANCEL_TOPIC = "ragent:stream:cancel";
    private static final String CANCEL_KEY_PREFIX = "ragent:stream:cancel:";
    private static final Duration CANCEL_TTL = Duration.ofMinutes(30);
    private final RedissonClient redissonClient;
    private int listenerId = -1;

    private final Cache<String, StreamTaskInfo> tasks = CacheBuilder.newBuilder()
            .expireAfterWrite(CANCEL_TTL)
            .maximumSize(10000)  // 限制最大数量，基本上不可能超出这个数量。如果觉得不稳妥，可以把值调大并在配置文件声明
            .build();

    public StreamTaskManager(RedissonClient redissonClient){
        this.redissonClient = redissonClient;
    }

    @PostConstruct
    public void subscribe(){
        RTopic topic = redissonClient.getTopic(CANCEL_TOPIC);
        topic.addListener(String.class, (channel, taskId) -> {
            if(StrUtil.isBlank(taskId)){
                return;
            }
            cancelLocal(taskId);
        })
    }

    private void cancelLocal(String taskId) {
        StreamTaskInfo taskInfo = tasks.getIfPresent(taskId);
        if(taskInfo == null){
            return;
        }
    }


    private static final class StreamTaskInfo {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile StreamCancellationHandle handle;
        private volatile SSEEmitterSender sender;
        private volatile Supplier<CompletionPayload> onCancelSupplier;
    }
}