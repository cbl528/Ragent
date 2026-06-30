package com.caobolun.bootstrap.core.parser.model;

/**
 * 资产引用：指向 RustFS 中已上传的二进制资源（图片等）
 * <p>
 * 由 MinerUResultUnpacker / Excel 等解析器在上传资产后构造，
 * 挂在 ImageBlock 上，并最终回填到 VectorChunk.assets 供检索使用
 *
 * @param publicUrl     浏览器可直连的公开预览 URL，如 "<a href="http://localhost:9000/ragent-assets/xxx.png">...</a>"
 *                      （asset-bucket 已开公共读；对应 s3://bucket/key 定位符可由该 URL 反解）
 * @param mime          MIME 类型，如 "image/png"
 * @param sourceBlockId 关联的 Block.id()，用于溯源
 */
public record AssetRef(String publicUrl, String mime, String sourceBlockId) {
}
