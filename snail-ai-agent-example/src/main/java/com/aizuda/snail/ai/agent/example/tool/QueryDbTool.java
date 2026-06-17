package com.aizuda.snail.ai.agent.example.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2026-05-24
 */
@Component
public class QueryDbTool {

    @Tool(name = "query_order", description = "查询订单信息")
    public String queryOrder(@ToolParam(description = "订单ID") String orderId, ToolContext toolContext) {
        Map<String, Object> context = toolContext.getContext();
        System.out.println(context);
        return """
        {
          "orderId": "%s",
          "orderStatus": "DELIVERED",
          "productName": "Apple iPhone 16 Pro 256GB",
          "quantity": 1,
          "amount": 8999.00,
          "currency": "CNY",
          "buyerName": "张三",
          "receiverName": "张三",
          "receiverPhone": "138****8888",
          "receiverAddress": "北京市朝阳区某某路100号",
          "createTime": "2026-05-24 10:30:00",
          "payTime": "2026-05-24 10:32:15",
          "deliveryTime": "2026-05-24 15:20:00",
          "finishTime": "2026-05-26 09:18:23",
          "trackingNo": "SF123456789CN",
          "logisticsCompany": "顺丰速运"
        }
        """.formatted(orderId);
    }
}
