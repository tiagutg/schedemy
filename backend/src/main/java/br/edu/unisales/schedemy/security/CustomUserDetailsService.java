package br.edu.unisales.schedemy.security;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado com o email: " + email));

        if (!usuario.getAtivo()) {
            throw new UsernameNotFoundException("Usuario esta inativo.");
        }

        return new User(
                usuario.getEmail(),
                usuario.getSenha(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoUsuario().name()))
        );
    }
}