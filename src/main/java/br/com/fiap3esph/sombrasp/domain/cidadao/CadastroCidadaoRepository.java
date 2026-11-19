package br.com.fiap3esph.sombrasp.domain.cidadao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CadastroCidadaoRepository extends JpaRepository<CadastroCidadao, Long> {

    boolean existsByCpfIndice(String cpfIndice);
}
