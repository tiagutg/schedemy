package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RespostaAgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "CRUD de Agendamentos de reuniao")
public class AgendamentoController {
    private final AgendamentoService agendamentoService;

    @PostMapping
    @Operation(summary = "Criar agendamento", description = "Cadastra uma nova solicitacao de reuniao, aplicando as regras de negocio.")
    public ResponseEntity<AgendamentoResponseDTO> criar(@Valid @RequestBody AgendamentoRequestDTO dto) {
        AgendamentoResponseDTO criado = agendamentoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/agendamentos/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    public ResponseEntity<AgendamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @GetMapping("/usuarios/{usuarioId}")
    @Operation(summary = "Listar agendamentos de um usuario")
    public ResponseEntity<PageResponseDTO<AgendamentoResponseDTO>> listarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 20, sort = "dataReuniao") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(agendamentoService.listarPorUsuario(usuarioId, status, dataInicio, dataFim, pageable)));
    }

    @GetMapping
    @Operation(summary = "Listar todos os agendamentos", description = "Visao administrativa, com filtros de status e periodo.")
    public ResponseEntity<PageResponseDTO<AgendamentoResponseDTO>> listarTodos(
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 20, sort = "dataReuniao") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(agendamentoService.listarTodos(status, dataInicio, dataFim, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento", description = "Permitido apenas enquanto o agendamento estiver PENDENTE.")
    public ResponseEntity<AgendamentoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoRequestDTO dto) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/resposta")
    @Operation(summary = "Aceitar ou recusar agendamento", description = "Resposta de um participante.")
    public ResponseEntity<AgendamentoResponseDTO> responder(@PathVariable Long id, @Valid @RequestBody RespostaAgendamentoRequestDTO dto) {
        return ResponseEntity.ok(agendamentoService.responder(id, dto));
    }

    @PatchMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar agendamento", description = "Exige confirmacao e motivo.")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequestDTO dto) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover agendamento definitivamente", description = "Permitido apenas para agendamentos PENDENTES ou CANCELADOS.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        agendamentoService.deletar(id);
    }
}