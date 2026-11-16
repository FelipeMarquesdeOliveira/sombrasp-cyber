package br.com.fiap3esph.sombrasp.domain.alerta.validacao;

import br.com.fiap3esph.sombrasp.domain.alerta.DadosEmissaoAlerta;

/**
 * Regra de emissão de alerta. Cada regra é uma classe (Strategy): o serviço recebe
 * todas as implementações por injeção de dependência e aplica uma a uma.
 */
public interface ValidadorEmissaoAlerta {
    void validar(DadosEmissaoAlerta dados);
}
