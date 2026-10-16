package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.DisponibilidadeService;
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
@RequestMapping("/api/v1/disponibilidades")
@RequiredArgsConstructor
@Tag(name = "Disponibilidades", description = "Grade semanal de horarios de atendimento do coordenador/professor.")
public class DisponibilidadeController {
    private final DisponibilidadeService disponibilidadeService;

    @PostMapping
    @Operation(summary = "Cadastrar disponibilidade", description = "Somente PROFESSOR ou COORDENADOR podem ter disponibilidade.")
    public ResponseEntity<DisponibilidadeResponseDTO> criar(@Valid @RequestBody DisponibilidadeRequestDTO dto) {
        DisponibilidadeResponseDTO criado = disponibilidadeService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/disponibilidades/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar disponibilidade por ID")
    public ResponseEntity<DisponibilidadeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadeService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar disponibilidades", description = "Filtros por usuario, dia da semana e status ativo.")
    public ResponseEntity<PageResponseDTO<DisponibilidadeResponseDTO>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Integer diaSemana,
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "diaSemana") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(disponibilidadeService.listar(usuarioId, diaSemana, ativo, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar disponibilidade")
    public ResponseEntity<DisponibilidadeResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody DisponibilidadeRequestDTO dto) {
        return ResponseEntity.ok(disponibilidadeService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover disponibilidade")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        disponibilidadeService.deletar(id);
    }
}