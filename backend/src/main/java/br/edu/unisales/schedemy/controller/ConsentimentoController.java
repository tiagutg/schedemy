package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.AceiteTermoRequestDTO;
import br.edu.unisales.schedemy.dto.response.ConsentimentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.TermoResponseDTO;
import br.edu.unisales.schedemy.service.ConsentimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "LGPD", description = "RNF 18 - termo de uso/consentimento, politica de privacidade e aceite.")
public class ConsentimentoController {
    private final ConsentimentoService consentimentoService;

    @GetMapping("/termos")
    @Operation(summary = "Termo e politica de privacidade em vigor", description = "Publico (pagina /privacidade).")
    public ResponseEntity<TermoResponseDTO> termoAtual() {
        return ResponseEntity.ok(consentimentoService.termoAtual());
    }

    @GetMapping("/consentimentos/meu")
    @Operation(summary = "Situacao do aceite do usuario logado")
    public ResponseEntity<ConsentimentoResponseDTO> situacao() {
        return ResponseEntity.ok(consentimentoService.situacaoDoLogado());
    }

    @PostMapping("/consentimentos")
    @Operation(summary = "Aceitar o termo em vigor",
            description = "Ate aceitar, a API recusa as demais operacoes com 403 (erro TERMO_PENDENTE).")
    public ResponseEntity<ConsentimentoResponseDTO> aceitar(@Valid @RequestBody AceiteTermoRequestDTO dto) {
        return ResponseEntity.ok(consentimentoService.aceitar(dto));
    }
}
