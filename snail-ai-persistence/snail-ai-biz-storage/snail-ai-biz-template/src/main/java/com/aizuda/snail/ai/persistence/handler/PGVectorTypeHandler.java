package com.aizuda.snail.ai.persistence.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PostgreSQL vector column type handler for MyBatis.
 */
public class PGVectorTypeHandler extends BaseTypeHandler<List<Double>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> parameter, JdbcType jdbcType) throws SQLException {
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public List<Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    private List<Double> parseVector(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        raw = raw.replace("[", "").replace("]", "").trim();
        if (raw.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = raw.split(",");
        List<Double> result = new ArrayList<>();
        for (String part : parts) {
            result.add(Double.parseDouble(part.trim()));
        }
        return result;
    }
}
