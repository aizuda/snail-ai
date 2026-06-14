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

@RestController
@RequestMapping("/store-instance")
@RequiredArgsConstructor
public class StoreInstanceController {

    private final StoreInstanceService storeInstanceService;

    @GetMapping
    @LoginRequired
    public Result<List<StoreInstanceVO>> list(StoreInstanceQueryVO queryVO) {
        return Result.ok(storeInstanceService.listByCategory(queryVO.getCategory()));
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<StoreInstanceVO>> page(StoreInstanceQueryVO queryVO) {
        return storeInstanceService.page(queryVO);
    }

    @GetMapping("/{id}")
    @LoginRequired
    public Result<StoreInstanceVO> get(@PathVariable("id") Long id) {
        StoreInstanceVO vo = storeInstanceService.getById(id);
        if (vo == null) {
            return Result.fail("存储实例不存在");
        }
        return Result.ok(vo);
    }

    @GetMapping("/{id}/dimension-constraint")
    @LoginRequired
    public Result<VectorDimensionConstraintVO> getDimensionConstraint(@PathVariable("id") Long id,
                                                                      @RequestParam("embeddingModelId") Long embeddingModelId) {
        return Result.ok(storeInstanceService.getDimensionConstraint(id, embeddingModelId));
    }

    @PostMapping
    @LoginRequired
    public Result<StoreInstanceVO> create(@RequestBody @Validated StoreInstanceRequestVO vo) {
        return Result.ok(storeInstanceService.create(vo));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<StoreInstanceVO> update(@PathVariable("id") Long id, @RequestBody StoreInstanceRequestVO vo) {
        return Result.ok(storeInstanceService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        storeInstanceService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/test")
    @LoginRequired
    public Result<Boolean> testConnection(@RequestBody @Validated StoreInstanceTestRequestVO req) {
        return Result.ok(storeInstanceService.testConnection(req));
    }
}
