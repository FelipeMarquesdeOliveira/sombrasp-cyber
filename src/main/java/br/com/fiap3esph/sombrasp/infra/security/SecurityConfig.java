package br.com.fiap3esph.sombrasp.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso:
 *  - públicas (cidadão/app): GET de pontos, alertas e parceiros, health-check, login e Swagger;
 *  - Cybersecurity: POST /cidadaos (inscrição pelo app), painel /painel e /risco-cidadao (sem PII)
 *    e /laboratorio (erros simulados de propósito; desligável por propriedade);
 *  - demais operações (cadastrar, atualizar, excluir, emitir/encerrar alerta): perfil GESTOR com JWT.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityFilter securityFilter;
    private final RestSecurityHandler restSecurityHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HttpMethod.POST, "/login").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/health-check", "/pontos/**", "/alertas/**", "/parceiros/**")
                            .permitAll();
                    // Cybersecurity: inscrição do cidadão (app), painel de Risco Cidadão e laboratório de demonstração
                    req.requestMatchers(HttpMethod.POST, "/cidadaos").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/", "/painel", "/painel/**", "/risco-cidadao/**", "/favicon.ico")
                            .permitAll();
                    req.requestMatchers("/laboratorio/**").permitAll();
                    req.requestMatchers("/error").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    req.anyRequest().hasRole("GESTOR");
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restSecurityHandler)
                        .accessDeniedHandler(restSecurityHandler))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
