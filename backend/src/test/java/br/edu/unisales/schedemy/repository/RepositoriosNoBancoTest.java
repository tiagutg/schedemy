package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.AvaliacaoReuniao;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.ConsentimentoTermo;
import br.edu.unisales.schedemy.domain.entity.LogAuditoria;
import br.edu.unisales.schedemy.domain.entity.SolicitacaoEdicao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Autowired private SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;
    @Autowired private ConsentimentoTermoRepository consentimentoTermoRepository;
    @Autowired private AvaliacaoReuniaoRepository avaliacaoReuniaoRepository;

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
    @DisplayName("Pedido de alteracao (RF 21/22)")
    class DeSolicitacao {

        private SolicitacaoEdicao pedido(Agendamento a, StatusSolicitacao status) {
            Set<Usuario> convidados = new LinkedHashSet<>(List.of(professor));
            return em.persist(SolicitacaoEdicao.builder().agendamento(a).solicitante(aluno).novaDuracao(duracao)
                    .novaData(DIA.plusDays(1)).novoHorarioInicio(LocalTime.of(14, 0)).novoFormato(FormatoReuniao.ONLINE)
                    .novoTopico("Novo topico").motivo("Prova").status(status).novosConvidados(convidados).build());
        }

        @Test
        @DisplayName("entidade x schema (convidados propostos), consultas do pedido e encerrarPendentes")
        void pedidos() {
            Agendamento a = reuniao(DIA, "10:00", StatusAgendamento.AGUARDANDO_RESPOSTA, "Com pedido");
            SolicitacaoEdicao antigo = pedido(a, StatusSolicitacao.RECUSADA);
            SolicitacaoEdicao atual = pedido(a, StatusSolicitacao.PENDENTE);
            em.flush();
            em.clear();

            assertThat(solicitacaoEdicaoRepository.buscarIdDoAgendamento(atual.getId())).contains(a.getId());
            assertThat(solicitacaoEdicaoRepository.existsByAgendamentoIdAndStatus(a.getId(), StatusSolicitacao.PENDENTE)).isTrue();
            SolicitacaoEdicao lido = solicitacaoEdicaoRepository
                    .findFirstByAgendamentoIdAndStatus(a.getId(), StatusSolicitacao.PENDENTE).orElseThrow();
            assertThat(lido.getId()).isEqualTo(atual.getId());
            assertThat(lido.getNovosConvidados()).extracting(Usuario::getId).containsExactly(professor.getId());
            assertThat(solicitacaoEdicaoRepository.findByAgendamentoIdOrderByCriadoEmDesc(a.getId()))
                    .extracting(SolicitacaoEdicao::getId).containsExactlyInAnyOrder(antigo.getId(), atual.getId());

            assertThat(solicitacaoEdicaoRepository.encerrarPendentes(a.getId(), StatusSolicitacao.CANCELADA,
                    LocalDateTime.now())).isEqualTo(1);
            em.clear();
            assertThat(solicitacaoEdicaoRepository.findById(atual.getId()).orElseThrow().getStatus())
                    .isEqualTo(StatusSolicitacao.CANCELADA);
            assertThat(solicitacaoEdicaoRepository.findById(antigo.getId()).orElseThrow().getStatus())
                    .isEqualTo(StatusSolicitacao.RECUSADA);
        }

        @Test
        @DisplayName("o schema aceita os valores novos: EXPIRADA e RECUSA_SOLICITACAO_EDICAO")
        void valoresNovosDoSchema() {
            Agendamento a = reuniao(DIA, "11:00", StatusAgendamento.CONFIRMADO, "Expirado");
            pedido(a, StatusSolicitacao.EXPIRADA);
            em.persist(RegistroMotivo.builder().agendamento(a).usuario(professor)
                    .tipoOperacao(TipoOperacaoMotivo.RECUSA_SOLICITACAO_EDICAO).justificativa("Banca").build());
            em.flush();
        }

        @Test
        @DisplayName("quem respondeu e motivo da recusa (colunas novas); listarNoEscopo por participante, gestor e status")
        void listaNoEscopo() {
            Usuario outroProfessor = usuario("Outro Professor Banco", TipoUsuario.PROFESSOR, null);
            Agendamento a = reuniao(DIA, "13:00", StatusAgendamento.AGUARDANDO_RESPOSTA, "Lista A");
            Agendamento depois = reuniao(DIA.plusDays(2), "09:00", StatusAgendamento.AGUARDANDO_RESPOSTA, "Lista B");
            SolicitacaoEdicao recusado = pedido(a, StatusSolicitacao.RECUSADA);
            recusado.setRespondidoPor(professor);
            recusado.setMotivoRecusa("Tenho banca");
            SolicitacaoEdicao pendenteA = pedido(a, StatusSolicitacao.PENDENTE);
            SolicitacaoEdicao pendenteB = pedido(depois, StatusSolicitacao.PENDENTE);
            em.flush();
            em.clear();

            SolicitacaoEdicao lido = solicitacaoEdicaoRepository.findById(recusado.getId()).orElseThrow();
            assertThat(lido.getRespondidoPor().getId()).isEqualTo(professor.getId());
            assertThat(lido.getMotivoRecusa()).isEqualTo("Tenho banca");

            var pendentes = List.of(StatusSolicitacao.PENDENTE);
            var pagina = PageRequest.of(0, 50);
            // Participante (convidado): so os pendentes das proprias reunioes, a mais proxima primeiro.
            assertThat(solicitacaoEdicaoRepository.listarNoEscopo(pendentes, false, professor.getId(), pagina).getContent())
                    .extracting(SolicitacaoEdicao::getId).containsExactly(pendenteA.getId(), pendenteB.getId());
            // Organizador tambem ve; sem filtro de status, inclui o recusado.
            assertThat(solicitacaoEdicaoRepository.listarNoEscopo(List.of(StatusSolicitacao.values()), false,
                    aluno.getId(), pagina).getContent())
                    .extracting(SolicitacaoEdicao::getId)
                    .containsExactlyInAnyOrder(recusado.getId(), pendenteA.getId(), pendenteB.getId());
            // Quem nao participa nao ve nada; a recepcao/admin ve todos (o banco pode ter outros pedidos).
            assertThat(solicitacaoEdicaoRepository.listarNoEscopo(pendentes, false, outroProfessor.getId(), pagina))
                    .isEmpty();
            var gestor = solicitacaoEdicaoRepository.listarNoEscopo(pendentes, true, outroProfessor.getId(), pagina);
            assertThat(gestor.getContent()).extracting(SolicitacaoEdicao::getId)
                    .contains(pendenteA.getId(), pendenteB.getId())
                    .doesNotContain(recusado.getId());
            assertThat(gestor.getTotalElements()).isGreaterThanOrEqualTo(2);
            // Os ManyToOne vieram no JOIN FETCH: o mapper le sem consultas extras.
            assertThat(gestor.getContent().getFirst().getAgendamento().getTopico()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Auditoria e LGPD (RNF 18/19)")
    class DeAuditoria {

        private LogAuditoria log(Usuario autor, AcaoAuditoria acao, Long idRegistro) {
            return em.persist(LogAuditoria.builder().usuario(autor).acao(acao).tabelaAfetada(acao.getTabela())
                    .idTabelaAfetada(idRegistro).detalhes("teste banco").ipOrigem("127.0.0.1").build());
        }

        @Test
        @DisplayName("linha sem autor (sistema); buscarComFiltros com todos os filtros nulos e com cada filtro")
        void trilha() {
            long idReuniao = 990_000L + (long) (Math.random() * 1000);
            LogAuditoria doAluno = log(aluno, AcaoAuditoria.AGENDAMENTO_CRIADO, idReuniao);
            LogAuditoria doSistema = log(null, AcaoAuditoria.AGENDAMENTO_EXPIRADO, idReuniao);
            LogAuditoria login = log(professor, AcaoAuditoria.LOGIN, professor.getId());
            em.flush();
            em.clear();

            var todas = List.of(AcaoAuditoria.values());
            var pagina = PageRequest.of(0, 500);
            // Todos os opcionais nulos/vazios: o PostgreSQL precisa aceitar os parametros sem tipo.
            assertThat(logAuditoriaRepository.buscarComFiltros(null, false, null, todas, "", null, null, null, pagina)
                    .getContent()).extracting(LogAuditoria::getId)
                    .contains(doAluno.getId(), doSistema.getId(), login.getId());
            // Por registro: a reuniao, da mais recente para a mais antiga, com o autor ja carregado.
            var daReuniao = logAuditoriaRepository.buscarComFiltros(null, false, null, todas, "agendamento", idReuniao,
                    null, null, pagina).getContent();
            assertThat(daReuniao).extracting(LogAuditoria::getId).containsExactly(doSistema.getId(), doAluno.getId());
            assertThat(daReuniao.get(1).getUsuario().getNome()).isEqualTo("Aluno Teste Banco");
            assertThat(daReuniao.get(0).getUsuario()).isNull();
            // Por autor e por acao.
            assertThat(logAuditoriaRepository.buscarComFiltros(professor.getId(), false, null, List.of(AcaoAuditoria.LOGIN), "",
                    null, null, null, pagina).getContent()).extracting(LogAuditoria::getId).containsExactly(login.getId());
            // So o sistema.
            assertThat(logAuditoriaRepository.buscarComFiltros(null, true, null, todas, "agendamento", idReuniao, null, null,
                    pagina).getContent()).extracting(LogAuditoria::getId).containsExactly(doSistema.getId());
            // Por perfil do autor: as acoes automaticas (sem autor) ficam de fora.
            assertThat(logAuditoriaRepository.buscarComFiltros(null, false, TipoUsuario.ALUNO, todas, "agendamento",
                    idReuniao, null, null, pagina).getContent()).extracting(LogAuditoria::getId)
                    .containsExactly(doAluno.getId());
            assertThat(logAuditoriaRepository.buscarComFiltros(null, false, TipoUsuario.PROFESSOR, todas, "",
                    null, null, null, pagina).getContent()).extracting(LogAuditoria::getId)
                    .contains(login.getId()).doesNotContain(doAluno.getId(), doSistema.getId());
            // Periodo [desde, ate): hoje entra, amanha em diante nao.
            LocalDateTime hoje = LocalDate.now().atStartOfDay();
            assertThat(logAuditoriaRepository.buscarComFiltros(null, false, null, todas, "agendamento", idReuniao,
                    hoje, hoje.plusDays(1), pagina).getTotalElements()).isEqualTo(2);
            assertThat(logAuditoriaRepository.buscarComFiltros(null, false, null, todas, "agendamento", idReuniao,
                    hoje.plusDays(1), null, pagina).getTotalElements()).isZero();

            assertThat(logAuditoriaRepository.existsByUsuarioId(aluno.getId())).isTrue();
            assertThat(logAuditoriaRepository.existsByUsuarioId(usuario("Sem Trilha", TipoUsuario.ALUNO, null).getId())).isFalse();
        }

        @Test
        @DisplayName("consentimento por versao: existe, ultimo aceite e unico por usuario e versao")
        void consentimento() {
            em.persist(ConsentimentoTermo.builder().usuario(aluno).versaoTermo("2026-01-01").ipOrigem("10.0.0.1").build());
            ConsentimentoTermo atual = em.persist(ConsentimentoTermo.builder().usuario(aluno).versaoTermo("2026-11-21").build());
            em.flush();
            em.clear();

            assertThat(consentimentoTermoRepository.existsByUsuarioIdAndVersaoTermo(aluno.getId(), "2026-11-21")).isTrue();
            assertThat(consentimentoTermoRepository.existsByUsuarioIdAndVersaoTermo(professor.getId(), "2026-11-21")).isFalse();
            assertThat(consentimentoTermoRepository.findFirstByUsuarioIdOrderByAceitoEmDescIdDesc(aluno.getId()))
                    .get().extracting(ConsentimentoTermo::getId).isEqualTo(atual.getId());

            // Com id IDENTITY o insert sai no persist; o flush cobre o caso de o provedor adiar.
            assertThatThrownBy(() -> {
                em.persist(ConsentimentoTermo.builder().usuario(aluno).versaoTermo("2026-11-21").build());
                em.flush();
            }).isInstanceOf(jakarta.persistence.PersistenceException.class);
        }
    }

    @Nested
    @DisplayName("Bloqueio")
    class DeBloqueio {

        @Test
        @DisplayName("buscarQueImpactamData respeita a recorrencia e o recorrente sem fim; buscarComFiltros aceita nulos")
        void bloqueios() {
            BloqueioPeriodo unico = em.persist(BloqueioPeriodo.builder().usuario(professor)
                    .dataInicio(DIA).dataFim(DIA).motivo("Congresso").build());
            BloqueioPeriodo recorrente = em.persist(BloqueioPeriodo.builder().usuario(professor)
                    .dataInicio(DIA.minusDays(10)).dataFim(DIA.plusDays(10))
                    .eRecorrente(true).diaSemanaRecorrente(3).build());
            // RF 05 - recorrente sem data de fim (data_fim nula)
            BloqueioPeriodo semFim = em.persist(BloqueioPeriodo.builder().usuario(professor)
                    .dataInicio(DIA.minusDays(30)).eRecorrente(true).diaSemanaRecorrente(3).build());
            em.flush();

            assertThat(bloqueioPeriodoRepository.buscarQueImpactamData(professor.getId(), DIA, 3))
                    .containsExactlyInAnyOrder(unico, recorrente, semFim);
            assertThat(bloqueioPeriodoRepository.buscarQueImpactamData(professor.getId(), DIA.plusYears(3), 3))
                    .containsExactly(semFim);
            assertThat(bloqueioPeriodoRepository.buscarQueImpactamData(professor.getId(), DIA, 4))
                    .containsExactly(unico);
            assertThat(bloqueioPeriodoRepository.buscarComFiltros(null, null, null, PAGINA)).isNotEmpty();
            assertThat(bloqueioPeriodoRepository.buscarComFiltros(professor.getId(), DIA, DIA, PAGINA)).hasSize(3);
            assertThat(bloqueioPeriodoRepository.buscarComFiltros(professor.getId(), DIA.plusYears(3), null, PAGINA))
                    .containsExactly(semFim);
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

    @Nested
    @DisplayName("Avaliacao")
    class DeAvaliacao {

        @Test
        @DisplayName("buscarEmReunioesDoParticipante traz so as avaliacoes das reunioes de que o docente participou")
        void doParticipante() {
            Agendamento comProfessor = reuniao(DIA, "10:00", StatusAgendamento.CONCLUIDO, "Avaliacao banco");
            Usuario outroProfessor = usuario("Outro Professor Banco", TipoUsuario.PROFESSOR, null);
            AvaliacaoReuniao avaliacao = em.persist(AvaliacaoReuniao.builder().agendamento(comProfessor).aluno(aluno)
                    .nota(5).comentario("Teste banco").build());
            em.flush();

            assertThat(avaliacaoReuniaoRepository.buscarEmReunioesDoParticipante(professor.getId(), PAGINA))
                    .containsExactly(avaliacao);
            assertThat(avaliacaoReuniaoRepository.buscarEmReunioesDoParticipante(outroProfessor.getId(), PAGINA))
                    .isEmpty();
        }
    }
}
