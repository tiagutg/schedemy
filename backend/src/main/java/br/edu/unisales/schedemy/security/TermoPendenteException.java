package br.edu.unisales.schedemy.security;

import org.springframework.security.access.AccessDeniedException;

/** RNF 18 - o usuario ainda nao aceitou a versao em vigor do termo (403 com erro TERMO_PENDENTE). */
public class TermoPendenteException extends AccessDeniedException {
    public TermoPendenteException() {
        super("Aceite o termo de uso e a politica de privacidade para continuar.");
    }
}
