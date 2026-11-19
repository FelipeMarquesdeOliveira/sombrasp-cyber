package br.com.fiap3esph.sombrasp.seguranca;

import br.com.fiap3esph.sombrasp.seguranca.pii.CifradorPii;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Criptografia AES-256-GCM e índice HMAC dos dados pessoais. */
class CifradorPiiTest {
    private final CifradorPii cifrador = new CifradorPii(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void cifraEDecifraSemPerderNada() {
        String cifrado = cifrador.cifrar("Hipertensão (CID I10)");
        assertThat(cifrado).startsWith("v1:").doesNotContain("Hipertensão");
        assertThat(cifrador.decifrar(cifrado)).isEqualTo("Hipertensão (CID I10)");
    }

    @Test
    void mesmoValorGeraCifrasDiferentes() {
        assertThat(cifrador.cifrar("12345678909")).isNotEqualTo(cifrador.cifrar("12345678909"));
    }

    @Test
    void indiceEDeterministicoENaoRevelaOValor() {
        assertThat(cifrador.indice("12345678909")).isEqualTo(cifrador.indice("12345678909"))
                .hasSize(64).doesNotContain("12345678909");
    }

    @Test
    void cifraAdulteradaEDetectada() {
        String cifrado = cifrador.cifrar("12345678909");
        char[] c = cifrado.toCharArray();
        c[10] = c[10] == 'A' ? 'B' : 'A';
        assertThatThrownBy(() -> cifrador.decifrar(new String(c))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chaveCurtaEhRecusada() {
        assertThatThrownBy(() -> new CifradorPii(Base64.getEncoder().encodeToString(new byte[8])))
                .isInstanceOf(IllegalStateException.class);
    }
}
