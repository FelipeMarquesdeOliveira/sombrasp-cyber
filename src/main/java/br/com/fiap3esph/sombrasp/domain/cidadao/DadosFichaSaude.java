package br.com.fiap3esph.sombrasp.domain.cidadao;

/**
 * Ficha com os dados decifrados. Só existe para o cenário de demonstração do laboratório:
 * se alguém devolver este objeto numa resposta, o guarda de resposta bloqueia (sem login)
 * ou mascara (gestor).
 */
public record DadosFichaSaude(Long id, String apelido, String cpf, String rg, String condicaoSaude) {
    @Override
    public String toString() {
        return "DadosFichaSaude[id=" + id + ", dados pessoais omitidos]";
    }
}
