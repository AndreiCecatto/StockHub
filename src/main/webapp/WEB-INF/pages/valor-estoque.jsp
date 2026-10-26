<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Valor do estoque (novo design, lote 5). Rota: GET /valor-estoque?pagina=N (100 por página, ordem de nome).
    Dados do DashboardController: resumo (valorTotalEstoque), produtos (categoria.nome, preco, quantidade, valorTotal),
    paginaAtual, totalPaginas, totalProdutosFiltro.
    Participação no valor = produto.valorTotal / resumo.valorTotalEstoque, calculada aqui (divisão por zero protegida).
    A largura da barra usa degraus de 5 % (.sh-w-N), sem style inline; o percentual também sai em texto.
    Exportar CSV: /produtos/exportar.csv sem filtro de nível (todos os produtos, em ordem de nome).
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="valorTotal" value="${empty resumo.valorTotalEstoque ? 0 : resumo.valorTotalEstoque}" />
<c:url var="urlCsv" value="/produtos/exportar.csv">
    <c:param name="ordenacao" value="nome" />
    <c:param name="direcao" value="asc" />
</c:url>

<sh:layout titulo="Valor do estoque" ativo="valorEstoque">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/relatorios.css">
        <script defer src="${ctx}/js/relatorios.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Valor do estoque" descricao="Quanto cada produto representa em dinheiro: preço vezes quantidade.">
            <button type="button" class="btn btn-outline-secondary" data-sh-imprimir hidden>
                <i class="bi bi-printer" aria-hidden="true"></i> Imprimir
            </button>
            <a class="btn btn-outline-secondary" href="<c:out value='${urlCsv}'/>">
                <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
            </a>
        </sh:cabecalhoPagina>

        <nav class="sh-rel__alternar sh-nao-imprimir" aria-label="Relatórios de estoque">
            <div class="btn-group">
                <a class="btn btn-outline-secondary" href="${ctx}/unidades">Unidades</a>
                <a class="btn btn-primary" href="${ctx}/valor-estoque" aria-current="page">Valor</a>
            </div>
        </nav>

        <div class="sh-rel__topo">
            <div class="sh-metric">
                <span class="sh-metric__topo">
                    <span class="sh-metric__rotulo">Valor total</span>
                    <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-cash-coin"></i></span>
                </span>
                <span class="sh-metric__valor">R$ <fmt:formatNumber value="${valorTotal}" pattern="#,##0.00" /></span>
                <span class="sh-metric__apoio">preço vezes quantidade, em todos os produtos</span>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty produtos and totalProdutosFiltro > 0}">
                <div class="sh-card">
                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                        texto="A lista tem menos páginas do que a que você pediu."
                        acaoUrl="/valor-estoque" acaoRotulo="Voltar ao início da lista" />
                </div>
            </c:when>
            <c:when test="${empty produtos}">
                <div class="sh-card">
                    <sh:estadoVazio icone="box-seam" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para ver quanto o estoque vale."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </div>
            </c:when>
            <c:otherwise>
                <p class="sh-rel__contagem">
                    <c:out value="${totalProdutosFiltro}" /> <c:out value="${totalProdutosFiltro == 1 ? 'produto' : 'produtos'}" />, em ordem de nome
                </p>
                <div class="sh-table-wrap">
                    <table class="sh-table sh-table--cards" role="table">
                        <caption class="visually-hidden">Valor em estoque por produto, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                        <thead role="rowgroup">
                            <tr role="row">
                                <th scope="col" role="columnheader">Produto</th>
                                <th scope="col" role="columnheader">Categoria</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Preço</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Quantidade</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Total</th>
                                <th scope="col" role="columnheader">Participação no valor</th>
                                <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                            </tr>
                        </thead>
                        <tbody role="rowgroup">
                            <c:forEach var="produto" items="${produtos}">
                                <%-- Participação: protege a divisão por zero (estoque sem valor) --%>
                                <c:set var="pct" value="0" />
                                <c:if test="${valorTotal > 0}">
                                    <c:set var="pct" value="${produto.valorTotal * 100 / valorTotal}" />
                                </c:if>
                                <c:if test="${pct > 100}"><c:set var="pct" value="100" /></c:if>
                                <fmt:formatNumber var="passo" value="${pct / 5}" maxFractionDigits="0" groupingUsed="false" />
                                <c:set var="degrau" value="${passo * 5}" />
                                <c:if test="${pct > 0 and degrau == 0}"><c:set var="degrau" value="5" /></c:if>
                                <tr role="row">
                                    <td role="cell" class="sh-rel__celula-produto">
                                        <a class="sh-rel__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                    </td>
                                    <td role="cell" data-label="Categoria"><c:out value="${produto.categoria.nome}" /></td>
                                    <td role="cell" class="sh-col-num" data-label="Preço">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></td>
                                    <td role="cell" class="sh-col-num" data-label="Quantidade"><c:out value="${produto.quantidade}" /> un.</td>
                                    <td role="cell" class="sh-col-num" data-label="Total"><strong>R$ <fmt:formatNumber value="${produto.valorTotal}" pattern="#,##0.00" /></strong></td>
                                    <td role="cell" data-label="Participação">
                                        <div class="sh-rel__parte">
                                            <span class="sh-meter sh-meter--normal" aria-hidden="true">
                                                <span class="sh-meter__fill sh-w-<c:out value='${degrau}'/>"></span>
                                            </span>
                                            <span class="sh-rel__parte-pct"><fmt:formatNumber value="${pct}" pattern="#,##0.0" />%</span>
                                        </div>
                                    </td>
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
                        tamanho="100" base="/valor-estoque" rotulo="Paginação da lista de valor em estoque" />
                </div>
            </c:otherwise>
        </c:choose>
    </jsp:body>
</sh:layout>
