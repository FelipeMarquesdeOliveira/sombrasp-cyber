package br.com.fiap3esph.sombrasp.controller;

import br.com.fiap3esph.sombrasp.domain.usuario.DadosLogin;
import br.com.fiap3esph.sombrasp.domain.usuario.Usuario;
import br.com.fiap3esph.sombrasp.infra.security.DadosTokenJWT;
import br.com.fiap3esph.sombrasp.infra.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login do gestor e emissão do token JWT")
public class AutenticacaoController {
    private final AuthenticationManager manager;
    private final TokenService tokenService;

    @PostMapping
    @Operation(summary = "Autentica o gestor e devolve o token JWT (válido por 2 horas)")
    public ResponseEntity<DadosTokenJWT> efetuarLogin(@RequestBody @Valid DadosLogin dados) {
        var autenticacaoToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var autenticacao = manager.authenticate(autenticacaoToken);
        return ResponseEntity.ok(tokenService.gerarToken((Usuario) autenticacao.getPrincipal()));
    }
}
