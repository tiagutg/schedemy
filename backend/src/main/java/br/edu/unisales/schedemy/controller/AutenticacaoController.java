package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.LoginRequestDTO;
import br.edu.unisales.schedemy.dto.response.TokenResponseDTO;
import br.edu.unisales.schedemy.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login por e-mail e senha que devolve o token de acesso")
public class AutenticacaoController {
    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    @SecurityRequirements // rota publica: nao exige token no Swagger
    @Operation(summary = "Entrar",
            description = "Confere e-mail e senha e devolve um token (JWT). Envie-o nas demais chamadas no "
                    + "cabecalho 'Authorization: Bearer <token>'. Credenciais invalidas ou usuario inativo: 401.")
    public ResponseEntity<TokenResponseDTO> entrar(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(autenticacaoService.entrar(dto));
    }
}
