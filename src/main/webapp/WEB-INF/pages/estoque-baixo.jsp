<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Estoque baixo (novo design, lote 5): a lista de compras do dono. Rota: GET /estoque-baixo?pagina=N (100 por página).
    Dados do DashboardController: produtos (Produto com categoria.nome, quantidade, estoqueMinimo, nivelEstoque; já em ordem
    de menor saldo, então os sem estoque vêm primeiro), paginaAtual, totalPaginas, totalProdutosFiltro.
    Exportar CSV reaproveita /produtos/exportar.csv: o filtro aceita UM nível por vez, então são dois links
    (nivelEstoque=semEstoque e nivelEstoque=baixo), que juntos cobrem esta lista.
    Impressão: css/impressao.css esconde menus e botões; aparece a coluna em branco "Comprar" (.sh-so-impressao).
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:url var="urlCsvSem" value="/produtos/exportar.csv">
    <c:param name="nivelEstoque" value="semEstoque" />
    <c:param name="ordenacao" value="quantidade" />
    <c:param name="direcao" value="asc" />
</c:url>
<c:url var="urlCsvBaixo" value="/produtos/exportar.csv">
    <c:param name="nivelEstoque" value="baixo" />
    <c:param name="ordenacao" value="quantidade" />
    <c:param name="direcao" value="asc" />
</c:url>

<sh:layout titulo="Estoque baixo" ativo="estoqueBaixo">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/relatorios.css">
        <script defer src="${ctx}/js/relatorios.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Estoque baixo" descricao="Produtos sem estoque ou no mínimo ou abaixo dele: a sua lista de compras.">
            <button type="button" class="btn btn-primary" data-sh-imprimir hidden>
                <i class="bi bi-printer" aria-hidden="true"></i> Imprimir lista
            </button>
            <div class="dropdown">
                <button type="button" class="btn btn-outline-secondary" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="<c:out value='${urlCsvSem}'/>">Produtos sem estoque</a></li>
                    <li><a class="dropdown-item" href="<c:out value='${urlCsvBaixo}'/>">Produtos com estoque baixo</a></li>
                </ul>
            </div>
        </sh:cabecalhoPagina>

        <p class="sh-imprimir-titulo">Lista de compras &middot; StockHub &middot; <c:out value="${totalProdutosFiltro}" /> produto(s)</p>

        <c:choose>
            <c:when test="${empty produtos and totalProdutosFiltro > 0}">
                <div class="sh-card">
                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                        texto="A lista tem menos páginas do que a que você pediu."
                        acaoUrl="/estoque-baixo" acaoRotulo="Voltar ao início da lista" />
                </div>
            </c:when>
            <c:when test="${empty produtos}">
                <div class="sh-card">
                    <sh:estadoVazio icone="check-circle" tom="sucesso" titulo="Nenhum produto abaixo do mínimo"
                        texto="Tudo em dia: todos os produtos estão acima do estoque mínimo."
                        acaoUrl="/produtos?tamanho=25" acaoRotulo="Ver produtos" />
                </div>
            </c:when>
            <c:otherwise>
                <p class="sh-rel__contagem sh-nao-imprimir">
                    <strong><c:out value="${totalProdutosFiltro}" /></strong>
                    <c:out value="${totalProdutosFiltro == 1 ? 'produto precisa' : 'produtos precisam'}" /> de reposição
                </p>
                <div class="sh-table-wrap">
                    <table class="sh-table sh-table--cards" role="table">
                        <caption class="visually-hidden">Produtos sem estoque ou com estoque baixo, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                        <thead role="rowgroup">
                            <tr role="row">
                                <th scope="col" role="columnheader">Produto</th>
                                <th scope="col" role="columnheader">Categoria</th>
                                <th scope="col" role="columnheader">Saldo / mínimo</th>
                                <th scope="col" role="columnheader">Para o mínimo</th>
                                <th scope="col" role="columnheader">Situação</th>
                                <th scope="col" role="columnheader" class="sh-so-impressao">Comprar</th>
                                <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                            </tr>
                        </thead>
                        <tbody role="rowgroup">
                            <c:forEach var="produto" items="${produtos}">
                                <c:set var="falta" value="${produto.estoqueMinimo - produto.quantidade}" />
                                <tr role="row" class="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sh-linha--sem' : 'sh-linha--baixo'}">
                                    <td role="cell" class="sh-rel__celula-produto">
                                        <a class="sh-rel__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                    </td>
                                    <td role="cell" data-label="Categoria"><c:out value="${produto.categoria.nome}" /></td>
                                    <td role="cell" data-label="Saldo">
                                        <div class="sh-saldo sh-rel__saldo">
                                            <span class="sh-saldo__texto">
                                                <strong><c:out value="${produto.quantidade}" /></strong> un.
                                                <span class="sh-saldo__minimo">(mínimo <c:out value="${produto.estoqueMinimo}" />)</span>
                                            </span>
                                            <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                        </div>
                                    </td>
                                    <td role="cell" data-label="Para o mínimo">
                                        <span>
                                            <c:choose>
                                                <c:when test="${falta > 0}">Faltam <strong><c:out value="${falta}" /></strong></c:when>
                                                <c:when test="${produto.quantidade <= 0}">Sem saldo</c:when>
                                                <c:otherwise>No mínimo</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </td>
                                    <td role="cell" data-label="Situação"><sh:badgeEstoque nivel="${produto.nivelEstoque}" /></td>
                                    <td role="cell" class="sh-so-impressao"></td>
                                    <td role="cell" class="sh-rel__celula-acoes" data-label="">
                                        <div class="sh-rel__acoes">
                                            <a class="btn btn-primary btn-sm"
                                                href="<c:out value='${ctx}/estoque?movProduto=${produto.id}&movTipo=ENTRADA'/>">
                                                <i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Repor<span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                            </a>
                                            <a class="btn btn-ghost btn-sm" href="<c:out value='${ctx}/produtos/${produto.id}'/>">
                                                <i class="bi bi-eye" aria-hidden="true"></i> Ver<span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                    <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalProdutosFiltro}"
                        tamanho="100" base="/estoque-baixo" rotulo="Paginação da lista de estoque baixo" />
                </div>
            </c:otherwise>
        </c:choose>
    </jsp:body>
</sh:layout>
