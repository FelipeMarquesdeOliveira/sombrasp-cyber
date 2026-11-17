package br.com.fiap3esph.sombrasp.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Relógio único da aplicação, no fuso de São Paulo (permite fixar a data nos testes). */
@Configuration
public class RelogioConfig {
    @Bean
    public Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
