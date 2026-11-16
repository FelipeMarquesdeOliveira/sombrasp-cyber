package br.com.fiap3esph.sombrasp.domain.parceiro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {
    Page<Parceiro> findAllByAtivoTrue(Pageable paginacao);

    Optional<Parceiro> findByIdAndAtivoTrue(Long id);

    boolean existsByEmailIgnoreCase(String email);
}
