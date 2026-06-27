package com.caobolun.infraai.rerank;

import cn.hutool.core.collection.CollUtil;
import com.caobolun.framework.convention.RetrievedChunk;
import com.caobolun.infraai.config.AIModelProperties;
import com.caobolun.infraai.enums.ModelCapability;
import com.caobolun.infraai.enums.ModelProvider;
import com.caobolun.infraai.http.*;
import com.caobolun.infraai.model.ModelTarget;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaiLianRerankClient implements RerankClient {

    @Qualifier("syncHttpClient")
    private final OkHttpClient httpClient;


    @Override
    public String provider() {
        return ModelProvider.BAI_LIAN.getId();
    }

    /**
     * 对候选文档片段列表进行排序
     * @param query      用户查询文本
     * @param candidates 待排序的候选文档片段列表
     * @param topN       返回前N个最相关的结果
     * @param target     目标模型配置信息
     * @return 排序后的候选文档片段列表
     */
    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target) {
        // 如果候选列表为空，则直接返回
        if(candidates == null || candidates.isEmpty()){
            return List.of();
        }

        List<RetrievedChunk> dedup = new ArrayList<>(candidates.size()); // 去重后的候选列表
        Set<String> seen = new HashSet<>(); // 用于去重的集合

        for(RetrievedChunk rc : candidates){
            if(seen.add(rc.getId())){
                dedup.add(rc);
            }
        }
        // 如果去重后的候选列表长度小于等于topN，则直接返回
        if(topN <= 0 || dedup.size() <= topN){
            return dedup;
        }
        // 调用具体实现的排序方法
        return doRerank(query, dedup, topN, target);
    }

    private List<RetrievedChunk> doRerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target) {
        // 获取目标模型的配置信息
        AIModelProperties.ProviderConfig providerConfig = HttpResponseHelper.requireProvider(target, provider());
        // 如果候选列表为空，则直接返回
        if(candidates == null || candidates.isEmpty() || topN <= 0){
            return List.of();
        }
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("model", HttpResponseHelper.requireModel(target, provider()));
        JsonObject input = new JsonObject();
        input.addProperty("query", query);

        JsonArray documentsArray = new JsonArray();
        for(RetrievedChunk each : candidates){
            documentsArray.add(each.getText() == null ? "" : each.getText());
        }
        input.add("documents", documentsArray);

        JsonObject params = new JsonObject();
        params.addProperty("top_n", topN);
        params.addProperty("return_documents", true);

        reqBody.add("input", input);
        reqBody.add("parameters", params);

        Request request = new Request.Builder()
                .url(ModelUrlResolver.resolveUrl(providerConfig, target.candidate(), ModelCapability.RERANK))
                .post(RequestBody.create(reqBody.toString(), HttpMediaTypes.JSON))
                .addHeader("Authorization", "Bearer " + providerConfig.getApiKey())
                .build();

        JsonObject response;
        try (Response resp = httpClient.newCall(request).execute()){
            if(!resp.isSuccessful()){
                String body = HttpResponseHelper.readBody(resp.body());
                log.warn("{} rerank 请求失败: status={}, body={}", provider(), resp.code(), body);
                throw new ModelClientException(
                        provider() + "rerank 请求失败: HTTP" + resp.code(),
                        ModelClientErrorType.fromHttpStatus(resp.code()),
                        resp.code()
                );
            }
            response = HttpResponseHelper.parseJson(resp.body(),provider());
        } catch (IOException e) {
            throw new ModelClientException(provider() + " rerank 请求失败: " + e.getMessage(), ModelClientErrorType.NETWORK_ERROR, null, e);
        }

        JsonObject output = requireOutput(response);
        JsonArray results = output.getAsJsonArray("results");
        if(CollUtil.isEmpty(results)){
            throw new ModelClientException(provider() + " rerank 的结果为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }

        List<RetrievedChunk> reranked = new ArrayList<>();
        Set<String> addedIds = new HashSet<>();

        for(JsonElement result : results){
            // 检查结果是否为JsonObject
            if(!result.isJsonObject()){
                continue;
            }
            JsonObject jsonObject = result.getAsJsonObject();
            if(!jsonObject.has("index")){
                continue;
            }
            int idx = jsonObject.get("index").getAsInt();

            if(idx<0 || idx >= candidates.size()){
                continue;
            }

            RetrievedChunk retrievedChunk = candidates.get(idx);
            Float score = null;
            if(jsonObject.has("relevance_score") && !jsonObject.get("relevance_score").isJsonNull()){
                score = jsonObject.get("relevance_score").getAsFloat();
            }

            RetrievedChunk hit = score != null ? new RetrievedChunk(retrievedChunk.getId(), retrievedChunk.getText(), score) : retrievedChunk;
            reranked.add(hit);
            addedIds.add(retrievedChunk.getId());
            if (reranked.size() >= topN){
                break;
            }
        }

        if(reranked.size() < topN){
            for(RetrievedChunk c : candidates){
                if (addedIds.add(c.getId())){
                    reranked.add(c);
                }
                if(reranked.size() >= topN){
                    break;
                }
            }
        }

        return reranked;
    }
    private JsonObject requireOutput(JsonObject respJson) {
        if (respJson == null || !respJson.has("output")) {
            throw new ModelClientException(provider() + " rerank 响应缺少 output", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonObject output = respJson.getAsJsonObject("output");
        if (output == null || !output.has("results")) {
            throw new ModelClientException(provider() + " rerank 响应缺少 results", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        return output;
    }
}