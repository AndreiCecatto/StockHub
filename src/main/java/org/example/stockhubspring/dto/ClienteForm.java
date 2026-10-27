package org.example.stockhubspring.dto;

/**
 * Dados digitados no cadastro de cliente (RN-91 a RN-96). Tudo chega como texto cru; o service limpa e valida.
 * Só o nome é obrigatório.
 *
 * @param tipoPessoa     "FISICA" ou "JURIDICA" (vazio: deduzido do CPF/CNPJ, se houver)
 * @param dataNascimento data no formato AAAA-MM-DD (nascimento da pessoa ou fundação da empresa)
 */
public record ClienteForm(String tipoPessoa, String documento, String nome, String nomeFantasia, String email,
                          String telefone, String whatsapp, String cep, String logradouro, String numero,
                          String complemento, String bairro, String cidade, String uf, String dataNascimento,
                          String observacoes, String origem) implements java.io.Serializable {

    /** Formulário preenchido com o cadastro existente (edição). */
    public static ClienteForm de(org.example.stockhubspring.model.Parceiro p) {
        return new ClienteForm(p.getTipoPessoa(), p.getDocumento(), p.getNome(), p.getNomeFantasia(), p.getEmail(),
                p.getTelefone(), p.getWhatsapp(), p.getCep(), p.getLogradouro(), p.getNumero(), p.getComplemento(),
                p.getBairro(), p.getCidade(), p.getUf(),
                p.getDataNascimento() == null ? null : p.getDataNascimento().toString(), p.getObservacoes(),
                p.getOrigem());
    }

    /** Formulário só com o básico (usado pelo cadastro antigo de três campos). */
    public static ClienteForm basico(String documento, String nome, String uf) {
        return new ClienteForm(null, documento, nome, null, null, null, null, null, null, null, null, null, null, uf,
                null, null, null);
    }
}
