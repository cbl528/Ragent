package com.caobolun.bootstrap.rag.service.impl;

import com.caobolun.bootstrap.core.prompt.PromptTemplateLoader;
import com.caobolun.bootstrap.rag.config.MemoryProperties;
import com.caobolun.infraai.chat.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationTitleGenerator {

    private final MemoryProperties memoryProperties;
    private final PromptTemplateLoader promptTemplateLoader;
    private final LLMService llmService;
}
