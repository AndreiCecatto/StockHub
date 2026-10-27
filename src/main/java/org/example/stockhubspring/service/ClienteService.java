package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.util.Documentos;
import org.example.stockhubspring.util.Textos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Clientes da loja (RN-76): o mesmo {@link Parceiro} do fornecedor, com o tipo CLIENTE ligado.
 * Toda consulta é da loja (RN-01).
 */
@Service
public class ClienteService {

    public static final int LIMITE_LISTA = 500;
    public static final int NOME_MAXIMO = 255;

    private final ParceiroRepository parceiroRepository;

    public ClienteService(ParceiroRepository parceiroRepository) {
        this.parceiroRepository = parceiroRepository;
    }

    @Transactional(readOnly = true)
    public List<Parceiro> listar(Integer lojaCodigo) {
        return parceiroRepository.findByUsuarioCodigoAndClienteTrueOrderByNomeAsc(lojaCodigo,
                PageRequest.of(0, LIMITE_LISTA));
    }

    /**
     * Cadastra um cliente. Se a loja já tem um parceiro com o mesmo CPF/CNPJ (um fornecedor, por exemplo), ele
     * passa a ser também cliente, sem mudar o nome dele (RN-38: o cadastro existente não é reescrito).
     */
    @Transactional
    public Parceiro cadastrar(Usuario loja, String documentoDigitado, String nomeDigitado, String ufDigitada) {
        String documento = Documentos.soDigitos(documentoDigitado);
        boolean cpf = documento.length() == 11 && Documentos.cpfValido(documento);
        boolean cnpj = documento.length() == 14 && Documentos.cnpjValido(documento);
        if (!cpf && !cnpj) {
            throw new RegraNegocioException("Informe um CPF ou CNPJ válido (só números ou com pontos e traço).");
        }
        String nome = Textos.limpar(nomeDigitado);
        if (nome == null) {
            throw new RegraNegocioException("Informe o nome do cliente.");
        }
        if (nome.length() > NOME_MAXIMO) {
            throw new RegraNegocioException("O nome deve ter no máximo " + NOME_MAXIMO + " caracteres.");
        }
        String uf = Textos.limpar(ufDigitada);
        if (uf != null) {
            uf = uf.toUpperCase(Locale.ROOT);
            if (!uf.matches("[A-Z]{2}")) {
                throw new RegraNegocioException("A UF deve ter 2 letras, como RS.");
            }
        }

        Parceiro existente = parceiroRepository.findByUsuarioCodigoAndDocumento(loja.getCodigo(), documento).orElse(null);
        if (existente != null && existente.isCliente()) {
            throw new RegraNegocioException("Já existe um cliente com este CPF ou CNPJ.");
        }
        try {
            if (existente != null) {
                existente.setCliente(true);
                return parceiroRepository.saveAndFlush(existente);
            }
            Parceiro novo = new Parceiro();
            novo.setUsuario(loja);
            novo.setDocumento(documento);
            novo.setNome(nome);
            novo.setUf(uf);
            novo.setFornecedor(false);
            novo.setCliente(true);
            novo.setCriadoEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
            return parceiroRepository.saveAndFlush(novo);
        } catch (DataIntegrityViolationException e) {
            // Duas pessoas cadastrando o mesmo documento ao mesmo tempo: a segunda cai na unicidade da loja
            throw new RegraNegocioException("Já existe um cadastro com este CPF ou CNPJ.");
        }
    }
}
