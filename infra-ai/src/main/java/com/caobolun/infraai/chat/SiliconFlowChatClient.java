package com.caobolun.infraai.chat;

import com.caobolun.framework.convention.ChatRequest;
import com.caobolun.framework.trace.RagTraceNode;
import com.caobolun.infraai.enums.ModelProvider;
import com.caobolun.infraai.model.ModelTarget;
import com.caobolun.framework.trace.RagStreamTraceSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Slf4j
@Service
public class SiliconFlowChatClient extends AbstractOpenAIStyleChatClient {

    public SiliconFlowChatClient(OkHttpClient syncHttpClient,
                                  OkHttpClient streamingHttpClient,
                                  Executor modelStreamExecutor,
                                  RagStreamTraceSupport ragStreamTraceSupport) {
        super(syncHttpClient, streamingHttpClient, modelStreamExecutor, ragStreamTraceSupport);
    }

    @Override
    public String provider() {
        return ModelProvider.SILICON_FLOW.getId();
    }

    @Override
    @RagTraceNode(name = "siliconflow-chat", type = "LLM_PROVIDER")
    public String chat(ChatRequest request, ModelTarget target) {
        return doChat(request, target);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target) {
        return doStreamChat(request, callback, target);
    }
}
