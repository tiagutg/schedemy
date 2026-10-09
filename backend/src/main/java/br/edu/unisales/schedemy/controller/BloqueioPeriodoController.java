package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.BloqueioPeriodoService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/bloqueios-periodo")
@RequiredArgsConstructor
@Tag(name = "Bloqueios de Periodo", description = "Periodos de indisponibilidade na agenda de professores/coordenadores.")
public class BloqueioPeriodoController {
    private final BloqueioPeriodoService bloqueioPeriodoService;

    @PostMapping
    @Operation(summary = "Cadastrar bloqueio de periodo")
    public ResponseEntity<BloqueioPeriodoResponseDTO> criar(@Valid @RequestBody BloqueioPeriodoRequestDTO dto) {
        BloqueioPeriodoResponseDTO criado = bloqueioPeriodoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/bloqueios-periodo/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar bloqueio por ID")
    public ResponseEntity<BloqueioPeriodoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bloqueioPeriodoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar bloqueios", description = "Filtros por usuario e intervalo de datas.")
    public ResponseEntity<PageResponseDTO<BloqueioPeriodoResponseDTO>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 20, sort = "dataInicio") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(bloqueioPeriodoService.listar(usuarioId, dataInicio, dataFim, pageable)));
    }

    @GetMapping("/{id}/agendamentos-impactados")
    @Operation(summary = "Listar agendamentos impactados", description = "Agendamentos que colidem com o bloqueio.")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarImpactados(@PathVariable Long id) {
        return ResponseEntity.ok(bloqueioPeriodoService.listarAgendamentosImpactados(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar bloqueio de periodo")
    public ResponseEntity<BloqueioPeriodoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody BloqueioPeriodoRequestDTO dto) {
        return ResponseEntity.ok(bloqueioPeriodoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover bloqueio de periodo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        bloqueioPeriodoService.deletar(id);
    }
}