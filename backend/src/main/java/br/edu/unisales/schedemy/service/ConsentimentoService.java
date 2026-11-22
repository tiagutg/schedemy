package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.AceiteTermoRequestDTO;
import br.edu.unisales.schedemy.dto.response.ConsentimentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.TermoResponseDTO;

/** RNF 18 - termo de uso/consentimento, politica de privacidade e o aceite do usuario logado. */
public interface ConsentimentoService {
    TermoResponseDTO termoAtual();

    ConsentimentoResponseDTO situacaoDoLogado();

    ConsentimentoResponseDTO aceitar(AceiteTermoRequestDTO dto);
}
