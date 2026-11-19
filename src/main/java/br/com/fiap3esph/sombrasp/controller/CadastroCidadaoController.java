package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.cidadao.DadosCadastroCidadao;
import br.com.fiap3esph.sombrasp.domain.cidadao.DadosDetalhamentoCidadao;
import br.com.fiap3esph.sombrasp.infra.openapi.OpenApiConfig;
import br.com.fiap3esph.sombrasp.service.CadastroCidadaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/cidadaos")
@RequiredArgsConstructor
@Tag(name = "Cidadãos", description = "Inscrição para alertas, com CPF, RG, saúde e senha protegidos")
public class CadastroCidadaoController {
    private final CadastroCidadaoService service;

    @PostMapping
    @Operation(summary = "Inscreve o cidadão para receber alertas (público, usado pelo app)")
    public ResponseEntity<DadosDetalhamentoCidadao> cadastrar(@RequestBody @Valid DadosCadastroCidadao dados,
                                                              UriComponentsBuilder uriBuilder) {
        DadosDetalhamentoCidadao dto = service.cadastrar(dados);
        URI uri = uriBuilder.path("/cidadaos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Detalha a inscrição, só com dados mascarados (gestor)")
    public ResponseEntity<DadosDetalhamentoCidadao> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }
}
