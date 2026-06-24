package com.caobolun.bootstrap.rag.controller;

import com.caobolun.bootstrap.rag.config.RAGDefaultProperties;
import com.caobolun.bootstrap.rag.service.RAGChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RAGChatController {

    private final RAGChatService ragChatService;
    private final RAGDefaultProperties ragDefaultProperties;

}