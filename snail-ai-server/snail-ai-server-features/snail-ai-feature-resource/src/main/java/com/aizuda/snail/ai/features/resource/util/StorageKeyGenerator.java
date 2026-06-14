package com.aizuda.snail.ai.features.resource.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class StorageKeyGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private StorageKeyGenerator() {}

    /**
     * 生成存储键：{bizType}/{yyyy}/{MM}/{dd}/{uuid}.{ext}
     */
    public static String generate(String bizType, String originalFileName) {
        String ext = extractExtension(originalFileName);
        String datePath = LocalDate.now().format(DATE_FMT);
        String uuid = UUID.randomUUID().toString();
        String suffix = ext.isEmpty() ? "" : "." + ext;
        return bizType.toLowerCase() + "/" + datePath + "/" + uuid + suffix;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
