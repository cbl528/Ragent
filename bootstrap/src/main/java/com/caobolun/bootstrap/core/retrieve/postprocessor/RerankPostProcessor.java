package com.caobolun.bootstrap.core.retrieve.postprocessor;

import com.caobolun.framework.convention.RetrievedChunk;
import com.caobolun.infraai.rerank.RerankService;
import com.caobolun.bootstrap.rag.config.RAGConfigProperties;
import com.caobolun.bootstrap.core.retrieve.channel.SearchChannelResult;
import com.caobolun.bootstrap.core.retrieve.channel.SearchContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rerank 后置处理器
 * <p>
 * 使用 Rerank 模型对结果进行重排序
 * 这是最后一个处理器，输出最终的 Top-K 结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RerankPostProcessor implements SearchResultPostProcessor {

    private final RerankService rerankService;
    private final RAGConfigProperties ragConfigProperties;

    @Override
    public String getName() {
        return "Rerank";
    }

    @Override
    public int getOrder() {
        return 10;  // 最后执行
    }

    @Override
    public boolean isEnabled(SearchContext context) {
        return ragConfigProperties.getRerankEnabled();
    }

    @Override
    public List<RetrievedChunk> process(List<RetrievedChunk> chunks,
                                        List<SearchChannelResult> results,
                                        SearchContext context) {
        if (chunks.isEmpty()) {
            log.info("Chunk 列表为空，跳过 Rerank");
            return chunks;
        }

        return rerankService.rerank(
                context.getMainQuestion(),
                chunks,
                context.getTopK()
        );
    }
}
