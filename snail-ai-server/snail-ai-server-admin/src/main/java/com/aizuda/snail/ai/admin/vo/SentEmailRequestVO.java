package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送邮件请求：目标邮箱地址。
 *
 * @author opensnail
 * @since 2025-07-12
 */
@Data
public class SentEmailRequestVO {
    @NotBlank
    private String email;
}
