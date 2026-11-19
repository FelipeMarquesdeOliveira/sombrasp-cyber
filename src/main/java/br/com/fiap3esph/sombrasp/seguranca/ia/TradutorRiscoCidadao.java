package br.com.fiap3esph.sombrasp.seguranca.ia;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoTecnico;

/** Traduz um evento técnico de segurança para uma mensagem que o cidadão entende. */
public interface TradutorRiscoCidadao {

    /** {@code false} quando não há chave de API ou a IA foi desligada na configuração. */
    boolean habilitado();

    /** Nunca lança exceção: se a IA falhar, devolve a mensagem padrão com status FALHOU. */
    Traducao traduzir(EventoTecnico evento);
}
