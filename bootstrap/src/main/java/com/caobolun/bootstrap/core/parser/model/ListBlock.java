package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 列表 Block：由 ListChunker 处理。短列表 atomic，长列表按项分组
 *
 * @param ordered 是否有序列表
 * @param items   列表项内容
 */
public record ListBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        boolean ordered,
        List<String> items
) implements Block {
}
