package com.aizuda.snail.ai.admin.service.model;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.aizuda.snail.ai.admin.vo.model.AiModelProviderQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelProviderVO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型提供商管理服务
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelProviderService {

    private final AiModelProviderMapper providerMapper;
    private final AiModelConfigMapper configMapper;

    /**
     * 新增提供商 (Admin权限)
     *
     * @param providerName 提供商名称
     * @param providerKey  提供商唯一标识
     * @param description  提供商描述
     * @param iconUrl      提供商图标地址
     * @return 新建提供商的主键 ID
     */
    @Transactional
    public Long addProvider(String providerName, String providerKey, String description, String iconUrl) {
        log.info("添加AI提供商: name={}, key={}", providerName, providerKey);

        // 检查提供商是否已存在
        LambdaQueryWrapper<AiModelProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelProviderPO::getProviderKey, providerKey);
        AiModelProviderPO existing = providerMapper.selectOne(wrapper);
        if (existing != null) {
            log.warn("提供商已存在: {}", providerKey);
            throw new SnailAiException("提供商已存在: " + providerKey);
        }

        AiModelProviderPO provider = AiModelProviderPO.builder()
                .providerName(providerName)
                .providerKey(providerKey)
                .description(description)
                .iconUrl(iconUrl)
                .isEnabled(true)
                .createdDt(LocalDateTime.now())
                .updatedDt(LocalDateTime.now())
                .build();

        providerMapper.insert(provider);
        log.info("添加提供商成功: id={}", provider.getId());
        return provider.getId();
    }

    /**
     * 新增提供商 (接收VO对象)
     *
     * @param vo 提供商信息
     * @return 新建提供商的主键 ID
     */
    @Transactional
    public Long addProvider(AiModelProviderVO vo) {
        return addProvider(vo.getProviderName(), vo.getProviderKey(), vo.getDescription(), vo.getIconUrl());
    }

    /**
     * 获取所有启用的提供商列表 (带缓存)
     *
     * @return 启用状态的提供商列表
     */
    public List<AiModelProviderVO> getProviderList() {
        log.debug("查询所有启用的提供商");

        LambdaQueryWrapper<AiModelProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelProviderPO::getIsEnabled, true)
                .orderByAsc(AiModelProviderPO::getCreatedDt);

        List<AiModelProviderPO> providers = providerMapper.selectList(wrapper);
        return providers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有提供商（包括禁用的，用于后台管理）
     *
     * @param queryVO 分页查询条件
     * @return 提供商分页结果
     */
    public PageResult<List<AiModelProviderVO>> getAllProviders(AiModelProviderQueryVO queryVO) {
        PageDTO<AiModelProviderPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        LambdaQueryWrapper<AiModelProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AiModelProviderPO::getCreatedDt);
        PageDTO<AiModelProviderPO> page = providerMapper.selectPage(pageDTO, wrapper);
        return new PageResult<>(page,page.getRecords()
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    /**
     * 按ID获取提供商
     *
     * @param providerId 提供商 ID
     * @return 提供商 PO；不存在或参数非法时返回 null
     */
    public AiModelProviderPO getProviderById(Long providerId) {
        if (providerId == null || providerId <= 0) {
            return null;
        }
        return providerMapper.selectById(providerId);
    }

    /**
     * 按ID获取提供商VO
     *
     * @param providerId 提供商 ID
     * @return 提供商 VO；不存在时返回 null
     */
    public AiModelProviderVO getProvider(Long providerId) {
        AiModelProviderPO po = getProviderById(providerId);
        if (po == null) {
            log.warn("提供商不存在: id={}", providerId);
            return null;
        }
        return convertToVO(po);
    }

    /**
     * 按提供商标识符获取提供商
     *
     * @param providerKey 提供商唯一标识
     * @return 启用状态的提供商 PO；不存在或参数为空时返回 null
     */
    public AiModelProviderPO getProviderByKey(String providerKey) {
        if (providerKey == null || providerKey.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<AiModelProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelProviderPO::getProviderKey, providerKey)
                .eq(AiModelProviderPO::getIsEnabled, true);
        return providerMapper.selectOne(wrapper);
    }

    /**
     * 更新提供商启用状态
     *
     * @param providerId 提供商 ID
     * @param enabled    是否启用
     * @return 更新是否成功
     */
    @Transactional
    public boolean updateProviderStatus(Long providerId, Boolean enabled) {
        log.info("更新提供商状态: id={}, enabled={}", providerId, enabled);

        AiModelProviderPO provider = AiModelProviderPO.builder()
                .id(providerId)
                .isEnabled(enabled)
                .updatedDt(LocalDateTime.now())
                .build();

        return providerMapper.updateById(provider) > 0;
    }

    /**
     * 更新提供商 (接收VO对象)
     *
     * @param providerId 提供商 ID
     * @param vo         提供商更新信息
     * @return 更新是否成功
     */
    @Transactional
    public boolean updateProvider(Long providerId, AiModelProviderVO vo) {
        log.info("更新AI提供商: id={}, name={}", providerId, vo.getProviderName());

        AiModelProviderPO provider = AiModelProviderPO.builder()
                .id(providerId)
                .providerName(vo.getProviderName())
                .description(vo.getDescription())
                .iconUrl(vo.getIconUrl())
                .updatedDt(LocalDateTime.now())
                .build();

        return providerMapper.updateById(provider) > 0;
    }

    /**
     * 删除提供商（仍被模型配置引用时拒绝删除）
     *
     * @param providerId 提供商 ID
     * @return 删除是否成功；参数非法时返回 false
     */
    @Transactional
    public boolean deleteProvider(Long providerId) {
        log.info("删除AI提供商: id={}", providerId);

        if (providerId == null || providerId <= 0) {
            return false;
        }

        long cnt = configMapper.selectCount(
                new LambdaQueryWrapper<AiModelConfigPO>().eq(AiModelConfigPO::getProviderId, providerId));
        if (cnt > 0) {
            throw new SnailAiException("该提供商下存在" + cnt + "个模型配置，无法删除");
        }

        return providerMapper.deleteById(providerId) > 0;
    }

    /**
     * 启用提供商
     *
     * @param providerId 提供商 ID
     * @return 更新是否成功
     */
    @Transactional
    public boolean enableProvider(Long providerId) {
        log.info("启用提供商: id={}", providerId);
        return updateProviderStatus(providerId, true);
    }

    /**
     * 禁用提供商
     *
     * @param providerId 提供商 ID
     * @return 更新是否成功
     */
    @Transactional
    public boolean disableProvider(Long providerId) {
        log.info("禁用提供商: id={}", providerId);
        return updateProviderStatus(providerId, false);
    }

    /**
     * 转换为VO
     */
    private AiModelProviderVO convertToVO(AiModelProviderPO po) {
        return AiModelProviderVO.builder()
                .id(po.getId())
                .providerName(po.getProviderName())
                .providerKey(po.getProviderKey())
                .description(po.getDescription())
                .iconUrl(po.getIconUrl())
                .isEnabled(po.getIsEnabled())
                .createdDt(po.getCreatedDt())
                .updatedDt(po.getUpdatedDt())
                .build();
    }
}
