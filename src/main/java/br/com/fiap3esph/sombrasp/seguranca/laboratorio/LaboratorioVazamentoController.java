package br.com.fiap3esph.sombrasp.seguranca.laboratorio;

import br.com.fiap3esph.sombrasp.domain.cidadao.DadosFichaSaude;
import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSegurancaRepository;
import br.com.fiap3esph.sombrasp.service.CadastroCidadaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * LABORATÓRIO DE VAZAMENTOS (somente demonstração; desligue com
 * {@code sombrasp.laboratorio.habilitado=false}).
 *
 * <p>Cada endpoint reproduz, de propósito, um erro comum de programação que vazaria PII.
 * Nenhum deles tem proteção própria: quem impede o vazamento são as guardas do Taint
 * Analysis ({@code GuardaTaintLog}, {@code GuardaTaintConsole}, {@code GuardaTaintResposta}).
 * É assim que o grupo prova, no vídeo e nos testes, que as guardas funcionam.</p>
 */
@Slf4j
@RestController
@RequestMapping("/laboratorio")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sombrasp.laboratorio.habilitado", havingValue = "true", matchIfMissing = true)
@Tag(name = "Laboratório de vazamentos", description = "Erros simulados de propósito para demonstrar o Taint Analysis")
public class LaboratorioVazamentoController {
    private final CadastroCidadaoService cadastroService;
    private final EventoSegurancaRepository eventoRepository;

    /** Dados de teste enviados pelo painel (fictícios). */
    public record DadosTesteVazamento(String cpf, String senha, String condicaoSaude) {
    }

    /** Resposta das tentativas: não carrega nenhum dado pessoal. */
    public record DadosTentativa(String tentativa, String guarda, String resultado) {
    }

    @PostMapping("/vazamento-log")
    @Operation(summary = "ERRO SIMULADO: grava CPF e senha em log (barrado pelo GuardaTaintLog)")
    public ResponseEntity<DadosTentativa> vazarEmLog(@RequestBody DadosTesteVazamento dados) {
        // ERRO PROPOSITAL: log de "depuração" com CPF e senha em claro
        log.info("Depuração do cadastro: cpf={}, senha={}", dados.cpf(), dados.senha());
        return ResponseEntity.accepted().body(new DadosTentativa("gravar CPF e senha no log da aplicação",
                "GuardaTaintLog (Logback)", "Veja no painel de Risco Cidadão se a linha foi bloqueada."));
    }

    @PostMapping("/vazamento-console")
    @Operation(summary = "ERRO SIMULADO: imprime o laudo de saúde com System.out.println (barrado pelo GuardaTaintConsole)")
    public ResponseEntity<DadosTentativa> vazarNoConsole(@RequestBody DadosTesteVazamento dados) {
        // ERRO PROPOSITAL: o equivalente Java do console.log(dados_de_saude)
        System.out.println("Laudo recebido do cidadão: " + dados.condicaoSaude());
        return ResponseEntity.accepted().body(new DadosTentativa("imprimir o laudo de saúde no console",
                "GuardaTaintConsole (System.out)", "Veja no painel de Risco Cidadão se a linha foi bloqueada."));
    }

    @GetMapping("/ficha-saude/{id}")
    @Operation(summary = "ERRO SIMULADO: devolve a ficha de saúde decifrada sem exigir login (barrado pelo GuardaTaintResposta)")
    public ResponseEntity<DadosFichaSaude> vazarNaResposta(@PathVariable Long id) {
        // ERRO PROPOSITAL: endpoint sem autorização que devolve CPF e saúde decifrados
        return ResponseEntity.ok(cadastroService.fichaDecifrada(id));
    }

    @PostMapping("/reiniciar")
    @Transactional
    @Operation(summary = "Apaga os eventos de segurança (para recomeçar a demonstração)")
    public ResponseEntity<Void> reiniciar() {
        eventoRepository.deleteAllInBatch();
        return ResponseEntity.noContent().build();
    }
}
