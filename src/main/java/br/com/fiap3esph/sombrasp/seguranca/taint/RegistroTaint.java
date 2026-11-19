package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.pii.AchadoPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registro de contaminação (taint) da requisição atual.
 *
 * <p>Tudo o que o motor de parsing marcou como PII na entrada (<i>source</i>) fica guardado
 * aqui, só na memória e só durante a requisição. As guardas de saída (<i>sinks</i>: log,
 * console e resposta HTTP) perguntam a este registro se o texto que vai sair carrega algum
 * desses valores. Valores decifrados do banco também são marcados (outra fonte de PII).</p>
 *
 * <p>Usa {@link ThreadLocal}: no Spring MVC cada requisição roda em uma thread, e o
 * {@link FiltroParsingPii} limpa o registro no fim (bloco {@code finally}).</p>
 */
public final class RegistroTaint {
    private static final ThreadLocal<Contexto> ATUAL = new ThreadLocal<>();
    /** Valores muito curtos geram falso positivo (ex.: "1234" em qualquer número). */
    private static final int TAMANHO_MINIMO = 6;

    private RegistroTaint() {
    }

    /** Valor contaminado: o tipo, o texto original e, para documentos, só os dígitos. */
    record Contaminado(TipoPii tipo, String valor, String digitos) {
        @Override
        public String toString() {
            return "Contaminado[" + tipo + "]";
        }
    }

    /** Estado de uma requisição: origem (método e caminho) e valores marcados. */
    public static final class Contexto {
        private final String metodo;
        private final String caminho;
        private final List<AchadoPii> achados = new ArrayList<>();
        private final List<Contaminado> contaminados = new ArrayList<>();

        Contexto(String metodo, String caminho) {
            this.metodo = metodo;
            this.caminho = caminho;
        }

        public String metodo() {
            return metodo;
        }

        public String caminho() {
            return caminho;
        }

        public List<AchadoPii> achados() {
            return Collections.unmodifiableList(achados);
        }
    }

    public static Contexto iniciar(String metodo, String caminho) {
        Contexto contexto = new Contexto(metodo, caminho);
        ATUAL.set(contexto);
        return contexto;
    }

    public static Optional<Contexto> atual() {
        return Optional.ofNullable(ATUAL.get());
    }

    public static void encerrar() {
        ATUAL.remove();
    }

    /** Marca como contaminados os dados pessoais achados na entrada. */
    public static void registrar(List<AchadoPii> achados) {
        Contexto contexto = ATUAL.get();
        if (contexto == null) {
            return;
        }
        for (AchadoPii achado : achados) {
            contexto.achados.add(achado);
            contaminar(achado.tipo(), achado.original());
        }
    }

    /** Marca um valor como contaminado (ex.: CPF que acabou de ser decifrado do banco). */
    public static void contaminar(TipoPii tipo, String valor) {
        Contexto contexto = ATUAL.get();
        if (contexto == null || valor == null || valor.isBlank()) {
            return;
        }
        String digitos = (tipo == TipoPii.CPF || tipo == TipoPii.RG) ? valor.replaceAll("\\D", "") : null;
        contexto.contaminados.add(new Contaminado(tipo, valor.strip(), digitos));
    }

    /** Tipos de PII contaminada que aparecem dentro do texto (vazio se nenhum). */
    public static Set<TipoPii> tiposContaminadosEm(String texto) {
        Set<TipoPii> tipos = EnumSet.noneOf(TipoPii.class);
        Contexto contexto = ATUAL.get();
        if (contexto == null || texto == null || texto.isEmpty()) {
            return tipos;
        }
        String digitosDoTexto = null;
        for (Contaminado c : contexto.contaminados) {
            if (c.valor().length() >= TAMANHO_MINIMO && texto.contains(c.valor())) {
                tipos.add(c.tipo());
            } else if (c.digitos() != null && c.digitos().length() >= 8) {
                if (digitosDoTexto == null) {
                    digitosDoTexto = texto.replaceAll("\\D", "");
                }
                if (digitosDoTexto.contains(c.digitos())) {
                    tipos.add(c.tipo());
                }
            }
        }
        return tipos;
    }

    /** Troca pelos valores mascarados todos os valores contaminados que aparecem no texto. */
    public static String mascararContaminados(String texto) {
        Contexto contexto = ATUAL.get();
        if (contexto == null || texto == null) {
            return texto;
        }
        String resultado = texto;
        for (Contaminado c : contexto.contaminados) {
            if (c.valor().length() >= TAMANHO_MINIMO) {
                resultado = resultado.replace(c.valor(), MascaradorPii.mascarar(c.tipo(), c.valor()));
            }
        }
        return resultado;
    }
}
