package bg.energo.phoenix.salesportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SwaggerConfig {

    private static final String API_TITLE = "Sales Portal API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION =
            "API for integration with the Sales Portal - customers, points of delivery, payment, liability, product, product contracts and nomenclatures.";

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .version(API_VERSION)
                        .description(API_DESCRIPTION))
                .addServersItem(new Server()
                        .url("/")
                        .description(API_TITLE))
                .components(new Components()
                        .addSecuritySchemes("bearer-token",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /**
     * Limit swagger/OpenAPI docs to sales-portal controllers only.
     * Without this, phoenix-core-lib controllers under {@code bg.energo.phoenix} are also scanned
     * because {@code @SpringBootApplication} is on that package.
     */
    @Bean
    public GroupedOpenApi salesPortalApi() {
        return GroupedOpenApi.builder()
                .group("sales-portal")
                .packagesToScan("bg.energo.phoenix.salesportal.controller")
                .build();
    }
}
