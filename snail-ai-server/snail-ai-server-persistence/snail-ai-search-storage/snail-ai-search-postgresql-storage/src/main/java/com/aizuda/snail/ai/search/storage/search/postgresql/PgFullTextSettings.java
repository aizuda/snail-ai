package com.aizuda.snail.ai.search.storage.search.postgresql;

import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import lombok.Data;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PostgreSQL 全文检索连接配置。
 *
 * @author opensnail
 * @since 2026-11-16
 */
@Data
public class PgFullTextSettings {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]*");
    private static final Set<String> SSL_MODES = Set.of(
            "disable", "allow", "prefer", "require", "verify-ca", "verify-full");

    private String host = "localhost";
    private int port = 5432;
    private String database = "snail_ai";
    private String username = "postgres";
    private String password = "";
    private String schema = "public";
    private boolean sslEnabled;
    private String sslMode = "disable";

    public void validate() {
        if (host == null || host.isBlank()) {
            throw new SearchEngineException("PostgreSQL host 不能为空");
        }
        if (port <= 0 || port > 65535) {
            throw new SearchEngineException("PostgreSQL port 必须在 1-65535 之间");
        }
        if (database == null || database.isBlank()) {
            throw new SearchEngineException("PostgreSQL database 不能为空");
        }
        if (username == null || username.isBlank()) {
            throw new SearchEngineException("PostgreSQL username 不能为空");
        }
        if (schema == null || !SQL_IDENTIFIER.matcher(schema).matches()) {
            throw new SearchEngineException("PostgreSQL schema 仅支持小写字母、数字和下划线，且不能以数字开头");
        }
        if (sslEnabled && (sslMode == null || !SSL_MODES.contains(sslMode))) {
            throw new SearchEngineException("不支持的 PostgreSQL sslMode: " + sslMode);
        }
    }

    public String resolvedSslMode() {
        return sslEnabled ? sslMode : "disable";
    }
}
