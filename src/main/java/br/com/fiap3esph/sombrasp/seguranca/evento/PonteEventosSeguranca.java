package br.com.fiap3esph.sombrasp.seguranca.evento;

import java.util.function.Function;

/**
 * Ponte estática entre as guardas que não são beans do Spring (o filtro do Logback e o
 * guarda do {@code System.out}) e o {@link EventoSegurancaService}, que grava o evento e
 * pede a tradução para a IA. Enquanto o Spring não sobe, os eventos são ignorados.
 */
public final class PonteEventosSeguranca {
    private static volatile Function<EventoTecnico, String> destino;

    private PonteEventosSeguranca() {
    }

    static void conectar(Function<EventoTecnico, String> novoDestino) {
        destino = novoDestino;
    }

    static void desconectar() {
        destino = null;
    }

    /** Publica o evento e devolve o protocolo (ou {@code null} se o serviço ainda não está pronto). */
    public static String publicar(EventoTecnico evento) {
        Function<EventoTecnico, String> atual = destino;
        if (atual == null) {
            return null;
        }
        try {
            return atual.apply(evento);
        } catch (RuntimeException e) {
            return null; // registrar o evento nunca pode derrubar a requisição do cidadão
        }
    }
}
