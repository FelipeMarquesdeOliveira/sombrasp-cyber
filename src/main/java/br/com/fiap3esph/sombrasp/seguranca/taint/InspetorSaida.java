package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.pii.DetectorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra condicional usada por todas as guardas de saída (sinks).
 *
 * <p>Um texto "vaza" PII se: (1) contém um valor marcado na entrada desta requisição
 * ({@link RegistroTaint}) ou (2) contém um padrão inequívoco de documento ou CID-10,
 * mesmo vindo de outra fonte (banco, exceção, outra thread).</p>
 */
public final class InspetorSaida {
    private InspetorSaida() {
    }

    /** Resultado da inspeção: tipos de PII encontrados e o texto já mascarado. */
    public record Resultado(Set<TipoPii> tipos, String textoMascarado) {
        public boolean vazou() {
            return !tipos.isEmpty();
        }

        public String tiposPorExtenso() {
            return tipos.stream().map(TipoPii::name).collect(Collectors.joining(", "));
        }
    }

    public static Resultado inspecionar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return new Resultado(Collections.emptySet(), texto);
        }
        Set<TipoPii> tipos = EnumSet.noneOf(TipoPii.class);
        tipos.addAll(RegistroTaint.tiposContaminadosEm(texto));
        DetectorPii.procurarEmTexto(texto, DetectorPii.Modo.SAIDA).forEach(t -> tipos.add(t.tipo()));
        if (tipos.isEmpty()) {
            return new Resultado(tipos, texto);
        }
        String mascarado = MascaradorPii.mascararTexto(RegistroTaint.mascararContaminados(texto));
        return new Resultado(tipos, mascarado);
    }
}
