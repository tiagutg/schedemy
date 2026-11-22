package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.ConsentimentoTermo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.dto.request.AceiteTermoRequestDTO;
import br.edu.unisales.schedemy.dto.response.ConsentimentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.TermoResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.repository.ConsentimentoTermoRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import br.edu.unisales.schedemy.service.ConsentimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * RNF 18 - aceite do termo. Enquanto o usuario nao aceitar a versao em vigor, a API recusa as demais operacoes
 * (ExigeAceiteDoTermoFilter). O aceite vale por versao: publicar uma versao nova pede o aceite de novo.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConsentimentoServiceImpl implements ConsentimentoService {

    private static final int MAX_IP = 45;

    private final ConsentimentoTermoRepository consentimentoTermoRepository;
    private final TermoDePrivacidade termoDePrivacidade;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Auditoria auditoria;

    @Override
    @Transactional(readOnly = true)
    public TermoResponseDTO termoAtual() {
        return termoDePrivacidade.documento();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentimentoResponseDTO situacaoDoLogado() {
        return situacao(usuarioAutenticado.obter());
    }

    /** So aceita a versao em vigor (quem leu uma versao antiga precisa ler a nova). Aceitar de novo nao duplica. */
    @Override
    public ConsentimentoResponseDTO aceitar(AceiteTermoRequestDTO dto) {
        Usuario usuario = usuarioAutenticado.obter();
        String versao = termoDePrivacidade.versao();
        if (!versao.equals(dto.versao().trim())) {
            throw new RegraDeNegocioException(
                    "O termo foi atualizado (versao " + versao + "). Leia a versao atual antes de aceitar.");
        }
        if (!consentimentoTermoRepository.existsByUsuarioIdAndVersaoTermo(usuario.getId(), versao)) {
            ConsentimentoTermo aceite = consentimentoTermoRepository.save(ConsentimentoTermo.builder()
                    .usuario(usuario)
                    .versaoTermo(versao)
                    .ipOrigem(ipDaRequisicao())
                    .build());
            auditoria.registrar(AcaoAuditoria.TERMO_ACEITO, aceite.getId(), "Versao " + versao); // RNF 19
        }
        return situacao(usuario);
    }

    private ConsentimentoResponseDTO situacao(Usuario usuario) {
        String versaoAtual = termoDePrivacidade.versao();
        var ultimo = consentimentoTermoRepository.findFirstByUsuarioIdOrderByAceitoEmDescIdDesc(usuario.getId());
        boolean aceitouAtual = consentimentoTermoRepository.existsByUsuarioIdAndVersaoTermo(usuario.getId(), versaoAtual);
        return new ConsentimentoResponseDTO(versaoAtual,
                ultimo.map(ConsentimentoTermo::getVersaoTermo).orElse(null),
                ultimo.map(ConsentimentoTermo::getAceitoEm).orElse(null),
                !aceitouAtual);
    }

    private static String ipDaRequisicao() {
        String ip = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                ? atributos.getRequest().getRemoteAddr() : null;
        return ip == null || ip.length() <= MAX_IP ? ip : ip.substring(0, MAX_IP);
    }
}
