package com.aizuda.snail.ai.admin.config;

import com.aizuda.snail.ai.features.rag.pipeline.DocumentContentResolver;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 通过资源库加载文档内容。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ResourceDocumentContentResolver implements DocumentContentResolver {

    private final ResourceService resourceService;

    @Override
    public InputStream resolve(RagDocumentPO document) {
        if (document.getResourceId() == null) {
            return null;
        }
        return resourceService.load(document.getResourceId());
    }
}
