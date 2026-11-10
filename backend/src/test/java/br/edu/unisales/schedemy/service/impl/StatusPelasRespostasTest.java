package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.edu.unisales.schedemy.domain.enums.StatusResposta.ACEITO;
import static br.edu.unisales.schedemy.domain.enums.StatusResposta.PENDENTE;
import static br.edu.unisales.schedemy.domain.enums.StatusResposta.RECUSADO;
import static org.assertj.core.api.Assertions.assertThat;

/** RN 18 (Documento de Requisitos v2.7.0) - quando a reuniao acontece, aguarda ou e cancelada. */
class StatusPelasRespostasTest {

    private long proximoId = 1;

    @Test
    @DisplayName("Aluno organizador (ja aceito) + professor que aceita -> CONFIRMADO")
    void confirmaComUmDocenteEUmAluno() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.ALUNO),
                convidado(TipoUsuario.PROFESSOR, ACEITO)));
        assertThat(status).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Professor ainda nao respondeu -> AGUARDANDO_RESPOSTA")
    void aguardaDocente() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.ALUNO),
                convidado(TipoUsuario.PROFESSOR, PENDENTE)));
        assertThat(status).isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
    }

    @Test
    @DisplayName("Recusa de um dos alunos nao impede a reuniao se outro aluno aceitou")
    void recusaParcialDeAlunosNaoCancela() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.PROFESSOR),
                convidado(TipoUsuario.ALUNO, RECUSADO),
                convidado(TipoUsuario.ALUNO, ACEITO)));
        assertThat(status).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Todos os alunos recusaram -> CANCELADO")
    void todosOsAlunosRecusaram() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.PROFESSOR),
                convidado(TipoUsuario.ALUNO, RECUSADO),
                convidado(TipoUsuario.ALUNO, RECUSADO)));
        assertThat(status).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Um de dois docentes recusou e o outro ainda nao respondeu -> continua aguardando")
    void recusaDeUmDocenteNaoCancelaSeHaOutro() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.ALUNO),
                convidado(TipoUsuario.PROFESSOR, RECUSADO),
                convidado(TipoUsuario.COORDENADOR, PENDENTE)));
        assertThat(status).isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
    }

    @Test
    @DisplayName("Todos os docentes recusaram -> CANCELADO")
    void todosOsDocentesRecusaram() {
        var status = AgendamentoServiceImpl.statusPelasRespostas(List.of(
                organizador(TipoUsuario.ALUNO),
                convidado(TipoUsuario.PROFESSOR, RECUSADO)));
        assertThat(status).isEqualTo(StatusAgendamento.CANCELADO);
    }

    private AgendamentoParticipante organizador(TipoUsuario tipo) {
        return participante(tipo, PapelParticipante.ORGANIZADOR, ACEITO);
    }

    private AgendamentoParticipante convidado(TipoUsuario tipo, StatusResposta resposta) {
        return participante(tipo, PapelParticipante.CONVIDADO, resposta);
    }

    private AgendamentoParticipante participante(TipoUsuario tipo, PapelParticipante papel, StatusResposta resposta) {
        Usuario usuario = new Usuario();
        usuario.setId(proximoId++);
        usuario.setNome(tipo.name());
        usuario.setTipoUsuario(tipo);
        return AgendamentoParticipante.builder().usuario(usuario).papel(papel).statusResposta(resposta).build();
    }
}
