package br.com.fiap3esph.sombrasp.seguranca.taint;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.PonteEventosSeguranca;
import br.com.fiap3esph.sombrasp.seguranca.evento.TipoEventoSeguranca;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * PONTO 2 (Taint Analysis): <i>sink</i> de CONSOLE.
 *
 * <p>Equivalente Java do {@code console.log(senha)} citado no enunciado: substitui
 * {@code System.out} e {@code System.err} e inspeciona cada {@code print}/{@code println}/
 * {@code printf}. Se o texto carrega PII contaminada, a linha não é impressa; sai só um
 * aviso mascarado e o bloqueio vira evento de segurança.</p>
 *
 * <p>Escritas de baixo nível ({@code write}) passam direto: é por elas que o Logback imprime,
 * e as linhas de log já foram inspecionadas pelo {@link GuardaTaintLog}.</p>
 */
public class GuardaTaintConsole extends PrintStream {
    private final PrintStream original;
    private final String nome;

    public GuardaTaintConsole(PrintStream original, String nome) {
        super(original, true, StandardCharsets.UTF_8);
        this.original = original;
        this.nome = nome;
    }

    public PrintStream original() {
        return original;
    }

    @Override
    public void println(String x) {
        if (liberado(x)) {
            original.println(x);
        }
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        println(new String(x));
    }

    @Override
    public void print(String s) {
        if (liberado(s)) {
            original.print(s);
        }
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(char[] s) {
        print(new String(s));
    }

    @Override
    public PrintStream format(String format, Object... args) {
        print(String.format(format, args));
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        print(String.format(l, format, args));
        return this;
    }

    private boolean liberado(String texto) {
        InspetorSaida.Resultado resultado = InspetorSaida.inspecionar(texto);
        if (!resultado.vazou()) {
            return true;
        }
        String origem = RegistroTaint.atual().map(c -> " durante " + c.metodo() + " " + c.caminho()).orElse("");
        String detalhe = "Taint sink bloqueado: " + nome + ".println tentou imprimir " + resultado.tiposPorExtenso()
                + " no console" + origem + ". Conteúdo mascarado: " + resultado.textoMascarado();
        original.println("[TAINT] Console bloqueado: " + detalhe);
        PonteEventosSeguranca.publicar(new EventoTecnico(TipoEventoSeguranca.VAZAMENTO_CONSOLE_BLOQUEADO,
                RegistroTaint.atual().map(RegistroTaint.Contexto::metodo).orElse(null),
                RegistroTaint.atual().map(RegistroTaint.Contexto::caminho).orElse(null),
                resultado.tipos(), detalhe));
        return false;
    }
}
