package br.edu.unisales.schedemy.exception;

import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import lombok.Getter;

import java.util.List;

/**
 * RF 06 - a mudanca na agenda cancelaria reunioes que o docente nao confirmou. Vira 422 com a lista
 * (ErrorResponseDTO.reunioesAfetadas) para o front mostrar e pedir a confirmacao com o motivo.
 */
@Getter
public class ReunioesAfetadasException extends RegraDeNegocioException {
    private final transient List<ReuniaoAfetadaResponseDTO> reunioes;

    public ReunioesAfetadasException(String mensagem, List<ReuniaoAfetadaResponseDTO> reunioes) {
        super(mensagem);
        this.reunioes = List.copyOf(reunioes);
    }
}
