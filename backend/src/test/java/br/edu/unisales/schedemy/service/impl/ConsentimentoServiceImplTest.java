package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.ConsentimentoTermo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.AceiteTermoRequestDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.repository.ConsentimentoTermoRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RNF 18 - termo de uso/politica de privacidade e o aceite por versao. */
class ConsentimentoServiceImplTest {

    private final ConsentimentoTermoRepository repositorio = mock(ConsentimentoTermoRepository.class);
    private final UsuarioAutenticado usuarioAutenticado = mock(UsuarioAutenticado.class);
    private final Auditoria auditoria = mock(Auditoria.class);
    private final TermoDePrivacidade termo = new TermoDePrivacidade();
    private final ConsentimentoServiceImpl service =
            new ConsentimentoServiceImpl(repositorio, termo, usuarioAutenticado, auditoria);

    private final Usuario aluno = Usuario.builder().id(1L).nome("Kevin").tipoUsuario(TipoUsuario.ALUNO).build();

    @BeforeEach
    void preparar() {
        when(usuarioAutenticado.obter()).thenReturn(aluno);
        when(repositorio.save(any())).then(i -> {
            ConsentimentoTermo c = i.getArgument(0);
            c.setId(77L);
            c.setAceitoEm(LocalDateTime.now());
            return c;
        });
    }

    @Test
    @DisplayName("o termo do arquivo tem versao, termo e politica com secoes")
    void termoCarregado() {
        var doc = service.termoAtual();
        assertThat(doc.versao()).isNotBlank().hasSizeLessThanOrEqualTo(20);
        assertThat(doc.termo().secoes()).isNotEmpty().allSatisfy(s -> assertThat(s.paragrafos()).isNotEmpty());
        assertThat(doc.politica().secoes()).isNotEmpty().allSatisfy(s -> assertThat(s.paragrafos()).isNotEmpty());
    }

    @Test
    @DisplayName("quem nunca aceitou esta pendente")
    void nuncaAceitou() {
        var situacao = service.situacaoDoLogado();
        assertThat(situacao.pendente()).isTrue();
        assertThat(situacao.versaoAceita()).isNull();
        assertThat(situacao.versaoAtual()).isEqualTo(termo.versao());
    }

    @Test
    @DisplayName("aceite da versao em vigor grava uma vez, com auditoria")
    void aceita() {
        service.aceitar(new AceiteTermoRequestDTO(termo.versao()));

        ArgumentCaptor<ConsentimentoTermo> captor = ArgumentCaptor.forClass(ConsentimentoTermo.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(aluno);
        assertThat(captor.getValue().getVersaoTermo()).isEqualTo(termo.versao());
        verify(auditoria).registrar(eq(AcaoAuditoria.TERMO_ACEITO), eq(77L), anyString());
    }

    @Test
    @DisplayName("aceitar de novo a mesma versao nao duplica")
    void aceiteRepetido() {
        when(repositorio.existsByUsuarioIdAndVersaoTermo(1L, termo.versao())).thenReturn(true);

        var situacao = service.aceitar(new AceiteTermoRequestDTO(termo.versao()));

        assertThat(situacao.pendente()).isFalse();
        verify(repositorio, never()).save(any());
        verify(auditoria, never()).registrar(any(), anyLong(), any());
    }

    @Test
    @DisplayName("versao antiga (o termo mudou enquanto a pessoa lia): 422, nada gravado")
    void versaoAntiga() {
        assertThatThrownBy(() -> service.aceitar(new AceiteTermoRequestDTO("2000-01-01")))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining(termo.versao());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("aceitou uma versao anterior: continua pendente para a atual")
    void versaoNova() {
        when(repositorio.findFirstByUsuarioIdOrderByAceitoEmDescIdDesc(1L)).thenReturn(Optional.of(
                ConsentimentoTermo.builder().versaoTermo("2026-01-01").aceitoEm(LocalDateTime.now()).build()));

        var situacao = service.situacaoDoLogado();

        assertThat(situacao.pendente()).isTrue();
        assertThat(situacao.versaoAceita()).isEqualTo("2026-01-01");
    }
}
