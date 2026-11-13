package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.LoginRequestDTO;
import br.edu.unisales.schedemy.dto.response.TokenResponseDTO;

public interface AutenticacaoService {

    /** Confere e-mail e senha e devolve o token de acesso (401 se nao conferirem). */
    TokenResponseDTO entrar(LoginRequestDTO dto);
}
