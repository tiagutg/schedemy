package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdMicrosoft(String idMicrosoft);

    Optional<Usuario> findByMatriculaRa(String matriculaRa);

    boolean existsByEmail(String email);

    boolean existsByIdMicrosoft(String idMicrosoft);

    /**
     * RN 01 - trava (SELECT ... FOR UPDATE) os usuarios envolvidos num agendamento ate o fim da transacao.
     * Duas requisicoes que envolvem o mesmo usuario passam a checar conflito e gravar uma depois da outra.
     * Ordenado por id para que todas as transacoes travem na mesma ordem e nao haja deadlock.
     * SQL nativo de proposito: o @Lock do Hibernate gera "FOR UPDATE OF alias", que o MariaDB nao aceita.
     * O retorno so serve para executar a consulta (o tipo do id varia: BIGINT UNSIGNED vira BigInteger no MariaDB).
     */
    @Query(value = "SELECT id_usuario FROM usuario WHERE id_usuario IN (:ids) ORDER BY id_usuario FOR UPDATE",
            nativeQuery = true)
    List<Object> travarParaAgendamento(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:tipo IS NULL OR u.tipoUsuario = :tipo)
              AND (:nome IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
                   OR LOWER(COALESCE(u.matriculaRa, '')) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:ativo IS NULL OR u.ativo = :ativo)
            """)
    Page<Usuario> buscarComFiltros(@Param("tipo") TipoUsuario tipo,
                                   @Param("nome") String nome,
                                   @Param("ativo") Boolean ativo,
                                   Pageable pageable);
}