package org.example.stockhubspring.service;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Autentica o MEMBRO ATIVO pelo e-mail (normalizado) e carrega a LOJA dele, que também precisa estar
 * ativa (AD-13). Toda a sessão passa a trabalhar com o código dessa loja.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final MembroLojaRepository membroRepository;
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(MembroLojaRepository membroRepository, UsuarioRepository usuarioRepository) {
        this.membroRepository = membroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        MembroLoja membro = membroRepository.findByEmailAndAtivoTrue(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
        Usuario loja = usuarioRepository.findById(membro.getLojaCodigo())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
        return new UsuarioPrincipal(loja, membro);
    }
}
