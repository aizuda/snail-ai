package com.aizuda.snail.ai.admin.service.model;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.model.crypto.CryptoHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模型凭证加解密组件
 *
 * <p>对 feature-model 模块的 {@link CryptoHelper}（SM4 对称加解密）做面向"模型 API Key"场景的薄封装，
 * 不重复实现算法与密钥管理，只收敛两条业务约定：
 * <ul>
 *     <li>空白输入直接返回空串，调用方无需自行判空；</li>
 *     <li>更新配置时前端不回传 Key（空白）表示"本次不修改凭证"，沿用库中已有密文，
 *     避免把空值或展示值误写回数据库。</li>
 * </ul>
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelCredentialCipher {

    private final CryptoHelper cryptoHelper;

    /**
     * 加密 API Key 明文（入库前调用）
     *
     * @param apiKey API Key 明文
     * @return 加密后的密文；空白输入返回空串
     */
    public String encrypt(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        return cryptoHelper.encrypt(apiKey);
    }

    /**
     * 解密 API Key 密文（回显 VO / 调用模型前使用）
     *
     * @param encryptedApiKey 加密态 API Key
     * @return 解密后的明文；空白输入返回空串
     */
    public String decrypt(String encryptedApiKey) {
        if (StrUtil.isBlank(encryptedApiKey)) {
            return "";
        }
        try {
            return cryptoHelper.decrypt(encryptedApiKey);
        } catch (RuntimeException e) {
            // 解密失败通常意味着加密密钥/IV 变更或密文被人为改动，原样抛出但留下现场便于排查
            log.error("API Key 解密失败，请检查 snail-ai.crypto 密钥配置是否变更: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 更新场景下解析应落库的 API Key：请求未携带新 Key（空白）时沿用已有密文，否则加密新明文。
     * 该语义集中在此处，保证所有更新入口对"空白即不修改"的处理全局一致。
     *
     * @param apiKey                  请求中的 API Key 明文（可能为空白）
     * @param existingEncryptedApiKey 数据库中已有的加密态 API Key
     * @return 应写入数据库的加密态 API Key
     */
    public String resolveUpdateApiKey(String apiKey, String existingEncryptedApiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return existingEncryptedApiKey;
        }
        return encrypt(apiKey);
    }
}
