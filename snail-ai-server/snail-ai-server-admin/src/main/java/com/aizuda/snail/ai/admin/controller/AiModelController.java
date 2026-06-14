package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigRequestVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelByProviderTypeQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelProviderRequestVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelProviderVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelProviderQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatsQueryVO;
import com.aizuda.snail.ai.admin.vo.model.ModelAdapterOptionVO;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.admin.service.model.AiModelProviderService;
import com.aizuda.snail.ai.admin.service.model.AiModelUsageService;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型管理控制器
 * 提供模型配置CRUD、模型切换、使用统计等API
 */
@RestController
@RequestMapping("/ai-model")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelConfigService modelConfigService;
    private final AiModelProviderService modelProviderService;
    private final AiModelUsageService modelUsageService;

    /**
     * 获取提供商列表（仅启用的）
     * GET /ai-model/providers
     */
    @GetMapping("/providers")
    @LoginRequired
    public List<AiModelProviderVO> listProviders() {
        return modelProviderService.getProviderList();
    }

    /**
     * 获取模型类型支持的适配器列表
     * GET /ai-model/adapters?modelType=CHAT
     */
    @GetMapping("/adapters")
    @LoginRequired
    public List<ModelAdapterOptionVO> listModelAdapters(@RequestParam("modelType") String modelType) {
        return modelConfigService.listModelAdapters(modelType);
    }

    /**
     * 获取所有提供商（包括禁用的，用于后台管理）
     * GET /ai-model/all-providers
     */
    @GetMapping("/all-providers")
    @LoginRequired
    public PageResult<List<AiModelProviderVO>> listAllProviders(AiModelProviderQueryVO queryVO) {
        return modelProviderService.getAllProviders(queryVO);
    }

    /**
     * 获取单个提供商
     * GET /ai-model/provider/{id}
     */
    @GetMapping("/provider/{id}")
    @LoginRequired
    public AiModelProviderVO getProvider(@PathVariable("id") Long id) {
        return modelProviderService.getProvider(id);
    }

    /**
     * 新增提供商 (Admin)
     * POST /ai-model/provider
     */
    @PostMapping("/provider")
    @LoginRequired
    public Long addProvider(@RequestBody @Validated AiModelProviderRequestVO requestVO) {
        return modelProviderService.addProvider(requestVO);
    }

    /**
     * 更新提供商 (Admin)
     * PUT /ai-model/provider/{id}
     */
    @PutMapping("/provider/{id}")
    @LoginRequired
    public boolean updateProvider(
            @PathVariable("id") Long id,
            @RequestBody @Validated AiModelProviderRequestVO requestVO) {
        return modelProviderService.updateProvider(id, requestVO);
    }

    /**
     * 删除提供商 (Admin)
     * DELETE /ai-model/provider/{id}
     */
    @DeleteMapping("/provider/{id}")
    @LoginRequired
    public boolean deleteProvider(@PathVariable("id") Long id) {
        return modelProviderService.deleteProvider(id);
    }

    /**
     * 启用提供商 (Admin)
     * PUT /ai-model/provider/{id}/enable
     */
    @PutMapping("/provider/{id}/enable")
    @LoginRequired
    public boolean enableProvider(@PathVariable("id") Long id) {
        return modelProviderService.enableProvider(id);
    }

    /**
     * 禁用提供商 (Admin)
     * PUT /ai-model/provider/{id}/disable
     */
    @PutMapping("/provider/{id}/disable")
    @LoginRequired
    public boolean disableProvider(@PathVariable("id") Long id) {
        return modelProviderService.disableProvider(id);
    }

    /**
     * 新增模型配置 (Admin)
     * POST /ai-model/config
     */
    @PostMapping("/config")
    @LoginRequired
    public Long addModelConfig(@RequestBody @Validated AiModelConfigRequestVO requestVO) {
        return modelConfigService.addModelConfig(requestVO);
    }

    /**
     * 更新模型配置 (Admin)
     * PUT /ai-model/config/{id}
     */
    @PutMapping("/config/{id}")
    @LoginRequired
    public boolean updateModelConfig(
            @PathVariable("id") Long id,
            @RequestBody @Validated AiModelConfigRequestVO requestVO) {
        return modelConfigService.updateModelConfig(id, requestVO);
    }

    /**
     * 删除模型配置 (Admin)
     * DELETE /ai-model/config/{id}
     */
    @DeleteMapping("/config/{id}")
    @LoginRequired
    public boolean deleteModelConfig(@PathVariable("id") Long id) {
        return modelConfigService.deleteModelConfig(id);
    }

    /**
     * 获取单个模型配置
     * GET /ai-model/config/{id}
     */
    @GetMapping("/config/{id}")
    @LoginRequired
    public AiModelConfigVO getModelConfig(@PathVariable("id") Long id) {
        return modelConfigService.getModelConfig(id);
    }

    /**
     * 分页查询模型配置列表
     * GET /ai-model/configs?pageNum=1&pageSize=10&providerKey=openai&modelType=CHAT&scope=GLOBAL
     */
    @GetMapping("/configs")
    @LoginRequired
    public PageResult<List<AiModelConfigVO>> listModelConfigs(AiModelConfigQueryVO queryVO) {
        return modelConfigService.listModelConfigs(queryVO);
    }

    /**
     * 按模型类型查询模型列表
     * GET /ai-model/by-type/CHAT
     */
    @GetMapping("/by-type/{modelType}")
    @LoginRequired
    public List<AiModelConfigVO> getModelsByType(@PathVariable("modelType") String modelType) {
        return modelConfigService.getModelsByType(modelType);
    }

    /**
     * 按提供商和模型类型查询模型
     * GET /ai-model/by-provider-type?providerKey=openai&modelType=CHAT
     */
    @GetMapping("/by-provider-type")
    @LoginRequired
    public List<AiModelConfigVO> getModelsByProviderAndType(AiModelByProviderTypeQueryVO queryVO) {
        return modelConfigService.getModelsByProviderAndType(queryVO.getProviderKey(), queryVO.getModelType());
    }

    /**
     * 获取全局默认模型
     * GET /ai-model/default
     */
    @GetMapping("/default")
    @LoginRequired
    public AiModelConfigVO getDefaultModel() {
        return modelConfigService.getDefaultModel();
    }

    /**
     * 按类型获取默认模型
     * GET /ai-model/default/CHAT
     */
    @GetMapping("/default/{modelType}")
    @LoginRequired
    public AiModelConfigVO getDefaultModelByType(@PathVariable("modelType") String modelType) {
        return modelConfigService.getDefaultModelByType(modelType);
    }

    /**
     * 切换默认模型 (Admin)
     * PUT /ai-model/switch-default/{modelId}
     */
    @PutMapping("/switch-default/{modelId}")
    @LoginRequired
    public boolean switchDefaultModel(@PathVariable("modelId") Long modelId) {
        return modelConfigService.switchDefaultModel(modelId);
    }

    /**
     * 启用模型配置 (Admin)
     * PUT /ai-model/config/{id}/enable
     */
    @PutMapping("/config/{id}/enable")
    @LoginRequired
    public boolean enableModelConfig(@PathVariable("id") Long id) {
        return modelConfigService.enableModelConfig(id);
    }

    /**
     * 禁用模型配置 (Admin)
     * PUT /ai-model/config/{id}/disable
     */
    @PutMapping("/config/{id}/disable")
    @LoginRequired
    public boolean disableModelConfig(@PathVariable("id") Long id) {
        return modelConfigService.disableModelConfig(id);
    }

    /**
     * 获取特定模型的使用统计
     * GET /ai-model/usage/stat/{modelId}
     */
    @GetMapping("/usage/stat/{modelId}")
    @LoginRequired
    public AiModelUsageStatVO getModelUsageStat(@PathVariable("modelId") Long modelId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        return modelUsageService.getUsageStats(modelId, userId);
    }

    /**
     * 获取当前用户的模型使用统计列表 (分页)
     * GET /ai-model/usage/user-stats?pageNum=1&pageSize=10
     */
    @GetMapping("/usage/user-stats")
    @LoginRequired
    public PageResult<List<AiModelUsageStatVO>> getUserModelStats(AiModelUsageStatsQueryVO queryVO) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        return modelUsageService.getUserModelStats(userId, queryVO);
    }

    /**
     * 获取全局模型使用统计列表 (分页, Admin)
     * GET /ai-model/usage/global-stats?pageNum=1&pageSize=10
     */
    @GetMapping("/usage/global-stats")
    @LoginRequired
    public PageResult<List<AiModelUsageStatVO>> getGlobalModelStats(AiModelUsageStatsQueryVO queryVO) {
        return modelUsageService.getGlobalModelStats(queryVO);
    }
}
