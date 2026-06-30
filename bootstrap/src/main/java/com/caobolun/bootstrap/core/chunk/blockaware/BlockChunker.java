package com.caobolun.bootstrap.core.chunk.blockaware;



import com.caobolun.bootstrap.core.chunk.VectorChunk;
import com.caobolun.bootstrap.core.parser.model.Block;

import java.util.List;

/**
 * Block 类型专属的切分器
 * <p>
 * 每个 Block 子类型有独立的 chunker：
 * <ul>
 *   <li>HeadingHandler：累积 outlinePath，不产 chunk</li>
 *   <li>ParagraphChunker：按 token 切，不跨 heading</li>
 *   <li>TableChunker：按 rowsPerChunk + 表头重复</li>
 *   <li>ImageChunker：atomic，渲染 ![caption](http://...)</li>
 *   <li>CodeChunker：atomic（代码切碎危害大）</li>
 *   <li>ListChunker:短列表 atomic,长列表按项分组</li>
 * </ul>
 *
 * @param <B> 该 chunker 处理的 Block 子类型
 */
public interface BlockChunker<B extends Block> {

    /**
     * 把单个 Block 切分为若干 VectorChunk
     *
     * @param block 待切分的 Block
     * @param ctx   切分上下文（outlinePath + 配置 + 起始 index）
     * @return 切分结果（可能为空列表，如 HeadingHandler）
     */
    List<VectorChunk> chunk(B block, ChunkContext ctx);
}
