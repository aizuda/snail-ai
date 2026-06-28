package com.aizuda.snail.ai.admin.service.resource;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.resource.ResourceQueryVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceResponseVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceUploadRequestVO;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceAdminService {

    private final ResourceService resourceService;
    private final ResourceMapper resourceMapper;

    public ResourceResponseVO upload(MultipartFile file, ResourceUploadRequestVO request) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        String bizType = StrUtil.isNotBlank(request.getBizType())
                ? request.getBizType()
                : ResourceBizTypeEnum.GENERAL.getValue();

        try {
            ResourcePO po = resourceService.upload(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getSize(),
                    bizType,
                    request.getBizId(),
                    userId
            );
            return toResponseVO(po);
        } catch (IOException e) {
            throw new SnailAiException("Failed to read uploaded file", e);
        }
    }

    public void delete(Long id) {
        requireAuthorizedResource(id);
        resourceService.delete(id);
    }

    public PageResult<List<ResourceResponseVO>> page(ResourceQueryVO query) {
        LambdaQueryWrapper<ResourcePO> wrapper = new LambdaQueryWrapper<>();
        UserPO currentUser = UserSessionUtils.currentUserSession();

        wrapper.eq(StrUtil.isNotBlank(query.getBizType()), ResourcePO::getBizType, query.getBizType())
                .like(StrUtil.isNotBlank(query.getOriginalName()), ResourcePO::getOriginalName, query.getOriginalName())
                .eq(ObjUtil.isNotNull(query.getBizId()), ResourcePO::getBizId, query.getBizId())
                .between(ObjUtil.isNotNull(query.getStartDt()) &&  ObjUtil.isNotNull(query.getEndDt()),
                        ResourcePO::getCreateDt, query.getStartDt(), query.getEndDt());
        if (!RoleEnum.isAdmin(currentUser.getRole())) {
            wrapper.eq(ResourcePO::getCreatorId, currentUser.getId());
        }
        wrapper.orderByDesc(ResourcePO::getCreateDt);

        IPage<ResourcePO> page = resourceMapper.selectPage(
                PageDTO.of(query.getPage(), query.getSize()), wrapper);

        List<ResourceResponseVO> list = page.getRecords().stream()
                .map(this::toResponseVO)
                .toList();

        PageResult<List<ResourceResponseVO>> result = new PageResult<>();
        result.setData(list);
        result.setPage((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setTotal(page.getTotal());
        return result;
    }

    private ResourcePO requireAuthorizedResource(Long id) {
        ResourcePO resource = resourceService.getById(id);
        if (resource == null) {
            throw new SnailAiException("Resource not found: " + id);
        }
        UserPO currentUser = UserSessionUtils.currentUserSession();
        if (!RoleEnum.isAdmin(currentUser.getRole())
                && !currentUser.getId().equals(resource.getCreatorId())) {
            throw new SnailAiException("No permission to access resource: " + id);
        }
        return resource;
    }

    private ResourceResponseVO toResponseVO(ResourcePO po) {
        return ResourceResponseVO.builder()
                .id(po.getId())
                .storageKey(po.getStorageKey())
                .originalName(po.getOriginalName())
                .fileSize(po.getFileSize())
                .mimeType(po.getMimeType())
                .storageType(po.getStorageType())
                .accessUrl(po.getAccessUrl())
                .bizType(po.getBizType())
                .bizId(po.getBizId())
                .creatorId(po.getCreatorId())
                .createDt(po.getCreateDt())
                .build();
    }
}
