package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdMicrosoft(String idMicrosoft);

    Optional<Usuario> findByMatriculaRa(String matriculaRa);

    boolean existsByEmail(String email);

    boolean existsByIdMicrosoft(String idMicrosoft);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:tipo IS NULL OR u.tipoUsuario = :tipo)
              AND (:nome IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:ativo IS NULL OR u.ativo = :ativo)
            """)
    Page<Usuario> buscarComFiltros(@Param("tipo") TipoUsuario tipo,
                                   @Param("nome") String nome,
                                   @Param("ativo") Boolean ativo,
                                   Pageable pageable);
}