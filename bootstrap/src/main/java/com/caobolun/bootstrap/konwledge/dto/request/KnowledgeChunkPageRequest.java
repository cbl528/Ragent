package com.caobolun.bootstrap.konwledge.dto.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class KnowledgeChunkPageRequest extends Page {

    private Integer enabled;
}
