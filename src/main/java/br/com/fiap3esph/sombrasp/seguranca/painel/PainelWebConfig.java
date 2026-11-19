package br.com.fiap3esph.sombrasp.seguranca.painel;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Endereços amigáveis do painel: {@code /} e {@code /painel} abrem {@code /painel/index.html}. */
@Configuration
public class PainelWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/painel/index.html");
        registry.addRedirectViewController("/painel", "/painel/index.html");
        registry.addRedirectViewController("/painel/", "/painel/index.html");
    }
}
