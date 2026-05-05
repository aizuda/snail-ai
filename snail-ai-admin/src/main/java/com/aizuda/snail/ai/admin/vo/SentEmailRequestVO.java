package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2025-07-12
 */
@Data
public class SentEmailRequestVO {
    @NotBlank
    private String email;
}
