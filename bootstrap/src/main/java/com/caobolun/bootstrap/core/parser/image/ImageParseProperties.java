package com.caobolun.bootstrap.core.parser.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片解析配置（图生文）
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.image-parse")
public class ImageParseProperties {

    /**
     * 图生文引导提示词:要求 VLM 输出中文描述 + 图中文字 OCR
     */
    private String descriptionPrompt = "请用中文详细描述这张图片的内容；若图中包含文字，请逐字识别并完整列出（OCR）。"
            + "先给出整体内容描述，再用\"图中文字：\"另起一段列出识别到的所有文字。";

    /**
     * 描述输出 token 上限,控成本与 embedding 体量;<=0 表示不限制
     */
    private Integer maxOutputTokens = 1024;
}
