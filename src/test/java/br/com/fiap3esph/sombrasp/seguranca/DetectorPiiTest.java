package br.com.fiap3esph.sombrasp.seguranca;

import br.com.fiap3esph.sombrasp.seguranca.pii.AchadoPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.DetectorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.ValidadorCpf;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** PONTO 1: motor de parsing de PII (regex + nome do campo) e máscaras. */
class DetectorPiiTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    private List<AchadoPii> analisar(String json) {
        return DetectorPii.analisarJson(mapper.readTree(json));
    }

    @Test
    void exemploDoEnunciadoDocumentoComCpfViraPiiMascarada() {
        List<AchadoPii> achados = analisar("""
                {"documento": "123.456.789-00"}""");
        assertThat(achados).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(TipoPii.CPF);
            assertThat(a.caminho()).isEqualTo("$.documento");
            assertThat(a.mascarado()).isEqualTo("***.456.***-00");
        });
    }

    @Test
    void reconheceCpfRgSaudeESenhaPeloNomeDoCampo() {
        List<AchadoPii> achados = analisar("""
                {"apelido": "Dona Maria", "cpf": "12345678909", "rg": "12.345.678-9",
                 "condicaoSaude": "Hipertensão", "senha": "Calor@2026", "bairro": "Sé"}""");
        assertThat(achados).extracting(AchadoPii::tipo)
                .containsExactly(TipoPii.CPF, TipoPii.RG, TipoPii.DADO_SAUDE, TipoPii.SENHA);
        assertThat(achados).extracting(AchadoPii::mascarado)
                .containsExactly("***.456.***-09", "**.***.678-9", MascaradorPii.SAUDE, MascaradorPii.SENHA);
    }

    @Test
    void achaPiiEmObjetosAninhadosEListas() {
        List<AchadoPii> achados = analisar("""
                {"contato": {"documento": "123.456.789-09"},
                 "dependentes": [{"nome": "Ana", "documento": "529.982.247-25"}]}""");
        assertThat(achados).extracting(AchadoPii::caminho)
                .containsExactly("$.contato.documento", "$.dependentes[0].documento");
    }

    @Test
    void achaPiiEmTextoLivre() {
        List<AchadoPii> achados = analisar("""
                {"relato": "Meu CPF é 529.982.247-25 e minha senha: abc12345"}""");
        assertThat(achados).extracting(AchadoPii::tipo).containsExactlyInAnyOrder(TipoPii.CPF, TipoPii.SENHA);
    }

    @Test
    void textoLivreQueFalaDeSaudeViraDadoSensivelInteiro() {
        List<AchadoPii> achados = analisar("""
                {"observacao": "Tenho diabetes, CID E11, e tomo insulina"}""");
        assertThat(achados).extracting(AchadoPii::tipo).contains(TipoPii.DADO_SAUDE);
        assertThat(achados.getFirst().mascarado()).isEqualTo(MascaradorPii.SAUDE);
    }

    @Test
    void numeroComOnzeDigitosSoContaComoCpfSeODigitoVerificadorBater() {
        assertThat(analisar("{\"protocolo\": \"52998224725\"}")).hasSize(1);
        assertThat(analisar("{\"protocolo\": \"52998224700\"}")).isEmpty();
        assertThat(ValidadorCpf.valido("529.982.247-25")).isTrue();
        assertThat(ValidadorCpf.valido("111.111.111-11")).isFalse();
    }

    @Test
    void dadosComunsDaApiNaoSaoMarcados() {
        assertThat(analisar("""
                {"nome": "Tenda da Sé", "cidade": "São Paulo", "latitude": -23.5503,
                 "telefone": null, "mensagem": "Calor forte agora no Centro.", "token": "eyJhbGciOi"}""")).isEmpty();
    }

    @Test
    void valorJaMascaradoNaoEMarcadoDeNovo() {
        assertThat(analisar("{\"cpf\": \"***.456.***-09\", \"senha\": \"[SENHA PROTEGIDA]\"}")).isEmpty();
    }

    @Test
    void toStringDoAchadoNuncaMostraOValorOriginal() {
        AchadoPii achado = analisar("{\"senha\": \"Calor@2026\"}").getFirst();
        assertThat(achado.toString()).doesNotContain("Calor@2026").contains("[SENHA PROTEGIDA]");
    }

    @Test
    void mascararTextoTrocaDocumentosESenhaEscrita() {
        String texto = MascaradorPii.mascararTexto("cpf=123.456.789-09, rg=12.345.678-9, senha=Calor@2026");
        assertThat(texto).isEqualTo("cpf=***.456.***-09, rg=**.***.678-9, senha=[SENHA PROTEGIDA]");
        assertThat(MascaradorPii.mascararTexto(texto)).isEqualTo(texto); // idempotente
    }
}
