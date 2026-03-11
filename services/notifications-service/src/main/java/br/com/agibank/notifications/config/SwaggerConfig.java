package br.com.agibank.notifications.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notifications Service API")
                        .version("1.0.0")
                        .description("API para envio de notificações em tempo real - Sistema Agibank")
                        .contact(new Contact()
                                .name("Agibank")
                                .email("dev@agibank.com.br")));
    }
}
