package br.com.fiap3esph.sombrasp.seguranca.painel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dados do painel de Risco Cidadão (página em {@code /painel/}). Não contém PII. */
@RestController
@RequestMapping("/risco-cidadao")
@RequiredArgsConstructor
@Tag(name = "Risco Cidadão", description = "Farol de risco e mensagens traduzidas pela IA")
public class RiscoCidadaoController {
    private final PainelRiscoService service;

    @GetMapping
    @Operation(summary = "Farol de risco (verde, amarelo, vermelho), mensagem para o cidadão e eventos recentes")
    public ResponseEntity<DadosPainelRisco> painel() {
        return ResponseEntity.ok(service.montar());
    }
}
