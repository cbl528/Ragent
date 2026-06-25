package com.caobolun.infraai.embedding;

import cn.hutool.core.collection.CollUtil;
import com.caobolun.infraai.config.AIModelProperties;
import com.caobolun.infraai.enums.ModelCapability;
import com.caobolun.infraai.http.HttpResponseHelper;
import com.caobolun.infraai.http.ModelUrlResolver;
import com.caobolun.infraai.model.ModelTarget;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.Collections;
import java.util.List;

/**
 * OpenAI 兼容协议 EmbeddingClient 抽象基类
 * 封装 /v1/embeddings 协议的通用逻辑，子类只需提供 provider 和覆写钩子方法
 */
@Slf4j
public abstract class AbstractOpenAIStyleEmbeddingClient implements EmbeddingClient {

    protected final OkHttpClient httpClient;

    protected AbstractOpenAIStyleEmbeddingClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 是否要求提供商配置 API Key，默认 true
     */
    protected boolean requiresApiKey() {
        return true;
    }

    /**
     * 子类可覆写此方法添加提供商特有的请求体字段
     * 默认实现：添加 encoding_format=float
     */
    protected void customizeRequestBody(JsonObject body, ModelTarget target) {
        body.addProperty("encoding_format", "float");
    }

    /**
     * 单次请求最大批量大小，0 表示不限制
     */
    protected int maxBatchSize() {
        return 0;
    }

    @Override
    public List<Float> embed(String text, ModelTarget target) {
        return List.of();
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, ModelTarget target) {
        if(CollUtil.isEmpty(texts)){
            return Collections.emptyList();
        }
    }

    /**
     * 构建请求、发送 HTTP、解析 OpenAI 格式响应
     */
    protected List<List<Float>> doEmbed(List<String> texts, ModelTarget target) {
        AIModelProperties.ProviderConfig providerConfig = HttpResponseHelper.requireProvider(target, provider());
        if(requiresApiKey()){
            HttpResponseHelper.requireApiKey(providerConfig, provider());
        }

        ModelUrlResolver.resolveUrl(providerConfig, target.candidate(), ModelCapability.EMBEDDING);

        JsonObject body = new JsonObject();
        body.addProperty("model", HttpResponseHelper.requireModel(target, provider()));
        JsonArray inputArray = new JsonArray();

    }
}