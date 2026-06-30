package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario cadastrarLoja(String nome, String email, String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(senha);
        usuario.setAtivo(true);

        validarCadastro(usuario);
        return usuarioRepository.save(usuario);
    }

    private void validarCadastro(Usuario usuario) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new IllegalArgumentException("Informe o nome da loja.");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("Informe o email.");
        }
        if (usuario.getSenha() == null || usuario.getSenha().length() < 3) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 3 caracteres.");
        }
        if (usuarioRepository.findByEmailAndAtivoTrue(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Este email ja esta cadastrado.");
        }
    }
}