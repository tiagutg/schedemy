package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.DisponibilidadeMapper;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Disponibilidade: horario valido, RN 06 (so docente) e janelas ativas sem sobreposicao no mesmo dia. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisponibilidadeServiceImplTest {

    private static final long ID_DOCENTE = 2L;
    private static final int QUARTA = 4;

    @Mock private DisponibilidadeRepository disponibilidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioAutenticado usuarioAutenticado;

    private DisponibilidadeServiceImpl service;
    private Usuario docente;

    @BeforeEach
    void preparar() {
        service = new DisponibilidadeServiceImpl(disponibilidadeRepository, usuarioRepository,
                new DisponibilidadeMapper(), usuarioAutenticado);
        docente = Usuario.builder().id(ID_DOCENTE).nome("Docente").tipoUsuario(TipoUsuario.PROFESSOR).build();
        when(usuarioRepository.findById(ID_DOCENTE)).thenReturn(Optional.of(docente));
        when(disponibilidadeRepository.save(any())).then(returnsFirstArg());
    }

    private static DisponibilidadeRequestDTO pedido(String inicio, String fim, Boolean ativo) {
        return new DisponibilidadeRequestDTO(ID_DOCENTE, QUARTA, LocalTime.parse(inicio), LocalTime.parse(fim), ativo);
    }

    private void haSobreposicao(boolean resposta) {
        when(disponibilidadeRepository.existeSobreposicao(eq(ID_DOCENTE), eq(QUARTA), any(), any(), anyLong()))
                .thenReturn(resposta);
    }

    private Disponibilidade existente(long id, boolean ativa) {
        Disponibilidade d = Disponibilidade.builder().id(id).usuario(docente).diaSemana(QUARTA)
                .horarioInicio(LocalTime.of(8, 0)).horarioFim(LocalTime.of(12, 0)).ativo(ativa).build();
        when(disponibilidadeRepository.findById(id)).thenReturn(Optional.of(d));
        return d;
    }

    @Test
    @DisplayName("cria janela sem sobreposicao, travando o docente antes de consultar")
    void criaSemSobreposicao() {
        haSobreposicao(false);

        service.criar(pedido("08:00", "12:00", null));

        InOrder ordem = inOrder(usuarioRepository, disponibilidadeRepository);
        ordem.verify(usuarioRepository).travarParaAgendamento(List.of(ID_DOCENTE));
        ordem.verify(disponibilidadeRepository).existeSobreposicao(
                ID_DOCENTE, QUARTA, LocalTime.of(8, 0), LocalTime.of(12, 0), -1L);
        ordem.verify(disponibilidadeRepository).save(any());
    }

    @Test
    @DisplayName("recusa criar janela ativa sobreposta a outra do mesmo dia")
    void recusaCriarSobreposta() {
        haSobreposicao(true);

        assertThatThrownBy(() -> service.criar(pedido("10:00", "14:00", true)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("sobrepoe");
        verify(disponibilidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("janela criada inativa nao precisa checar sobreposicao")
    void criaInativaSemChecar() {
        haSobreposicao(true);

        service.criar(pedido("10:00", "14:00", false));

        verify(disponibilidadeRepository, never()).existeSobreposicao(anyLong(), any(), any(), any(), anyLong());
        verify(disponibilidadeRepository).save(any());
    }

    @Test
    @DisplayName("edicao ignora a propria janela na checagem")
    void edicaoIgnoraAPropria() {
        existente(7L, true);
        haSobreposicao(false);

        service.atualizar(7L, pedido("08:00", "13:00", null));

        verify(disponibilidadeRepository).existeSobreposicao(
                ID_DOCENTE, QUARTA, LocalTime.of(8, 0), LocalTime.of(13, 0), 7L);
        verify(disponibilidadeRepository).save(any());
    }

    @Test
    @DisplayName("recusa editar janela ativa para um horario sobreposto")
    void recusaEditarSobreposta() {
        existente(7L, true);
        haSobreposicao(true);

        assertThatThrownBy(() -> service.atualizar(7L, pedido("11:00", "15:00", null)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(disponibilidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("recusa reativar janela inativa que ficou sobreposta")
    void recusaReativarSobreposta() {
        existente(7L, false);
        haSobreposicao(true);

        assertThatThrownBy(() -> service.atualizar(7L, pedido("08:00", "12:00", true)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("editar janela inativa sem reativar nao checa sobreposicao")
    void edicaoDeInativaNaoChecar() {
        existente(7L, false);
        haSobreposicao(true);

        service.atualizar(7L, pedido("09:00", "12:00", null));

        verify(disponibilidadeRepository, never()).existeSobreposicao(anyLong(), any(), any(), any(), anyLong());
        verify(disponibilidadeRepository).save(any());
    }

    @Test
    @DisplayName("fim antes do inicio continua recusado")
    void recusaFimAntesDoInicio() {
        assertThatThrownBy(() -> service.criar(pedido("12:00", "08:00", null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("posterior");
    }
}
