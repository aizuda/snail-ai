package com.aizuda.snail.ai.admin.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.StoreInstanceTestRequestVO;
import com.aizuda.snail.ai.common.enums.CommonStatusEnum;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.common.enums.rag.StoreInstanceCategoryEnum;
import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreConfigDTO;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.StoreInstanceQueryVO;
import com.aizuda.snail.ai.admin.vo.StoreInstanceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreInstanceService {

    private static final Set<Integer> VECTOR_TYPES = Set.of(
            StoreInstanceTypeEnum.PG_VECTOR.getType(),
            StoreInstanceTypeEnum.MILVUS.getType(),
            StoreInstanceTypeEnum.ELASTICSEARCH.getType());
    private static final Set<Integer> SEARCH_TYPES = Set.of(StoreInstanceTypeEnum.ELASTICSEARCH.getType());

    private final StoreInstanceMapper mapper;
    private final RagMapper knowledgeMapper;

    public List<StoreInstanceVO> listByCategory(Integer category) {
        LambdaQueryWrapper<StoreInstancePO> qw = new LambdaQueryWrapper<>();
        if (category != null) {
            qw.eq(StoreInstancePO::getCategory, category);
        }
        qw.orderByDesc(StoreInstancePO::getIsDefault).orderByDesc(StoreInstancePO::getCreateDt);
        return mapper.selectList(qw).stream().map(this::toVo).collect(Collectors.toList());
    }

    public PageResult<List<StoreInstanceVO>> page(StoreInstanceQueryVO query) {
        LambdaQueryWrapper<StoreInstancePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ObjUtil.isNotNull(query.getStartDt()) && ObjUtil.isNotNull(query.getEndDt()),
                        StoreInstancePO::getCreateDt, query.getStartDt(), query.getEndDt())
                .eq(Objects.nonNull(query.getCategory()), StoreInstancePO::getCategory, query.getCategory())
                .orderByDesc(StoreInstancePO::getIsDefault)
                .orderByDesc(StoreInstancePO::getCreateDt);

        IPage<StoreInstancePO> page = mapper.selectPage(
                PageDTO.of(query.getPage(), query.getSize()), wrapper);

        List<StoreInstanceVO> list = page.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());

        PageResult<List<StoreInstanceVO>> result = new PageResult<>();
        result.setData(list);
        result.setPage((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setTotal(page.getTotal());
        return result;
    }

    public StoreInstanceVO getById(Long id) {
        StoreInstancePO po = mapper.selectById(id);
        if (po == null) {
            return null;
        }
        return toVo(po);
    }

    @Transactional
    public StoreInstanceVO create(StoreInstanceVO vo) {
        validateCategoryType(vo.getCategory(), vo.getType());
        StoreInstancePO po = new StoreInstancePO();
        po.setName(vo.getName().trim());
        po.setCategory(vo.getCategory());
        po.setType(vo.getType());
        po.setConfig(JsonUtil.toJsonString(vo.getConfig() != null ? vo.getConfig() : Map.of()));
        po.setStatus(vo.getStatus() != null ? vo.getStatus() : CommonStatusEnum.ENABLED.getValue());
        po.setIsDefault(Boolean.TRUE.equals(vo.getIsDefault()));
        po.setCreateDt(LocalDateTime.now());
        po.setUpdateDt(LocalDateTime.now());
        if (Boolean.TRUE.equals(po.getIsDefault())) {
            clearDefaultInCategory(po.getCategory(), null);
        }
        mapper.insert(po);
        return toVo(mapper.selectById(po.getId()));
    }

    @Transactional
    public StoreInstanceVO update(Long id, StoreInstanceVO vo) {
        StoreInstancePO existing = mapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("存储实例不存在: " + id);
        }
        Integer cat = vo.getCategory() != null ? vo.getCategory() : existing.getCategory();
        if (vo.getCategory() != null && vo.getType() != null) {
            validateCategoryType(vo.getCategory(), vo.getType());
        }
        if (Boolean.TRUE.equals(vo.getIsDefault())) {
            clearDefaultInCategory(cat, id);
        }
        LambdaUpdateWrapper<StoreInstancePO> uw = new LambdaUpdateWrapper<StoreInstancePO>()
                .eq(StoreInstancePO::getId, id)
                .set(StoreInstancePO::getUpdateDt, LocalDateTime.now());
        if (StrUtil.isNotBlank(vo.getName())) {
            uw.set(StoreInstancePO::getName, vo.getName().trim());
        }
        if (vo.getCategory() != null) {
            uw.set(StoreInstancePO::getCategory, vo.getCategory());
        }
        if (vo.getType() != null) {
            uw.set(StoreInstancePO::getType, vo.getType());
        }
        if (vo.getConfig() != null) {
            uw.set(StoreInstancePO::getConfig, JsonUtil.toJsonString(vo.getConfig()));
        }
        if (vo.getStatus() != null) {
            uw.set(StoreInstancePO::getStatus, vo.getStatus());
        }
        if (vo.getIsDefault() != null) {
            uw.set(StoreInstancePO::getIsDefault, vo.getIsDefault());
        }
        mapper.update(null, uw);
        return toVo(mapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        StoreInstancePO instance = mapper.selectById(id);
        if (instance == null) {
            return;
        }
        StoreInstanceCategoryEnum cat = StoreInstanceCategoryEnum.fromCategory(instance.getCategory());
        LambdaQueryWrapper<RagPO> qw = new LambdaQueryWrapper<>();
        if (cat == StoreInstanceCategoryEnum.VECTOR_STORE) {
            qw.eq(RagPO::getVectorStoreInstanceId, id);
        } else {
            qw.eq(RagPO::getSearchEngineInstanceId, id);
        }
        long cnt = knowledgeMapper.selectCount(qw);
        if (cnt > 0) {
            throw new IllegalArgumentException("该实例仍被 " + cnt + " 个知识库引用，无法删除");
        }
        mapper.deleteById(id);
    }

    public boolean testConnection(StoreInstanceTestRequestVO req) {
        SnailAiVectorStore apply = VectorStoreFactory.REGISTER.get(VectorStoreType.valueOf(req.getType()))
                .apply(VectorStoreConfigDTO
                        .builder()
                        .config(JsonUtil.toJsonString(req.getConfig()))
                        .build());
        return apply.test();
    }

    private void clearDefaultInCategory(Integer category, Long excludeId) {
        LambdaUpdateWrapper<StoreInstancePO> uw = new LambdaUpdateWrapper<StoreInstancePO>()
                .eq(StoreInstancePO::getCategory, category)
                .set(StoreInstancePO::getIsDefault, false);
        if (excludeId != null) {
            uw.ne(StoreInstancePO::getId, excludeId);
        }
        mapper.update(null, uw);
    }

    private void validateCategoryType(Integer category, Integer type) {
        StoreInstanceCategoryEnum cat = StoreInstanceCategoryEnum.fromCategory(category);
        if (cat == StoreInstanceCategoryEnum.VECTOR_STORE) {
            if (!VECTOR_TYPES.contains(type)) {
                throw new IllegalArgumentException("向量库分类下不支持的类型: " + type);
            }
        } else {
            if (!SEARCH_TYPES.contains(type)) {
                throw new IllegalArgumentException("搜索引擎分类下不支持的类型: " + type);
            }
        }
    }

    private StoreInstanceVO toVo(StoreInstancePO po) {
        Map<String, Object> cfg = Map.of();
        if (StrUtil.isNotBlank(po.getConfig())) {
            try {
                cfg = JsonUtil.parseObject(po.getConfig(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.warn("解析实例 config JSON 失败 id={}: {}", po.getId(), e.getMessage());
                cfg = new HashMap<>();
            }
        }
        StoreInstanceVO vo = new StoreInstanceVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setCategory(po.getCategory());
        vo.setType(po.getType());
        vo.setConfig(cfg);
        vo.setStatus(po.getStatus());
        vo.setIsDefault(po.getIsDefault());
        vo.setCreateDt(po.getCreateDt());
        vo.setUpdateDt(po.getUpdateDt());
        return vo;
    }
}
