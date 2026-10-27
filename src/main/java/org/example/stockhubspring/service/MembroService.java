package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Equipe da loja (RN-61 a RN-65). Toda operação é escopada à loja de quem pede (RN-01) e confere o papel
 * de quem pede, mesmo que a tela já esconda o botão.
 */
@Service
public class MembroService {

    public static final String MSG_CRIACAO_GENERICA =
            "Não foi possível criar a pessoa. Confira os dados; o e-mail pode já estar em uso.";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MembroLojaRepository membroRepository;
    private final PasswordEncoder passwordEncoder;

    public MembroService(MembroLojaRepository membroRepository, PasswordEncoder passwordEncoder) {
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Equipe da loja: ativos primeiro, depois por nome. */
    @Transactional(readOnly = true)
    public List<MembroLoja> listar(Integer lojaCodigo) {
        return membroRepository.findByLojaCodigoOrderByAtivoDescNomeAsc(lojaCodigo);
    }

    /**
     * Cria uma pessoa na loja com a senha inicial definida por quem convida (mínimo 8 caracteres, BCrypt).
     * O papel DONO não pode ser criado (a loja só tem um); o gerente não cria outro gerente.
     */
    @Transactional
    public MembroLoja criar(Integer lojaCodigo, PapelMembro papelDoAtor, String nome, String email, String senha,
                            PapelMembro papel) {
        exigirGestor(papelDoAtor);
        String nomeLimpo = nome == null ? null : nome.trim();
        String emailNormalizado = UsuarioService.normalizarEmail(email);
        if (nomeLimpo == null || nomeLimpo.isBlank()) {
            throw new RegraNegocioException("Informe o nome da pessoa.");
        }
        if (nomeLimpo.length() > 255) {
            throw new RegraNegocioException("O nome da pessoa é muito longo.");
        }
        if (emailNormalizado == null || emailNormalizado.length() > 255
                || !EMAIL.matcher(emailNormalizado).matches()) {
            throw new RegraNegocioException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < UsuarioService.SENHA_MINIMA) {
            throw new RegraNegocioException(
                    "A senha inicial deve ter pelo menos " + UsuarioService.SENHA_MINIMA + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > UsuarioService.SENHA_MAXIMA) {
            throw new RegraNegocioException(
                    "A senha inicial deve ter no máximo " + UsuarioService.SENHA_MAXIMA + " bytes.");
        }
        if (papel == null || !papel.escolhivel()) {
            throw new RegraNegocioException("Escolha o papel da pessoa.");
        }
        if (papel == PapelMembro.GERENTE && papelDoAtor != PapelMembro.DONO) {
            throw new RegraNegocioException("Só o dono pode criar um gerente.");
        }
        // Mensagem genérica: não revela se o e-mail já existe em outra loja
        if (membroRepository.existsByEmail(emailNormalizado)) {
            throw new RegraNegocioException(MSG_CRIACAO_GENERICA);
        }

        MembroLoja membro = new MembroLoja();
        membro.setLojaCodigo(lojaCodigo);
        membro.setNome(nomeLimpo);
        membro.setEmail(emailNormalizado);
        membro.setSenha(passwordEncoder.encode(senha));
        membro.setPapel(papel);
        membro.setAtivo(true);
        try {
            return membroRepository.saveAndFlush(membro);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException(MSG_CRIACAO_GENERICA);
        }
    }

    /** Desativa (nunca exclui): a pessoa deixa de entrar e o histórico dela continua (RN-65). */
    @Transactional
    public MembroLoja desativar(Integer lojaCodigo, Integer membroDoAtor, PapelMembro papelDoAtor, Integer id) {
        MembroLoja alvo = buscarDaLoja(lojaCodigo, papelDoAtor, id);
        if (alvo.getId().equals(membroDoAtor)) {
            throw new RegraNegocioException("Você não pode desativar a si mesmo.");
        }
        exigirPodeMexer(papelDoAtor, alvo);
        alvo.setAtivo(false);
        return membroRepository.save(alvo);
    }

    /** Volta a ativar uma pessoa desativada. */
    @Transactional
    public MembroLoja reativar(Integer lojaCodigo, PapelMembro papelDoAtor, Integer id) {
        MembroLoja alvo = buscarDaLoja(lojaCodigo, papelDoAtor, id);
        exigirPodeMexer(papelDoAtor, alvo);
        alvo.setAtivo(true);
        return membroRepository.save(alvo);
    }

    private MembroLoja buscarDaLoja(Integer lojaCodigo, PapelMembro papelDoAtor, Integer id) {
        exigirGestor(papelDoAtor);
        // Membro de outra loja se comporta como inexistente (RN-01)
        return membroRepository.findByIdAndLojaCodigo(id, lojaCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada."));
    }

    private static void exigirGestor(PapelMembro papelDoAtor) {
        if (papelDoAtor == null || !papelDoAtor.podeGerenciarMembros()) {
            throw new RegraNegocioException("Só o dono ou um gerente cuida da equipe.");
        }
    }

    /** O dono ninguém mexe; o gerente só mexe em quem não é gerente. */
    private static void exigirPodeMexer(PapelMembro papelDoAtor, MembroLoja alvo) {
        if (alvo.getPapel() == PapelMembro.DONO) {
            throw new RegraNegocioException("O dono da loja não pode ser desativado.");
        }
        if (alvo.getPapel() == PapelMembro.GERENTE && papelDoAtor != PapelMembro.DONO) {
            throw new RegraNegocioException("Só o dono pode mexer em um gerente.");
        }
    }
}
