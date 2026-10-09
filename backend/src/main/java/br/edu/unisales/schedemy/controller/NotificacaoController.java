package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.dto.request.NotificacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.NotificacaoResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificacoes", description = "Fila e historico de notificacoes dos envolvidos nas reunioes (RF 23).")
public class NotificacaoController {
    private final NotificacaoService notificacaoService;

    @PostMapping
    @Operation(summary = "Enfileirar notificacao", description = "Registra a notificacao com status inicial PENDENTE.")
    public ResponseEntity<NotificacaoResponseDTO> criar(@Valid @RequestBody NotificacaoRequestDTO dto) {
        NotificacaoResponseDTO criado = notificacaoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/notificacoes/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar notificacao por ID")
    public ResponseEntity<NotificacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar notificacoes", description = "Filtros por usuario e status de envio.")
    public ResponseEntity<PageResponseDTO<NotificacaoResponseDTO>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) StatusEnvio status,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(notificacaoService.listar(usuarioId, status, pageable)));
    }

    @PatchMapping("/{id}/enviar")
    @Operation(summary = "Marcar notificacao como enviada",
            description = "Registra a data de envio e muda o status para ENVIADO.")
    public ResponseEntity<NotificacaoResponseDTO> marcarComoEnviada(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.marcarComoEnviada(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover notificacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        notificacaoService.deletar(id);
    }
}
