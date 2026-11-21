package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queries dos repositories rodando no banco de verdade (MySQL ou PostgreSQL, conforme o perfil ativo).
 * Pega o que os testes com Mockito nao pegam: JPQL que um dos bancos recusa, parametros nulos sem tipo
 * (PostgreSQL), SQL nativo das travas (RN 01) e mapeamento divergente do schema.
 *
 * Tag "banco": fica fora do ./mvnw test padrao. Para rodar com o banco local ligado:
 *   ./mvnw test -Dgroups=banco -DexcludedGroups=nenhum
 * Cada teste roda numa transacao desfeita no fim: nao deixa dados no banco.
 */
@Tag("banco")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoriosNoBancoTest {

    private static final Pageable PAGINA = PageRequest.of(0, 50);
    private static final LocalDate DIA = LocalDate.now().plusDays(7);

    @Autowired private TestEntityManager em;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private DisponibilidadeRepository disponibilidadeRepository;
    @Autowired private BloqueioPeriodoRepository bloqueioPeriodoRepository;
    @Autowired private NotificacaoRepository notificacaoRepository;

    private Usuario professor;
    private Usuario aluno;
    private DuracaoReuniao duracao;

    @BeforeEach
    void preparar() {
        professor = usuario("Professor Teste Banco", TipoUsuario.PROFESSOR, null);
        aluno = usuario("Aluno Teste Banco", TipoUsuario.ALUNO, "RA-" + sufixo());
        duracao = em.getEntityManager()
                .createQuery("SELECT d FROM DuracaoReuniao d WHERE d.minutos = 30", DuracaoReuniao.class)
                .getResultStream().findFirst()
                .orElseGet(() -> em.persist(DuracaoReuniao.builder().minutos(30).build()));
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario usuario(String nome, TipoUsuario tipo, String ra) {
        String s = sufixo();
        return em.persist(Usuario.builder()
                .idMicrosoft("teste-banco:" + s)
                .nome(nome)
                .email("teste-banco-" + s + "@exemplo.com")
                .senha("{noop}x")
                .tipoUsuario(tipo)
                .matriculaRa(ra)
                .build());
    }

    /** Reuniao organizada pelo aluno com o professor convidado (participantes como o service grava). */
    private Agendamento reuniao(LocalDate data, String horario, StatusAgendamento status, String topico) {
        Agendamento a = Agendamento.builder()
                .organizador(aluno)
                .duracao(duracao)
                .dataReuniao(data)
                .horarioInicio(LocalTime.parse(horario))
                .formato(FormatoReuniao.PRESENCIAL)
                .topico(topico)
                .status(status)
                .build();
        a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(aluno)
                .papel(PapelParticipante.ORGANIZADOR).statusResposta(StatusResposta.ACEITO).build());
        a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(professor).build());
        return em.persist(a);
    }

    private List<StatusAgendamento> todos() {
        return List.of(StatusAgendamento.values());
    }

    @Nested
    @DisplayName("Disponibilidade")
    class DeDisponibilidade {

        private Disponibilidade janela(int dia, String inicio, String fim, boolean ativa) {
            return em.persist(Disponibilidade.builder().usuario(professor).diaSemana(dia)
                    .horarioInicio(LocalTime.parse(inicio)).horarioFim(LocalTime.parse(fim)).ativo(ativa).build());
        }

        private boolean sobrepoe(int dia, String inicio, String fim, long ignorado) {
            return disponibilidadeRepository.existeSobreposicao(professor.getId(), dia,
                    LocalTime.parse(inicio), LocalTime.parse(fim), ignorado);
        }

        @Test
        @DisplayName("existeSobreposicao: sobreposta sim; encostada, outro dia, inativa ou a propria nao")
        void sobreposicao() {
            Disponibilidade manha = janela(4, "08:00", "12:00", true);
            janela(4, "14:00", "16:00", false);
            em.flush();

            assertThat(sobrepoe(4, "10:00", "13:00", -1)).isTrue();
            assertThat(sobrepoe(4, "07:00", "08:30", -1)).isTrue();
            assertThat(sobrepoe(4, "12:00", "14:00", -1)).isFalse();
            assertThat(sobrepoe(5, "08:00", "12:00", -1)).isFalse();
            assertThat(sobrepoe(4, "14:30", "15:00", -1)).isFalse();
            assertThat(sobrepoe(4, "09:00", "11:00", manha.getId())).isFalse();
        }

        @Test
        @DisplayName("buscarComFiltros aceita todos os filtros nulos")
        void filtrosNulos() {
            janela(2, "08:00", "09:00", true);
            em.flush();

            assertThat(disponibilidadeRepository.buscarComFiltros(null, null, null, PAGINA)).isNotEmpty();
            assertThat(disponibilidadeRepository.buscarComFiltros(professor.getId(), 2, true, PAGINA)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Usuario")
    class DeUsuario {

        @Test
        @DisplayName("buscarComFiltros casa nome ou RA; sem filtro usa \"\" como o service")
        void buscaPorNomeOuRa() {
            em.flush();

            assertThat(usuarioRepository.buscarComFiltros(null, "", null, PAGINA).getContent())
                    .contains(professor, aluno);
            assertThat(usuarioRepository.buscarComFiltros(null, "teste banco", true, PAGINA).getContent())
                    .contains(professor, aluno);
            assertThat(usuarioRepository.buscarComFiltros(TipoUsuario.ALUNO, aluno.getMatriculaRa().toLowerCase(), null, PAGINA)
                    .getContent()).containsExactly(aluno);
        }

        @Test
        @DisplayName("travarParaAgendamento (SELECT ... FOR UPDATE nativo) roda no banco")
        void trava() {
            em.flush();

            assertThat(usuarioRepository.travarParaAgendamento(List.of(professor.getId(), aluno.getId()))).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Agendamento")
    class DeAgendamento {

        @Test
        @DisplayName("travarPorId (SELECT ... FOR UPDATE nativo) roda no banco")
        void trava() {
            Agendamento a = reuniao(DIA, "10:00", StatusAgendamento.PENDENTE, "Trava");
            em.flush();

            assertThat(agendamentoRepository.travarPorId(a.getId())).hasSize(1);
        }

        @Test
        @DisplayName("buscarPorUsuarioComFiltros e buscarComFiltros aceitam datas nulas e busca vazia")
        void listagens() {
            Agendamento a = reuniao(DIA, "10:00", StatusAgendamento.PENDENTE, "Orientacao de TCC");
            em.flush();

            assertThat(agendamentoRepository.buscarPorUsuarioComFiltros(professor.getId(), todos(), null, null, "", PAGINA)
                    .getContent()).containsExactly(a);
            assertThat(agendamentoRepository.buscarPorUsuarioComFiltros(professor.getId(), todos(), DIA, DIA, "tcc", PAGINA)
                    .getContent()).containsExactly(a);
            // busca pelo nome de um envolvido
            assertThat(agendamentoRepository.buscarPorUsuarioComFiltros(aluno.getId(), todos(), null, null, "professor teste", PAGINA)
                    .getContent()).containsExactly(a);
            assertThat(agendamentoRepository.buscarComFiltros(List.of(StatusAgendamento.PENDENTE), null, null, "", PAGINA)
                    .getContent()).contains(a);
            assertThat(agendamentoRepository.buscarComFiltros(todos(), DIA.plusDays(1), null, "", PAGINA)
                    .getContent()).doesNotContain(a);
        }

        @Test
        @DisplayName("buscarAgendamentosAtivosDoUsuarioNaData ignora canceladas e a reuniao informada")
        void ativosNaData() {
            Agendamento ativa = reuniao(DIA, "10:00", StatusAgendamento.CONFIRMADO, "Ativa");
            reuniao(DIA, "11:00", StatusAgendamento.CANCELADO, "Cancelada");
            em.flush();

            assertThat(agendamentoRepository.buscarAgendamentosAtivosDoUsuarioNaData(professor.getId(), DIA, null))
                    .containsExactly(ativa);
            assertThat(agendamentoRepository.buscarAgendamentosAtivosDoUsuarioNaData(professor.getId(), DIA, ativa.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("RF 06 - buscarDoParticipanteNoPeriodo, buscarIdsDoParticipanteDesde e contarAgendamentosDoAlunoDesde")
        void afetadasEContagem() {
            List<StatusAgendamento> afetaveis = List.of(StatusAgendamento.PENDENTE,
                    StatusAgendamento.AGUARDANDO_RESPOSTA, StatusAgendamento.CONFIRMADO);
            Agendamento depois = reuniao(DIA, "11:00", StatusAgendamento.CONFIRMADO, "Confirmada");
            Agendamento antes = reuniao(DIA, "10:00", StatusAgendamento.PENDENTE, "Pendente");
            reuniao(DIA, "12:00", StatusAgendamento.CANCELADO, "Cancelada");
            reuniao(DIA, "13:00", StatusAgendamento.EM_ANDAMENTO, "Em andamento");
            Agendamento seguinte = reuniao(DIA.plusDays(2), "09:00", StatusAgendamento.PENDENTE, "Outro dia");
            em.flush();

            // ordenadas por data e horario; o professor (convidado) e o aluno (organizador) enxergam as mesmas
            assertThat(agendamentoRepository.buscarDoParticipanteNoPeriodo(professor.getId(), DIA.minusDays(1), DIA.plusDays(1), afetaveis))
                    .containsExactly(antes, depois);
            assertThat(agendamentoRepository.buscarDoParticipanteNoPeriodo(aluno.getId(), DIA, DIA.plusDays(2), afetaveis))
                    .containsExactly(antes, depois, seguinte);
            assertThat(agendamentoRepository.buscarIdsDoParticipanteDesde(professor.getId(), DIA, afetaveis))
                    .containsExactly(depois.getId(), antes.getId(), seguinte.getId());
            assertThat(agendamentoRepository.buscarIdsDoParticipanteDesde(professor.getId(), DIA.plusDays(1), afetaveis))
                    .containsExactly(seguinte.getId());
            assertThat(agendamentoRepository.contarAgendamentosDoAlunoDesde(aluno.getId(), LocalDateTime.now().minusHours(24)))
                    .isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Bloqueio")
    class DeBloqueio {

        @Test
        @DisplayName("buscarQueImpactamData respeita a recorrencia; buscarComFiltros aceita nulos")
        void bloqueios() {
            BloqueioPeriodo unico = em.persist(BloqueioPeriodo.builder().usuario(professor)
                    .dataInicio(DIA).dataFim(DIA).motivo("Congresso").build());
            BloqueioPeriodo recorrente = em.persist(BloqueioPeriodo.builder().usuario(professor)
                    .dataInicio(DIA.minusDays(10)).dataFim(DIA.plusDays(10))
                    .eRecorrente(true).diaSemanaRecorrente(3).build());
            em.flush();

            assertThat(bloqueioPeriodoRepository.buscarQueImpactamData(professor.getId(), DIA, 3))
                    .containsExactlyInAnyOrder(unico, recorrente);
            assertThat(bloqueioPeriodoRepository.buscarQueImpactamData(professor.getId(), DIA, 4))
                    .containsExactly(unico);
            assertThat(bloqueioPeriodoRepository.buscarComFiltros(null, null, null, PAGINA)).isNotEmpty();
            assertThat(bloqueioPeriodoRepository.buscarComFiltros(professor.getId(), DIA, DIA, PAGINA)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Notificacao")
    class DeNotificacao {

        private Notificacao aviso(Usuario usuario, Agendamento agendamento, CanalEnvio canal) {
            return em.persist(Notificacao.builder().usuario(usuario).agendamento(agendamento).canalEnvio(canal)
                    .tipoEvento("CONVITE").mensagem("Teste banco").build());
        }

        @Test
        @DisplayName("buscarComFiltros separa os canais; fila de e-mail traz destinatario e reuniao; expirar so pega EMAIL antigo")
        void filaDeEmail() {
            Agendamento a = reuniao(DIA, "10:00", StatusAgendamento.PENDENTE, "Notificacao banco");
            Notificacao sistema = aviso(professor, a, CanalEnvio.SISTEMA);
            Notificacao email = aviso(professor, a, CanalEnvio.EMAIL);
            Notificacao semReuniao = aviso(aluno, null, CanalEnvio.EMAIL);
            em.flush();

            assertThat(notificacaoRepository.buscarComFiltros(professor.getId(), CanalEnvio.SISTEMA, null, PAGINA))
                    .containsExactly(sistema);
            assertThat(notificacaoRepository.buscarComFiltros(professor.getId(), CanalEnvio.EMAIL, StatusEnvio.PENDENTE, PAGINA))
                    .containsExactly(email);
            assertThat(notificacaoRepository.buscarComFiltros(null, CanalEnvio.EMAIL, null, PAGINA))
                    .contains(email, semReuniao).doesNotContain(sistema);

            assertThat(notificacaoRepository.buscarEmailsPendentes(PageRequest.of(0, 1000)))
                    .contains(email, semReuniao).doesNotContain(sistema);

            // criado_em nao e atualizavel pela entidade: envelhece direto no banco.
            em.getEntityManager().createNativeQuery("UPDATE notificacao SET criado_em = :velho WHERE id_notificacao IN (:ids)")
                    .setParameter("velho", LocalDateTime.now().minusDays(3))
                    .setParameter("ids", List.of(sistema.getId(), email.getId()))
                    .executeUpdate();
            em.clear();
            assertThat(notificacaoRepository.expirarEmailsPendentes(LocalDateTime.now().minusDays(1))).isPositive();
            assertThat(notificacaoRepository.findById(email.getId())).get()
                    .extracting(Notificacao::getStatusEnvio).isEqualTo(StatusEnvio.FALHA);
            assertThat(notificacaoRepository.findById(sistema.getId())).get()
                    .extracting(Notificacao::getStatusEnvio).isEqualTo(StatusEnvio.PENDENTE);
            assertThat(notificacaoRepository.findById(semReuniao.getId())).get()
                    .extracting(Notificacao::getStatusEnvio).isEqualTo(StatusEnvio.PENDENTE);
        }
    }
}
