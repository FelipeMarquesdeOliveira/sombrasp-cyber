package br.com.fiap3esph.sombrasp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health-check")
@Tag(name = "Health check")
public class HealthCheckController {
    @GetMapping
    @Operation(summary = "Verifica se a API está no ar")
    public Map<String, String> healthCheck() {
        return Map.of("status", "UP", "api", "SombraSP - SOA e Web Services (GS 2026.2, 3ESPH)");
    }
}
