package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.ConsentimentoTermo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimentoTermoRepository extends JpaRepository<ConsentimentoTermo, Long> {

    /** RNF 18 - consultado a cada requisicao (ConversorDeToken): o usuario aceitou a versao em vigor? */
    boolean existsByUsuarioIdAndVersaoTermo(Long usuarioId, String versaoTermo);

    /** Ultimo aceite do usuario, de qualquer versao. */
    Optional<ConsentimentoTermo> findFirstByUsuarioIdOrderByAceitoEmDescIdDesc(Long usuarioId);
}
