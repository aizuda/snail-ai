package com.aizuda.snail.ai.search.storage.search;

import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.enums.SearchEngineEnum;
import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 全文检索引擎工厂，按存储实例配置选择并构建对应的 {@link SearchEngine} 实现，
 * 各引擎通过 {@link #REGISTER} 在启动时注册自身的构造逻辑。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchEngineFactory {

    /**
     * 引擎类型 → 构造函数注册表，由各引擎实现在启动时注册
     */
    public static final Map<SearchEngineEnum, Function<SearchEngineConfigDTO, SearchEngine>> REGISTER
            = new ConcurrentHashMap<>();

    private final StoreInstanceMapper storeInstanceMapper;

    /**
     * 按存储实例 ID 解析配置并构建对应的全文检索引擎。
     *
     * @param instanceId 存储实例 ID
     * @return 对应的检索引擎实现
     */
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

        log.debug("构建全文检索引擎 instanceId={} type={}", instanceId, type);
        return create(engineType, SearchEngineConfigDTO.builder()
                .storeInstance(inst)
                .config(inst.getConfig())
                .build());
    }

    /**
     * 按引擎类型与原始配置构建搜索引擎，供未持久化配置的连接测试使用。
     *
     * @param type   搜索引擎类型
     * @param config 配置 JSON
     * @return 搜索引擎实例
     */
    public SearchEngine create(String type, String config) {
        SearchEngineEnum engineType = SearchEngineEnum.fromType(type);
        if (engineType == null) {
            throw new SearchEngineException("搜索引擎类型不存在 type:" + type);
        }
        return create(engineType, SearchEngineConfigDTO.builder().config(config).build());
    }

    private SearchEngine create(SearchEngineEnum engineType, SearchEngineConfigDTO config) {
        Function<SearchEngineConfigDTO, SearchEngine> constructor = REGISTER.get(engineType);
        if (constructor == null) {
            throw new SearchEngineException("搜索引擎类型未注册 type:" + engineType.getType());
        }
        return constructor.apply(config);
    }
}
