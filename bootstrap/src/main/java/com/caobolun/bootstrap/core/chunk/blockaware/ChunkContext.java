package com.caobolun.bootstrap.core.chunk.blockaware;

import java.util.List;

/**
 * BlockChunker 调用上下文
 * <p>
 * 由 ChunkerNode 在遍历 Block 列表时构造并传入每个 chunker，承载：
 * <ul>
 *   <li>{@link #outlinePath}：当前 Block 所在的章节路径（由 HeadingHandler 累积）</li>
 *   <li>{@link #config}：切分参数（chunk 大小、表格 rowsPerChunk 等）</li>
 *   <li>{@link #startIndex}：当前 chunk 序号起点（用于 VectorChunk.index 单调递增）</li>
 * </ul>
 *
 * @param outlinePath 章节路径（不可变副本由调用方保证）
 * @param config      切分配置
 * @param startIndex  本次产出 VectorChunk 的起始 index
 */
public record ChunkContext(
        List<String> outlinePath,
        BlockChunkConfig config,
        int startIndex
) {

    public static ChunkContext of(List<String> outlinePath, BlockChunkConfig config) {
        return new ChunkContext(outlinePath, config, 0);
    }

    public static ChunkContext of(List<String> outlinePath, BlockChunkConfig config, int startIndex) {
        return new ChunkContext(outlinePath, config, startIndex);
    }
}
