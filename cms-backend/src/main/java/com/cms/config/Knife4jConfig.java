package com.cms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置（config/Knife4jConfig.java）
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI cmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CMS 内容管理系统 API")
                        .description("编辑部 Editorial · 认证/文章/评论/AI 接口文档")
                        .version("v2.1"))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
