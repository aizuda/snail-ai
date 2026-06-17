package com.aizuda.snail.ai.persistence.enums;

import java.util.Arrays;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.env.Environment;

/**
 * 业务库数据库类型。
 *
 * @author suiyaner
 * @date 2026-06-17
 */
@Getter
@AllArgsConstructor
public enum BizDbTypeEnum {

    MYSQL(DbType.MYSQL),
    POSTGRESQL(DbType.POSTGRE_SQL);

    private static final String[] DATABASE_TYPE_PROPERTIES = {
            "spring.datasource.url",
            "spring.datasource.hikari.jdbc-url",
            "spring.datasource.jdbc-url",
            "spring.datasource.driver-class-name",
            "spring.datasource.hikari.driver-class-name",
            "spring.datasource.driverClassName"
    };

    private final DbType dbType;

    public String getDb() {
        return dbType.getDb();
    }

    public static BizDbTypeEnum from(Environment environment) {
        for (String property : DATABASE_TYPE_PROPERTIES) {
            String value = environment.getProperty(property);
            if (value != null) {
                BizDbTypeEnum dbType = modeOf(value);
                if (dbType != null) {
                    return dbType;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported or missing business database url or driver class name");
    }

    public static BizDbTypeEnum modeOf(String db) {
        if (db == null) {
            return null;
        }

        String lowerDb = db.toLowerCase();
        return Arrays.stream(values())
                .filter(dbType -> lowerDb.contains(dbType.getDb()))
                .findFirst()
                .orElse(null);
    }
}
