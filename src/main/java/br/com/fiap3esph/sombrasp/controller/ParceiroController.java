package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.parceiro.*;
import br.com.fiap3esph.sombrasp.infra.openapi.OpenApiConfig;
import br.com.fiap3esph.sombrasp.service.ParceiroService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/parceiros")
@RequiredArgsConstructor
@Tag(name = "Parceiros", description = "Organizações que mantêm os pontos (poder público, comércio, ONG)")
public class ParceiroController {
    private final ParceiroService service;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Cadastra um parceiro (gestor)")
    public ResponseEntity<DadosDetalhamentoParceiro> cadastrar(@RequestBody @Valid DadosCadastroParceiro dados,
                                                               UriComponentsBuilder uriBuilder) {
        DadosDetalhamentoParceiro dto = service.cadastrar(dados);
        URI uri = uriBuilder.path("/parceiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping
    @Operation(summary = "Lista os parceiros ativos (paginado)")
    public ResponseEntity<Page<DadosListagemParceiro>> listar(@PageableDefault(size = 10, sort = "nome") Pageable paginacao) {
        return ResponseEntity.ok(service.listar(paginacao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um parceiro")
    public ResponseEntity<DadosDetalhamentoParceiro> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }

    @PutMapping
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Atualiza nome, tipo ou telefone do parceiro (gestor)")
    public ResponseEntity<DadosDetalhamentoParceiro> atualizar(@RequestBody @Valid DadosAtualizacaoParceiro dados) {
        return ResponseEntity.ok(service.atualizar(dados));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "Exclui (lógico) um parceiro sem pontos ativos (gestor)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
