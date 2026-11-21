package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.security.UsuarioAutenticado;

import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.DisponibilidadeMapper;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.DisponibilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DisponibilidadeServiceImpl implements DisponibilidadeService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeMapper disponibilidadeMapper;
    private final UsuarioAutenticado usuarioAutenticado;
    private final ImpactoNaAgenda impactoNaAgenda;

    // Sem id na criacao: nenhum id real e negativo, entao nada e ignorado na checagem de sobreposicao.
    private static final long SEM_ID = -1L;

    // depois da trava, a checagem de sobreposicao enxerga o que outra transacao acabou de gravar.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DisponibilidadeResponseDTO criar(DisponibilidadeRequestDTO dto) {
        Usuario usuario = buscarUsuario(dto.idUsuario());
        usuarioAutenticado.exigirProprioOuAdmin(usuario.getId(), "Voce so pode gerenciar a sua propria disponibilidade.");
        validarPapelAtendente(usuario);
        validarHorarios(dto.horarioInicio(), dto.horarioFim());
        if (dto.ativo() == null || dto.ativo()) {
            validarSemSobreposicao(usuario.getId(), dto.diaSemana(), dto.horarioInicio(), dto.horarioFim(), SEM_ID);
        }

        Disponibilidade entidade = disponibilidadeMapper.paraEntidade(dto, usuario);
        entidade = disponibilidadeRepository.save(entidade);
        return disponibilidadeMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadeResponseDTO buscarPorId(Long id) {
        return disponibilidadeMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DisponibilidadeResponseDTO> listar(Long usuarioId, Integer diaSemana, Boolean ativo, Pageable pageable) {
        return disponibilidadeRepository.buscarComFiltros(usuarioId, diaSemana, ativo, pageable)
                .map(disponibilidadeMapper::paraResponseDTO);
    }

    /**
     * RF 06 - editar ou desativar a janela pode deixar reunioes marcadas fora da agenda: elas sao canceladas na mesma
     * transacao, se o docente confirmou (ver ImpactoNaAgenda). Sem a confirmacao, recusa com a lista (422).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DisponibilidadeResponseDTO atualizar(Long id, DisponibilidadeRequestDTO dto) {
        Disponibilidade entidade = buscarEntidadePorId(id);
        Usuario usuario = validarEdicao(entidade, dto);
        Disponibilidade depois = comoFicara(entidade, dto, usuario);
        impactoNaAgenda.cancelarAfetadas(entidade.getUsuario(),
                () -> impactoNaAgenda.afetadasPelaDisponibilidade(entidade, depois), dto.cancelarAfetadas());
        if (Boolean.TRUE.equals(depois.getAtivo())) {
            validarSemSobreposicao(usuario.getId(), dto.diaSemana(), dto.horarioInicio(), dto.horarioFim(), entidade.getId());
        }

        disponibilidadeMapper.atualizarEntidade(entidade, dto, usuario);
        return disponibilidadeMapper.paraResponseDTO(disponibilidadeRepository.save(entidade));
    }

    /** RF 06/RF 07 - reunioes que a edicao cancelaria, sem gravar nada. */
    @Override
    @Transactional(readOnly = true)
    public List<ReuniaoAfetadaResponseDTO> previaDaEdicao(Long id, DisponibilidadeRequestDTO dto) {
        Disponibilidade entidade = buscarEntidadePorId(id);
        Usuario usuario = validarEdicao(entidade, dto);
        return impactoNaAgenda.paraResposta(
                impactoNaAgenda.afetadasPelaDisponibilidade(entidade, comoFicara(entidade, dto, usuario)),
                entidade.getUsuario());
    }

    /** RF 06/RF 07 - reunioes que a remocao cancelaria, sem gravar nada. */
    @Override
    @Transactional(readOnly = true)
    public List<ReuniaoAfetadaResponseDTO> previaDaRemocao(Long id) {
        Disponibilidade entidade = buscarEntidadeDoDono(id);
        return impactoNaAgenda.paraResposta(impactoNaAgenda.afetadasPelaDisponibilidade(entidade, null),
                entidade.getUsuario());
    }

    /** Sem confirmacao: so remove se nenhuma reuniao for afetada (senao, 422 com a lista). */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletar(Long id) {
        remover(id, null);
    }

    /** RF 03/RF 06 - remove a janela e cancela as reunioes afetadas que o docente confirmou. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remover(Long id, CancelamentoDeAfetadasRequestDTO confirmacao) {
        Disponibilidade entidade = buscarEntidadeDoDono(id);
        impactoNaAgenda.cancelarAfetadas(entidade.getUsuario(),
                () -> impactoNaAgenda.afetadasPelaDisponibilidade(entidade, null), confirmacao);
        disponibilidadeRepository.delete(entidade);
    }

    /** Regras comuns a edicao e a sua previa; devolve o docente que ficara com a janela. */
    private Usuario validarEdicao(Disponibilidade entidade, DisponibilidadeRequestDTO dto) {
        usuarioAutenticado.exigirProprioOuAdmin(entidade.getUsuario().getId(), "Voce so pode gerenciar a sua propria disponibilidade.");
        Usuario usuario = buscarUsuario(dto.idUsuario());
        usuarioAutenticado.exigirProprioOuAdmin(usuario.getId(), "Voce so pode gerenciar a sua propria disponibilidade.");
        validarPapelAtendente(usuario);
        validarHorarios(dto.horarioInicio(), dto.horarioFim());
        return usuario;
    }

    /**
     * A janela como ficara depois da edicao, sem tocar na entidade gravada (ativo nulo mantem o valor atual,
     * como em DisponibilidadeMapper.atualizarEntidade).
     */
    private static Disponibilidade comoFicara(Disponibilidade entidade, DisponibilidadeRequestDTO dto, Usuario usuario) {
        return Disponibilidade.builder()
                .usuario(usuario)
                .diaSemana(dto.diaSemana())
                .horarioInicio(dto.horarioInicio())
                .horarioFim(dto.horarioFim())
                .ativo(dto.ativo() != null ? dto.ativo() : Boolean.TRUE.equals(entidade.getAtivo()))
                .build();
    }

    private Disponibilidade buscarEntidadeDoDono(Long id) {
        Disponibilidade entidade = buscarEntidadePorId(id);
        usuarioAutenticado.exigirProprioOuAdmin(entidade.getUsuario().getId(), "Voce so pode gerenciar a sua propria disponibilidade.");
        return entidade;
    }

    /**
     * Uma janela ativa nao pode se sobrepor a outra janela ativa do mesmo docente no mesmo dia (janelas que so
     * encostam, como 08:00-12:00 e 12:00-14:00, sao aceitas). Trava a linha do docente antes de consultar para que
     * dois cadastros simultaneos nao passem juntos pela checagem.
     */
    private void validarSemSobreposicao(Long idUsuario, Integer diaSemana, LocalTime inicio, LocalTime fim, Long idIgnorado) {
        usuarioRepository.travarParaAgendamento(List.of(idUsuario));
        if (disponibilidadeRepository.existeSobreposicao(idUsuario, diaSemana, inicio, fim, idIgnorado)) {
            throw new RegraDeNegocioException(
                    "Ja existe uma disponibilidade ativa neste dia que se sobrepoe a este horario. Edite a janela existente ou escolha outro horario.");
        }
    }

    private void validarHorarios(LocalTime inicio, LocalTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("O horario de fim deve ser posterior ao horario de inicio.");
        }
    }

    /** RN 06 - somente professores e coordenadores podem cadastrar disponibilidade. */
    private void validarPapelAtendente(Usuario usuario) {
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR && usuario.getTipoUsuario() != TipoUsuario.COORDENADOR) {
            throw new RegraDeNegocioException(
                    "Somente usuarios do tipo PROFESSOR ou COORDENADOR podem ter disponibilidade cadastrada.");
        }
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }

    private Disponibilidade buscarEntidadePorId(Long id) {
        return disponibilidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Disponibilidade", id));
    }
}