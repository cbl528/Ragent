package com.caobolun.bootstrap.ingestion.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.caobolun.bootstrap.core.chunk.*;
import com.caobolun.bootstrap.core.parser.model.Block;
import com.caobolun.framework.exception.ClientException;
import com.caobolun.bootstrap.ingestion.domain.context.IngestionContext;
import com.caobolun.bootstrap.ingestion.domain.enums.IngestionNodeType;
import com.caobolun.bootstrap.ingestion.domain.pipeline.NodeConfig;
import com.caobolun.bootstrap.ingestion.domain.result.NodeResult;
import com.caobolun.bootstrap.ingestion.domain.settings.ChunkerSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 文本分块节点
 * 负责将输入的完整文本（原始文本或增强后的文本）按照指定的策略切分成多个较小的文本块（Chunk）
 */
@Component
@RequiredArgsConstructor
public class ChunkerNode implements IngestionNode {

    private final ObjectMapper objectMapper;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final StructuredChunkingService structuredChunkingService;

    @Override
    public String getNodeType() {
        return IngestionNodeType.CHUNKER.getValue();
    }

    @Override
    public NodeResult execute(IngestionContext context, NodeConfig config) {
        ChunkerSettings settings = parseSettings(config.getSettings());

        // blocks 非空走 block-aware，否则用纯文本走 legacy（判断收口在 StructuredChunkingService）
        List<Block> blocks = context.getDocument() == null ? null : context.getDocument().getBlocks();
        boolean hasBlocks = blocks != null && !blocks.isEmpty();
        String text = StringUtils.hasText(context.getEnhancedText())
                ? context.getEnhancedText()
                : context.getRawText();
        ChunkingOptions options = settings.getStrategy()
                .createDefaultOptions(settings.getChunkSize(), settings.getOverlapSize());

        List<VectorChunk> chunks = structuredChunkingService.chunk(
                blocks, text, settings.getStrategy(), options, settings.getRowsPerChunk());

        if (chunks.isEmpty()) {
            return NodeResult.fail(new ClientException(hasBlocks ? "分块结果为空" : "可分块文本为空"));
        }

        // 嵌入：为切分后的文本块生成向量
        chunkEmbeddingService.embed(chunks, null);

        context.setChunks(chunks);
        return NodeResult.ok("已分块 " + chunks.size() + " 段, path=" + (hasBlocks ? "block-aware" : "legacy-text"));
    }

    private ChunkerSettings parseSettings(JsonNode node) {
        ChunkerSettings settings = objectMapper.convertValue(node, ChunkerSettings.class);
        if (settings.getStrategy() == null) {
            settings.setStrategy(ChunkingMode.STRUCTURE_AWARE);
        }
        // 放行 -1（不分块哨兵）；其余 null / 非正值回落默认 512
        Integer chunkSize = settings.getChunkSize();
        if (chunkSize == null
                || (chunkSize <= 0 && chunkSize != StructuredChunkingService.WHOLE_DOCUMENT_SENTINEL)) {
            settings.setChunkSize(512);
        }
        if (settings.getOverlapSize() == null || settings.getOverlapSize() < 0) {
            settings.setOverlapSize(128);
        }
        return settings;
    }
}
