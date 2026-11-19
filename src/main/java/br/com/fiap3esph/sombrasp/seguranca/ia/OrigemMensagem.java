package br.com.fiap3esph.sombrasp.seguranca.ia;

/** De onde veio a mensagem mostrada ao cidadão. */
public enum OrigemMensagem {
    /** Gerada pelo LLM (MiniMax) a partir do evento técnico. */
    IA,
    /** Texto fixo, usado enquanto a IA responde ou se ela falhar. */
    PADRAO
}
