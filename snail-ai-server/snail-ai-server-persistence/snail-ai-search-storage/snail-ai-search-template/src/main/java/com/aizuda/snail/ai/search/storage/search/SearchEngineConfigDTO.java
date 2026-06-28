package com.aizuda.snail.ai.search.storage.search;

import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SearchEngineConfigDTO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchEngineConfigDTO {

    private StoreInstancePO storeInstance;

    private String config;
}
