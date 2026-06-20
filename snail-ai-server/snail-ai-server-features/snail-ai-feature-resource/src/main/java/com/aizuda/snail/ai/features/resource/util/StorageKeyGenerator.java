package com.aizuda.snail.ai.features.resource.util;

import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class StorageKeyGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String DEFAULT_BIZ_TYPE = ResourceBizTypeEnum.GENERAL.getValue().toLowerCase(Locale.ROOT);
    private static final Pattern UNSAFE_BIZ_TYPE_PATTERN = Pattern.compile("[^a-z0-9_-]");
    private static final Pattern UNSAFE_EXTENSION_PATTERN = Pattern.compile("[^a-z0-9]");

    private StorageKeyGenerator() {}

    /**
     * 生成存储键：{bizType}/{yyyy}/{MM}/{dd}/{uuid}.{ext}
     */
    public static String generate(String bizType, String originalFileName) {
        String safeBizType = sanitizeBizType(bizType);
        String ext = extractExtension(originalFileName);
        String datePath = LocalDate.now().format(DATE_FMT);
        String uuid = UUID.randomUUID().toString();
        String suffix = ext.isEmpty() ? "" : "." + ext;
        return safeBizType + "/" + datePath + "/" + uuid + suffix;
    }

    private static String sanitizeBizType(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            return DEFAULT_BIZ_TYPE;
        }
        String value = UNSAFE_BIZ_TYPE_PATTERN.matcher(bizType.trim().toLowerCase(Locale.ROOT))
                .replaceAll("-");
        value = trimSeparators(value);
        return value.isBlank() ? DEFAULT_BIZ_TYPE : value;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return UNSAFE_EXTENSION_PATTERN.matcher(ext).replaceAll("");
    }

    private static String trimSeparators(String value) {
        return value.replaceAll("^[^a-z0-9]+", "")
                .replaceAll("[^a-z0-9]+$", "");
    }
}
