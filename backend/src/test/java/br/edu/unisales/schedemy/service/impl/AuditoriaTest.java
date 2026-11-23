package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.LogAuditoria;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.repository.LogAuditoriaRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RNF 19 - gravacao e consulta da trilha de auditoria (sem Spring). */
class AuditoriaTest {

    private final LogAuditoriaRepository repositorio = mock(LogAuditoriaRepository.class);
    private final UsuarioAutenticado usuarioAutenticado = mock(UsuarioAutenticado.class);
    private final Auditoria auditoria = new Auditoria(repositorio, usuarioAutenticado);

    private final Usuario admin = Usuario.builder().id(14L).nome("ADMIN").tipoUsuario(TipoUsuario.ADMIN).build();

    private LogAuditoria gravado() {
        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("Gravacao")
    class Gravacao {

        @Test
        @DisplayName("autor = usuario logado; tabela vem da acao")
        void autorLogado() {
            when(usuarioAutenticado.obterSeAutenticado()).thenReturn(Optional.of(admin));

            auditoria.registrar(AcaoAuditoria.AGENDAMENTO_CANCELADO, 42L, "  Orientacao - motivo: prova  ");

            LogAuditoria l = gravado();
            assertThat(l.getUsuario()).isSameAs(admin);
            assertThat(l.getAcao()).isEqualTo(AcaoAuditoria.AGENDAMENTO_CANCELADO);
            assertThat(l.getTabelaAfetada()).isEqualTo("agendamento");
            assertThat(l.getIdTabelaAfetada()).isEqualTo(42L);
            assertThat(l.getDetalhes()).isEqualTo("Orientacao - motivo: prova");
            assertThat(l.getIpOrigem()).isNull(); // fora de uma requisicao HTTP
        }

        @Test
        @DisplayName("acao automatica (rotina) fica sem autor")
        void sistema() {
            auditoria.registrarDoSistema(AcaoAuditoria.PEDIDO_ALTERACAO_EXPIRADO, 5L, "Sem resposta");

            LogAuditoria l = gravado();
            assertThat(l.getUsuario()).isNull();
            assertThat(l.getTabelaAfetada()).isEqualTo("solicitacao_edicao");
        }

        @Test
        @DisplayName("detalhes longos sao cortados em 200 caracteres; vazios viram nulo")
        void cortaDetalhes() {
            assertThat(Auditoria.cortar("x".repeat(300), Auditoria.MAX_DETALHES)).hasSize(200).endsWith("...");
            assertThat(Auditoria.cortar("   ", Auditoria.MAX_DETALHES)).isNull();
        }

        @Test
        @DisplayName("resumo da reuniao: topico, data/hora e status")
        void descreveReuniao() {
            Agendamento a = Agendamento.builder().topico("TCC").dataReuniao(LocalDate.of(2026, 12, 1))
                    .horarioInicio(LocalTime.of(10, 0)).status(StatusAgendamento.CONFIRMADO).build();
            assertThat(Auditoria.descrever(a)).isEqualTo("TCC - 01/12/2026 10:00 - CONFIRMADO");
        }

        @Test
        @DisplayName("toda acao tem tabela")
        void todaAcaoTemTabela() {
            for (AcaoAuditoria acao : AcaoAuditoria.values()) {
                assertThat(acao.getTabela()).as(acao.name()).isNotBlank().hasSizeLessThanOrEqualTo(50);
            }
        }
    }

    @Nested
    @DisplayName("Consulta (so admin)")
    class Consulta {

        private final AuditoriaServiceImpl service = new AuditoriaServiceImpl(repositorio);

        @Test
        @DisplayName("sem filtros: todas as acoes, tabela \"\" e sem periodo; o sort da requisicao e ignorado")
        void semFiltros() {
            when(repositorio.buscarComFiltros(any(), anyBoolean(), any(), any(), anyString(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            service.listar(null, null, null, null, null, null, null, null, PageRequest.of(2, 10, Sort.by("acao")));

            verify(repositorio).buscarComFiltros(null, false, null, List.of(AcaoAuditoria.values()), "", null, null, null,
                    PageRequest.of(2, 10));
        }

        @Test
        @DisplayName("periodo por dia, com o fim inclusivo")
        void periodo() {
            when(repositorio.buscarComFiltros(any(), anyBoolean(), any(), any(), anyString(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            service.listar(6L, true, TipoUsuario.PROFESSOR, List.of(AcaoAuditoria.LOGIN), " usuario ", 6L,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), PageRequest.of(0, 50));

            verify(repositorio).buscarComFiltros(6L, true, TipoUsuario.PROFESSOR, List.of(AcaoAuditoria.LOGIN), "usuario", 6L,
                    LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0), PageRequest.of(0, 50));
        }

        @Test
        @DisplayName("data final antes da inicial: 422")
        void periodoInvertido() {
            assertThatThrownBy(() -> service.listar(null, null, null, null, null, null,
                    LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), PageRequest.of(0, 50)))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("linha sem autor sai com autor nulo (o front mostra \"Sistema\")")
        void linhaDoSistema() {
            LogAuditoria l = LogAuditoria.builder().id(1L).acao(AcaoAuditoria.AGENDAMENTO_EXPIRADO)
                    .tabelaAfetada("agendamento").idTabelaAfetada(3L).criadoEm(LocalDateTime.now()).build();
            when(repositorio.buscarComFiltros(any(), anyBoolean(), any(), any(), anyString(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(l)));

            var item = service.listar(null, null, null, null, null, null, null, null, PageRequest.of(0, 50)).getContent().getFirst();

            assertThat(item.idUsuario()).isNull();
            assertThat(item.nomeUsuario()).isNull();
            assertThat(item.idRegistro()).isEqualTo(3L);
        }
    }
}
