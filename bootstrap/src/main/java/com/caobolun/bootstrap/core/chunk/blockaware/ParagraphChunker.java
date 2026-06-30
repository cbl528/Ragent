package com.caobolun.bootstrap.core.chunk.blockaware;

import cn.hutool.core.util.IdUtil;
import com.caobolun.bootstrap.core.chunk.VectorChunk;
import com.caobolun.bootstrap.core.parser.model.ParagraphBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 段落 chunker：按 maxChars 切分文本，相邻 chunk 重叠 overlapChars 字符
 * <p>
 * 不跨 heading 的约束由 ChunkerNode 主流程保证（HeadingBlock 不通过 ParagraphChunker，
 * 会更新 outlinePath 但不破坏单个 ParagraphChunker 调用的 atomicity）
 */
@Component
public class ParagraphChunker implements BlockChunker<ParagraphBlock> {

    @Override
    public List<VectorChunk> chunk(ParagraphBlock block, ChunkContext ctx) {
        if (block == null) {
            return List.of();
        }
        String text = block.text() == null ? "" : block.text();
        if (text.isEmpty()) {
            return List.of();
        }

        int maxChars = ctx.config().maxChars();
        int overlap = ctx.config().overlapChars();
        List<String> pieces = splitByChars(text, maxChars, overlap);

        List<VectorChunk> result = new ArrayList<>(pieces.size());
        int chunkIndex = ctx.startIndex();
        for (String piece : pieces) {
            VectorChunk chunk = VectorChunk.builder()
                    .chunkId(IdUtil.getSnowflakeNextIdStr())
                    .index(chunkIndex++)
                    .content(piece)
                    .blockType("PARAGRAPH")
                    .outlinePath(new ArrayList<>(ctx.outlinePath()))
                    .sourceBlockIds(List.of(block.id()))
                    .build();
            result.add(chunk);
        }
        return result;
    }

    /**
     * 按字符切分，相邻片段重叠 overlap 字符
     * <ul>
     *   <li>text.length() ≤ maxChars：返回单元素列表</li>
     *   <li>否则按 step = maxChars - overlap 步长切</li>
     * </ul>
     */
    private static List<String> splitByChars(String text, int maxChars, int overlap) {
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        int step = maxChars - overlap;
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            pieces.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start += step;
        }
        return pieces;
    }
}
