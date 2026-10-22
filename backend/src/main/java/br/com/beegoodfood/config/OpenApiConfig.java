package br.com.beegoodfood.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI beeGoodFoodOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bee Good Food API")
                .version("v1")
                .description("Consulta pública de categorias. Autenticação e gestão de pedidos serão adicionadas nas próximas etapas."));
    }
}
