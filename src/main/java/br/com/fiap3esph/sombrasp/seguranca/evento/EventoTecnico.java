package br.com.fiap3esph.sombrasp.seguranca.evento;

import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evento técnico capturado pelas guardas, antes de ser gravado e traduzido pela IA.
 * O detalhe técnico passa de novo pelo mascarador: nunca carrega PII em claro.
 */
public record EventoTecnico(TipoEventoSeguranca tipo, String metodo, String caminho,
                            Set<TipoPii> tiposPii, String detalheTecnico) {

    public EventoTecnico {
        tiposPii = tiposPii == null || tiposPii.isEmpty() ? EnumSet.noneOf(TipoPii.class) : EnumSet.copyOf(tiposPii);
        detalheTecnico = MascaradorPii.mascararTexto(detalheTecnico);
    }

    /** "CPF, SENHA" (para gravar e mostrar). */
    public String tiposComoTexto() {
        return tiposPii.stream().map(TipoPii::name).collect(Collectors.joining(","));
    }

    /** Área do sistema afetada, em linguagem simples (vai para o prompt e para o painel). */
    public String modulo() {
        if (caminho != null && caminho.startsWith("/cidadaos")) {
            return "Cadastro do cidadão";
        }
        if (tiposPii.contains(TipoPii.DADO_SAUDE)) {
            return "Saúde";
        }
        if (caminho == null) {
            return "Sistema";
        }
        if (caminho.startsWith("/login")) {
            return "Acesso ao sistema";
        }
        if (caminho.startsWith("/laboratorio")) {
            return "Cadastro do cidadão";
        }
        return "Sistema";
    }
}
