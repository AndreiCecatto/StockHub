package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.dto.ClienteForm;
import org.example.stockhubspring.dto.ClienteLinha;
import org.example.stockhubspring.dto.ClienteResumo;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.ClienteSpecs;
import org.example.stockhubspring.repository.OrdemServicoRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.util.Documentos;
import org.example.stockhubspring.util.Paginas;
import org.example.stockhubspring.util.Textos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clientes da loja (RN-76, RN-91 a RN-98): o mesmo {@link Parceiro} do fornecedor, com o tipo CLIENTE ligado.
 * Toda consulta é da loja (RN-01): cliente de outra loja se comporta como inexistente.
 */
@Service
public class ClienteService {

    public static final int LIMITE_LISTA = 500;
    public static final int NOME_MAXIMO = 255;
    public static final int ULTIMAS_OS = 5;

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String PADRAO_EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final ParceiroRepository parceiroRepository;
    private final OrdemServicoRepository osRepository;

    public ClienteService(ParceiroRepository parceiroRepository, OrdemServicoRepository osRepository) {
        this.parceiroRepository = parceiroRepository;
        this.osRepository = osRepository;
    }

    /** Clientes ATIVOS por nome, para escolher na ordem de serviço. */
    @Transactional(readOnly = true)
    public List<Parceiro> listar(Integer lojaCodigo) {
        return parceiroRepository.findByUsuarioCodigoAndClienteTrueAndAtivoTrueOrderByNomeAsc(lojaCodigo,
                PageRequest.of(0, LIMITE_LISTA));
    }

    /** Página da lista de clientes com busca e filtros (RN-97), cada linha com a contagem de OS. */
    @Transactional(readOnly = true)
    public Page<ClienteLinha> listarPagina(Integer lojaCodigo, ClienteFiltro filtro) {
        Sort ordem = filtro.ordem() == ClienteFiltro.Ordem.RECENTES
                ? Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));
        Page<Parceiro> pagina = Paginas.buscar(filtro.pagina(), filtro.tamanho(), ClienteFiltro.TAMANHO_MAXIMO, ordem,
                pageable -> parceiroRepository.findAll(ClienteSpecs.de(lojaCodigo, filtro), pageable));
        Map<Integer, Long> totais = pagina.isEmpty() ? Map.of()
                : osRepository.contarPorClientes(lojaCodigo, pagina.stream().map(Parceiro::getId).toList()).stream()
                        .collect(Collectors.toMap(OrdemServicoRepository.ContagemCliente::getClienteId,
                                OrdemServicoRepository.ContagemCliente::getTotal));
        return pagina.map(p -> new ClienteLinha(p, totais.getOrDefault(p.getId(), 0L)));
    }

    /** O cliente da loja (ativo ou não); outro tipo de parceiro ou de outra loja é 404. */
    @Transactional(readOnly = true)
    public Parceiro buscar(Integer lojaCodigo, Integer id) {
        return parceiroRepository.findByIdAndUsuarioCodigo(id, lojaCodigo).filter(Parceiro::isCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    /** O que o modelo sabe do cliente: ordens de serviço, total entregue e última visita (RN-98). */
    @Transactional(readOnly = true)
    public ClienteResumo resumo(Integer lojaCodigo, Integer id) {
        Integer loja = lojaCodigo;
        long total = osRepository.countByUsuarioCodigoAndClienteId(loja, id);
        long andamento = osRepository.countByUsuarioCodigoAndClienteIdAndStatusIn(loja, id,
                EnumSet.of(StatusOs.ORCAMENTO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO, StatusOs.CONCLUIDA));
        long entregues = osRepository.countByUsuarioCodigoAndClienteIdAndStatus(loja, id, StatusOs.ENTREGUE);
        BigDecimal soma = osRepository.somarEntregues(loja, id);
        LocalDateTime ultima = osRepository.ultimaAbertura(loja, id);
        return new ClienteResumo(total, andamento, entregues, soma == null ? BigDecimal.ZERO : soma, ultima,
                osRepository.doCliente(loja, id, PageRequest.of(0, ULTIMAS_OS)));
    }

    /** Cadastro antigo de três campos (documento obrigatório); mantido para quem já chamava assim. */
    @Transactional
    public Parceiro cadastrar(Usuario loja, String documentoDigitado, String nomeDigitado, String ufDigitada) {
        if (Documentos.soDigitos(documentoDigitado).isEmpty()) {
            throw new RegraNegocioException("Informe um CPF ou CNPJ válido (só números ou com pontos e traço).");
        }
        return cadastrar(loja, ClienteForm.basico(documentoDigitado, nomeDigitado, ufDigitada));
    }

    /**
     * Cadastra um cliente. Só o nome é obrigatório (RN-91). Se a loja já tem um parceiro com o mesmo CPF/CNPJ (um
     * fornecedor, por exemplo), ele passa a ser também cliente, sem mudar o nome dele (RN-38) e só recebendo os
     * campos que ainda estavam vazios.
     */
    @Transactional
    public Parceiro cadastrar(Usuario loja, ClienteForm form) {
        Dados d = validar(form);
        Parceiro existente = d.documento == null ? null
                : parceiroRepository.findByUsuarioCodigoAndDocumento(loja.getCodigo(), d.documento).orElse(null);
        if (existente != null && existente.isCliente()) {
            throw new RegraNegocioException("Já existe um cliente com este CPF ou CNPJ.");
        }
        try {
            if (existente != null) {
                existente.setCliente(true);
                existente.setAtivo(true);
                preencherVazios(existente, d);
                existente.setAtualizadoEm(agora());
                return parceiroRepository.saveAndFlush(existente);
            }
            Parceiro novo = new Parceiro();
            novo.setUsuario(loja);
            novo.setNome(d.nome);
            novo.setFornecedor(false);
            novo.setCliente(true);
            novo.setCriadoEm(agora());
            aplicar(novo, d);
            return parceiroRepository.saveAndFlush(novo);
        } catch (DataIntegrityViolationException e) {
            // Duas pessoas cadastrando o mesmo documento ao mesmo tempo: a segunda cai na unicidade da loja
            throw new RegraNegocioException("Já existe um cadastro com este CPF ou CNPJ.");
        }
    }

    /** Atualiza o cadastro (RN-95). Quem também é fornecedor não pode ter o CPF/CNPJ trocado por aqui. */
    @Transactional
    public Parceiro atualizar(Integer lojaCodigo, Integer id, ClienteForm form) {
        Parceiro cliente = buscar(lojaCodigo, id);
        Dados d = validar(form);
        boolean mudouDocumento = d.documento == null ? cliente.getDocumento() != null
                : !d.documento.equals(cliente.getDocumento());
        if (mudouDocumento) {
            if (cliente.isFornecedor() && cliente.getDocumento() != null) {
                throw new RegraNegocioException(
                        "Este cadastro também é fornecedor, por isso o CPF ou CNPJ não pode ser trocado aqui.");
            }
            if (d.documento != null) {
                parceiroRepository.findByUsuarioCodigoAndDocumento(lojaCodigo, d.documento)
                        .filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> {
                            throw new RegraNegocioException(outro.isCliente()
                                    ? "Já existe um cliente com este CPF ou CNPJ."
                                    : "Já existe um cadastro com este CPF ou CNPJ.");
                        });
            }
        }
        cliente.setNome(d.nome);
        aplicar(cliente, d);
        cliente.setAtualizadoEm(agora());
        try {
            return parceiroRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Já existe um cadastro com este CPF ou CNPJ.");
        }
    }

    /** Inativa ou reativa o cliente (RN-96): nada é apagado; o inativo some da escolha na OS. */
    @Transactional
    public Parceiro alterarSituacao(Integer lojaCodigo, Integer id, boolean ativo) {
        Parceiro cliente = buscar(lojaCodigo, id);
        cliente.setAtivo(ativo);
        cliente.setAtualizadoEm(agora());
        return parceiroRepository.save(cliente);
    }

    // ------------------------------------------------------------------ validação

    /** Dados já limpos e conferidos, prontos para gravar. */
    private static final class Dados {
        String tipoPessoa;
        String documento;
        String nome;
        String nomeFantasia;
        String email;
        String telefone;
        String whatsapp;
        String cep;
        String logradouro;
        String numero;
        String complemento;
        String bairro;
        String cidade;
        String uf;
        LocalDate dataNascimento;
        String observacoes;
        String origem;
    }

    private Dados validar(ClienteForm f) {
        Dados d = new Dados();
        d.nome = Textos.limpar(f.nome());
        if (d.nome == null) {
            throw new RegraNegocioException("Informe o nome do cliente.");
        }
        limite(d.nome, NOME_MAXIMO, "O nome");

        String documento = Documentos.soDigitos(f.documento());
        String tipoDoDocumento = null;
        if (!documento.isEmpty()) {
            if (documento.length() == 11 && Documentos.cpfValido(documento)) {
                tipoDoDocumento = "FISICA";
            } else if (documento.length() == 14 && Documentos.cnpjValido(documento)) {
                tipoDoDocumento = "JURIDICA";
            } else {
                throw new RegraNegocioException(
                        "O CPF ou CNPJ informado não é válido. Confira os números ou deixe o campo em branco.");
            }
            d.documento = documento;
        }

        String tipo = Textos.limpar(f.tipoPessoa());
        if (tipo != null) {
            tipo = tipo.toUpperCase(Locale.ROOT);
            if (!tipo.equals("FISICA") && !tipo.equals("JURIDICA")) {
                throw new RegraNegocioException("Escolha se o cliente é pessoa física ou jurídica.");
            }
            if (tipoDoDocumento != null && !tipo.equals(tipoDoDocumento)) {
                throw new RegraNegocioException("O tipo de pessoa não combina com o "
                        + (tipoDoDocumento.equals("FISICA") ? "CPF" : "CNPJ") + " informado.");
            }
        }
        d.tipoPessoa = tipoDoDocumento != null ? tipoDoDocumento : tipo;

        d.nomeFantasia = texto(f.nomeFantasia(), NOME_MAXIMO, "O nome fantasia");
        String email = texto(f.email(), 255, "O e-mail");
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
            if (!email.matches(PADRAO_EMAIL)) {
                throw new RegraNegocioException("O e-mail informado não parece correto. Exemplo: nome@exemplo.com.");
            }
        }
        d.email = email;
        d.telefone = telefone(f.telefone(), "O telefone");
        d.whatsapp = telefone(f.whatsapp(), "O WhatsApp");

        String cep = Documentos.soDigitos(f.cep());
        if (!cep.isEmpty() && cep.length() != 8) {
            throw new RegraNegocioException("O CEP deve ter 8 números, como 97010-000.");
        }
        d.cep = cep.isEmpty() ? null : cep;
        d.logradouro = texto(f.logradouro(), 255, "A rua");
        d.numero = texto(f.numero(), 20, "O número");
        d.complemento = texto(f.complemento(), 100, "O complemento");
        d.bairro = texto(f.bairro(), 100, "O bairro");
        d.cidade = texto(f.cidade(), 100, "A cidade");

        String uf = Textos.limpar(f.uf());
        if (uf != null) {
            uf = uf.toUpperCase(Locale.ROOT);
            if (!uf.matches("[A-Z]{2}")) {
                throw new RegraNegocioException("A UF deve ter 2 letras, como RS.");
            }
        }
        d.uf = uf;

        String data = Textos.limpar(f.dataNascimento());
        if (data != null) {
            try {
                d.dataNascimento = LocalDate.parse(data);
            } catch (DateTimeParseException e) {
                throw new RegraNegocioException("A data informada não é válida.");
            }
            if (d.dataNascimento.isAfter(LocalDate.now(FUSO)) || d.dataNascimento.getYear() < 1900) {
                throw new RegraNegocioException("A data de nascimento ou fundação não pode ser futura nem anterior a 1900.");
            }
        }
        d.observacoes = texto(f.observacoes(), 1000, "As observações");
        d.origem = texto(f.origem(), 120, "A origem");
        return d;
    }

    private static String texto(String valor, int maximo, String campo) {
        String limpo = Textos.limpar(valor);
        if (limpo != null) {
            limite(limpo, maximo, campo);
        }
        return limpo;
    }

    private static void limite(String valor, int maximo, String campo) {
        if (valor.length() > maximo) {
            throw new RegraNegocioException(campo + " deve ter no máximo " + maximo + " caracteres.");
        }
    }

    /** Telefone com DDD (10 ou 11 dígitos); aceita o +55 na frente. */
    private static String telefone(String digitado, String campo) {
        String digitos = Documentos.soDigitos(digitado);
        if (digitos.isEmpty()) {
            return null;
        }
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new RegraNegocioException(campo + " deve ter o DDD e o número, como (55) 99999-0000.");
        }
        return digitos;
    }

    private static void aplicar(Parceiro p, Dados d) {
        p.setDocumento(d.documento);
        p.setTipoPessoa(d.tipoPessoa);
        p.setNomeFantasia(d.nomeFantasia);
        p.setEmail(d.email);
        p.setTelefone(d.telefone);
        p.setWhatsapp(d.whatsapp);
        p.setCep(d.cep);
        p.setLogradouro(d.logradouro);
        p.setNumero(d.numero);
        p.setComplemento(d.complemento);
        p.setBairro(d.bairro);
        p.setCidade(d.cidade);
        p.setUf(d.uf);
        p.setDataNascimento(d.dataNascimento);
        p.setObservacoes(d.observacoes);
        p.setOrigem(d.origem);
    }

    /** Fornecedor que vira cliente: só entra o que o cadastro dele ainda não tinha. */
    private static void preencherVazios(Parceiro p, Dados d) {
        p.setTipoPessoa(ou(p.getTipoPessoa(), d.tipoPessoa));
        p.setNomeFantasia(ou(p.getNomeFantasia(), d.nomeFantasia));
        p.setEmail(ou(p.getEmail(), d.email));
        p.setTelefone(ou(p.getTelefone(), d.telefone));
        p.setWhatsapp(ou(p.getWhatsapp(), d.whatsapp));
        p.setCep(ou(p.getCep(), d.cep));
        p.setLogradouro(ou(p.getLogradouro(), d.logradouro));
        p.setNumero(ou(p.getNumero(), d.numero));
        p.setComplemento(ou(p.getComplemento(), d.complemento));
        p.setBairro(ou(p.getBairro(), d.bairro));
        p.setCidade(ou(p.getCidade(), d.cidade));
        p.setUf(ou(p.getUf(), d.uf));
        if (p.getDataNascimento() == null) {
            p.setDataNascimento(d.dataNascimento);
        }
        p.setObservacoes(ou(p.getObservacoes(), d.observacoes));
        p.setOrigem(ou(p.getOrigem(), d.origem));
    }

    private static String ou(String atual, String novo) {
        return atual != null ? atual : novo;
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(FUSO);
    }
}
