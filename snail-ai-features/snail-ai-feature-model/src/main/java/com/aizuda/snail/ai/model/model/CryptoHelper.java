package com.aizuda.snail.ai.model.model;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CryptoHelper {

    private final SM4 sm4;

    public CryptoHelper(@Value("${snail-ai.crypto.secret-key}") String secretKey,
                        @Value("${snail-ai.crypto.iv}") String iv) {
        byte[] keyBytes = HexUtil.decodeHex(secretKey);
        byte[] ivBytes = HexUtil.decodeHex(iv);
        this.sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
    }

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return "";
        }
        return sm4.encryptBase64(plaintext);
    }

    public String decrypt(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return "";
        }
        return sm4.decryptStr(ciphertext);
    }
}
