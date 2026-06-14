package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ConfigResolveHandler implements RagSearchHandler {

    private final RagMapper knowledgeMapper;

    @Override
    public void handle(RagSearchContext ctx) {
        RagPO knowledge = knowledgeMapper.selectById(ctx.getRagId());
        if (knowledge == null) {
            log.error("Knowledge not found: {}", ctx.getRagId());
            ctx.setTerminated(true);
            return;
        }
        ctx.setKnowledge(knowledge);

        RagConfigDO configDO = ctx.getExternalConfigDO();
        if (configDO == null) {
            configDO = StrUtil.isNotBlank(knowledge.getConfig())
                    ? JsonUtil.parseObject(knowledge.getConfig(), RagConfigDO.class)
                    : new RagConfigDO();
            if (configDO == null) {
                configDO = new RagConfigDO();
            }
        }

        ctx.setSearchParams(configDO.getSearchParams() != null
                ? configDO.getSearchParams() : new RagConfigDO.SearchParams());
        ctx.setModelParams(configDO.getModelParams());
        ctx.setQuery(ctx.getOriginalQuery());
    }
}
