package com.bookworm.common.config;

import com.bookworm.common.security.CurrentUserId;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata for the generated API documentation, including the JWT scheme behind the "Authorize" button.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    static {
        // The user id comes from the JWT, so it must not appear as a request parameter.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId.class);
    }

    @Bean
    OpenAPI bookWormOpenApi() {
        return new OpenAPI()
                .info(new Info().title("BookWorm API").version("1.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
