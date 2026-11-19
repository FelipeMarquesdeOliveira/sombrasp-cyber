package br.com.fiap3esph.sombrasp.seguranca.evento;

/**
 * Eventos técnicos de segurança que alimentam o painel de Risco Cidadão.
 *
 * <ul>
 *   <li>Dado protegido na entrada ou mascarado para quem tem permissão: risco <b>baixo</b> (verde).</li>
 *   <li>Código interno tentou gravar PII em log ou console e foi barrado: <b>atenção</b> (amarelo).</li>
 *   <li>Resposta sem login tentou levar PII para fora e foi barrada: risco <b>alto</b> (vermelho).</li>
 * </ul>
 */
public enum TipoEventoSeguranca {
    PII_DETECTADA(NivelRisco.BAIXO, "Dado pessoal protegido na entrada", "ENTRADA"),
    DADO_MASCARADO_NA_RESPOSTA(NivelRisco.BAIXO, "Dado mascarado para usuário autorizado", "HTTP"),
    VAZAMENTO_LOG_BLOQUEADO(NivelRisco.MEDIO, "Vazamento em log bloqueado", "LOG"),
    VAZAMENTO_CONSOLE_BLOQUEADO(NivelRisco.MEDIO, "Vazamento no console bloqueado", "CONSOLE"),
    VAZAMENTO_RESPOSTA_BLOQUEADO(NivelRisco.ALTO, "Resposta sem login bloqueada", "HTTP");

    private final NivelRisco nivel;
    private final String descricao;
    private final String saida;

    TipoEventoSeguranca(NivelRisco nivel, String descricao, String saida) {
        this.nivel = nivel;
        this.descricao = descricao;
        this.saida = saida;
    }

    public NivelRisco nivel() {
        return nivel;
    }

    public String descricao() {
        return descricao;
    }

    /** Onde o dado estava: ENTRADA (source) ou a saída vigiada (sink): LOG, CONSOLE, HTTP. */
    public String saida() {
        return saida;
    }
}
