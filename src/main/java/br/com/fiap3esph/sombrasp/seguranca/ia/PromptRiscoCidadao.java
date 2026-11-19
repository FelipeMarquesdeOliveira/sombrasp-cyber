package br.com.fiap3esph.sombrasp.seguranca.ia;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;
import br.com.fiap3esph.sombrasp.seguranca.evento.NivelRisco;
import br.com.fiap3esph.sombrasp.seguranca.pii.MascaradorPii;
import br.com.fiap3esph.sombrasp.seguranca.pii.TipoPii;

import java.util.stream.Collectors;

/**
 * Monta o prompt dinâmico enviado ao LLM, no formato pedido no enunciado:
 * "Traduza a falha técnica '...' para um cidadão leigo de 60 anos, em 2 frases".
 *
 * <p>O prompt também é uma saída do sistema (vai para um serviço externo), então passa pelo
 * mascarador antes de sair: a IA nunca recebe CPF, RG, senha ou dado de saúde em claro.</p>
 */
public final class PromptRiscoCidadao {
    private PromptRiscoCidadao() {
    }

    public static final String SISTEMA = """
            Você é o assistente de privacidade do SombraSP, um serviço público que avisa sobre calor extremo \
            no Centro de São Paulo. Você explica eventos de segurança para cidadãos leigos, principalmente \
            idosos e trabalhadores de rua.
            Regras:
            - Responda em português do Brasil, em exatamente 2 frases curtas (no máximo 22 palavras cada).
            - Use linguagem simples e acolhedora, como quem conversa com uma pessoa de 60 anos.
            - Não use termos técnicos (PII, taint, log, console, HTTP, API, sink, servidor, requisição, código,
              protocolo, depuração) nem copie trechos do detalhe técnico.
            - Diga o que aconteceu com os dados da pessoa e se ela precisa fazer alguma coisa.
            - Quando o sistema bloqueou ou protegeu o dado, deixe claro que os dados estão seguros; só peça
              alguma ação se ela for realmente necessária.
            - Nunca invente números, nomes, documentos ou datas. Não use emojis, aspas nem listas.
            Exemplo do tom esperado: Tentaram acessar seu documento de saúde, mas nosso sistema bloqueou a ação \
            para proteger sua privacidade. Você não precisa fazer nada.""";

    public static String usuario(EventoTecnico evento) {
        String tipos = evento.tiposPii().isEmpty() ? "dado pessoal"
                : evento.tiposPii().stream().map(TipoPii::descricao).collect(Collectors.joining(", "));
        NivelRisco nivel = evento.tipo().nivel();
        String prompt = "Traduza a falha técnica \"" + evento.detalheTecnico() + "\" para um cidadão leigo de 60 anos, "
                + "em 2 frases. Contexto: área do sistema: " + evento.modulo() + "; dado envolvido: " + tipos
                + "; o que o sistema fez: " + acao(evento) + "; nível de risco: " + nivel.titulo().toLowerCase() + ".";
        return MascaradorPii.mascararTexto(prompt);
    }

    private static String acao(EventoTecnico evento) {
        return switch (evento.tipo()) {
            case PII_DETECTADA -> "recebeu o dado e guardou de forma protegida (escondido e criptografado)";
            case DADO_MASCARADO_NA_RESPOSTA -> "mostrou o dado escondido em parte para um atendente autorizado";
            case VAZAMENTO_LOG_BLOQUEADO, VAZAMENTO_CONSOLE_BLOQUEADO ->
                    "impediu que o dado fosse anotado em um registro interno";
            case VAZAMENTO_RESPOSTA_BLOQUEADO -> "bloqueou o envio do dado para quem não tinha permissão";
        };
    }
}
