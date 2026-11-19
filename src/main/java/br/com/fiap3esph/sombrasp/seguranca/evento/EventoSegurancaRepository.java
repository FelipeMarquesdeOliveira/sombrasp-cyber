package br.com.fiap3esph.sombrasp.seguranca.evento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long> {

    List<EventoSeguranca> findTop100ByCriadoEmAfterOrderByCriadoEmDescIdDesc(LocalDateTime desde);

    Optional<EventoSeguranca> findByProtocolo(String protocolo);
}
