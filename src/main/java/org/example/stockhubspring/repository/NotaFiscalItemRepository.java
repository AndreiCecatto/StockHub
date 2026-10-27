package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.NotaFiscalItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Toda consulta passa pela loja dona da nota (nota_fiscal.usuario_codigo). */
public interface NotaFiscalItemRepository extends JpaRepository<NotaFiscalItem, Integer> {

    /** Vínculo aprendido: código do fornecedor (cProd) e o produto ao qual já foi ligado. */
    interface VinculoDoCodigo {
        String getCodigo();

        Integer getProdutoId();
    }

    @EntityGraph(attributePaths = {"produto", "movimentacao"})
    List<NotaFiscalItem> findByNotaFiscalIdAndNotaFiscalUsuarioCodigoOrderByNumeroItemAsc(Integer notaFiscalId,
                                                                                           Integer usuarioCodigo);

    /**
     * Memória do fornecedor, em UMA consulta para todos os códigos da nota: produtos ATIVOS da loja já vinculados a
     * esses códigos (cProd) em notas anteriores do mesmo parceiro, do vínculo mais recente para o mais antigo (o
     * chamador fica com o primeiro de cada código).
     */
    @Query("select i.codigoFornecedor as codigo, i.produto.id as produtoId from NotaFiscalItem i "
            + "where i.notaFiscal.usuario.codigo = :usuarioCodigo and i.notaFiscal.parceiro.id = :parceiroId "
            + "and i.codigoFornecedor in :codigos and i.produto is not null "
            + "and i.produto.usuario.codigo = :usuarioCodigo and i.produto.ativo = true "
            + "order by i.id desc")
    List<VinculoDoCodigo> vinculosDosCodigos(@Param("usuarioCodigo") Integer usuarioCodigo,
                                             @Param("parceiroId") Integer parceiroId,
                                             @Param("codigos") Collection<String> codigos,
                                             Pageable pageable);
}
