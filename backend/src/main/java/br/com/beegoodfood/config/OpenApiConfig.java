package br.com.beegoodfood.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI beeGoodFoodOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bee Good Food API")
                .version("v1")
                .description("Bee Good Food: ofertas de excedentes para retirada no estabelecimento. Cadastro adulto e JWT, "
                        + "estabelecimentos com moderação, catálogo, busca pública, pedidos idempotentes e indicadores. "
                        + "Pagamento apenas declarado; datas em UTC, apresentadas no fuso do estabelecimento."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
