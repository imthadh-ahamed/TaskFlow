package com.taskflow.common.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI taskflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .description("""
                                REST API for TaskFlow — task and project management.

                                **Base path:** `/api/v1`

                                **Error format:** Every error response uses the stable `ErrorResponse` shape \
                                (`status`, `error`, `code`, `message`, `details`, `timestamp`, `path`). \
                                Validation errors include a `details` array with per-field messages.

                                **Authentication:** Cookie-based JWT authentication will be added in a future \
                                phase. All endpoints are currently unauthenticated.
                                """)
                        .version("v1")
                        .contact(new Contact().name("TaskFlow")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("taskflow_session")
                                .description("HTTP-only session cookie set on login. "
                                        + "JWT-based cookie auth is added in the auth phase.")));
    }
}
