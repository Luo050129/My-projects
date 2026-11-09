package com.luoxianjun.community.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 文档元数据。生产环境通过 springdoc.api-docs.enabled=false 关闭，
 * 这里只定义「文档长什么样」，不控制是否暴露。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zhiduOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("知渡 API")
                        .version("1.0.0")
                        .description("内容 / 知识社区接口。除 /auth/** 与内容读接口外，"
                                + "需在 Authorization 头携带 Bearer JWT。"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
