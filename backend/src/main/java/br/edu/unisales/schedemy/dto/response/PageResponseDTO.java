package br.edu.unisales.schedemy.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

// Envelope padrao de paginacao devolvido pela API.
public record PageResponseDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int tamanhoPagina,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {
    public static <T> PageResponseDTO<T> de(Page<T> pagina) {
        return new PageResponseDTO<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}