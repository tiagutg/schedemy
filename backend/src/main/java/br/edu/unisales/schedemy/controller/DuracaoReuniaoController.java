package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.dto.request.DuracaoReuniaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.DuracaoReuniaoResponseDTO;
import br.edu.unisales.schedemy.service.DuracaoReuniaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/duracoes-reuniao")
@RequiredArgsConstructor
@Tag(name = "Duracoes de Reuniao", description = "Duracoes padronizadas disponiveis para agendamento")
public class DuracaoReuniaoController {
    private final DuracaoReuniaoService duracaoReuniaoService;

    @PostMapping
    @Operation(summary = "Cadastrar duracao de reuniao")
    public ResponseEntity<DuracaoReuniaoResponseDTO> criar(@Valid @RequestBody DuracaoReuniaoRequestDTO dto) {
        DuracaoReuniaoResponseDTO criado = duracaoReuniaoService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/duracoes-reuniao/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar duracao por ID")
    public ResponseEntity<DuracaoReuniaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(duracaoReuniaoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar duracoes", description = "Use ativas=true para retornar apenas duracoes habilitadas.")
    public ResponseEntity<List<DuracaoReuniaoResponseDTO>> listar(
            @RequestParam(name = "ativas", required = false, defaultValue = "false") boolean apenasAtivas) {
        return ResponseEntity.ok(apenasAtivas ? duracaoReuniaoService.listarAtivas() : duracaoReuniaoService.listarTodas());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar duracao de reuniao")
    public ResponseEntity<DuracaoReuniaoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody DuracaoReuniaoRequestDTO dto) {
        return ResponseEntity.ok(duracaoReuniaoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover duracao de reuniao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        duracaoReuniaoService.deletar(id);
    }
}