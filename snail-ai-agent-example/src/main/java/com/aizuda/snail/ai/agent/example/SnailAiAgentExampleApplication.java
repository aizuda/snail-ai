package com.aizuda.snail.ai.agent.example;

import com.aizuda.snail.ai.agent.starter.EnableSnailAiAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Snail-AI Agent 客户端示例
 * <p>
 * 启动后自动连接 Server，注册心跳，接收 Chat 分发请求。
 *
 * <pre>
 * 使用步骤：
 * 1. 在 Server 端「应用管理」页面创建应用，获取 app-id 和 token
 * 2. 修改 application.yml 中的 server-host、app-id、token
 * 3. 启动此应用
 * 4. 在 Server 端创建 Agent，关联此应用
 * 5. 发送消息，观察日志确认 Chat 分发到此客户端执行
 * </pre>
 */
@EnableSnailAiAgent
@SpringBootApplication
public class SnailAiAgentExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnailAiAgentExampleApplication.class, args);
    }
}
