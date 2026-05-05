package com.aizuda.snail.ai.agent.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置
 *
 * @author opensnail
 * @date 2026-04-25
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Snail AI OpenAPI Client Demo")
                        .version("1.0.0")
                        .description("演示如何使用 Snail AI OpenAPI Client 调用服务端接口")
                        .contact(new Contact()
                                .name("OpenSnail")
                                .url("https://github.com/aizuda/snail-ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
