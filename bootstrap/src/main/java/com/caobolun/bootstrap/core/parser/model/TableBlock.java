package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 表格 Block：由 TableChunker 按 rowsPerChunk 切分，每个 chunk 都包含 headers。
 * <p>
 * 合并单元格已在 Excel 解析器（ExcelTableNormalizer）展开填充；
 * 多行表头已展平为单行，列名以分隔符拼接（如 "财务|收入"）
 *
 * @param headers     列名列表（已展平）
 * @param rows        数据行（合并单元格已展开）
 * @param captionText 表格标题（若有）
 */
public record TableBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        List<String> headers,
        List<List<String>> rows,
        String captionText
) implements Block {
}
