package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 段落 Block：由 ParagraphChunker 按 token 切分，可跨段落合并到目标长度，不跨 heading
 *
 * @param text 段落文本（不含 markdown 标记）
 */
public record ParagraphBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        String text
) implements Block {
}
