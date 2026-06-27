package com.caobolun.bootstrap.rag.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池执行器配置类
 * 为系统中不同的业务场景配置独立的线程池，提高并发处理能力
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * CPU核心数,用于动态调整线程池大小
     */
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * mcp批量处理线程池
     */
    @Bean
    public Executor mcpBatchExecutor(){
        ThreadPoolExecutor mcpBatchExecutor = new ThreadPoolExecutor(
                CPU_COUNT,
                CPU_COUNT << 1,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("mcp_batch_executor_")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return TtlExecutors.getTtlExecutor(mcpBatchExecutor);
    }

    /**
     * RAG上下文处理线程池（子问题级并行：检索+MCP）
     */
    @Bean
    public Executor ragContextExecutor() {
        ThreadPoolExecutor ragContextExecutor = new ThreadPoolExecutor(
                CPU_COUNT << 2,
                CPU_COUNT << 2,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("rag_context_executor_")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return TtlExecutors.getTtlExecutor(ragContextExecutor);
    }

    /**
     * RAG 内部检索线程池
     */
    @Bean
    public Executor innerRetrievalExecutor() {
        ThreadPoolExecutor innerRetrievalExecutor = new ThreadPoolExecutor(
                CPU_COUNT << 1,
                CPU_COUNT << 2,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("rag_inner_retrieval_executor_")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return TtlExecutors.getTtlExecutor(innerRetrievalExecutor);
    }

    /**
     * 意图识别并行执行线程池
     */
    @Bean
    public Executor intentClassifyExecutor() {
        ThreadPoolExecutor intentClassifyExecutor = new ThreadPoolExecutor(
                CPU_COUNT,
                CPU_COUNT << 1,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("intent_classify_executor_")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return TtlExecutors.getTtlExecutor(intentClassifyExecutor);
    }

    /**
     * 对话记忆摘要生成线程池
     */
    @Bean
    public Executor memorySummaryExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                Math.max(2, CPU_COUNT >> 1),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("memory_summary_executor_")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return TtlExecutors.getTtlExecutor(executor);
    }

    /**
     * 模型流式输出线程池
     */
    @Bean
    public Executor modelStreamExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                Math.max(2, CPU_COUNT >> 1),
                Math.max(4, CPU_COUNT),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("model_stream_executor_")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        return TtlExecutors.getTtlExecutor(executor);
    }

    /**
     * SSE 排队后执行入口线程池
     */
    @Bean
    public Executor chatEntryExecutor(RAGRateLimitProperties rateLimitProperties) {
        int size = rateLimitProperties.getGlobalMaxConcurrent();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                size,
                size,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("chat_entry_executor_")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return TtlExecutors.getTtlExecutor(executor);
    }



}