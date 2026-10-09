package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.RegistroMotivoRequestDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.dto.response.RegistroMotivoResponseDTO;
import br.edu.unisales.schedemy.service.RegistroMotivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/registros-motivo")
@RequiredArgsConstructor
@Tag(name = "Registros de motivo", description = "Justificativas de cancelamento, edicao e remarcacao (RF 11).")
public class RegistroMotivoController {
    private final RegistroMotivoService registroMotivoService;

    @PostMapping
    @Operation(summary = "Registrar motivo")
    public ResponseEntity<RegistroMotivoResponseDTO> criar(@Valid @RequestBody RegistroMotivoRequestDTO dto) {
        RegistroMotivoResponseDTO criado = registroMotivoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/registros-motivo/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar registro de motivo por ID")
    public ResponseEntity<RegistroMotivoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(registroMotivoService.buscarPorId(id));
    }

    @GetMapping("/agendamentos/{agendamentoId}")
    @Operation(summary = "Listar motivos de um agendamento", description = "Historico de justificativas da reuniao.")
    public ResponseEntity<PageResponseDTO<RegistroMotivoResponseDTO>> listarPorAgendamento(
            @PathVariable Long agendamentoId,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity.ok(
                PageResponseDTO.de(registroMotivoService.listarPorAgendamento(agendamentoId, pageable)));
    }
}
