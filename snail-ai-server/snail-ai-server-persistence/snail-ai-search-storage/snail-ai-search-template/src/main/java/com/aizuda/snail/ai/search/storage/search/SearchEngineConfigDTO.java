package com.aizuda.snail.ai.search.storage.search;

import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全文检索引擎构建配置，携带存储实例及其原始配置 JSON，供工厂创建引擎实例使用。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchEngineConfigDTO {

    /**
     * 存储实例信息
     */
    private StoreInstancePO storeInstance;

    /**
     * 原始配置 JSON 字符串
     */
    private String config;
}
