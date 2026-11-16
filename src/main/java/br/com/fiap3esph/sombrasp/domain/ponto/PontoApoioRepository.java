package br.com.fiap3esph.sombrasp.domain.ponto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PontoApoioRepository extends JpaRepository<PontoApoio, Long> {

    @Query("""
            select p from PontoApoio p
            where p.ativo = true
            and (:tipo is null or p.tipo = :tipo)
            and (:bairro is null or lower(p.endereco.bairro) = lower(:bairro))
            and (:acessivel is null or p.acessivel = :acessivel)
            """)
    Page<PontoApoio> buscarAtivos(TipoPonto tipo, String bairro, Boolean acessivel, Pageable paginacao);

    @Query("select p from PontoApoio p join fetch p.parceiro where p.id = :id and p.ativo = true")
    Optional<PontoApoio> buscarAtivoPorId(Long id);

    List<PontoApoio> findAllByAtivoTrue();

    long countByParceiroIdAndAtivoTrue(Long parceiroId);
}
