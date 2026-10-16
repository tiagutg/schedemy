package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.AvaliacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AvaliacaoResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.AvaliacaoService;
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
@RequestMapping("/api/v1/avaliacoes")
@RequiredArgsConstructor
@Tag(name = "Avaliacoes", description = "Avaliacoes das reunioes concluidas, registradas pelos alunos (RF 24).")
public class AvaliacaoController {
    private final AvaliacaoService avaliacaoService;

    @PostMapping
    @Operation(summary = "Registrar avaliacao",
            description = "Somente ALUNO participante de uma reuniao com status CONCLUIDO, uma vez por reuniao.")
    public ResponseEntity<AvaliacaoResponseDTO> criar(@Valid @RequestBody AvaliacaoRequestDTO dto) {
        AvaliacaoResponseDTO criado = avaliacaoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/avaliacoes/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar avaliacao por ID")
    public ResponseEntity<AvaliacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.buscarPorId(id));
    }

    @GetMapping("/alunos/{alunoId}")
    @Operation(summary = "Listar avaliacoes de um aluno")
    public ResponseEntity<PageResponseDTO<AvaliacaoResponseDTO>> listarPorAluno(
            @PathVariable Long alunoId,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(avaliacaoService.listarPorAluno(alunoId, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar avaliacao", description = "Permite rever nota e comentario da propria avaliacao.")
    public ResponseEntity<AvaliacaoResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody AvaliacaoRequestDTO dto) {
        return ResponseEntity.ok(avaliacaoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover avaliacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        avaliacaoService.deletar(id);
    }
}
