package com.caobolun.infraai.chat;

import com.caobolun.framework.convention.ChatRequest;
import com.caobolun.framework.trace.RagStreamTraceSupport;
import com.caobolun.infraai.enums.ModelProvider;
import com.caobolun.infraai.model.ModelTarget;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Slf4j
@Service
public class AIHubMixChatClient extends AbstractOpenAIStyleChatClient {

    @Override
    public String provider() {
        return ModelProvider.AI_HUB_MIX.getId();
    }

    @Override
    public String chat(ChatRequest request, ModelTarget target) {
        return doChat(request, target);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target) {
        return doStreamChat(request, callback, target);
    }
}
