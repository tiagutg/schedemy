package br.edu.unisales.schedemy.config;

import br.edu.unisales.schedemy.controller.AgendamentoController;
import br.edu.unisales.schedemy.controller.AvaliacaoController;
import br.edu.unisales.schedemy.controller.NotificacaoController;
import br.edu.unisales.schedemy.controller.RegistroMotivoController;
import br.edu.unisales.schedemy.controller.BloqueioPeriodoController;
import br.edu.unisales.schedemy.controller.DisponibilidadeController;
import br.edu.unisales.schedemy.controller.DuracaoReuniaoController;
import br.edu.unisales.schedemy.controller.UsuarioController;
import br.edu.unisales.schedemy.service.AgendamentoService;
import br.edu.unisales.schedemy.service.AvaliacaoService;
import br.edu.unisales.schedemy.service.NotificacaoService;
import br.edu.unisales.schedemy.service.RegistroMotivoService;
import br.edu.unisales.schedemy.service.BloqueioPeriodoService;
import br.edu.unisales.schedemy.service.DisponibilidadeService;
import br.edu.unisales.schedemy.service.DuracaoReuniaoService;
import br.edu.unisales.schedemy.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Verifica a matriz de autorizacao do SecurityConfig contra a secao 5.3 do
 * Documento de Definicao de Requisitos.
 *
 * O teste olha apenas para o veredito do filtro de seguranca: 403 significa
 * barrado pelo perfil; qualquer outro status (inclusive 404, quando o endpoint
 * ainda nao existe, ou 400, quando falta corpo) significa que a autorizacao
 * deixou passar. Assim a matriz pode ser validada sem banco de dados.
 */
@WebMvcTest(controllers = {
        AgendamentoController.class,
        BloqueioPeriodoController.class,
        DisponibilidadeController.class,
        DuracaoReuniaoController.class,
        UsuarioController.class,
        AvaliacaoController.class,
        NotificacaoController.class,
        RegistroMotivoController.class
})
@Import({ SecurityConfig.class, AcessoNegadoJsonHandler.class, br.edu.unisales.schedemy.exception.GlobalExceptionHandler.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AgendamentoService agendamentoService;
    @MockitoBean private BloqueioPeriodoService bloqueioPeriodoService;
    @MockitoBean private DisponibilidadeService disponibilidadeService;
    @MockitoBean private DuracaoReuniaoService duracaoReuniaoService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private AvaliacaoService avaliacaoService;
    @MockitoBean private NotificacaoService notificacaoService;
    @MockitoBean private RegistroMotivoService registroMotivoService;

    private static final String SENHA_POR_CONTA = "123";

    private int statusDe(String conta, String metodo, String caminho) throws Exception {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), caminho)
                .with(httpBasic(conta, conta + SENHA_POR_CONTA));
        return mockMvc.perform(req).andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1} {2} -> permitido")
    @DisplayName("Acessos que cada perfil deve ter")
    @CsvSource({
            // O login valida a credencial consultando os usuarios: todos precisam ler.
            "aluno,         GET,    /api/v1/usuarios",
            "professor,     GET,    /api/v1/usuarios",
            "recepcionista, GET,    /api/v1/usuarios",
            "coordenador,   GET,    /api/v1/usuarios",
            "admin,         GET,    /api/v1/usuarios",

            // Agendamentos: RF 13, 15, 20 valem para todos os perfis.
            "aluno,         GET,    /api/v1/agendamentos",
            "recepcionista, GET,    /api/v1/agendamentos",
            "aluno,         POST,   /api/v1/agendamentos",
            "recepcionista, POST,   /api/v1/agendamentos",
            "professor,     POST,   /api/v1/agendamentos",
            "aluno,         PATCH,  /api/v1/agendamentos/1/cancelamento",
            "recepcionista, PATCH,  /api/v1/agendamentos/1/cancelamento",

            // RF 19: convidados respondem ao convite.
            "aluno,         PATCH,  /api/v1/agendamentos/1/resposta",
            "professor,     PATCH,  /api/v1/agendamentos/1/resposta",
            "coordenador,   PATCH,  /api/v1/agendamentos/1/resposta",

            // Duracoes: todos consultam para montar o agendamento.
            "aluno,         GET,    /api/v1/duracoes-reuniao",
            "recepcionista, GET,    /api/v1/duracoes-reuniao",
            // Professor e coordenador gerenciam.
            "professor,     POST,   /api/v1/duracoes-reuniao",
            "coordenador,   POST,   /api/v1/duracoes-reuniao",

            // Disponibilidades: consulta aberta (RF 14), escrita so da docencia (RN 06).
            "aluno,         GET,    /api/v1/disponibilidades",
            "professor,     POST,   /api/v1/disponibilidades",
            "coordenador,   POST,   /api/v1/disponibilidades",

            // Bloqueios: gestao de agenda da docencia.
            "professor,     GET,    /api/v1/bloqueios-periodo",
            "coordenador,   POST,   /api/v1/bloqueios-periodo",

            // Admin tem acesso total.
            "admin,         POST,   /api/v1/usuarios",
            "admin,         DELETE, /api/v1/usuarios/1",
            "admin,         DELETE, /api/v1/agendamentos/1",
            "admin,         POST,   /api/v1/duracoes-reuniao",
            "admin,         GET,    /api/v1/bloqueios-periodo",

            // RF 24: o aluno registra e consulta as proprias avaliacoes.
            "aluno,         POST,   /api/v1/avaliacoes",
            "aluno,         GET,    /api/v1/avaliacoes/alunos/1",
            // A docencia acompanha as avaliacoes (RNF 11).
            "professor,     GET,    /api/v1/avaliacoes/alunos/1",
            "coordenador,   GET,    /api/v1/avaliacoes/alunos/1",

            // RF 23: log de notificacoes para a docencia e a administracao.
            "professor,     GET,    /api/v1/notificacoes",
            "coordenador,   GET,    /api/v1/notificacoes",
            "admin,         GET,    /api/v1/notificacoes",

            // RF 11: todos os perfis registram justificativa.
            "aluno,         POST,   /api/v1/registros-motivo",
            "recepcionista, POST,   /api/v1/registros-motivo",
            "professor,     GET,    /api/v1/registros-motivo/agendamentos/1",
            "aluno,         GET,    /api/v1/registros-motivo/agendamentos/1"
    })
    void devePermitir(String conta, String metodo, String caminho) throws Exception {
        assertThat(statusDe(conta, metodo, caminho))
                .as("%s deveria poder %s %s", conta, metodo, caminho)
                .isNotEqualTo(403);
    }

    @ParameterizedTest(name = "{0} {1} {2} -> 403")
    @DisplayName("Acessos que cada perfil NAO deve ter")
    @CsvSource({
            // Gestao de usuarios e exclusiva da administracao.
            "aluno,         POST,   /api/v1/usuarios",
            "professor,     POST,   /api/v1/usuarios",
            "coordenador,   POST,   /api/v1/usuarios",
            "recepcionista, POST,   /api/v1/usuarios",
            "aluno,         DELETE, /api/v1/usuarios/1",
            "coordenador,   DELETE, /api/v1/usuarios/1",
            "coordenador,   PUT,    /api/v1/usuarios/1",

            // RN 06: so professor e coordenador definem disponibilidade.
            "aluno,         POST,   /api/v1/disponibilidades",
            "recepcionista, POST,   /api/v1/disponibilidades",
            "aluno,         DELETE, /api/v1/disponibilidades/1",
            "recepcionista, PUT,    /api/v1/disponibilidades/1",

            // Bloqueios de periodo: area da docencia.
            "aluno,         GET,    /api/v1/bloqueios-periodo",
            "recepcionista, GET,    /api/v1/bloqueios-periodo",
            "aluno,         POST,   /api/v1/bloqueios-periodo",
            "recepcionista, POST,   /api/v1/bloqueios-periodo",

            // Duracoes: parametro academico, aluno e recepcao nao alteram.
            "aluno,         POST,   /api/v1/duracoes-reuniao",
            "recepcionista, POST,   /api/v1/duracoes-reuniao",
            "aluno,         DELETE, /api/v1/duracoes-reuniao/1",

            // RN 03: a recepcao nao e convidada, entao nao aceita/recusa convites.
            "recepcionista, PATCH,  /api/v1/agendamentos/1/resposta",

            // Remocao definitiva de agendamento: so administracao.
            "aluno,         DELETE, /api/v1/agendamentos/1",
            "professor,     DELETE, /api/v1/agendamentos/1",
            "coordenador,   DELETE, /api/v1/agendamentos/1",
            "recepcionista, DELETE, /api/v1/agendamentos/1",

            // Notificacoes: log operacional da docencia/administracao.
            "aluno,         GET,    /api/v1/notificacoes",
            "recepcionista, GET,    /api/v1/notificacoes",

            // Avaliacoes: RF 24 restringe a criacao ao aluno.
            "professor,     POST,   /api/v1/avaliacoes",
            "coordenador,   POST,   /api/v1/avaliacoes",
            "recepcionista, POST,   /api/v1/avaliacoes",
            "recepcionista, GET,    /api/v1/avaliacoes"
    })
    void deveNegar(String conta, String metodo, String caminho) throws Exception {
        assertThat(statusDe(conta, metodo, caminho))
                .as("%s NAO deveria poder %s %s", conta, metodo, caminho)
                .isEqualTo(403);
    }

    @ParameterizedTest(name = "sem credencial: {0} {1} -> 401")
    @DisplayName("Sem autenticacao a API responde 401")
    @CsvSource({
            "GET,  /api/v1/usuarios",
            "GET,  /api/v1/agendamentos",
            "POST, /api/v1/agendamentos"
    })
    void deveExigirAutenticacao(String metodo, String caminho) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(metodo), caminho))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }
}
