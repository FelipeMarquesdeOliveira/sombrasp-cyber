package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.ponto.*;
import br.com.fiap3esph.sombrasp.infra.openapi.OpenApiConfig;
import br.com.fiap3esph.sombrasp.service.PontoApoioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pontos")
@RequiredArgsConstructor
@Tag(name = "Pontos de apoio", description = "Pontos de sombra e hidratação no Centro de SP (Sé e República)")
public class PontoApoioController {
    private final PontoApoioService service;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Cadastra um ponto de sombra/hidratação (gestor)")
    public ResponseEntity<DadosDetalhamentoPonto> cadastrar(@RequestBody @Valid DadosCadastroPonto dados,
                                                            UriComponentsBuilder uriBuilder) {
        DadosDetalhamentoPonto dto = service.cadastrar(dados);
        URI uri = uriBuilder.path("/pontos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping
    @Operation(summary = "Lista os pontos ativos (paginado), com filtros opcionais")
    public ResponseEntity<Page<DadosListagemPonto>> listar(
            @Parameter(description = "SOMBRA, HIDRATACAO ou SOMBRA_E_HIDRATACAO") @RequestParam(required = false) TipoPonto tipo,
            @Parameter(description = "Ex.: Sé ou República") @RequestParam(required = false) String bairro,
            @RequestParam(required = false) Boolean acessivel,
            @PageableDefault(size = 10, sort = "nome") Pageable paginacao) {
        return ResponseEntity.ok(service.listar(tipo, bairro, acessivel, paginacao));
    }

    @GetMapping("/proximos")
    @Operation(summary = "Pontos ativos mais próximos de uma coordenada (distância em km, do mais perto ao mais longe)")
    public ResponseEntity<List<DadosPontoProximo>> proximos(
            @Parameter(description = "Latitude do cidadão (ex.: -23.5505)") @RequestParam double latitude,
            @Parameter(description = "Longitude do cidadão (ex.: -46.6333)") @RequestParam double longitude,
            @Parameter(description = "Raio em km (padrão 1, máximo 10)") @RequestParam(defaultValue = "1.0") double raioKm,
            @Parameter(description = "Filtra por SOMBRA ou HIDRATACAO (SOMBRA_E_HIDRATACAO atende os dois)")
            @RequestParam(required = false) TipoPonto tipo,
            @RequestParam(required = false) Boolean acessivel) {
        return ResponseEntity.ok(service.buscarProximos(latitude, longitude, raioKm, tipo, acessivel));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um ponto ativo")
    public ResponseEntity<DadosDetalhamentoPonto> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }

    @PutMapping
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Atualiza os dados de um ponto (gestor)")
    public ResponseEntity<DadosDetalhamentoPonto> atualizar(@RequestBody @Valid DadosAtualizacaoPonto dados) {
        return ResponseEntity.ok(service.atualizar(dados));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Exclui (lógico) um ponto (gestor)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
