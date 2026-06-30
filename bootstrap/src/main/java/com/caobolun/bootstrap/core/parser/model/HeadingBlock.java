package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 标题 Block：在 ChunkerNode 中由 HeadingHandler 消费，不直接产 chunk，而是累积到后续 chunk 的 outlinePath
 *
 * @param level markdown 标题级别，1-6
 * @param text  标题文本
 */
public record HeadingBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        int level,
        String text
) implements Block {
}
