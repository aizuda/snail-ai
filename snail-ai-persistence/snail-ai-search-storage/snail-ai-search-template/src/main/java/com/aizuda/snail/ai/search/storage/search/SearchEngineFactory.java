package com.aizuda.snail.ai.search.storage.search;

import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.enums.SearchEngineEnum;
import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class SearchEngineFactory {

    public static final Map<SearchEngineEnum, Function<SearchEngineConfigDTO, SearchEngine>> REGISTER
            = new ConcurrentHashMap<>();

    private final StoreInstanceMapper storeInstanceMapper;

    public SearchEngine forStoreInstance(Long instanceId) {
        if (instanceId == null) {
            throw new SearchEngineException("搜索引擎实例 ID 不能为空");
        }
        StoreInstancePO inst = storeInstanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new SearchEngineException("搜索引擎实例不存在: " + instanceId);
        }

        StoreInstanceTypeEnum typeEnum = StoreInstanceTypeEnum.fromType(inst.getType());
        String type = typeEnum != null ? typeEnum.name() : null;
        SearchEngineEnum engineType = SearchEngineEnum.fromType(type);
        if (engineType == null) {
            throw new SearchEngineException("搜索引擎类型不存在 type:" + type);
        }

        Function<SearchEngineConfigDTO, SearchEngine> constructor = REGISTER.get(engineType);
        if (constructor == null) {
            throw new SearchEngineException("搜索引擎类型未注册 type:" + type);
        }
        return constructor.apply(SearchEngineConfigDTO.builder()
                .storeInstance(inst)
                .config(inst.getConfig())
                .build());
    }
}
