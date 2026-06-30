package com.caobolun.bootstrap.core.chunk.blockaware;

/**
 * BlockAwareChunker 切分配置
 * <p>
 * 提供常用的切分参数；具体 chunker 按需读取自己关心的字段
 *
 * @param maxChars          单个 chunk 最大字符数（ParagraphChunker / CodeChunker 长块切分时用）
 * @param overlapChars      chunk 重叠字符数（ParagraphChunker token 切分时用）
 * @param rowsPerChunk      TableChunker 每个 chunk 包含的数据行数
 * @param maxListItems      ListChunker 短列表 atomic 的阈值
 * @param listItemsPerChunk 长列表每个 chunk 的列表项数
 */
public record BlockChunkConfig(
        int maxChars,
        int overlapChars,
        int rowsPerChunk,
        int maxListItems,
        int listItemsPerChunk
) {

    /**
     * 默认配置（用于测试 / 早期未配置场景）
     */
    public static BlockChunkConfig defaults() {
        return new BlockChunkConfig(512, 64, 5, 15, 10);
    }

    public BlockChunkConfig {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be > 0, got " + maxChars);
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars must be in [0, maxChars), got " + overlapChars);
        }
        if (rowsPerChunk <= 0) {
            throw new IllegalArgumentException("rowsPerChunk must be > 0, got " + rowsPerChunk);
        }
        if (maxListItems <= 0) {
            throw new IllegalArgumentException("maxListItems must be > 0, got " + maxListItems);
        }
        if (listItemsPerChunk <= 0) {
            throw new IllegalArgumentException("listItemsPerChunk must be > 0, got " + listItemsPerChunk);
        }
    }
}
