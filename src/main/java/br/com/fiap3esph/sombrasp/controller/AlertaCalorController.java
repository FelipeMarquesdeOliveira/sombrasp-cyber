package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.alerta.DadosDetalhamentoAlerta;
import br.com.fiap3esph.sombrasp.domain.alerta.DadosEmissaoAlerta;
import br.com.fiap3esph.sombrasp.domain.alerta.DadosListagemAlerta;
import br.com.fiap3esph.sombrasp.infra.openapi.OpenApiConfig;
import br.com.fiap3esph.sombrasp.service.AlertaCalorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas de calor", description = "Alertas emitidos quando a máxima chega a 31 °C (IA&ML/INMET, ESP32 ou gestor)")
public class AlertaCalorController {
    private final AlertaCalorService service;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Emite um alerta (o nível é calculado pela temperatura: >= 31 ALERTA, >= 35 EMERGENCIA)")
    public ResponseEntity<DadosDetalhamentoAlerta> emitir(@RequestBody @Valid DadosEmissaoAlerta dados,
                                                         UriComponentsBuilder uriBuilder) {
        DadosDetalhamentoAlerta dto = service.emitir(dados);
        URI uri = uriBuilder.path("/alertas/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping
    @Operation(summary = "Lista alertas (por padrão só os ativos), do mais recente ao mais antigo")
    public ResponseEntity<Page<DadosListagemAlerta>> listar(
            @RequestParam(defaultValue = "true") boolean ativos,
            @PageableDefault(size = 10, sort = "dataReferencia", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(service.listar(ativos, paginacao));
    }

    @GetMapping("/hoje")
    @Operation(summary = "Alerta ativo mais grave de hoje (204 se não houver alerta)")
    public ResponseEntity<DadosDetalhamentoAlerta> hoje() {
        return service.alertaDeHoje()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um alerta")
    public ResponseEntity<DadosDetalhamentoAlerta> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Encerra um alerta ativo (exclusão lógica, gestor)")
    public ResponseEntity<Void> encerrar(@PathVariable Long id) {
        service.encerrar(id);
        return ResponseEntity.noContent().build();
    }
}
