package com.aizuda.snail.ai.common.grpc.handler;

import com.aizuda.snail.ai.common.grpc.constant.HeaderConstants;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * gRPC 请求上下文封装
 * <p>
 * 统一入参对象，替代 Handler 中的裸 Map 参数。
 *
 * @author opensnail
 */
@Data
@Builder
public class GrpcHandlerRequest {

    private long reqId;
    private String uri;
    private Map<String, String> headers;
    private String body;

    public String getHeader(String key) {
        return headers != null ? headers.getOrDefault(key, "") : "";
    }

    public String getAppId() {
        return getHeader(HeaderConstants.APP_ID);
    }

    public String getToken() {
        return getHeader(HeaderConstants.TOKEN);
    }

    public String getHostId() {
        return getHeader(HeaderConstants.HOST_ID);
    }

    public String getHostIp() {
        return getHeader(HeaderConstants.HOST_IP);
    }

    public int getHostPort() {
        String port = headers != null ? headers.getOrDefault(HeaderConstants.HOST_PORT, "0") : "0";
        return Integer.parseInt(port);
    }
}
