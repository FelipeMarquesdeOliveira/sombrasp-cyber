package br.com.fiap3esph.sombrasp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * SombraSP - backend REST da Global Solution 2026.2 (SOA e Web Services, FIAP 3ESPH).
 * Gestão de pontos de sombra/hidratação e de alertas de calor no Centro de São Paulo.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SombraSpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SombraSpApplication.class, args);
    }
}
