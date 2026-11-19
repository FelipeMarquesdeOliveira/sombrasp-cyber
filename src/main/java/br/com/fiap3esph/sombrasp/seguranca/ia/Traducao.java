package br.com.fiap3esph.sombrasp.seguranca.ia;

/** Resultado de uma tradução (da IA ou o texto padrão, se a IA falhou). */
public record Traducao(String mensagem, OrigemMensagem origem, StatusTraducao status, String modelo, Long latenciaMs) {

    public static Traducao daIa(String mensagem, String modelo, long latenciaMs) {
        return new Traducao(mensagem, OrigemMensagem.IA, StatusTraducao.CONCLUIDA, modelo, latenciaMs);
    }

    public static Traducao padrao(String mensagem, StatusTraducao status, String modelo, Long latenciaMs) {
        return new Traducao(mensagem, OrigemMensagem.PADRAO, status, modelo, latenciaMs);
    }
}
