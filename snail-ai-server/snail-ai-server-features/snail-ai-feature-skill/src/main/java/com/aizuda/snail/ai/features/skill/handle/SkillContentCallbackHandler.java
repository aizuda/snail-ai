package com.aizuda.snail.ai.features.skill.handle;

import com.aizuda.snail.ai.common.dto.agent.SkillContentRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillFileMapper;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 回调：获取 Skill 完整内容 + 支撑文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillContentCallbackHandler implements GrpcRequestHandler {

    private final SkillMapper skillMapper;
    private final SkillFileMapper skillFileMapper;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CALLBACK_SKILL_CONTENT.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        try {
            SkillContentRequest req = JsonUtil.parseObject(request.getBody(), SkillContentRequest.class);
            if (req == null || req.getSkillId() == null) {
                return buildError("skillId is required");
            }

            SkillPO skill = skillMapper.selectById(req.getSkillId());
            if (skill == null) {
                return buildError("Skill not found: " + req.getSkillId());
            }

            // 构建响应
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("skillContent", skill.getSkillContent());
            result.put("version", skill.getVersion());

            // 加载支撑文件
            List<SkillFilePO> files = skillFileMapper.selectList(
                    new LambdaQueryWrapper<SkillFilePO>()
                            .eq(SkillFilePO::getSkillId, req.getSkillId()));

            List<Map<String, Object>> fileList = files.stream().map(f -> {
                Map<String, Object> fileMap = new LinkedHashMap<>();
                fileMap.put("filePath", f.getFilePath());
                fileMap.put("content", f.getContent());
                fileMap.put("encoding", f.getEncoding());
                return fileMap;
            }).toList();
            result.put("files", fileList);

            return GrpcSnailAiResult.newBuilder()
                    .setStatus(1).setMessage("OK")
                    .setData(JsonUtil.toJsonString(result))
                    .build();
        } catch (Exception e) {
            log.error("Callback skill content failed", e);
            return buildError(e.getMessage());
        }
    }

    private GrpcSnailAiResult buildError(String msg) {
        return GrpcSnailAiResult.newBuilder().setStatus(0).setMessage(msg).build();
    }
}
