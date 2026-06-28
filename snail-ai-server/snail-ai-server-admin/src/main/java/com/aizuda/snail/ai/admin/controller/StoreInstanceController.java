package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.StoreInstanceQueryVO;
import com.aizuda.snail.ai.admin.vo.StoreInstanceRequestVO;
import com.aizuda.snail.ai.admin.vo.StoreInstanceTestRequestVO;
import com.aizuda.snail.ai.admin.vo.StoreInstanceVO;
import com.aizuda.snail.ai.admin.vo.VectorDimensionConstraintVO;
import com.aizuda.snail.ai.admin.service.StoreInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储实例管理接口：向量库 / 搜索引擎等存储实例的 CRUD、维度约束查询与连通性测试。
 * <p>
 * 全部端点均需 ADMIN 角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/store-instance")
@RequiredArgsConstructor
public class StoreInstanceController {

    private final StoreInstanceService storeInstanceService;

    /** 按分类查询存储实例列表（管理员）。 */
    @GetMapping
    @LoginRequired
    public Result<List<StoreInstanceVO>> list(StoreInstanceQueryVO queryVO) {
        return Result.ok(storeInstanceService.listByCategory(queryVO.getCategory()));
    }

    /** 分页查询存储实例（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<StoreInstanceVO>> page(StoreInstanceQueryVO queryVO) {
        return storeInstanceService.page(queryVO);
    }

    /** 按 ID 查询存储实例详情（管理员）。 */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<StoreInstanceVO> get(@PathVariable("id") Long id) {
        StoreInstanceVO vo = storeInstanceService.getById(id);
        if (vo == null) {
            return Result.fail("存储实例不存在");
        }
        return Result.ok(vo);
    }

    /** 查询存储实例对指定嵌入模型的向量维度约束（管理员）。 */
    @GetMapping("/{id}/dimension-constraint")
    @LoginRequired
    public Result<VectorDimensionConstraintVO> getDimensionConstraint(@PathVariable("id") Long id,
                                                                      @RequestParam("embeddingModelId") Long embeddingModelId) {
        return Result.ok(storeInstanceService.getDimensionConstraint(id, embeddingModelId));
    }

    /** 创建存储实例（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<StoreInstanceVO> create(@RequestBody @Validated StoreInstanceRequestVO vo) {
        return Result.ok(storeInstanceService.create(vo));
    }

    /** 更新存储实例（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<StoreInstanceVO> update(@PathVariable("id") Long id, @RequestBody StoreInstanceRequestVO vo) {
        return Result.ok(storeInstanceService.update(id, vo));
    }

    /** 删除存储实例（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        storeInstanceService.delete(id);
        return Result.ok(null);
    }

    /** 按入参测试存储实例连通性（管理员）。 */
    @PostMapping("/test")
    @LoginRequired
    public Result<Boolean> testConnection(@RequestBody @Validated StoreInstanceTestRequestVO req) {
        return Result.ok(storeInstanceService.testConnection(req));
    }
}
