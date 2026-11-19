package br.com.fiap3esph.sombrasp.seguranca.ia;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;

/**
 * Mensagens fixas para o cidadão. Aparecem enquanto a IA ainda não respondeu e substituem a
 * tradução se o LLM estiver fora do ar ou sem chave (o painel mostra a origem "padrão").
 */
public final class MensagensPadrao {
    private MensagensPadrao() {
    }

    public static String para(EventoTecnico evento) {
        boolean saude = evento.tiposPii().contains(TipoPii.DADO_SAUDE);
        return switch (evento.tipo()) {
            case PII_DETECTADA -> "Recebemos seus dados e guardamos tudo de forma protegida. "
                    + "Ninguém consegue ver seu documento ou sua senha por completo.";
            case DADO_MASCARADO_NA_RESPOSTA -> "Um atendente autorizado consultou seu cadastro. "
                    + "Ele só viu seus documentos escondidos em parte.";
            case VAZAMENTO_LOG_BLOQUEADO, VAZAMENTO_CONSOLE_BLOQUEADO -> (saude
                    ? "O sistema tentou anotar uma informação da sua saúde em um registro interno, mas nós impedimos. "
                    : "O sistema tentou anotar um dado pessoal seu em um registro interno, mas nós impedimos. ")
                    + "Seus dados continuam protegidos e você não precisa fazer nada.";
            case VAZAMENTO_RESPOSTA_BLOQUEADO -> (saude
                    ? "Tentaram acessar seu documento de saúde, mas nosso sistema bloqueou a ação para proteger sua privacidade. "
                    : "Tentaram ver seus dados pessoais sem permissão, mas nosso sistema bloqueou a ação. ")
                    + "Você não precisa fazer nada.";
        };
    }
}
