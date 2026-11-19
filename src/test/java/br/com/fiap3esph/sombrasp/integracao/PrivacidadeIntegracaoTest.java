package br.com.fiap3esph.sombrasp.integracao;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSegurancaRepository;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.laboratorio.LaboratorioVazamentoController;
import br.com.fiap3esph.sombrasp.seguranca.taint.GuardaTaintConsole;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo completo da camada de Cybersecurity com a aplicação inteira no ar (H2 + Flyway):
 * entrada (parsing de PII), gravação protegida, guardas de saída e painel de Risco Cidadão.
 */
@ExtendWith(OutputCaptureExtension.class)
class PrivacidadeIntegracaoTest extends IntegrationTest {
    private static final String CPF = "123.456.789-09";
    private static final String RG = "12.345.678-9";
    private static final String SENHA = "Calor@2026";
    private static final String SAUDE = "Hipertensão (CID I10), usa diurético";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EventoSegurancaRepository eventos;

    @BeforeEach
    void limparEventos() {
        eventos.deleteAll();
    }

    private Long cadastrar() throws Exception {
        String json = mvc.perform(post("/cidadaos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"apelido": "Dona Maria (fictícia)", "bairro": "Sé", "cpf": "%s", "rg": "%s",
                         "condicaoSaude": "%s", "senha": "%s", "aceitaAlertas": true}""".formatted(CPF, RG, SAUDE, SENHA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void cadastroDevolveSoMascarasEGravaTudoProtegidoNoBanco() throws Exception {
        mvc.perform(post("/cidadaos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"apelido": "Seu João (fictício)", "bairro": "República", "cpf": "529.982.247-25",
                         "rg": "%s", "condicaoSaude": "%s", "senha": "%s"}""".formatted(RG, SAUDE, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***.982.***-25"))
                .andExpect(jsonPath("$.rg").value("**.***.678-9"))
                .andExpect(jsonPath("$.condicaoSaude").value("[DADO DE SAÚDE PROTEGIDO]"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(content().string(not(containsString("529.982.247-25"))));

        Map<String, Object> linha = jdbc.queryForMap("select * from cidadaos where apelido = 'Seu João (fictício)'");
        String tudo = linha.values().toString();
        assertThat(tudo).doesNotContain("529.982.247-25", "52998224725", RG, SAUDE, SENHA);
        assertThat((String) linha.get("cpf_mascarado")).isEqualTo("***.982.***-25");
        assertThat((String) linha.get("cpf_cifrado")).startsWith("v1:");
        assertThat((String) linha.get("condicao_saude_cifrada")).startsWith("v1:");
        assertThat((String) linha.get("senha_hash")).startsWith("$2a$");

        assertThat(eventos.findAll()).extracting(EventoSeguranca::getTipo)
                .containsExactly(TipoEventoSeguranca.PII_DETECTADA);
        assertThat(eventos.findAll().getFirst().getDetalheTecnico())
                .contains("CPF em $.cpf (***.982.***-25)").doesNotContain("529.982.247-25");
    }

    @Test
    void cpfRepetidoOuInvalidoEhRecusadoSemEcoarODocumento() throws Exception {
        cadastrar();
        mvc.perform(post("/cidadaos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"apelido": "Outra", "bairro": "Sé", "cpf": "%s", "senha": "OutraSenha1"}""".formatted(CPF)))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString(CPF))));
        mvc.perform(post("/cidadaos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"apelido": "Outra", "bairro": "Sé", "cpf": "123.456.789-00", "senha": "OutraSenha1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].mensagem").value("CPF inválido"))
                .andExpect(content().string(not(containsString("123.456.789-00"))));
    }

    @Test
    void logComCpfESenhaEhBloqueadoEViraEventoAmarelo(CapturedOutput saida) throws Exception {
        // nos testes o log da aplicação fica em WARN; o erro simulado usa INFO, então liga o INFO aqui
        Logger logger = (Logger) LoggerFactory.getLogger(LaboratorioVazamentoController.class);
        Level nivelAnterior = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            mvc.perform(post("/laboratorio/vazamento-log").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cpf\": \"%s\", \"senha\": \"%s\"}".formatted(CPF, SENHA)))
                    .andExpect(status().isAccepted());
        } finally {
            logger.setLevel(nivelAnterior);
        }

        assertThat(saida.getOut()).doesNotContain(CPF).doesNotContain(SENHA)
                .contains("[TAINT] Log bloqueado").contains("cpf=***.456.***-09, senha=[SENHA PROTEGIDA]");
        assertThat(eventos.findAll()).extracting(EventoSeguranca::getTipo)
                .contains(TipoEventoSeguranca.VAZAMENTO_LOG_BLOQUEADO);
        mvc.perform(get("/risco-cidadao")).andExpect(jsonPath("$.farol").value("AMARELO"));
    }

    @Test
    void consoleComLaudoEhBloqueado(CapturedOutput saida) throws Exception {
        // a captura de saída do teste substitui o System.out; o guarda é religado por cima dela
        PrintStream antes = System.out;
        System.setOut(new GuardaTaintConsole(antes, "System.out"));
        try {
            mvc.perform(post("/laboratorio/vazamento-console").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"condicaoSaude\": \"%s\"}".formatted(SAUDE)))
                    .andExpect(status().isAccepted());
        } finally {
            System.setOut(antes);
        }
        assertThat(saida.getOut()).doesNotContain(SAUDE).contains("[TAINT] Console bloqueado");

        List<EventoSeguranca> lista = eventos.findAll();
        assertThat(lista).extracting(EventoSeguranca::getTipo).contains(TipoEventoSeguranca.VAZAMENTO_CONSOLE_BLOQUEADO);
        assertThat(lista).allSatisfy(e -> assertThat(e.getDetalheTecnico()).doesNotContain(SAUDE));
        assertThat(lista).filteredOn(e -> e.getTipo() == TipoEventoSeguranca.VAZAMENTO_CONSOLE_BLOQUEADO)
                .singleElement().extracting(EventoSeguranca::getModulo).isEqualTo("Saúde");
    }

    @Test
    void respostaSemLoginComPiiEhTrocadaPor403EFarolFicaVermelho() throws Exception {
        Long id = cadastrar();
        eventos.deleteAll();

        mvc.perform(get("/laboratorio/ficha-saude/" + id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Resposta bloqueada"))
                .andExpect(jsonPath("$.protocolo").isNotEmpty())
                .andExpect(content().string(not(containsString(CPF))))
                .andExpect(content().string(not(containsString("Hipertensão"))));

        mvc.perform(get("/risco-cidadao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farol").value("VERMELHO"))
                .andExpect(jsonPath("$.mensagemCidadao").value(containsString("documento de saúde")))
                .andExpect(jsonPath("$.indicadores.respostasBloqueadas").value(1))
                .andExpect(jsonPath("$.eventos[0].modulo").value("Saúde"));
    }

    @Test
    void gestorAutenticadoRecebeAFichaMascarada() throws Exception {
        Long id = cadastrar();
        mvc.perform(get("/laboratorio/ficha-saude/" + id).header("Authorization", bearerGestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("***.456.***-09"))
                .andExpect(jsonPath("$.condicaoSaude").value("[DADO DE SAÚDE PROTEGIDO]"))
                .andExpect(content().string(not(containsString(CPF))));
        assertThat(eventos.findAll()).extracting(EventoSeguranca::getTipo)
                .contains(TipoEventoSeguranca.DADO_MASCARADO_NA_RESPOSTA);
    }

    @Test
    void painelSemEventosFicaVerde() throws Exception {
        mvc.perform(get("/risco-cidadao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farol").value("VERDE"))
                .andExpect(jsonPath("$.eventos.length()").value(0));
        mvc.perform(get("/painel/index.html")).andExpect(status().isOk());
    }

    @Test
    void cpfNaUrlTambemEhMarcadoNaEntrada() throws Exception {
        mvc.perform(get("/pontos").param("cpf", CPF)).andExpect(status().isOk());
        assertThat(eventos.findAll()).extracting(EventoSeguranca::getTipo)
                .containsExactly(TipoEventoSeguranca.PII_DETECTADA);
    }
}
