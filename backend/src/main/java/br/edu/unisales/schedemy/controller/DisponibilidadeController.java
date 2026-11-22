package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
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
import java.util.List;

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

    @PostMapping("/{id}/previa")
    @Operation(summary = "Previa da edicao (RF 06/RF 07)",
            description = "Reunioes que seriam canceladas se a disponibilidade ficasse como no corpo. Nao grava nada.")
    public ResponseEntity<List<ReuniaoAfetadaResponseDTO>> previaDaEdicao(@PathVariable Long id,
                                                                          @Valid @RequestBody DisponibilidadeRequestDTO dto) {
        return ResponseEntity.ok(disponibilidadeService.previaDaEdicao(id, dto));
    }

    @PostMapping("/{id}/remocao/previa")
    @Operation(summary = "Previa da remocao (RF 06/RF 07)",
            description = "Reunioes que seriam canceladas se a disponibilidade fosse removida. Nao grava nada.")
    public ResponseEntity<List<ReuniaoAfetadaResponseDTO>> previaDaRemocao(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadeService.previaDaRemocao(id));
    }

    @PostMapping("/{id}/remocao")
    @Operation(summary = "Remover disponibilidade cancelando as reunioes afetadas (RF 03/RF 06)",
            description = "Remove a janela e cancela, na mesma transacao, as reunioes afetadas confirmadas no corpo. "
                    + "Se houver afetada fora da lista, recusa (422) com a lista atualizada.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @Valid @RequestBody CancelamentoDeAfetadasRequestDTO dto) {
        disponibilidadeService.remover(id, dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar disponibilidade",
            description = "Se a mudanca cancelar reunioes, exige cancelarAfetadas (ids da previa + motivo); sem isso, 422 com a lista.")
    public ResponseEntity<DisponibilidadeResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody DisponibilidadeRequestDTO dto) {
        return ResponseEntity.ok(disponibilidadeService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover disponibilidade", description = "Sem reunioes afetadas. Com afetadas, 422 com a lista: use POST /{id}/remocao.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        disponibilidadeService.deletar(id);
    }
}