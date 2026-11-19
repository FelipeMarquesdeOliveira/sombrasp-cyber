package br.com.fiap3esph.sombrasp.seguranca.painel;

import br.com.fiap3esph.sombrasp.seguranca.evento.EventoSeguranca;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta de {@code GET /risco-cidadao}: o farol, a mensagem traduzida e a trilha recente.
 * Só leva dados mascarados (o próprio guarda de resposta confere isso).
 */
public record DadosPainelRisco(
        String farol,
        String nivel,
        String titulo,
        String mensagemCidadao,
        String origemMensagem,
        String statusTraducao,
        String modeloIa,
        String protocoloPrincipal,
        int janelaMinutos,
        Indicadores indicadores,
        List<DadosEventoPainel> eventos,
        LocalDateTime atualizadoEm) {

    /** Números do topo do painel. */
    public record Indicadores(long dadosProtegidos, long vazamentosBloqueadosLog, long respostasBloqueadas,
                              long traducoesIa, long totalEventos) {
    }

    /** Uma linha da trilha de eventos. */
    public record DadosEventoPainel(String protocolo, LocalDateTime criadoEm, String tipo, String descricao,
                                    String nivel, String cor, String saida, String modulo, String metodo,
                                    String caminho, String tiposPii, String detalheTecnico, String mensagemCidadao,
                                    String origemMensagem, String statusTraducao, String modeloIa, Long latenciaIaMs) {
        public DadosEventoPainel(EventoSeguranca e) {
            this(e.getProtocolo(), e.getCriadoEm(), e.getTipo().name(), e.getTipo().descricao(), e.getNivel().name(),
                    e.getNivel().cor(), e.getSaida(), e.getModulo(), e.getMetodo(), e.getCaminho(), e.getTiposPii(),
                    e.getDetalheTecnico(), e.getMensagemCidadao(), e.getOrigemMensagem().name(),
                    e.getStatusTraducao().name(), e.getModeloIa(), e.getLatenciaIaMs());
        }
    }
}
