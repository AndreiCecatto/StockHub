package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.util.Csv;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera os arquivos CSV de produtos e do histórico. Os dados vêm dos services (sempre escopados
 * à loja, RN-01) e os valores de texto passam pela proteção contra injeção de fórmula de {@link Csv}.
 */
@Service
public class ExportacaoService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Arquivo pronto para download.
     *
     * @param truncado verdadeiro se havia mais linhas do que o limite de segurança
     */
    public record ArquivoCsv(String nome, byte[] conteudo, boolean truncado) {
    }

    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public ExportacaoService(ProdutoService produtoService, EstoqueService estoqueService) {
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    /** Produtos ativos da loja, com os mesmos filtros e ordenação da listagem (sem paginação). */
    public ArquivoCsv exportarProdutos(ProdutoFiltro filtro, Integer usuarioCodigo) {
        return exportarProdutos(filtro, usuarioCodigo, List.of());
    }

    /** Igual ao anterior, mas restrito aos ids marcados na tela (lista vazia = todos do filtro). */
    public ArquivoCsv exportarProdutos(ProdutoFiltro filtro, Integer usuarioCodigo, List<Integer> ids) {
        Page<Produto> produtos = ids.isEmpty()
                ? produtoService.listarParaExportar(filtro, usuarioCodigo)
                : produtoService.listarParaExportar(filtro, usuarioCodigo, ids);
        List<String> linhas = new ArrayList<>();
        linhas.add(Csv.linha(Csv.texto("Código"), Csv.texto("Nome"), Csv.texto("Descrição"),
                Csv.texto("Categoria"), Csv.texto("Preço (R$)"), Csv.texto("Quantidade"),
                Csv.texto("Estoque mínimo"), Csv.texto("Nível de estoque"), Csv.texto("Valor em estoque (R$)")));
        for (Produto produto : produtos.getContent()) {
            linhas.add(Csv.linha(
                    Csv.inteiro(produto.getId()),
                    Csv.texto(produto.getNome()),
                    Csv.texto(produto.getDescricao()),
                    Csv.texto(produto.getCategoria().getNome()),
                    Csv.decimal(produto.getPreco()),
                    Csv.inteiro(produto.getQuantidade()),
                    Csv.inteiro(produto.getEstoqueMinimo()),
                    Csv.texto(produto.getNivelEstoque().getDescricao()),
                    Csv.decimal(produto.getValorTotal())));
        }
        return new ArquivoCsv(nomeComData("produtos"), Csv.bytes(linhas), truncado(produtos));
    }

    /** Histórico da loja com os mesmos filtros da tela de movimentações (sem paginação). */
    public ArquivoCsv exportarMovimentacoes(MovimentacaoFiltro filtro, Integer usuarioCodigo) {
        Page<MovimentacaoEstoque> movimentacoes = estoqueService.listarParaExportar(usuarioCodigo, filtro);
        List<String> linhas = new ArrayList<>();
        linhas.add(Csv.linha(Csv.texto("Data e hora"), Csv.texto("Código do produto"), Csv.texto("Produto"),
                Csv.texto("Categoria"), Csv.texto("Tipo"), Csv.texto("Quantidade"),
                Csv.texto("Saldo anterior"), Csv.texto("Saldo novo"), Csv.texto("Observação")));
        for (MovimentacaoEstoque mov : movimentacoes.getContent()) {
            linhas.add(Csv.linha(
                    Csv.texto(mov.getCriadoEm() == null ? null : DATA_HORA.format(mov.getCriadoEm())),
                    Csv.inteiro(mov.getProduto().getId()),
                    Csv.texto(mov.getProduto().getNome()),
                    Csv.texto(mov.getProduto().getCategoria().getNome()),
                    Csv.texto(descricao(mov.getTipo())),
                    Csv.inteiro(mov.getQuantidade()),
                    Csv.inteiro(mov.getQuantidadeAnterior()),
                    Csv.inteiro(mov.getQuantidadeNova()),
                    Csv.texto(mov.getObservacao())));
        }
        return new ArquivoCsv(nomeComData("movimentacoes"), Csv.bytes(linhas), truncado(movimentacoes));
    }

    private static boolean truncado(Page<?> pagina) {
        return pagina.getTotalElements() > pagina.getNumberOfElements();
    }

    private static String nomeComData(String base) {
        return base + "-" + LocalDate.now() + ".csv";
    }

    private static String descricao(TipoMovimentacao tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case ENTRADA -> "Entrada";
            case SAIDA -> "Saída";
            case AJUSTE -> "Ajuste";
        };
    }
}
