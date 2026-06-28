package com.aizuda.snail.ai.vector.storage.embedding;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import tools.jackson.core.type.TypeReference;
import com.aizuda.snail.ai.common.model.embedding.SnailEmbeddingResponse;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 从模型配置（及必要时通过一次 embedding 调用）解析向量维度，并回写 {@code config_json.embeddingDimension}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingModelDimensionService {

    private static final int FALLBACK_DIMENSION = 1024;

    private final AiModelConfigMapper modelConfigMapper;
    private final ModelRuntimeHandler modelRuntimeHandler;

    /**
     * 获取 embedding 模型向量维度：优先 config_json，其次 API 探测，最后 {@value #FALLBACK_DIMENSION} 并持久化。
     */
    public Integer getEmbeddingDimension(Long embeddingModelId) {
        if (embeddingModelId == null) {
            throw new VectorStoreException("embeddingModelId 不能为空");
        }
        AiModelConfigPO po = modelConfigMapper.selectById(embeddingModelId);
        if (po == null) {
            throw new VectorStoreException("Embedding 模型不存在: " + embeddingModelId);
        }
        ConfigExtAttrsDTO attrs = parseAttrs(po);
        if (attrs.getEmbeddingDimension() != null) {
            return attrs.getEmbeddingDimension();
        }
        Integer fromApi = probeDimensionViaApi(embeddingModelId);
        if (fromApi != null) {
            persistDimensionIfAbsent(po, fromApi);
            return fromApi;
        }
        log.warn("无法通过 API 获取模型维度，使用默认值 {}: modelId={}", FALLBACK_DIMENSION, embeddingModelId);
        persistDimensionIfAbsent(po, FALLBACK_DIMENSION);
        return FALLBACK_DIMENSION;
    }

    private ConfigExtAttrsDTO parseAttrs(AiModelConfigPO po) {
        ConfigExtAttrsDTO attrs = JsonUtil.parseObject(Optional.ofNullable(po.getConfigJson()).orElse("{}"), ConfigExtAttrsDTO.class);
        return attrs != null ? attrs : new ConfigExtAttrsDTO();
    }

    private Integer probeDimensionViaApi(Long embeddingModelId) {
        try {
            SnailEmbeddingResponse resp = modelRuntimeHandler.embed(
                    new ModelRuntimeHandler.EmbeddingRequestDTO(embeddingModelId, "test", null));
            if (resp == null) {
                return null;
            }
            if (resp.getDimensions() != null) {
                return resp.getDimensions();
            }
            if (resp.getVectors() != null && !resp.getVectors().isEmpty()) {
                float[] v = resp.firstVector();
                return v != null ? v.length : null;
            }
        } catch (Exception e) {
            log.warn("探测 embedding 维度失败: modelId={}, {}", embeddingModelId, e.getMessage());
        }
        return null;
    }

    private void persistDimensionIfAbsent(AiModelConfigPO po, int dimension) {
        try {
            ConfigExtAttrsDTO attrs = parseAttrs(po);
            if (attrs.getEmbeddingDimension() != null) {
                return;
            }
            String raw = Optional.ofNullable(po.getConfigJson()).orElse("{}");
            Map<String, Object> map = JsonUtil.parseObject(raw, new TypeReference<Map<String, Object>>() {});
            if (map == null) {
                map = new HashMap<>();
            } else {
                map = new HashMap<>(map);
            }
            map.put("embeddingDimension", dimension);
            po.setConfigJson(JsonUtil.toJsonString(map));
            po.setUpdatedDt(LocalDateTime.now());
            modelConfigMapper.updateById(po);
            log.info("已自动写入模型 {} 的 embeddingDimension={}", po.getId(), dimension);
        } catch (Exception e) {
            log.error("持久化 embedding 维度失败: modelId={}", po.getId(), e);
        }
    }
}
