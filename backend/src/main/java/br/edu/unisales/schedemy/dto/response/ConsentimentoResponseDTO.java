package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDateTime;

/** RNF 18 - situacao do aceite do termo pelo usuario logado. */
public record ConsentimentoResponseDTO(
        String versaoAtual,
        /** Ultima versao aceita (nula se nunca aceitou). */
        String versaoAceita,
        LocalDateTime aceitoEm,
        /** Precisa aceitar a versao atual para usar o sistema. */
        boolean pendente
) {
}
