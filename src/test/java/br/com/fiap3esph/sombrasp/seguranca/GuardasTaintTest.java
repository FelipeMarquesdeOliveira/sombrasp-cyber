package br.com.fiap3esph.sombrasp.seguranca;

import br.com.fiap3esph.sombrasp.seguranca.pii.DetectorPii;
import br.com.fiap3esph.sombrasp.seguranca.taint.GuardaTaintConsole;
import br.com.fiap3esph.sombrasp.seguranca.taint.GuardaTaintLog;
import br.com.fiap3esph.sombrasp.seguranca.taint.RegistroTaint;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** PONTO 2: as guardas de saída (sinks) de log e de console. */
class GuardasTaintTest {
    private LoggerContext contexto;
    private GuardaTaintLog guarda;
    private Logger logger;
    private ListAppender<ILoggingEvent> gravado;

    @BeforeEach
    void preparar() {
        contexto = (LoggerContext) LoggerFactory.getILoggerFactory();
        guarda = new GuardaTaintLog();
        guarda.start();
        contexto.addTurboFilter(guarda);
        logger = contexto.getLogger("teste.taint");
        gravado = new ListAppender<>();
        gravado.start();
        logger.addAppender(gravado);
        RegistroTaint.iniciar("POST", "/cidadaos");
        // simula o que o FiltroParsingPii faz na entrada
        RegistroTaint.registrar(DetectorPii.analisarCampo("$.senha", "senha", "Calor@2026"));
        RegistroTaint.registrar(DetectorPii.analisarCampo("$.condicaoSaude", "condicaoSaude", "Hipertensão grave"));
    }

    @AfterEach
    void limpar() {
        RegistroTaint.encerrar();
        logger.detachAppender(gravado);
        contexto.getTurboFilterList().remove(guarda);
    }

    private String tudoQueFoiGravado() {
        return String.join("\n", gravado.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    @Test
    void logComSenhaContaminadaEhDescartadoEViraAvisoMascarado() {
        logger.info("Usuário entrou com senha {}", "Calor@2026");
        assertThat(tudoQueFoiGravado())
                .doesNotContain("Calor@2026")
                .contains("[TAINT] Log bloqueado")
                .contains("SENHA")
                .contains("[SENHA PROTEGIDA]");
    }

    @Test
    void logComDadoDeSaudeContaminadoEhDescartado() {
        logger.warn("Laudo: Hipertensão grave");
        assertThat(tudoQueFoiGravado()).doesNotContain("Hipertensão grave").contains("DADO_SAUDE");
    }

    @Test
    void cpfFormatadoEhBarradoMesmoSemTerSidoMarcadoNaEntrada() {
        logger.error("Falha ao gravar o registro 529.982.247-25");
        assertThat(tudoQueFoiGravado()).doesNotContain("529.982.247-25").contains("***.982.***-25");
    }

    @Test
    void cpfDentroDeUmaExcecaoTambemEhBarrado() {
        logger.error("Erro inesperado", new IllegalStateException("Duplicate entry '123.456.789-09'"));
        assertThat(gravado.list).noneMatch(e -> e.getThrowableProxy() != null);
        assertThat(tudoQueFoiGravado()).contains("[TAINT] Log bloqueado").doesNotContain("123.456.789-09");
    }

    @Test
    void logSemDadoPessoalPassaNormalmente() {
        logger.info("Cidadão {} cadastrado: cpf={}", 1, "***.456.***-09");
        assertThat(gravado.list).singleElement()
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("Cidadão 1 cadastrado: cpf=***.456.***-09");
    }

    @Test
    void consoleNaoImprimeDadoContaminado() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PrintStream console = new GuardaTaintConsole(new PrintStream(saida, true, StandardCharsets.UTF_8), "System.out");
        console.println("Laudo recebido: Hipertensão grave");
        console.printf("senha=%s%n", "Calor@2026");
        console.println("Ponto de água a 110 m");
        String impresso = saida.toString(StandardCharsets.UTF_8);
        assertThat(impresso)
                .doesNotContain("Hipertensão grave")
                .doesNotContain("Calor@2026")
                .contains("[TAINT] Console bloqueado")
                .contains("Ponto de água a 110 m");
    }
}
