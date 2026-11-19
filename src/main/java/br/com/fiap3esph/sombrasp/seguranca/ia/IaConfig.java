package br.com.fiap3esph.sombrasp.seguranca.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Cliente HTTP do LLM: URL, chave (vinda do .env) e tempos-limite. */
@Configuration
public class IaConfig {

    @Bean
    public RestClient restClientMiniMax(@Value("${sombrasp.ia.minimax.url}") String url,
                                        @Value("${sombrasp.ia.minimax.chave:}") String chave,
                                        @Value("${sombrasp.ia.timeout-segundos:25}") int timeoutSegundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + chave)
                .requestFactory(fabrica)
                .build();
    }
}
