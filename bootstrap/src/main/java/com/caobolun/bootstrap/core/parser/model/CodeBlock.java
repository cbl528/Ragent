package com.caobolun.bootstrap.core.parser.model;

import java.util.List;

/**
 * 代码块 Block：由 CodeChunker 产生 atomic chunk（代码切碎危害大，永不切）
 *
 * @param language 编程语言标识（如 "java"、"bash"），可空
 * @param code     代码内容
 */
public record CodeBlock(
        String id,
        Provenance provenance,
        List<String> outlinePath,
        String language,
        String code
) implements Block {
}
