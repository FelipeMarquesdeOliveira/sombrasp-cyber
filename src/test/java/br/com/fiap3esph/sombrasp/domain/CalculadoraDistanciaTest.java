package br.com.fiap3esph.sombrasp.domain;

import br.com.fiap3esph.sombrasp.domain.ponto.CalculadoraDistancia;
import br.com.fiap3esph.sombrasp.domain.ponto.TipoPonto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraDistanciaTest {

    @Test
    void mesmaCoordenadaTemDistanciaZero() {
        assertEquals(0.0, CalculadoraDistancia.haversineKm(-23.5503, -46.6339, -23.5503, -46.6339), 1e-9);
    }

    @Test
    void praçaDaSeAtePraçaDaRepublicaFicaPertoDe1Km() {
        double km = CalculadoraDistancia.haversineKm(-23.5503, -46.6339, -23.5435, -46.6426);
        assertTrue(km > 1.0 && km < 1.3, "distância calculada: " + km);
    }

    @Test
    void distanciaESimetrica() {
        double ida = CalculadoraDistancia.haversineKm(-23.5503, -46.6339, -23.5457, -46.6321);
        double volta = CalculadoraDistancia.haversineKm(-23.5457, -46.6321, -23.5503, -46.6339);
        assertEquals(ida, volta, 1e-9);
    }

    @Test
    void pontoDeSombraEHidratacaoAtendeQuemProcuraQualquerUmDosDois() {
        assertTrue(TipoPonto.SOMBRA_E_HIDRATACAO.atende(TipoPonto.SOMBRA));
        assertTrue(TipoPonto.SOMBRA_E_HIDRATACAO.atende(TipoPonto.HIDRATACAO));
        assertTrue(TipoPonto.SOMBRA.atende(null));
        assertFalse(TipoPonto.SOMBRA.atende(TipoPonto.HIDRATACAO));
    }
}
