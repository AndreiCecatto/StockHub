package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RN-14: cadastro de loja/usuário. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    private static final String SENHA_OK = "senha-segura-123";

    @Mock
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(usuarioRepository, encoder);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void verificarQueNadaFoiSalvo() {
        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("normaliza o e-mail: sem espaços nas pontas e em minúsculas")
    void normalizaEmail() {
        Usuario salvo = service.cadastrarLoja("  Minha Loja ", "  Loja@EMAIL.Teste  ", SENHA_OK);

        assertThat(salvo.getEmail()).isEqualTo("loja@email.teste");
        assertThat(salvo.getNome()).isEqualTo("Minha Loja");
        assertThat(salvo.isAtivo()).isTrue();
        verify(usuarioRepository).existsByEmail("loja@email.teste");
    }

    @Test
    @DisplayName("e-mail duplicado é recusado com mensagem genérica, que não cita o e-mail")
    void emailDuplicadoMensagemGenerica() {
        when(usuarioRepository.existsByEmail("loja@email.teste")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrarLoja("Loja", "Loja@Email.Teste", SENHA_OK))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(UsuarioService.MSG_CADASTRO_GENERICA)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase()).doesNotContain("e-mail já", "existe", "loja@email"));
        verifyNadaSalvoEBuscaIncluiInativos();
    }

    private void verifyNadaSalvoEBuscaIncluiInativos() {
        verificarQueNadaFoiSalvo();
        // existsByEmail não filtra por ativo: e-mail de usuário inativo também conta como duplicado
        verify(usuarioRepository, never()).findByEmailAndAtivoTrue(anyString());
    }

    @Test
    @DisplayName("corrida entre dois cadastros (violação de unicidade) também responde com a mensagem genérica")
    void corridaRespondeComMensagemGenerica() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.cadastrarLoja("Loja", "novo@email.teste", SENHA_OK))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(UsuarioService.MSG_CADASTRO_GENERICA);
    }

    @Test
    @DisplayName("a senha é gravada com hash BCrypt e nunca igual à digitada")
    void senhaGravadaComHashBCrypt() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

        service.cadastrarLoja("Loja", "novo@email.teste", SENHA_OK);

        verify(usuarioRepository).saveAndFlush(captor.capture());
        String gravada = captor.getValue().getSenha();
        assertThat(gravada).isNotEqualTo(SENHA_OK).startsWith("$2");
        assertThat(encoder.matches(SENHA_OK, gravada)).isTrue();
        assertThat(encoder.matches("outra-senha-xyz", gravada)).isFalse();
    }

    @Test
    @DisplayName("a mesma senha em dois cadastros gera hashes diferentes (salt)")
    void mesmaSenhaGeraHashesDiferentes() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        service.cadastrarLoja("Loja A", "a@email.teste", SENHA_OK);
        service.cadastrarLoja("Loja B", "b@email.teste", SENHA_OK);

        verify(usuarioRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues().get(0).getSenha()).isNotEqualTo(captor.getAllValues().get(1).getSenha());
    }

    @ParameterizedTest(name = "senha [{0}] é recusada")
    @NullSource
    @ValueSource(strings = {"", "1234567", "abc"})
    @DisplayName("senha nula ou com menos de 8 caracteres é recusada")
    void senhaCurtaRecusada(String senha) {
        assertThatThrownBy(() -> service.cadastrarLoja("Loja", "novo@email.teste", senha))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pelo menos 8");
        verificarQueNadaFoiSalvo();
    }

    @Test
    @DisplayName("senha com exatamente 8 caracteres é aceita")
    void senhaDeOitoCaracteresAceita() {
        assertThat(service.cadastrarLoja("Loja", "novo@email.teste", "12345678")).isNotNull();
    }

    @Test
    @DisplayName("senha com mais de 72 bytes é recusada e com exatamente 72 bytes é aceita")
    void limiteDe72Bytes() {
        assertThat(service.cadastrarLoja("Loja", "a@email.teste", "a".repeat(72))).isNotNull();

        assertThatThrownBy(() -> service.cadastrarLoja("Loja", "b@email.teste", "a".repeat(73)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("72 bytes");
    }

    @Test
    @DisplayName("o limite de 72 é em bytes, não em caracteres: 37 letras 'é' (74 bytes) são recusadas")
    void limiteEmBytesComAcentos() {
        assertThat(service.cadastrarLoja("Loja", "a@email.teste", "é".repeat(36))).isNotNull();

        assertThatThrownBy(() -> service.cadastrarLoja("Loja", "b@email.teste", "é".repeat(37)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("72 bytes");
    }

    @ParameterizedTest(name = "e-mail [{0}] é recusado")
    @NullSource
    @ValueSource(strings = {"", "   ", "semarroba", "a@b", "@dominio.com", "a b@dominio.com", "a@@dominio.com"})
    @DisplayName("e-mail vazio ou mal formado é recusado")
    void emailInvalidoRecusado(String email) {
        assertThatThrownBy(() -> service.cadastrarLoja("Loja", email, SENHA_OK))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail válido");
        verificarQueNadaFoiSalvo();
    }

    @ParameterizedTest(name = "nome [{0}] é recusado")
    @NullSource
    @ValueSource(strings = {"", "    "})
    @DisplayName("nome da loja vazio é recusado")
    void nomeVazioRecusado(String nome) {
        assertThatThrownBy(() -> service.cadastrarLoja(nome, "novo@email.teste", SENHA_OK))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nome da loja");
        verificarQueNadaFoiSalvo();
    }

    @Test
    @DisplayName("nome da loja com mais de 255 caracteres é recusado")
    void nomeMuitoLongoRecusado() {
        assertThatThrownBy(() -> service.cadastrarLoja("x".repeat(256), "novo@email.teste", SENHA_OK))
                .isInstanceOf(RegraNegocioException.class);
        verificarQueNadaFoiSalvo();
    }

    @Test
    @DisplayName("nome com <script> é gravado como dado, sem alteração (o escape é feito na saída)")
    void nomeComScriptEGravadoComoDado() {
        Usuario salvo = service.cadastrarLoja("<script>alert(1)</script>", "novo@email.teste", SENHA_OK);
        assertThat(salvo.getNome()).isEqualTo("<script>alert(1)</script>");
    }

    @Test
    @DisplayName("normalizarEmail trata null")
    void normalizarEmailNulo() {
        assertThat(UsuarioService.normalizarEmail(null)).isNull();
        assertThat(UsuarioService.normalizarEmail("  A@B.C ")).isEqualTo("a@b.c");
    }
}
