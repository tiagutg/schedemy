package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Formato padronizado de erro devolvido pelo tratamento centralizado de excecoes.
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> camposInvalidos
) {
    public record CampoErro(String campo, String mensagem) {
    }

    public static ErrorResponseDTO simples(int status, String erro, String mensagem, String caminho) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }

    public static ErrorResponseDTO comCampos(int status, String erro, String mensagem, String caminho, Map<String, String> erros) {
        List<CampoErro> lista = erros.entrySet().stream()
                .map(e -> new CampoErro(e.getKey(), e.getValue())).toList();
        return new ErrorResponseDTO(LocalDateTime.now(), status, erro, mensagem, caminho, lista);
    }
}