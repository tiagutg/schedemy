package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RemarcacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RespostaAgendamentoRequestDTO;
import br.edu.unisales.schedemy.exception.AcessoNegadoNegocioException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.AgendamentoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.DuracaoReuniaoRepository;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.SalaVirtualRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negocio do agendamento (RN 01, 02, 03, 04, 05, 13, 14, 16, 17; RF 19, 28). Roda sem banco:
 * a CalculadoraDeAgenda (horario livre) tem testes proprios e aqui so diz "livre" ou "conflito".
 * O desfazer da remarcacao quando a nova reuniao falha e garantido pela transacao do banco (testado a mao).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoServiceImplTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DuracaoReuniaoRepository duracaoReuniaoRepository;
    @Mock private SalaVirtualRepository salaVirtualRepository;
    @Mock private RegistroMotivoRepository registroMotivoRepository;
    @Mock private AgendamentoMapper agendamentoMapper;
    @Mock private UsuarioAutenticado usuarioAutenticado;
    @Mock private CalculadoraDeAgenda calculadoraDeAgenda;
    @Mock private Notificador notificador;
    @Mock private NotificacaoRepository notificacaoRepository;

    private AgendamentoServiceImpl service;

    private final Usuario aluno = usuario(1L, "Kevin Lins", TipoUsuario.ALUNO);
    private final Usuario outroAluno = usuario(2L, "Pedro Souza", TipoUsuario.ALUNO);
    private final Usuario professor = usuario(9L, "Lucas Barboza", TipoUsuario.PROFESSOR);
    private final Usuario recepcionista = usuario(12L, "Ana Recepcao", TipoUsuario.RECEPCIONISTA);
    private final DuracaoReuniao trintaMinutos = duracao(4L, 30);

    /** Uma semana a frente, as 10h: dentro da janela de 30 dias e longe dos prazos de 1 h e 24 h. */
    private final LocalDate semanaQueVem = LocalDate.now().plusDays(7);
    private final LocalTime dezHoras = LocalTime.of(10, 0);

    @BeforeEach
    void preparar() {
        service = new AgendamentoServiceImpl(agendamentoRepository, usuarioRepository, duracaoReuniaoRepository,
                salaVirtualRepository, registroMotivoRepository, agendamentoMapper, usuarioAutenticado,
                calculadoraDeAgenda, notificador, notificacaoRepository);

        for (Usuario u : List.of(aluno, outroAluno, professor, recepcionista)) {
            when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(duracaoReuniaoRepository.findById(trintaMinutos.getId())).thenReturn(Optional.of(trintaMinutos));
        when(agendamentoRepository.save(any())).thenAnswer(returnsFirstArg());
        when(calculadoraDeAgenda.verificar(any(Usuario.class), any(), any(), any(), any())).thenReturn(Optional.empty());
        logadoComo(aluno);
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Criar")
    class Criar {

        @Test
        @DisplayName("Aluno agenda com professor: PENDENTE, organizador ja aceito, convidado pendente e aviso enviado")
        void criaReuniao() {
            service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras));

            Agendamento salvo = agendamentoSalvo();
            assertThat(salvo.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
            assertThat(salvo.getParticipantes()).extracting(p -> p.getUsuario().getId(), AgendamentoParticipante::getPapel,
                            AgendamentoParticipante::getStatusResposta)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(1L, PapelParticipante.ORGANIZADOR, StatusResposta.ACEITO),
                            org.assertj.core.groups.Tuple.tuple(9L, PapelParticipante.CONVIDADO, StatusResposta.PENDENTE));
            verify(notificador).novoAgendamento(salvo, aluno);
        }

        @Test
        @DisplayName("RN 01 - trava os envolvidos (por id) antes de checar o horario")
        void travaOsEnvolvidosAntesDeChecar() {
            service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras));

            InOrder ordem = inOrder(usuarioRepository, calculadoraDeAgenda);
            ordem.verify(usuarioRepository).travarParaAgendamento(idsTravados(List.of(1L, 9L)));
            ordem.verify(calculadoraDeAgenda).verificar(eq(aluno), any(), any(), any(), isNull());
        }

        @Test
        @DisplayName("RN 01 - horario ocupado por outra reuniao de um envolvido: recusa e nao grava")
        void recusaConflito() {
            Agendamento existente = agendamento(50L, professor, aluno, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras);
            when(calculadoraDeAgenda.verificar(eq(professor), any(), any(), any(), any())).thenReturn(Optional.of(
                    new CalculadoraDeAgenda.Impedimento(professor, CalculadoraDeAgenda.TipoImpedimento.REUNIAO_CONFLITANTE,
                            existente)));

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Lucas Barboza").hasMessageContaining("conflitante");
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN 15 - docente fora da disponibilidade: recusa sem revelar o motivo")
        void recusaForaDaDisponibilidade() {
            when(calculadoraDeAgenda.verificar(eq(professor), any(), any(), any(), any())).thenReturn(Optional.of(
                    new CalculadoraDeAgenda.Impedimento(professor, CalculadoraDeAgenda.TipoImpedimento.BLOQUEADO, null)));

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("O professor Lucas Barboza nao esta disponivel neste horario.");
        }

        @Test
        @DisplayName("RNF 15 - aluno nao cria em nome de outra pessoa")
        void alunoNaoCriaPorTerceiros() {
            assertThatThrownBy(() -> service.criar(pedido(outroAluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("RF 13 - recepcao cria em nome do aluno")
        void recepcaoCriaPorTerceiros() {
            logadoComo(recepcionista);
            when(usuarioAutenticado.podeGerirAgendamentosDeTerceiros()).thenReturn(true);

            service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras));

            assertThat(agendamentoSalvo().getOrganizador()).isEqualTo(aluno);
        }

        @Test
        @DisplayName("RN 02/04 - so alunos, sem docente: recusa")
        void exigeDocente() {
            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(outroAluno), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("ALUNO e um PROFESSOR/COORDENADOR");
        }

        @Test
        @DisplayName("RN 03 - recepcionista nao pode ser convidada")
        void recepcaoNaoParticipa() {
            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor, recepcionista), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Ana Recepcao");
        }

        @Test
        @DisplayName("Convidado inativo: recusa")
        void recusaConvidadoInativo() {
            professor.setAtivo(false);

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("RN 16 - mais de 30 dias de antecedencia: recusa")
        void recusaAlemDe30Dias() {
            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), LocalDate.now().plusDays(31), dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("30 dias");
        }

        @Test
        @DisplayName("RN 16 - exatamente 30 dias a frente ainda e aceito")
        void aceita30Dias() {
            service.criar(pedido(aluno, List.of(professor), LocalDate.now().plusDays(30), dezHoras));

            verify(agendamentoRepository).save(any());
        }

        @Test
        @DisplayName("Inicio que ja passou: recusa")
        void recusaInicioNoPassado() {
            LocalDateTime haPouco = LocalDateTime.now().minusMinutes(5);

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), haPouco.toLocalDate(),
                    haPouco.toLocalTime())))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("RN 17 - duracao acima de 60 minutos: recusa")
        void recusaDuracaoLonga() {
            when(duracaoReuniaoRepository.findById(7L)).thenReturn(Optional.of(duracao(7L, 90)));
            AgendamentoRequestDTO dto = new AgendamentoRequestDTO(aluno.getId(), List.of(professor.getId()), 7L,
                    semanaQueVem, dezHoras, FormatoReuniao.PRESENCIAL, "TCC", null, null, null);

            assertThatThrownBy(() -> service.criar(dto))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("60 minutos");
        }

        @Test
        @DisplayName("Duracao desativada: recusa")
        void recusaDuracaoInativa() {
            trintaMinutos.setAtivo(false);

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nao esta mais ativa");
        }

        @Test
        @DisplayName("RN 13 - aluno com 2 agendamentos ativos nas ultimas 24 h: recusa o terceiro")
        void limitaAlunoA2Em24h() {
            when(agendamentoRepository.contarAgendamentosDoAlunoDesde(eq(aluno.getId()), any())).thenReturn(2L);

            assertThatThrownBy(() -> service.criar(pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Limite de 2");
        }

        @Test
        @DisplayName("RN 13 - o limite nao vale para docente organizador")
        void limiteNaoValeParaDocente() {
            logadoComo(professor);
            when(agendamentoRepository.contarAgendamentosDoAlunoDesde(anyLong(), any())).thenReturn(5L);

            service.criar(pedido(professor, List.of(aluno), semanaQueVem, dezHoras));

            verify(agendamentoRepository).save(any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Editar")
    class Editar {

        @Test
        @DisplayName("RN 14 - reuniao ja confirmada nao e editada diretamente (vai pela remarcacao)")
        void soPendente() {
            Agendamento confirmada = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.atualizar(confirmada.getId(), pedido(aluno, List.of(professor), semanaQueVem,
                    LocalTime.of(11, 0))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("remarcacao");
        }

        @Test
        @DisplayName("RN 14 - menos de 24 h antes: nao edita")
        void exige24Horas() {
            LocalDateTime amanhaCedo = LocalDateTime.now().plusHours(20);
            Agendamento pendente = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE,
                    amanhaCedo.toLocalDate(), amanhaCedo.toLocalTime()));

            assertThatThrownBy(() -> service.atualizar(pendente.getId(), pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("24 hora");
        }

        @Test
        @DisplayName("Edicao valida: trava a reuniao, ignora ela mesma no conflito e reabre os convites")
        void editaPendente() {
            Agendamento pendente = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));
            pendente.getParticipantes().get(1).setStatusResposta(StatusResposta.ACEITO);

            service.atualizar(pendente.getId(), pedido(aluno, List.of(professor), semanaQueVem, LocalTime.of(11, 0)));

            assertThat(pendente.getHorarioInicio()).isEqualTo(LocalTime.of(11, 0));
            assertThat(pendente.getParticipantes()).filteredOn(p -> p.getPapel() == PapelParticipante.CONVIDADO)
                    .singleElement().extracting(AgendamentoParticipante::getStatusResposta).isEqualTo(StatusResposta.PENDENTE);
            verify(agendamentoRepository).travarPorId(10L);
            verify(calculadoraDeAgenda).verificar(eq(professor), any(), any(), any(), eq(10L));
            verify(notificador).alteracao(pendente, aluno);
        }

        @Test
        @DisplayName("Convidado nao edita a reuniao de outra pessoa")
        void soOrganizadorEdita() {
            Agendamento pendente = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));
            logadoComo(professor);

            assertThatThrownBy(() -> service.atualizar(pendente.getId(), pedido(aluno, List.of(professor), semanaQueVem, dezHoras)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("Cancelamento valido: CANCELADO, motivo registrado (RF 11) e aviso aos envolvidos")
        void cancela() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));

            service.cancelar(reuniao.getId(), new CancelamentoRequestDTO(aluno.getId(), "Imprevisto", true));

            assertThat(reuniao.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
            RegistroMotivo motivo = motivoRegistrado();
            assertThat(motivo.getTipoOperacao()).isEqualTo(TipoOperacaoMotivo.CANCELAMENTO);
            assertThat(motivo.getJustificativa()).isEqualTo("Imprevisto");
            verify(agendamentoRepository).travarPorId(10L);
            verify(notificador).cancelamento(reuniao, aluno);
        }

        @Test
        @DisplayName("RN 05 - menos de 1 h antes: nao cancela")
        void exigeUmaHora() {
            LocalDateTime daquiAMeiaHora = LocalDateTime.now().plusMinutes(30);
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO,
                    daquiAMeiaHora.toLocalDate(), daquiAMeiaHora.toLocalTime()));

            assertThatThrownBy(() -> service.cancelar(reuniao.getId(), new CancelamentoRequestDTO(aluno.getId(), "x", true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("1 hora");
            assertThat(reuniao.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("RNF 10 - sem confirmacao explicita: nao cancela")
        void exigeConfirmacao() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.cancelar(reuniao.getId(), new CancelamentoRequestDTO(aluno.getId(), "x", false)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("confirmar");
        }

        @Test
        @DisplayName("Reuniao ja concluida nao e cancelada")
        void naoCancelaConcluida() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONCLUIDO, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.cancelar(reuniao.getId(), new CancelamentoRequestDTO(aluno.getId(), "x", true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("CONCLUIDO");
        }

        @Test
        @DisplayName("RNF 15 - idUsuario do corpo diferente do logado: recusa")
        void soEmNomeProprio() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.cancelar(reuniao.getId(), new CancelamentoRequestDTO(professor.getId(), "x", true)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Remarcar (RF 28)")
    class Remarcar {

        @Test
        @DisplayName("Cancela a original com o motivo e cria a nova ligada a ela, sem aviso de cancelamento")
        void remarca() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));

            service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(10, 15), true));

            assertThat(original.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
            assertThat(motivoRegistrado().getJustificativa()).isEqualTo(
                    "Remarcada para %s às 10:15: Conflito com prova".formatted(
                            semanaQueVem.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

            Agendamento nova = agendamentoSalvoDiferenteDe(original);
            assertThat(nova.getAgendamentoOrigem()).isSameAs(original);
            assertThat(nova.getOrganizador()).isEqualTo(aluno);
            assertThat(nova.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
            assertThat(nova.getHorarioInicio()).isEqualTo(LocalTime.of(10, 15));

            verify(notificador).novoAgendamento(nova, aluno);
            verify(notificador, never()).cancelamento(any(), any());
        }

        @Test
        @DisplayName("A original nao conta como conflito para a nova (pode sobrepor o horario antigo)")
        void ignoraAOriginalNoConflito() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(10, 15), true));

            verify(calculadoraDeAgenda).verificar(eq(aluno), any(), any(), any(), eq(10L));
            verify(calculadoraDeAgenda).verificar(eq(professor), any(), any(), any(), eq(10L));
        }

        @Test
        @DisplayName("Trava a reuniao original antes dos usuarios (mesma ordem em todas as operacoes: sem deadlock)")
        void ordemDasTravas() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(11, 0), true));

            InOrder ordem = inOrder(agendamentoRepository, usuarioRepository);
            ordem.verify(agendamentoRepository).travarPorId(10L);
            ordem.verify(usuarioRepository).travarParaAgendamento(idsTravados(List.of(1L, 9L)));
        }

        @Test
        @DisplayName("Recepcao remarca pela pessoa; a nova reuniao continua com o organizador original")
        void recepcaoRemarca() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));
            logadoComo(recepcionista);
            when(usuarioAutenticado.podeGerirAgendamentosDeTerceiros()).thenReturn(true);

            service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(11, 0), true));

            assertThat(agendamentoSalvoDiferenteDe(original).getOrganizador()).isEqualTo(aluno);
            assertThat(motivoRegistrado().getUsuario()).isEqualTo(recepcionista);
        }

        @Test
        @DisplayName("Convidado nao remarca: so o organizador (ou a recepcao)")
        void convidadoNaoRemarca() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));
            logadoComo(professor);

            assertThatThrownBy(() -> service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(11, 0), true)))
                    .isInstanceOf(AcessoNegadoNegocioException.class);
            assertThat(original.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reuniao ja cancelada (ex.: segunda remarcacao simultanea) nao e remarcada de novo")
        void naoRemarcaCancelada() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CANCELADO, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(11, 0), true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("CANCELADO");
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN 05 - menos de 1 h antes da original: nao remarca")
        void exigeUmaHora() {
            LocalDateTime daquiAMeiaHora = LocalDateTime.now().plusMinutes(30);
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO,
                    daquiAMeiaHora.toLocalDate(), daquiAMeiaHora.toLocalTime()));

            assertThatThrownBy(() -> service.remarcar(original.getId(), remarcacao(semanaQueVem, dezHoras, true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("remarcado").hasMessageContaining("1 hora");
            assertThat(original.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("RNF 10 - sem confirmacao explicita: nao remarca")
        void exigeConfirmacao() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(11, 0), false)))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThat(original.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
        }

        @Test
        @DisplayName("Nova reuniao com conflito: a excecao sobe (e a transacao desfaz o cancelamento da original)")
        void novaComConflitoFalha() {
            Agendamento original = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CONFIRMADO, semanaQueVem, dezHoras));
            when(calculadoraDeAgenda.verificar(eq(professor), any(), any(), any(), any())).thenReturn(Optional.of(
                    new CalculadoraDeAgenda.Impedimento(professor, CalculadoraDeAgenda.TipoImpedimento.FORA_DA_DISPONIBILIDADE,
                            null)));

            assertThatThrownBy(() -> service.remarcar(original.getId(), remarcacao(semanaQueVem, LocalTime.of(19, 0), true)))
                    .isInstanceOf(RegraDeNegocioException.class);
            verify(notificador, never()).novoAgendamento(any(), any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Responder (RF 19)")
    class Responder {

        @Test
        @DisplayName("Organizador nao responde ao proprio convite")
        void organizadorNaoResponde() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));

            assertThatThrownBy(() -> service.responder(reuniao.getId(),
                    new RespostaAgendamentoRequestDTO(aluno.getId(), true, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("organizador");
        }

        @Test
        @DisplayName("RF 11 - recusar sem motivo: recusa")
        void recusaExigeMotivo() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));
            logadoComo(professor);

            assertThatThrownBy(() -> service.responder(reuniao.getId(),
                    new RespostaAgendamentoRequestDTO(professor.getId(), false, " ")))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("RN 18 - professor aceita: CONFIRMADO")
        void aceitarConfirma() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));
            logadoComo(professor);

            service.responder(reuniao.getId(), new RespostaAgendamentoRequestDTO(professor.getId(), true, null));

            assertThat(reuniao.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("RN 18 - o unico professor recusa: CANCELADO, com o motivo registrado")
        void recusarCancela() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.PENDENTE, semanaQueVem, dezHoras));
            logadoComo(professor);

            service.responder(reuniao.getId(), new RespostaAgendamentoRequestDTO(professor.getId(), false, "Viagem"));

            assertThat(reuniao.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
            assertThat(motivoRegistrado().getTipoOperacao()).isEqualTo(TipoOperacaoMotivo.RECUSA_AGENDAMENTO);
        }

        @Test
        @DisplayName("Reuniao cancelada nao volta a mudar por uma resposta")
        void naoRespondeCancelada() {
            Agendamento reuniao = noBanco(agendamento(10L, aluno, professor, StatusAgendamento.CANCELADO, semanaQueVem, dezHoras));
            logadoComo(professor);

            assertThatThrownBy(() -> service.responder(reuniao.getId(),
                    new RespostaAgendamentoRequestDTO(professor.getId(), true, null)))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThat(reuniao.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }
    }

    // ------------------------------------------------------------------

    private void logadoComo(Usuario usuario) {
        when(usuarioAutenticado.obter()).thenReturn(usuario);
        when(usuarioAutenticado.id()).thenReturn(usuario.getId());
        when(usuarioAutenticado.podeGerirAgendamentosDeTerceiros()).thenReturn(false);
    }

    private AgendamentoRequestDTO pedido(Usuario organizador, List<Usuario> convidados, LocalDate data, LocalTime inicio) {
        return new AgendamentoRequestDTO(organizador.getId(), convidados.stream().map(Usuario::getId).toList(),
                trintaMinutos.getId(), data, inicio, FormatoReuniao.PRESENCIAL, "Orientacao do TCC", null, null, null);
    }

    private RemarcacaoRequestDTO remarcacao(LocalDate data, LocalTime inicio, boolean confirmar) {
        return new RemarcacaoRequestDTO(List.of(professor.getId()), trintaMinutos.getId(), data, inicio,
                FormatoReuniao.PRESENCIAL, "Orientacao do TCC", null, null, "Conflito com prova", confirmar);
    }

    /** Reuniao entre organizador e um convidado (que ainda nao respondeu), devolvida pelo repositorio. */
    private static Agendamento agendamento(Long id, Usuario organizador, Usuario convidado, StatusAgendamento status,
                                           LocalDate data, LocalTime inicio) {
        Agendamento a = Agendamento.builder()
                .id(id).organizador(organizador).duracao(duracao(4L, 30)).dataReuniao(data).horarioInicio(inicio)
                .formato(FormatoReuniao.PRESENCIAL).topico("Orientacao do TCC").status(status).build();
        a.adicionarParticipante(AgendamentoParticipante.builder().usuario(organizador)
                .papel(PapelParticipante.ORGANIZADOR).statusResposta(StatusResposta.ACEITO).build());
        a.adicionarParticipante(AgendamentoParticipante.builder().usuario(convidado)
                .papel(PapelParticipante.CONVIDADO).statusResposta(StatusResposta.PENDENTE).build());
        return a;
    }

    private Agendamento noBanco(Agendamento agendamento) {
        when(agendamentoRepository.findById(agendamento.getId())).thenReturn(Optional.of(agendamento));
        return agendamento;
    }

    private Agendamento agendamentoSalvo() {
        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository).save(captor.capture());
        return captor.getValue();
    }

    private Agendamento agendamentoSalvoDiferenteDe(Agendamento original) {
        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().filter(a -> a != original).findFirst().orElseThrow();
    }

    private RegistroMotivo motivoRegistrado() {
        ArgumentCaptor<RegistroMotivo> captor = ArgumentCaptor.forClass(RegistroMotivo.class);
        verify(registroMotivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Collection<Long> idsTravados(List<Long> ids) {
        return org.mockito.ArgumentMatchers.argThat(c -> c != null && List.copyOf(c).equals(ids));
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
