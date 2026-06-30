package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 图片 Block:由 ImageChunker 产生 atomic chunk,渲染为 ![caption](http://...)
 * <p>
 * 必须 atomic 保护,避免 chunker 切碎 markdown 图片链接导致前端渲染失败
 *
 * @param asset       图片资产引用(指向 RustFS)
 * @param caption     图片标题(如 "图3-1:系统架构图")
 * @param altText     无障碍替代文本
 * @param description VLM 图生文结果:一段自包含的知识文本(说明图是什么 + 完整结构化 OCR),
 *                    同时用于 embedding 检索与喂 LLM 答题;MinerU 等不产图生文的来源为 null
 */
public record ImageBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        AssetRef asset,
        String caption,
        String altText,
        String description
) implements Block {

    /**
     * 向后兼容构造器:不产图生文的来源(MinerU / Excel 等)继续用 6 参,description 置空
     */
    public ImageBlock(String id, Provenance provenance, List<String> outlinePath,
                      AssetRef asset, String caption, String altText) {
        this(id, provenance, outlinePath, asset, caption, altText, null);
    }
}
