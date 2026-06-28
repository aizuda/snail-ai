package com.aizuda.snail.ai.persistence.openapi.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sai_openapi_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpenApiUserPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;

    private String openId;

    private Long platformUserId;

    private String externalId;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
