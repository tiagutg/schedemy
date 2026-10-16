package br.edu.unisales.schedemy.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

// Metadados exibidos na interface do Swagger UI/OpenAPI.

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Schedemy API",
                version = "1.0.0",
                description = "API REST do Schedemy - Sistema de Agendamento de Reunioes no AVA. " +
                        "Projeto academico desenvolvido para a disciplina de Projeto de Website-Back End.",
                contact = @Contact(name = "Equipe Schedemy"),
                license = @License(name = "Uso academico")
        ),
        servers = {
                @Server(url = "/", description = "Servidor local")
        }
)
@SecurityScheme(
        name = "basicAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "basic",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}