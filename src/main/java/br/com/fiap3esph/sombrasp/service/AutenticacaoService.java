package br.com.fiap3esph.sombrasp.service;

import br.com.fiap3esph.sombrasp.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Usado pelo AuthenticationManager no login: busca o usuário pelo login (e-mail). */
@Service
@RequiredArgsConstructor
public class AutenticacaoService implements UserDetailsService {
    private final UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return repository.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
