package br.com.fiap3esph.sombrasp.domain.alerta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AlertaCalorRepository extends JpaRepository<AlertaCalor, Long> {
    Page<AlertaCalor> findAllByAtivoTrue(Pageable paginacao);

    boolean existsByDataReferenciaAndFonteAndAtivoTrue(LocalDate dataReferencia, FonteAlerta fonte);

    /** Alerta ativo mais grave (maior temperatura) para o dia. */
    Optional<AlertaCalor> findFirstByDataReferenciaAndAtivoTrueOrderByTemperaturaMaximaDesc(LocalDate dataReferencia);
}
