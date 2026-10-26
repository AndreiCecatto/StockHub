<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Unidades em estoque (novo design, lote 5). Rota: GET /unidades?pagina=N (100 por página, ordem de nome).
    Dados do DashboardController: resumo (quantidadeTotalItens), produtos (categoria.nome, quantidade, estoqueMinimo,
    nivelEstoque), paginaAtual, totalPaginas, totalProdutosFiltro.
    Exportar CSV: /produtos/exportar.csv sem filtro de nível (todos os produtos, em ordem de nome).
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:url var="urlCsv" value="/produtos/exportar.csv">
    <c:param name="ordenacao" value="nome" />
    <c:param name="direcao" value="asc" />
</c:url>

<sh:layout titulo="Unidades em estoque" ativo="unidades">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/relatorios.css">
        <script defer src="${ctx}/js/relatorios.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Unidades em estoque" descricao="Quanto há de cada produto, somando o saldo de toda a loja.">
            <button type="button" class="btn btn-outline-secondary" data-sh-imprimir hidden>
                <i class="bi bi-printer" aria-hidden="true"></i> Imprimir
            </button>
            <a class="btn btn-outline-secondary" href="<c:out value='${urlCsv}'/>">
                <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
            </a>
        </sh:cabecalhoPagina>

        <nav class="sh-rel__alternar sh-nao-imprimir" aria-label="Relatórios de estoque">
            <div class="btn-group">
                <a class="btn btn-primary" href="${ctx}/unidades" aria-current="page">Unidades</a>
                <a class="btn btn-outline-secondary" href="${ctx}/valor-estoque">Valor</a>
            </div>
        </nav>

        <div class="sh-rel__topo">
            <div class="sh-metric">
                <span class="sh-metric__topo">
                    <span class="sh-metric__rotulo">Total de unidades</span>
                    <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-stack"></i></span>
                </span>
                <span class="sh-metric__valor"><fmt:formatNumber value="${resumo.quantidadeTotalItens}" pattern="#,##0" /></span>
                <span class="sh-metric__apoio">soma do saldo de todos os produtos</span>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty produtos and totalProdutosFiltro > 0}">
                <div class="sh-card">
                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                        texto="A lista tem menos páginas do que a que você pediu."
                        acaoUrl="/unidades" acaoRotulo="Voltar ao início da lista" />
                </div>
            </c:when>
            <c:when test="${empty produtos}">
                <div class="sh-card">
                    <sh:estadoVazio icone="box-seam" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para ver as unidades em estoque."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </div>
            </c:when>
            <c:otherwise>
                <p class="sh-rel__contagem">
                    <c:out value="${totalProdutosFiltro}" /> <c:out value="${totalProdutosFiltro == 1 ? 'produto' : 'produtos'}" />, em ordem de nome
                </p>
                <div class="sh-table-wrap">
                    <table class="sh-table sh-table--cards" role="table">
                        <caption class="visually-hidden">Unidades em estoque por produto, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                        <thead role="rowgroup">
                            <tr role="row">
                                <th scope="col" role="columnheader">Produto</th>
                                <th scope="col" role="columnheader">Categoria</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Quantidade</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Mínimo</th>
                                <th scope="col" role="columnheader">Situação</th>
                                <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                            </tr>
                        </thead>
                        <tbody role="rowgroup">
                            <c:forEach var="produto" items="${produtos}">
                                <tr role="row" class="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sh-linha--sem' : (produto.nivelEstoque == 'BAIXO' ? 'sh-linha--baixo' : '')}">
                                    <td role="cell" class="sh-rel__celula-produto">
                                        <a class="sh-rel__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                    </td>
                                    <td role="cell" data-label="Categoria"><c:out value="${produto.categoria.nome}" /></td>
                                    <td role="cell" class="sh-col-num" data-label="Quantidade"><strong><c:out value="${produto.quantidade}" /></strong> un.</td>
                                    <td role="cell" class="sh-col-num" data-label="Mínimo"><c:out value="${produto.estoqueMinimo}" /></td>
                                    <td role="cell" data-label="Situação"><sh:badgeEstoque nivel="${produto.nivelEstoque}" /></td>
                                    <td role="cell" class="sh-rel__celula-acoes" data-label="">
                                        <div class="sh-rel__acoes">
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
                        tamanho="100" base="/unidades" rotulo="Paginação da lista de unidades" />
                </div>
            </c:otherwise>
        </c:choose>
    </jsp:body>
</sh:layout>
