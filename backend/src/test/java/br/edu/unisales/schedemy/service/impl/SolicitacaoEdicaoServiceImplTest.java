package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.SolicitacaoEdicao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.RespostaSolicitacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.SolicitacaoEdicaoRequestDTO;
import br.edu.unisales.schedemy.exception.AcessoNegadoNegocioException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.SolicitacaoEdicaoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.DuracaoReuniaoRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.SolicitacaoEdicaoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RF 21/22, RN 12 - pedido de alteracao de reuniao confirmada. Roda sem banco; as regras de agendamento sao as de
 * verdade (RegrasDeAgendamento) e a CalculadoraDeAgenda so diz "livre" ou "ocupado".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitacaoEdicaoServiceImplTest {

    @Mock private SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private RegistroMotivoRepository registroMotivoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DuracaoReuniaoRepository duracaoReuniaoRepository;
    @Mock private CalculadoraDeAgenda calculadoraDeAgenda;
    @Mock private UsuarioAutenticado usuarioAutenticado;
    @Mock private Notificador notificador;

    private SolicitacaoEdicaoServiceImpl service;

    private final Usuario aluno = usuario(1L, "Kevin Lins", TipoUsuario.ALUNO);
    private final Usuario outroAluno = usuario(2L, "Pedro Souza", TipoUsuario.ALUNO);
    private final Usuario professor = usuario(9L, "Rômulo Ferreira", TipoUsuario.PROFESSOR);
    private final Usuario coordenador = usuario(10L, "Marcelo Souza", TipoUsuario.COORDENADOR);
    private final Usuario recepcionista = usuario(12L, "Ana Recepcao", TipoUsuario.RECEPCIONISTA);
    private final DuracaoReuniao trinta = duracao(4L, 30);
    private final DuracaoReuniao quarentaECinco = duracao(5L, 45);

    private final LocalDate semanaQueVem = LocalDate.now().plusDays(7);
    private final LocalTime dezHoras = LocalTime.of(10, 0);

    @BeforeEach
    void preparar() {
        RegrasDeAgendamento regras = new RegrasDeAgendamento(usuarioRepository, duracaoReuniaoRepository, calculadoraDeAgenda);
        service = new SolicitacaoEdicaoServiceImpl(solicitacaoEdicaoRepository, agendamentoRepository,
                registroMotivoRepository, regras, usuarioAutenticado, notificador, new SolicitacaoEdicaoMapper());
        for (Usuario u : List.of(aluno, outroAluno, professor, coordenador, recepcionista)) {
            when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        for (DuracaoReuniao d : List.of(trinta, quarentaECinco)) {
            when(duracaoReuniaoRepository.findById(d.getId())).thenReturn(Optional.of(d));
        }
        when(agendamentoRepository.save(any())).then(returnsFirstArg());
        when(solicitacaoEdicaoRepository.save(any())).then(returnsFirstArg());
        when(calculadoraDeAgenda.verificar(any(Usuario.class), any(), any(), any(), any())).thenReturn(Optional.empty());
        logadoComo(aluno);
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Pedir (RF 21)")
    class Pedir {

        @Test
        @DisplayName("aluno participante pede: pedido PENDENTE, reuniao AGUARDANDO_RESPOSTA, motivo registrado e aviso")
        void alunoPede() {
            Agendamento a = noBanco(confirmada(10L, semanaQueVem, dezHoras));

            var resposta = service.solicitar(10L, pedido(semanaQueVem.plusDays(1), LocalTime.of(14, 0), List.of(professor)));

            assertThat(resposta.status()).isEqualTo(StatusSolicitacao.PENDENTE);
            assertThat(resposta.novoHorarioFim()).isEqualTo(LocalTime.of(14, 30));
            assertThat(a.getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
            // a reuniao continua valendo no horario original
            assertThat(a.getDataReuniao()).isEqualTo(semanaQueVem);
            assertThat(a.getHorarioInicio()).isEqualTo(dezHoras);
            assertThat(motivoRegistrado().getTipoOperacao()).isEqualTo(TipoOperacaoMotivo.SOLICITACAO_EDICAO);
            verify(notificador).pedidoDeAlteracao(eq(a), eq(aluno), any());
        }

        @Test
        @DisplayName("RN 01 - trava a reuniao antes de le-la e confere o horario novo para todos, ignorando a propria")
        void travaEConfereHorario() {
            noBanco(confirmada(10L, semanaQueVem, dezHoras));

            service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor)));

            InOrder ordem = inOrder(agendamentoRepository, usuarioRepository, calculadoraDeAgenda);
            ordem.verify(agendamentoRepository).travarPorId(10L);
            ordem.verify(agendamentoRepository).findById(10L);
            ordem.verify(usuarioRepository).travarParaAgendamento(anyList());
            ordem.verify(calculadoraDeAgenda).verificar(eq(professor), eq(semanaQueVem), eq(LocalTime.of(14, 0)),
                    eq(LocalTime.of(14, 30)), eq(10L));
        }

        @Test
        @DisplayName("a recepcao pede em nome do aluno")
        void recepcaoPede() {
            noBanco(confirmada(10L, semanaQueVem, dezHoras));
            logadoComo(recepcionista);
            when(usuarioAutenticado.podeGerirAgendamentosDeTerceiros()).thenReturn(true);

            assertThat(service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))).idSolicitante())
                    .isEqualTo(recepcionista.getId());
        }

        @Test
        @DisplayName("docente e aluno que nao participa nao pedem")
        void quemNaoPode() {
            noBanco(confirmada(10L, semanaQueVem, dezHoras));

            logadoComo(professor);
            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            logadoComo(outroAluno);
            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            verify(solicitacaoEdicaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("so reuniao CONFIRMADO: pendente se edita direto; outro pedido aguardando bloqueia")
        void soConfirmada() {
            Agendamento a = noBanco(confirmada(10L, semanaQueVem, dezHoras));

            a.setStatus(StatusAgendamento.PENDENTE);
            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("edite");
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("aguardando");
            verify(solicitacaoEdicaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN 14 - a menos de 24 h do horario original nao pede")
        void prazo() {
            LocalDateTime daqui20h = LocalDateTime.now().plusHours(20);
            noBanco(confirmada(10L, daqui20h.toLocalDate(), daqui20h.toLocalTime().withSecond(0).withNano(0)));

            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("24");
        }

        @Test
        @DisplayName("pedido que nao muda nada e recusado")
        void nadaMuda() {
            noBanco(confirmada(10L, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, dezHoras, List.of(professor))))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nao muda");
        }

        @Test
        @DisplayName("horario novo ocupado e recusado (mesma regra do agendamento)")
        void horarioOcupado() {
            noBanco(confirmada(10L, semanaQueVem, dezHoras));
            when(calculadoraDeAgenda.verificar(eq(professor), any(), any(), any(), any())).thenReturn(Optional.of(
                    new CalculadoraDeAgenda.Impedimento(professor, CalculadoraDeAgenda.TipoImpedimento.BLOQUEADO, null)));

            assertThatThrownBy(() -> service.solicitar(10L, pedido(semanaQueVem, LocalTime.of(14, 0), List.of(professor))))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nao esta disponivel");
            verify(solicitacaoEdicaoRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Responder (RF 22, RN 12)")
    class Responder {

        @Test
        @DisplayName("aceito: a reuniao passa aos dados novos; quem fica mantem a resposta, quem entra responde, quem sai sai")
        void aceitar() {
            Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
            a.adicionarParticipante(participante(outroAluno, StatusResposta.ACEITO));
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            // pedido: outro dia, 45 min, on-line, sai o outro aluno e entra o coordenador
            SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), quarentaECinco,
                    Set.of(professor, coordenador));
            logadoComo(professor);

            service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(true, null));

            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.ACEITA);
            assertThat(s.getRespondidoEm()).isNotNull();
            assertThat(a.getDataReuniao()).isEqualTo(semanaQueVem.plusDays(1));
            assertThat(a.getHorarioInicio()).isEqualTo(LocalTime.of(14, 0));
            assertThat(a.getDuracao()).isSameAs(quarentaECinco);
            assertThat(a.getFormato()).isEqualTo(FormatoReuniao.ONLINE);
            assertThat(a.getParticipantes())
                    .extracting(p -> p.getUsuario().getId(), AgendamentoParticipante::getStatusResposta)
                    .containsExactlyInAnyOrder(tuple(1L, StatusResposta.ACEITO), tuple(9L, StatusResposta.ACEITO),
                            tuple(10L, StatusResposta.PENDENTE));
            assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
            verify(notificador).alteracaoAceita(a, professor, List.of(outroAluno));
        }

        @Test
        @DisplayName("aceitar confere as regras de novo: se o horario novo foi ocupado, nada muda")
        void aceitarConfereDeNovo() {
            Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), trinta, Set.of(professor));
            logadoComo(professor);
            when(calculadoraDeAgenda.verificar(eq(aluno), any(), any(), any(), any())).thenReturn(Optional.of(
                    new CalculadoraDeAgenda.Impedimento(aluno, CalculadoraDeAgenda.TipoImpedimento.REUNIAO_CONFLITANTE,
                            confirmada(99L, semanaQueVem.plusDays(1), LocalTime.of(14, 0)))));

            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(true, null)))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("conflitante");
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);
            assertThat(a.getDataReuniao()).isEqualTo(semanaQueVem);
        }

        @Test
        @DisplayName("recusa exige motivo; recusada, a reuniao volta a CONFIRMADO como estava")
        void recusar() {
            Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), trinta, Set.of(professor));
            logadoComo(professor);

            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(false, " ")))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("motivo");

            service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(false, "Banca nesse horario"));

            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.RECUSADA);
            assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
            assertThat(a.getDataReuniao()).isEqualTo(semanaQueVem);
            RegistroMotivo motivo = motivoRegistrado();
            assertThat(motivo.getTipoOperacao()).isEqualTo(TipoOperacaoMotivo.RECUSA_SOLICITACAO_EDICAO);
            assertThat(motivo.getUsuario()).isSameAs(professor);
            verify(notificador).alteracaoRecusada(a, aluno, professor, "Banca nesse horario");
        }

        @Test
        @DisplayName("so docente da reuniao (ou admin) responde; aluno e docente de fora nao")
        void quemResponde() {
            Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), trinta, Set.of(professor));

            logadoComo(aluno);
            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(true, null)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            logadoComo(coordenador);
            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(true, null)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);
        }

        @Test
        @DisplayName("pedido ja respondido nao muda; o primeiro docente decide (trava a reuniao antes de ler)")
        void primeiroDecide() {
            Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
            SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), trinta, Set.of(professor));
            s.setStatus(StatusSolicitacao.ACEITA);
            logadoComo(professor);

            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(false, "Nao")))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("encerrado");
            InOrder ordem = inOrder(solicitacaoEdicaoRepository, agendamentoRepository);
            ordem.verify(solicitacaoEdicaoRepository).buscarIdDoAgendamento(s.getId());
            ordem.verify(agendamentoRepository).travarPorId(10L);
            ordem.verify(solicitacaoEdicaoRepository).findById(s.getId());
            verifyNoInteractions(notificador);
        }

        @Test
        @DisplayName("RN 14 - a menos de 24 h do horario original nao responde mais (o pedido vai expirar)")
        void prazoDaResposta() {
            LocalDateTime daqui20h = LocalDateTime.now().plusHours(20);
            Agendamento a = confirmada(10L, daqui20h.toLocalDate(), daqui20h.toLocalTime().withSecond(0).withNano(0));
            a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            SolicitacaoEdicao s = pendente(a, semanaQueVem, LocalTime.of(14, 0), trinta, Set.of(professor));
            logadoComo(professor);

            assertThatThrownBy(() -> service.responder(s.getId(), new RespostaSolicitacaoRequestDTO(true, null)))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("prazo");
        }
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("quem pediu desiste: CANCELADA e a reuniao volta a CONFIRMADO; outro aluno nao desiste por ele")
    void desistir() {
        Agendamento a = confirmada(10L, semanaQueVem, dezHoras);
        a.adicionarParticipante(participante(outroAluno, StatusResposta.ACEITO));
        a.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
        SolicitacaoEdicao s = pendente(a, semanaQueVem.plusDays(1), LocalTime.of(14, 0), trinta, Set.of(professor));

        logadoComo(outroAluno);
        assertThatThrownBy(() -> service.cancelar(s.getId())).isInstanceOf(AcessoNegadoNegocioException.class);

        logadoComo(aluno);
        service.cancelar(s.getId());

        assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verify(notificador).pedidoEncerrado(a, aluno, aluno, false);
    }

    // ------------------------------------------------------------------

    private void logadoComo(Usuario usuario) {
        when(usuarioAutenticado.obter()).thenReturn(usuario);
        when(usuarioAutenticado.id()).thenReturn(usuario.getId());
        when(usuarioAutenticado.podeGerirAgendamentosDeTerceiros()).thenReturn(false);
        when(usuarioAutenticado.ehAdmin()).thenReturn(false);
    }

    private SolicitacaoEdicaoRequestDTO pedido(LocalDate data, LocalTime inicio, List<Usuario> convidados) {
        return new SolicitacaoEdicaoRequestDTO(convidados.stream().map(Usuario::getId).toList(), trinta.getId(), data,
                inicio, FormatoReuniao.PRESENCIAL, "Orientacao do TCC", null, "Prova no mesmo horario");
    }

    /** Reuniao confirmada: o aluno organiza e o professor aceitou. */
    private Agendamento confirmada(Long id, LocalDate data, LocalTime inicio) {
        Agendamento a = Agendamento.builder()
                .id(id).organizador(aluno).duracao(trinta).dataReuniao(data).horarioInicio(inicio)
                .formato(FormatoReuniao.PRESENCIAL).topico("Orientacao do TCC").status(StatusAgendamento.CONFIRMADO).build();
        a.adicionarParticipante(AgendamentoParticipante.builder().usuario(aluno)
                .papel(PapelParticipante.ORGANIZADOR).statusResposta(StatusResposta.ACEITO).build());
        a.adicionarParticipante(participante(professor, StatusResposta.ACEITO));
        return a;
    }

    private static AgendamentoParticipante participante(Usuario u, StatusResposta resposta) {
        return AgendamentoParticipante.builder().usuario(u).papel(PapelParticipante.CONVIDADO).statusResposta(resposta).build();
    }

    private Agendamento noBanco(Agendamento a) {
        when(agendamentoRepository.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    /** Pedido do aluno, ja gravado (on-line). */
    private SolicitacaoEdicao pendente(Agendamento a, LocalDate data, LocalTime inicio, DuracaoReuniao duracao,
                                       Set<Usuario> convidados) {
        noBanco(a);
        SolicitacaoEdicao s = SolicitacaoEdicao.builder()
                .id(50L).agendamento(a).solicitante(aluno).novaDuracao(duracao).novaData(data).novoHorarioInicio(inicio)
                .novoFormato(FormatoReuniao.ONLINE).novoTopico("Orientacao do TCC").motivo("Prova")
                .status(StatusSolicitacao.PENDENTE).novosConvidados(new LinkedHashSet<>(convidados)).build();
        when(solicitacaoEdicaoRepository.buscarIdDoAgendamento(50L)).thenReturn(Optional.of(a.getId()));
        when(solicitacaoEdicaoRepository.findById(50L)).thenReturn(Optional.of(s));
        return s;
    }

    private RegistroMotivo motivoRegistrado() {
        ArgumentCaptor<RegistroMotivo> captor = ArgumentCaptor.forClass(RegistroMotivo.class);
        verify(registroMotivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Usuario usuario(Long id, String nome, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setTipoUsuario(tipo);
        u.setAtivo(true);
        return u;
    }

    private static DuracaoReuniao duracao(Long id, int minutos) {
        DuracaoReuniao d = new DuracaoReuniao();
        d.setId(id);
        d.setMinutos(minutos);
        d.setAtivo(true);
        return d;
    }
}
