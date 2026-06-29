package com.aizuda.snail.ai.features.rag.util;

import com.aizuda.snail.ai.common.execption.SnailAiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 内容哈希工具：计算字节 / 文本的 SHA-256 十六进制摘要（用于内容去重）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public final class ContentHashUtil {

    private ContentHashUtil() {
    }

    /**
     * 计算字节数组的 SHA-256 十六进制摘要。
     *
     * @param data 待计算的字节数组
     * @return 64 位十六进制 SHA-256 摘要
     */
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new SnailAiException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 计算文本（UTF-8）的 SHA-256 十六进制摘要；text 为 null 时返回 null。
     *
     * @param text 待计算的文本
     * @return 64 位十六进制 SHA-256 摘要；text 为 null 时返回 null
     */
    public static String sha256Hex(String text) {
        if (text == null) {
            return null;
        }
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
