package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.LoginRequestDTO;
import br.edu.unisales.schedemy.dto.response.TokenResponseDTO;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.EmissorDeToken;
import br.edu.unisales.schedemy.service.AutenticacaoService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login por e-mail e senha que devolve um token de acesso (item 2.3 da ANALISE.md).
 *
 * Substitui o Basic Auth, que obrigava o front a guardar a senha e envia-la em toda requisicao.
 * Passo intermediario ate o login com a conta Microsoft (RNF 16, RF 31).
 */
@Service
public class AutenticacaoServiceImpl implements AutenticacaoService {

    /** Mesma mensagem para e-mail inexistente, senha errada e usuario inativo (nao revela quem tem conta). */
    static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha invalidos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmissorDeToken emissorDeToken;

    /**
     * Hash de uma senha qualquer: conferido quando o e-mail nao existe, para a resposta levar
     * o mesmo tempo nos dois casos e nao denunciar quais e-mails estao cadastrados.
     */
    private final String hashFicticio;

    public AutenticacaoServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                   EmissorDeToken emissorDeToken) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emissorDeToken = emissorDeToken;
        this.hashFicticio = passwordEncoder.encode("usuario-inexistente");
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDTO entrar(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email().trim()).orElse(null);
        boolean senhaConfere = senhaConfere(dto.senha(), usuario == null ? hashFicticio : usuario.getSenha());
        if (usuario == null || !senhaConfere || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }
        EmissorDeToken.TokenEmitido emitido = emissorDeToken.emitir(usuario);
        return new TokenResponseDTO(emitido.token(), "Bearer", emitido.expiraEm());
    }

    private boolean senhaConfere(String senha, String hash) {
        try {
            return hash != null && passwordEncoder.matches(senha, hash);
        } catch (IllegalArgumentException e) {
            // BCrypt recusa senha com mais de 72 bytes ou hash malformado: trata como senha errada.
            return false;
        }
    }
}
