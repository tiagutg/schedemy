package br.edu.unisales.schedemy.config;

import br.edu.unisales.schedemy.controller.AgendamentoController;
import br.edu.unisales.schedemy.controller.AuditoriaController;
import br.edu.unisales.schedemy.controller.ConsentimentoController;
import br.edu.unisales.schedemy.controller.AutenticacaoController;
import br.edu.unisales.schedemy.controller.AvaliacaoController;
import br.edu.unisales.schedemy.controller.NotificacaoController;
import br.edu.unisales.schedemy.controller.RegistroMotivoController;
import br.edu.unisales.schedemy.controller.BloqueioPeriodoController;
import br.edu.unisales.schedemy.controller.DisponibilidadeController;
import br.edu.unisales.schedemy.controller.DuracaoReuniaoController;
import br.edu.unisales.schedemy.controller.UsuarioController;
import br.edu.unisales.schedemy.repository.ConsentimentoTermoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.ConversorDeToken;
import br.edu.unisales.schedemy.security.ExigeAceiteDoTermoFilter;
import br.edu.unisales.schedemy.service.AgendamentoService;
import br.edu.unisales.schedemy.service.AuditoriaService;
import br.edu.unisales.schedemy.service.ConsentimentoService;
import br.edu.unisales.schedemy.service.impl.TermoDePrivacidade;
import br.edu.unisales.schedemy.service.AutenticacaoService;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Verifica a matriz de autorizacao do SecurityConfig (perfis do Documento de
 * Definicao de Requisitos v3.1.0: RFs, RN 03, RN 06 e RNF 15).
 *
 * O teste olha apenas para o veredito do filtro de seguranca: 403 significa
 * barrado pelo perfil; qualquer outro status (inclusive 404, quando o endpoint
 * ainda nao existe, ou 400, quando falta corpo) significa que a autorizacao
 * deixou passar. Assim a matriz pode ser validada sem banco de dados.
 *
 * Cada conta e simulada com um token ja validado (jwt()) que carrega o papel; a conversao
 * real do token em usuario (ConversorDeToken, que consulta o banco) tem teste proprio.
 */
@WebMvcTest(controllers = {
        AgendamentoController.class,
        BloqueioPeriodoController.class,
        DisponibilidadeController.class,
        DuracaoReuniaoController.class,
        UsuarioController.class,
        AvaliacaoController.class,
        NotificacaoController.class,
        RegistroMotivoController.class,
        AutenticacaoController.class,
        AuditoriaController.class,
        ConsentimentoController.class
})
@Import({ SecurityConfig.class, AcessoNegadoJsonHandler.class, NaoAutenticadoJsonEntryPoint.class, JwtConfig.class, ConversorDeToken.class,
        br.edu.unisales.schedemy.exception.GlobalExceptionHandler.class })
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
    @MockitoBean private AutenticacaoService autenticacaoService;
    @MockitoBean private UsuarioRepository usuarioRepository;
    @MockitoBean private AuditoriaService auditoriaService;
    @MockitoBean private ConsentimentoService consentimentoService;
    @MockitoBean private ConsentimentoTermoRepository consentimentoTermoRepository;
    @MockitoBean private TermoDePrivacidade termoDePrivacidade;

    /** A conta do CSV ("aluno", "recepcionista"...) e o proprio papel. */
    private int statusDe(String conta, String metodo, String caminho) throws Exception {
        var papel = new SimpleGrantedAuthority("ROLE_" + conta.toUpperCase(Locale.ROOT));
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), caminho)
                .with(jwt().authorities(papel));
        return mockMvc.perform(req).andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1} {2} -> permitido")
    @DisplayName("Acessos que cada perfil deve ter")
    @CsvSource({
            // Todos leem o proprio cadastro (login) e o resumo para escolher convidados (RF 14, RF 33).
            "aluno,         GET,    /api/v1/usuarios/me",
            "professor,     GET,    /api/v1/usuarios/me",
            "recepcionista, GET,    /api/v1/usuarios/me",
            "aluno,         GET,    /api/v1/usuarios/resumo",
            "professor,     GET,    /api/v1/usuarios/resumo",
            "coordenador,   GET,    /api/v1/usuarios/resumo",
            "recepcionista, GET,    /api/v1/usuarios/resumo",
            "admin,         GET,    /api/v1/usuarios/resumo",
            // Cadastro completo (RNF 18): administracao e recepcao.
            "recepcionista, GET,    /api/v1/usuarios",
            "recepcionista, GET,    /api/v1/usuarios/1",
            "admin,         GET,    /api/v1/usuarios",
            "admin,         GET,    /api/v1/usuarios/1",

            // Agendamentos: RF 13, 15, 20 valem para todos os perfis.
            "admin,         GET,    /api/v1/agendamentos",
            "aluno,         GET,    /api/v1/agendamentos/1",
            "aluno,         GET,    /api/v1/agendamentos/usuarios/1",
            "aluno,         POST,   /api/v1/agendamentos",
            "recepcionista, POST,   /api/v1/agendamentos",
            "professor,     POST,   /api/v1/agendamentos",
            "aluno,         PATCH,  /api/v1/agendamentos/1/cancelamento",
            "recepcionista, PATCH,  /api/v1/agendamentos/1/cancelamento",
            // Remarcacao (o service exige ser o organizador, ou recepcao/admin por ele).
            "aluno,         POST,   /api/v1/agendamentos/1/remarcacao",
            "professor,     POST,   /api/v1/agendamentos/1/remarcacao",
            "coordenador,   POST,   /api/v1/agendamentos/1/remarcacao",
            "recepcionista, POST,   /api/v1/agendamentos/1/remarcacao",

            // RF 13, 20, 21: a recepcao ve todos os agendamentos, consulta, edita e cancela.
            "recepcionista, GET,    /api/v1/agendamentos",
            "recepcionista, GET,    /api/v1/agendamentos/1",
            "recepcionista, PUT,    /api/v1/agendamentos/1",
            "recepcionista, GET,    /api/v1/registros-motivo/agendamentos/1",

            // RF 14: horarios livres para todos os envolvidos, sem expor a agenda de ninguem.
            "aluno,         GET,    /api/v1/agendamentos/horarios-livres",
            "professor,     GET,    /api/v1/agendamentos/horarios-livres",
            "recepcionista, GET,    /api/v1/agendamentos/horarios-livres",

            // RF 19: convidados respondem ao convite.
            "aluno,         PATCH,  /api/v1/agendamentos/1/resposta",
            "professor,     PATCH,  /api/v1/agendamentos/1/resposta",
            "coordenador,   PATCH,  /api/v1/agendamentos/1/resposta",

            // Duracoes: todos consultam para montar o agendamento.
            "aluno,         GET,    /api/v1/duracoes-reuniao",
            "recepcionista, GET,    /api/v1/duracoes-reuniao",
            "professor,     GET,    /api/v1/duracoes-reuniao",

            // Disponibilidades: consulta aberta (RF 14), escrita so da docencia (RN 06).
            "aluno,         GET,    /api/v1/disponibilidades",
            "professor,     POST,   /api/v1/disponibilidades",
            "coordenador,   POST,   /api/v1/disponibilidades",
            // RF 06: previa e remocao com cancelamento das reunioes afetadas.
            "professor,     POST,   /api/v1/disponibilidades/1/previa",
            "coordenador,   POST,   /api/v1/disponibilidades/1/remocao/previa",
            "professor,     POST,   /api/v1/disponibilidades/1/remocao",
            "admin,         POST,   /api/v1/disponibilidades/1/remocao",

            // Bloqueios: gestao de agenda da docencia.
            "professor,     GET,    /api/v1/bloqueios-periodo",
            "coordenador,   POST,   /api/v1/bloqueios-periodo",
            "professor,     POST,   /api/v1/bloqueios-periodo/previa",
            "coordenador,   POST,   /api/v1/bloqueios-periodo/1/previa",

            // Admin tem acesso total.
            "admin,         POST,   /api/v1/usuarios",
            "admin,         DELETE, /api/v1/usuarios/1",
            "admin,         DELETE, /api/v1/agendamentos/1",
            "admin,         POST,   /api/v1/duracoes-reuniao",
            "admin,         GET,    /api/v1/bloqueios-periodo",

            // RF 21/22: o aluno (ou a recepcao) pede a alteracao; a docencia decide; todos consultam (escopo no service).
            "aluno,         POST,   /api/v1/agendamentos/1/solicitacoes-edicao",
            "recepcionista, POST,   /api/v1/agendamentos/1/solicitacoes-edicao",
            "professor,     GET,    /api/v1/agendamentos/1/solicitacoes-edicao",
            "professor,     PATCH,  /api/v1/solicitacoes-edicao/1/resposta",
            "coordenador,   PATCH,  /api/v1/solicitacoes-edicao/1/resposta",
            "aluno,         PATCH,  /api/v1/solicitacoes-edicao/1/cancelamento",
            "recepcionista, PATCH,  /api/v1/solicitacoes-edicao/1/cancelamento",
            "admin,         PATCH,  /api/v1/solicitacoes-edicao/1/resposta",
            // RF 22: a lista de pedidos e de todos os perfis (o escopo pelo logado fica no service).
            "aluno,         GET,    /api/v1/solicitacoes-edicao",
            "professor,     GET,    /api/v1/solicitacoes-edicao",
            "coordenador,   GET,    /api/v1/solicitacoes-edicao",
            "recepcionista, GET,    /api/v1/solicitacoes-edicao",
            "admin,         GET,    /api/v1/solicitacoes-edicao",

            // RF 24: o aluno registra e consulta as proprias avaliacoes.
            "aluno,         POST,   /api/v1/avaliacoes",
            "aluno,         GET,    /api/v1/avaliacoes/alunos/1",
            // A docencia acompanha as avaliacoes (RNF 11).
            "professor,     GET,    /api/v1/avaliacoes/alunos/1",
            "coordenador,   GET,    /api/v1/avaliacoes/alunos/1",
            "professor,     GET,    /api/v1/avaliacoes",
            "coordenador,   GET,    /api/v1/avaliacoes",
            "aluno,         GET,    /api/v1/avaliacoes",

            // RF 23: cada perfil le e marca como lidos os proprios avisos (escopo no service).
            "aluno,         GET,    /api/v1/notificacoes",
            "recepcionista, GET,    /api/v1/notificacoes",
            "aluno,         PATCH,  /api/v1/notificacoes/1/lida",
            "recepcionista, PATCH,  /api/v1/notificacoes/lidas",
            "professor,     GET,    /api/v1/notificacoes",
            "coordenador,   GET,    /api/v1/notificacoes",
            "admin,         GET,    /api/v1/notificacoes",

            // RNF 19: a trilha de auditoria e da administracao.
            "admin,         GET,    /api/v1/auditoria",
            // RNF 18: cada perfil ve e da o proprio aceite do termo.
            "aluno,         GET,    /api/v1/consentimentos/meu",
            "recepcionista, POST,   /api/v1/consentimentos",
            "admin,         POST,   /api/v1/consentimentos",

            // RF 11: todos os perfis consultam o historico de justificativas (escopo no service).
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
            "recepcionista, PUT,    /api/v1/usuarios/1",
            "recepcionista, DELETE, /api/v1/usuarios/1",

            // RNF 18: e-mail e telefone de toda a base nao ficam abertos a qualquer logado.
            "aluno,         GET,    /api/v1/usuarios",
            "professor,     GET,    /api/v1/usuarios",
            "coordenador,   GET,    /api/v1/usuarios",
            "aluno,         GET,    /api/v1/usuarios/1",
            "professor,     GET,    /api/v1/usuarios/1",
            "coordenador,   GET,    /api/v1/usuarios/1",

            // RN 06: so professor e coordenador definem disponibilidade.
            "aluno,         POST,   /api/v1/disponibilidades",
            "recepcionista, POST,   /api/v1/disponibilidades",
            "aluno,         DELETE, /api/v1/disponibilidades/1",
            "recepcionista, PUT,    /api/v1/disponibilidades/1",
            // RF 06: a previa e o cancelamento das afetadas sao da docencia (o aluno so consulta a grade).
            "aluno,         POST,   /api/v1/disponibilidades/1/previa",
            "recepcionista, POST,   /api/v1/disponibilidades/1/remocao/previa",
            "aluno,         POST,   /api/v1/disponibilidades/1/remocao",

            // Bloqueios de periodo: area da docencia.
            "aluno,         GET,    /api/v1/bloqueios-periodo",
            "recepcionista, GET,    /api/v1/bloqueios-periodo",
            "aluno,         POST,   /api/v1/bloqueios-periodo",
            "recepcionista, POST,   /api/v1/bloqueios-periodo",
            "aluno,         POST,   /api/v1/bloqueios-periodo/previa",
            "recepcionista, POST,   /api/v1/bloqueios-periodo/1/previa",

            // Duracoes: definidas pela instituicao (RN 17), so a administracao altera.
            "aluno,         POST,   /api/v1/duracoes-reuniao",
            "recepcionista, POST,   /api/v1/duracoes-reuniao",
            "aluno,         DELETE, /api/v1/duracoes-reuniao/1",
            "professor,     POST,   /api/v1/duracoes-reuniao",
            "professor,     PUT,    /api/v1/duracoes-reuniao/1",
            "professor,     DELETE, /api/v1/duracoes-reuniao/1",
            "coordenador,   POST,   /api/v1/duracoes-reuniao",
            "coordenador,   PUT,    /api/v1/duracoes-reuniao/1",
            "coordenador,   DELETE, /api/v1/duracoes-reuniao/1",

            // RN 03: a recepcao nao e convidada, entao nao aceita/recusa convites.
            "recepcionista, PATCH,  /api/v1/agendamentos/1/resposta",

            // RF 21/RN 12: docente nao pede alteracao (cancela e agenda de novo); so a docencia decide.
            "professor,     POST,   /api/v1/agendamentos/1/solicitacoes-edicao",
            "coordenador,   POST,   /api/v1/agendamentos/1/solicitacoes-edicao",
            "aluno,         PATCH,  /api/v1/solicitacoes-edicao/1/resposta",
            "recepcionista, PATCH,  /api/v1/solicitacoes-edicao/1/resposta",
            "professor,     PATCH,  /api/v1/solicitacoes-edicao/1/cancelamento",
            // So a lista e liberada: os demais caminhos sem regra propria ficam com a administracao.
            "aluno,         GET,    /api/v1/solicitacoes-edicao/1",
            "recepcionista, DELETE, /api/v1/solicitacoes-edicao/1",

            // RF 11: nao ha registro de motivo avulso (o motivo vem da propria operacao).
            "aluno,         POST,   /api/v1/registros-motivo",
            "recepcionista, POST,   /api/v1/registros-motivo",
            "professor,     POST,   /api/v1/registros-motivo",

            // Remocao definitiva de agendamento: so administracao.
            "aluno,         DELETE, /api/v1/agendamentos/1",
            "professor,     DELETE, /api/v1/agendamentos/1",
            "coordenador,   DELETE, /api/v1/agendamentos/1",
            "recepcionista, DELETE, /api/v1/agendamentos/1",

            // Notificacoes: criar, enviar e remover sao da administracao.
            "aluno,         POST,   /api/v1/notificacoes",
            "professor,     DELETE, /api/v1/notificacoes/1",
            "recepcionista, PATCH,  /api/v1/notificacoes/1/enviar",

            // Listagem global de agendamentos: so recepcao e administracao (RNF 15).
            "aluno,         GET,    /api/v1/agendamentos",
            "professor,     GET,    /api/v1/agendamentos",
            "coordenador,   GET,    /api/v1/agendamentos",

            // Avaliacoes: RF 24 restringe a criacao ao aluno.
            "professor,     POST,   /api/v1/avaliacoes",
            "coordenador,   POST,   /api/v1/avaliacoes",
            "recepcionista, POST,   /api/v1/avaliacoes",
            "recepcionista, GET,    /api/v1/avaliacoes",

            // RNF 19: auditoria so para a administracao (tem dados pessoais e IP).
            "aluno,         GET,    /api/v1/auditoria",
            "professor,     GET,    /api/v1/auditoria",
            "coordenador,   GET,    /api/v1/auditoria",
            "recepcionista, GET,    /api/v1/auditoria",
            // RNF 18: o aceite nao e editado nem apagado pela API.
            "aluno,         DELETE, /api/v1/consentimentos/1",
            "recepcionista, PUT,    /api/v1/consentimentos/1"
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
            "GET,  /api/v1/usuarios/resumo",
            "GET,  /api/v1/agendamentos",
            "POST, /api/v1/agendamentos",
            "POST, /api/v1/agendamentos/1/remarcacao",
            "GET,  /api/v1/agendamentos/horarios-livres"
    })
    void deveExigirAutenticacao(String metodo, String caminho) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(metodo), caminho))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    /** RNF 18 - autenticacao como o ConversorDeToken monta para quem ainda nao aceitou o termo em vigor. */
    private int statusComTermoPendente(String conta, String metodo, String caminho) throws Exception {
        var papel = new SimpleGrantedAuthority("ROLE_" + conta.toUpperCase(Locale.ROOT));
        var pendente = new SimpleGrantedAuthority(ExigeAceiteDoTermoFilter.TERMO_PENDENTE);
        return mockMvc.perform(request(HttpMethod.valueOf(metodo), caminho)
                        .with(jwt().authorities(papel, pendente)))
                .andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "termo pendente: {0} {1} {2} -> 403")
    @DisplayName("RNF 18 - sem aceitar o termo em vigor, nenhum perfil usa o resto da API")
    @CsvSource({
            "aluno,         GET,    /api/v1/agendamentos/usuarios/1",
            "aluno,         POST,   /api/v1/agendamentos",
            "professor,     GET,    /api/v1/disponibilidades",
            "recepcionista, GET,    /api/v1/agendamentos",
            "admin,         GET,    /api/v1/usuarios",
            "admin,         GET,    /api/v1/auditoria",
            "aluno,         GET,    /api/v1/usuarios/resumo",
            "aluno,         GET,    /api/v1/notificacoes"
    })
    void termoPendenteBarra(String conta, String metodo, String caminho) throws Exception {
        assertThat(statusComTermoPendente(conta, metodo, caminho))
                .as("%s com termo pendente NAO deveria poder %s %s", conta, metodo, caminho)
                .isEqualTo(403);
    }

    @ParameterizedTest(name = "termo pendente: {0} {1} {2} -> liberado")
    @DisplayName("RNF 18 - com o termo pendente, da para ver o proprio cadastro, ler o termo e aceitar")
    @CsvSource({
            "aluno,         GET,    /api/v1/usuarios/me",
            "aluno,         GET,    /api/v1/termos",
            "professor,     GET,    /api/v1/consentimentos/meu",
            "admin,         POST,   /api/v1/consentimentos"
    })
    void termoPendenteLibera(String conta, String metodo, String caminho) throws Exception {
        assertThat(statusComTermoPendente(conta, metodo, caminho))
                .as("%s com termo pendente deveria poder %s %s", conta, metodo, caminho)
                .isNotEqualTo(403);
    }

    @Test
    @DisplayName("RNF 18 - o 403 do termo pendente sai em JSON com erro TERMO_PENDENTE")
    void termoPendenteEmJson() throws Exception {
        var resposta = mockMvc.perform(request(HttpMethod.GET, "/api/v1/agendamentos/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUNO"),
                                new SimpleGrantedAuthority(ExigeAceiteDoTermoFilter.TERMO_PENDENTE))))
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(resposta.getContentAsString()).contains("\"erro\":\"TERMO_PENDENTE\"");
    }

    @Test
    @DisplayName("RNF 18 - o termo e a politica sao publicos (pagina /privacidade)")
    void termoEhPublico() throws Exception {
        int status = mockMvc.perform(request(HttpMethod.GET, "/api/v1/termos")).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("O login e publico")
    void loginEhPublico() throws Exception {
        int status = mockMvc.perform(request(HttpMethod.POST, "/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"senha\":\"x\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("Basic Auth nao e mais aceito (a senha so trafega no login)")
    void basicAuthNaoValeMais() throws Exception {
        int status = mockMvc.perform(request(HttpMethod.GET, "/api/v1/usuarios/me")
                        .with(httpBasic("admin@hotmail.com", "admin123")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    @Test
    @DisplayName("Token malformado ou de outro emissor responde 401 em JSON")
    void tokenInvalidoResponde401() throws Exception {
        var resposta = mockMvc.perform(request(HttpMethod.GET, "/api/v1/usuarios/me")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).isEqualTo(401);
        assertThat(resposta.getContentType()).contains("application/json");
    }
}
