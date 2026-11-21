package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.dto.response.LogAuditoriaResponseDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "RNF 19 - trilha de quem fez o que, quando e de onde. So administracao.")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    @GetMapping
    @Operation(summary = "Consultar a trilha de auditoria",
            description = "Mais recente primeiro. Filtros opcionais: idUsuario (autor), somenteSistema (acoes automaticas), "
                    + "acao (uma ou mais), tabela + idRegistro (ex.: agendamento 42) e periodo por dia.")
    public ResponseEntity<PageResponseDTO<LogAuditoriaResponseDTO>> listar(
            @RequestParam(required = false) Long idUsuario,
            @RequestParam(required = false) Boolean somenteSistema,
            @RequestParam(required = false) List<AcaoAuditoria> acao,
            @RequestParam(required = false) String tabela,
            @RequestParam(required = false) Long idRegistro,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(auditoriaService.listar(idUsuario, somenteSistema, acao, tabela,
                idRegistro, dataInicio, dataFim, pageable)));
    }
}
