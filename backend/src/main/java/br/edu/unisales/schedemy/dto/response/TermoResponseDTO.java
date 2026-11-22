package br.edu.unisales.schedemy.dto.response;

import java.util.List;

/** RNF 18 - termo de uso/consentimento e politica de privacidade em vigor (arquivo lgpd/termo.json). */
public record TermoResponseDTO(
        String versao,
        String publicadoEm,
        /** Texto-base ainda em revisao pela equipe/instituicao: o front mostra o aviso. */
        boolean rascunho,
        String avisoRascunho,
        Documento termo,
        Documento politica
) {
    public record Documento(String titulo, List<Secao> secoes) {
    }

    public record Secao(String titulo, List<String> paragrafos) {
    }
}
