package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Cadastro de lojas/usuários (RN-14). A senha é sempre guardada com hash (BCrypt). */
@Service
public class UsuarioService {

    // Mensagem genérica: não revela se o e-mail já existe (evita enumeração de usuários).
    static final String MSG_CADASTRO_GENERICA =
            "Não foi possível concluir o cadastro. Verifique os dados e tente novamente.";
    static final int SENHA_MINIMA = 8;
    // O BCrypt só considera os primeiros 72 bytes da senha.
    static final int SENHA_MAXIMA = 72;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarLoja(String nome, String email, String senha) {
        String nomeLimpo = nome == null ? null : nome.trim();
        String emailNormalizado = normalizarEmail(email);
        validarCadastro(nomeLimpo, emailNormalizado, senha);

        // Considera também usuários inativos (o e-mail é único no banco).
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new RegraNegocioException(MSG_CADASTRO_GENERICA);
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nomeLimpo);
        usuario.setEmail(emailNormalizado);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setAtivo(true);

        try {
            // saveAndFlush para a violação de unicidade (corrida entre dois cadastros) estourar aqui.
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException(MSG_CADASTRO_GENERICA);
        }
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarCadastro(String nome, String email, String senha) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Informe o nome da loja.");
        }
        if (nome.length() > 255) {
            throw new RegraNegocioException("O nome da loja é muito longo.");
        }
        if (email == null || email.isBlank() || email.length() > 255 || !EMAIL.matcher(email).matches()) {
            throw new RegraNegocioException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMA) {
            throw new RegraNegocioException("A senha deve ter no máximo " + SENHA_MAXIMA + " bytes.");
        }
    }
}
