package br.com.fiap3esph.sombrasp.domain;

import br.com.fiap3esph.sombrasp.domain.alerta.NivelAlerta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NivelAlertaTest {

    @ParameterizedTest
    @CsvSource({"31.0, ALERTA", "33.5, ALERTA", "34.9, ALERTA", "35.0, EMERGENCIA", "40.2, EMERGENCIA"})
    void temperaturaAPartirDe31GeraONivelCorreto(String temperatura, NivelAlerta esperado) {
        assertEquals(esperado, NivelAlerta.paraTemperatura(new BigDecimal(temperatura)).orElseThrow());
    }

    @ParameterizedTest
    @CsvSource({"30.9", "25.0", "-3.0"})
    void temperaturaAbaixoDe31NaoGeraAlerta(String temperatura) {
        assertTrue(NivelAlerta.paraTemperatura(new BigDecimal(temperatura)).isEmpty());
    }

    @Test
    void temperaturaNulaNaoGeraAlerta() {
        assertTrue(NivelAlerta.paraTemperatura(null).isEmpty());
    }

    @Test
    void cadaNivelTemMensagemPadraoParaOCidadao() {
        for (NivelAlerta nivel : NivelAlerta.values()) {
            assertFalse(nivel.getMensagemPadrao().isBlank());
        }
    }
}
