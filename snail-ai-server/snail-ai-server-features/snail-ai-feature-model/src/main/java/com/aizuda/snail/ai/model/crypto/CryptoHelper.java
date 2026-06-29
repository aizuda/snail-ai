package com.aizuda.snail.ai.model.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 加解密助手，基于国密 SM4（CBC/PKCS5Padding）对敏感信息（如模型 API Key）做对称加解密。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
public class CryptoHelper {

    private final SM4 sm4;

    /**
     * 基于配置的密钥与 IV 初始化 SM4 加解密器
     *
     * @param secretKey 十六进制编码的 SM4 密钥
     * @param iv        十六进制编码的初始化向量
     */
    public CryptoHelper(@Value("${snail-ai.crypto.secret-key}") String secretKey,
                        @Value("${snail-ai.crypto.iv}") String iv) {
        byte[] keyBytes = HexUtil.decodeHex(secretKey);
        byte[] ivBytes = HexUtil.decodeHex(iv);
        this.sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
    }

    /**
     * 加密明文为 Base64 密文，空白输入返回空串
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plaintext) {
        if (StrUtil.isBlank(plaintext)) {
            return "";
        }
        return sm4.encryptBase64(plaintext);
    }

    /**
     * 解密 Base64 密文为明文，空白输入返回空串
     *
     * @param ciphertext Base64 编码的密文
     * @return 解密后的明文
     */
    public String decrypt(String ciphertext) {
        if (StrUtil.isBlank(ciphertext)) {
            return "";
        }
        return sm4.decryptStr(ciphertext);
    }
}
