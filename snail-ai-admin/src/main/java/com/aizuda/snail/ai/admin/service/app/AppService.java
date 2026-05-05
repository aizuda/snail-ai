package com.aizuda.snail.ai.admin.service.app;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.route.RouteStrategyType;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.app.*;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.app.mapper.AppMapper;
import com.aizuda.snail.ai.persistence.app.mapper.ClientNodeMapper;
import com.aizuda.snail.ai.persistence.app.po.AppPO;
import com.aizuda.snail.ai.persistence.app.po.ClientNodePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 应用管理服务
 * <p>
 * 负责客户端应用的创建、更新、删除和查询，以及客户端节点的管理。
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    /** 应用状态：启用 */
    private static final int APP_STATUS_ENABLED = 1;
    
    /** 应用状态：禁用 */
    private static final int APP_STATUS_DISABLED = 0;
    
    /** Token 前缀 */
    private static final String TOKEN_PREFIX = "SAI_";

    private final AppMapper appMapper;
    private final ClientNodeMapper clientNodeMapper;

    public AppResponseVO create(AppRequestVO request) {
        Long count = appMapper.selectCount(new LambdaQueryWrapper<AppPO>().eq(AppPO::getAppId, request.getAppId()));
        if (count > 0) {
            throw new SnailAiCommonException("appId:{} 已存在", request.getAppId());
        }
        AppPO po = AppPO.builder()
                .appId(request.getAppId())
                .appName(request.getAppName())
                .description(request.getDescription())
                .routeStrategy(resolveRouteStrategy(request.getRouteStrategy()))
                .token(generateToken())
                .status(APP_STATUS_ENABLED)
                .build();
        appMapper.insert(po);
        return toResponseVO(po);
    }

    public AppResponseVO update(Long id, AppRequestVO request) {
        AppPO po = findAppById(id);
        updateAppFields(po, request);
        appMapper.updateById(po);
        return toResponseVO(po);
    }

    public void delete(Long id) {
        AppPO po = appMapper.selectById(id);
        if (po == null) return;
        
        deleteRelatedNodes(po.getAppId());
        appMapper.deleteById(id);
    }

    public void toggleStatus(Long id) {
        AppPO po = findAppById(id);
        po.setStatus(isEnabled(po) ? APP_STATUS_DISABLED : APP_STATUS_ENABLED);
        appMapper.updateById(po);
    }

    public PageResult<List<AppResponseVO>> page(AppQueryVO query) {
        IPage<AppPO> page = appMapper.selectPage(
                PageDTO.of(query.getPage(), query.getSize()), buildQueryWrapper(query));

        return buildPageResult(page);
    }

    public List<AppResponseVO> listEnabled() {
        List<AppPO> list = appMapper.selectList(
                new LambdaQueryWrapper<AppPO>()
                        .eq(AppPO::getStatus, APP_STATUS_ENABLED)
                        .orderByDesc(AppPO::getCreateDt));
        return list.stream().map(this::toResponseVO).toList();
    }

    public List<ClientNodeVO> getNodes(String appId) {
        List<ClientNodePO> nodes = clientNodeMapper.selectList(
                new LambdaQueryWrapper<ClientNodePO>()
                        .eq(ClientNodePO::getAppId, appId)
                        .orderByDesc(ClientNodePO::getExpireDt));

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> appNameMap = buildAppNameMap();
        return nodes.stream()
                .map(node -> convertToClientNodeVO(node, now, appNameMap))
                .toList();
    }

    /**
     * 获取所有应用下的客户端节点（全局视图）
     */
    public List<ClientNodeVO> getAllNodes() {
        List<ClientNodePO> nodes = clientNodeMapper.selectList(
                new LambdaQueryWrapper<ClientNodePO>()
                        .orderByDesc(ClientNodePO::getExpireDt));

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> appNameMap = buildAppNameMap();
        return nodes.stream()
                .map(node -> convertToClientNodeVO(node, now, appNameMap))
                .toList();
    }

    public void kickNode(String appId, String hostId) {
        clientNodeMapper.delete(new LambdaQueryWrapper<ClientNodePO>()
                .eq(ClientNodePO::getAppId, appId)
                .eq(ClientNodePO::getHostId, hostId));
    }

    // ==================== 私有辅助方法 ====================

    private AppPO findAppById(Long id) {
        AppPO po = appMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("应用不存在: " + id);
        }
        return po;
    }

    private void updateAppFields(AppPO po, AppRequestVO request) {
        if (request.getAppName() != null) {
            po.setAppName(request.getAppName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getRouteStrategy() != null) {
            po.setRouteStrategy(request.getRouteStrategy());
        }
    }

    private void deleteRelatedNodes(String appId) {
        clientNodeMapper.delete(new LambdaQueryWrapper<ClientNodePO>()
                .eq(ClientNodePO::getAppId, appId));
    }

    private boolean isEnabled(AppPO po) {
        return po.getStatus() == APP_STATUS_ENABLED;
    }

    private String resolveRouteStrategy(String routeStrategy) {
        return StrUtil.isNotBlank(routeStrategy) ? routeStrategy : RouteStrategyType.LEAST_LOAD;
    }

    private LambdaQueryWrapper<AppPO> buildQueryWrapper(AppQueryVO query) {
        LambdaQueryWrapper<AppPO> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(AppPO::getAppName, query.getKeyword())
                    .or()
                    .like(AppPO::getAppId, query.getKeyword());
        }

        wrapper.eq(ObjUtil.isNotNull(query.getStatus()), AppPO::getStatus, query.getStatus())
                .between(ObjUtil.isNotNull(query.getStartDt()) &&  ObjUtil.isNotNull(query.getEndDt()),
                        AppPO::getCreateDt, query.getStartDt(), query.getEndDt())
                .orderByDesc(AppPO::getCreateDt);
        return wrapper;
    }

    private PageResult<List<AppResponseVO>> buildPageResult(IPage<AppPO> page) {
        List<AppResponseVO> list = page.getRecords().stream()
                .map(this::toResponseVO)
                .toList();

        PageResult<List<AppResponseVO>> result = new PageResult<>();
        result.setData(list);
        result.setPage((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setTotal(page.getTotal());
        return result;
    }

    private AppResponseVO toResponseVO(AppPO po) {
        long onlineCount = countOnlineNodes(po.getAppId());

        return AppResponseVO.builder()
                .id(po.getId())
                .appId(po.getAppId())
                .appName(po.getAppName())
                .description(po.getDescription())
                .token(po.getToken())
                .routeStrategy(po.getRouteStrategy())
                .status(po.getStatus())
                .onlineNodes((int) onlineCount)
                .createDt(po.getCreateDt())
                .build();
    }

    private long countOnlineNodes(String appId) {
        return clientNodeMapper.selectCount(
                new LambdaQueryWrapper<ClientNodePO>()
                        .eq(ClientNodePO::getAppId, appId)
                        .gt(ClientNodePO::getExpireDt, LocalDateTime.now()));
    }

    private ClientNodeVO convertToClientNodeVO(ClientNodePO node, LocalDateTime now, Map<String, String> appNameMap) {
        return ClientNodeVO.builder()
                .id(node.getId())
                .appId(node.getAppId())
                .appName(appNameMap.getOrDefault(node.getAppId(), node.getAppId()))
                .hostId(node.getHostId())
                .hostIp(node.getHostIp())
                .grpcPort(node.getGrpcPort())
                .maxConcurrent(node.getMaxConcurrent())
                .activeChats(node.getActiveChats())
                .labels(parseLabels(node.getLabels()))
                .expireDt(node.getExpireDt())
                .online(isNodeOnline(node, now))
                .build();
    }

    /**
     * 构建 appId → appName 映射
     */
    private Map<String, String> buildAppNameMap() {
        List<AppPO> apps = appMapper.selectList(
                new LambdaQueryWrapper<AppPO>()
                        .select(AppPO::getAppId, AppPO::getAppName));
        Map<String, String> map = new HashMap<>();
        for (AppPO app : apps) {
            map.put(app.getAppId(), app.getAppName());
        }
        return map;
    }

    private Map<String, String> parseLabels(String labelsJson) {
        if (StrUtil.isBlank(labelsJson)) {
            return Map.of();
        }
        try {
            return JsonUtil.parseHashMap(labelsJson);
        } catch (Exception e) {
            log.warn("Failed to parse labels: {}", labelsJson, e);
            return Map.of();
        }
    }

    private boolean isNodeOnline(ClientNodePO node, LocalDateTime now) {
        return node.getExpireDt() != null && node.getExpireDt().isAfter(now);
    }

    private String generateToken() {
        return TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
