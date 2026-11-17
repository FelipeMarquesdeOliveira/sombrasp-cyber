package br.com.fiap3esph.sombrasp.infra.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    public static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SombraSP API")
                        .description("""
                                Backend REST da CitizenTech SombraSP (Global Solution 2026.2 - SOA e Web Services, FIAP 3ESPH).
                                Gestão de pontos de sombra e hidratação e de alertas de calor (>= 31 °C) no Centro de São Paulo.
                                Consultas são públicas (cidadão/app). Cadastrar, atualizar, excluir e emitir/encerrar alertas
                                exigem token JWT de gestor: faça POST /login e clique em Authorize.
                                Equipe: Felipe Marques de Oliveira (RM 556319) e Gabriel Barros Cisoto (RM 556309).""")
                        .version("v1")
                        .contact(new Contact().name("Equipe SombraSP - 3ESPH")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
