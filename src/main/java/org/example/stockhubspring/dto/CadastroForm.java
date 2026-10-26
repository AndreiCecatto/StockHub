package org.example.stockhubspring.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.stockhubspring.util.Textos;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Cadastro de loja/usuário (RN-14). O e-mail é normalizado (sem espaços, minúsculo).
 * Sem {@code toString}: a senha nunca deve ir para log.
 */
public class CadastroForm {

    // O BCrypt só considera os primeiros 72 bytes da senha
    public static final int SENHA_MAXIMA_BYTES = 72;

    @NotBlank(message = "Informe o nome da loja.")
    @Size(max = 255, message = "O nome da loja é muito longo.")
    private String nome;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 255, message = "O e-mail é muito longo.")
    private String email;

    @NotNull(message = "Informe a senha.")
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String senha;

    @AssertTrue(message = "A senha deve ter no máximo 72 bytes.")
    public boolean isSenhaDentroDoLimite() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= SENHA_MAXIMA_BYTES;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Textos.limpar(nome);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String limpo = Textos.limpar(email);
        this.email = limpo == null ? null : limpo.toLowerCase(Locale.ROOT);
    }

    public String getSenha() {
        return senha;
    }

    // A senha NÃO é aparada nem alterada: espaços fazem parte dela.
    public void setSenha(String senha) {
        this.senha = senha;
    }
}
