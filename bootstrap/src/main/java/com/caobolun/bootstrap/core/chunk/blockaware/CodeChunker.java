package com.caobolun.bootstrap.core.chunk.blockaware;

import cn.hutool.core.util.IdUtil;
import com.caobolun.bootstrap.core.chunk.VectorChunk;
import com.caobolun.bootstrap.core.parser.model.CodeBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码块 chunker：每个 CodeBlock 产生一个 atomic VectorChunk
 * <p>
 * 永不切分 —— 代码块语法对完整性敏感（缺少 fence 或半截行会破坏前端渲染与 LLM 理解）
 * 渲染为标准 markdown 代码块 ``` 围栏
 */
@Component
public class CodeChunker implements BlockChunker<CodeBlock> {

    @Override
    public List<VectorChunk> chunk(CodeBlock block, ChunkContext ctx) {
        if (block == null) {
            return List.of();
        }
        String language = block.language() == null ? "" : block.language();
        String code = block.code() == null ? "" : block.code();
        String markdown = "```" + language + "\n" + code + "\n```";

        VectorChunk chunk = VectorChunk.builder()
                .chunkId(IdUtil.getSnowflakeNextIdStr())
                .index(ctx.startIndex())
                .content(markdown)
                .blockType("CODE")
                .outlinePath(new ArrayList<>(ctx.outlinePath()))
                .sourceBlockIds(List.of(block.id()))
                .build();

        return List.of(chunk);
    }
}
