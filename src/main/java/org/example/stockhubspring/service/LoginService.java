package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;

    public LoginService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario autenticar(String email, String senha) {
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .filter(usuario -> senha != null && senha.equals(usuario.getSenha()))
                .orElse(null);
    }
}