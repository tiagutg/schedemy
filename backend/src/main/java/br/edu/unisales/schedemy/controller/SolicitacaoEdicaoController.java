package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.dto.request.RespostaSolicitacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.SolicitacaoEdicaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.dto.response.SolicitacaoEdicaoResponseDTO;
import br.edu.unisales.schedemy.service.SolicitacaoEdicaoService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Pedidos de alteracao", description = "RF 21/22 - alteracao de reuniao ja confirmada, decidida por um docente (RN 12).")
public class SolicitacaoEdicaoController {
    private final SolicitacaoEdicaoService solicitacaoEdicaoService;

    @PostMapping("/agendamentos/{id}/solicitacoes-edicao")
    @Operation(summary = "Pedir alteracao de reuniao confirmada (RF 21)",
            description = "Aluno participante ou recepcao. Ate 24 h antes do horario original (RN 14); a reuniao fica "
                    + "AGUARDANDO_RESPOSTA e continua valendo no horario original ate um docente responder.")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoEdicaoResponseDTO solicitar(@PathVariable Long id, @Valid @RequestBody SolicitacaoEdicaoRequestDTO dto) {
        return solicitacaoEdicaoService.solicitar(id, dto);
    }

    @GetMapping("/agendamentos/{id}/solicitacoes-edicao")
    @Operation(summary = "Pedidos de alteracao da reuniao", description = "Do mais recente para o mais antigo.")
    public ResponseEntity<List<SolicitacaoEdicaoResponseDTO>> listar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitacaoEdicaoService.listarDoAgendamento(id));
    }

    @GetMapping("/solicitacoes-edicao")
    @Operation(summary = "Pedidos de alteracao que voce acompanha (RF 22)",
            description = "Docente e aluno: os das reunioes que organizam ou de que participam; recepcao e admin: todos. "
                    + "Filtre por status (ex.: PENDENTE); sem status, todos. Ordem: data da reuniao, a mais proxima primeiro.")
    public ResponseEntity<PageResponseDTO<SolicitacaoEdicaoResponseDTO>> listarNoEscopo(
            @RequestParam(required = false) List<StatusSolicitacao> status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(solicitacaoEdicaoService.listar(status, pageable)));
    }

    @PatchMapping("/solicitacoes-edicao/{id}/resposta")
    @Operation(summary = "Aceitar ou recusar o pedido (RF 22, RN 12)",
            description = "O primeiro docente da reuniao que responder decide. Recusar exige motivo (RF 11).")
    public ResponseEntity<SolicitacaoEdicaoResponseDTO> responder(@PathVariable Long id,
                                                                  @Valid @RequestBody RespostaSolicitacaoRequestDTO dto) {
        return ResponseEntity.ok(solicitacaoEdicaoService.responder(id, dto));
    }

    @PatchMapping("/solicitacoes-edicao/{id}/cancelamento")
    @Operation(summary = "Desistir do pedido", description = "Quem pediu, ou a recepcao. A reuniao continua como estava.")
    public ResponseEntity<SolicitacaoEdicaoResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitacaoEdicaoService.cancelar(id));
    }
}
