package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.dto.response.TermoResponseDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * RNF 18 - termo de uso/consentimento e politica de privacidade em vigor, lidos de lgpd/termo.json na inicializacao.
 * O arquivo e a unica fonte do texto e da versao: o front mostra o que vem de GET /api/v1/termos, e o aceite so
 * vale para a versao daqui. Para publicar uma versao nova, troque "versao" no arquivo: todos aceitam de novo.
 */
@Component
public class TermoDePrivacidade {

    static final String ARQUIVO = "lgpd/termo.json";

    private final TermoResponseDTO documento;

    public TermoDePrivacidade() {
        this(ARQUIVO);
    }

    TermoDePrivacidade(String arquivo) {
        try (InputStream entrada = new ClassPathResource(arquivo).getInputStream()) {
            this.documento = JsonMapper.builder().build().readValue(entrada, TermoResponseDTO.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o termo de privacidade em " + arquivo, e);
        }
        if (documento.versao() == null || documento.versao().isBlank() || documento.versao().length() > 20) {
            throw new IllegalStateException("O termo em " + arquivo + " precisa de uma versao com ate 20 caracteres.");
        }
    }

    public String versao() {
        return documento.versao();
    }

    public TermoResponseDTO documento() {
        return documento;
    }
}
