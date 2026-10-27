<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Valor do estoque v3. Rota: GET /valor-estoque?pagina=N (100 por página, ordem de nome).
    Dados do DashboardController: resumo (valorTotalEstoque), produtos (categoria.nome, preco, quantidade, valorTotal),
    paginaAtual, totalPaginas, totalProdutosFiltro.
    Participação = produto.valorTotal / resumo.valorTotalEstoque (divisão por zero protegida). A barra é proporcional ao
    MAIOR valor da página; a largura usa degraus de 1 % (.sh-seg-N), sem style inline.
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
<%-- Valor para o contador animado: ponto decimal, sem milhar --%>
<fmt:formatNumber var="valorBruto" value="${valorTotal}" pattern="0.00" groupingUsed="false" />
<c:set var="valorBruto" value="${fn:replace(valorBruto, ',', '.')}" />

<%-- Maior valor da página (referência da barra) --%>
<c:set var="maior" value="0" />
<c:set var="nomeMaior" value="" />
<c:forEach var="p" items="${produtos}">
    <c:if test="${p.valorTotal > maior}"><c:set var="maior" value="${p.valorTotal}" /><c:set var="nomeMaior" value="${p.nome}" /></c:if>
</c:forEach>
<fmt:formatNumber var="maiorBruto" value="${maior}" pattern="0.00" groupingUsed="false" />
<c:set var="maiorBruto" value="${fn:replace(maiorBruto, ',', '.')}" />
<c:set var="escopo" value="${totalPaginas > 1 ? 'desta página' : 'da lista'}" />
<jsp:useBean id="agoraData" class="java.util.Date" />

<sh:layout titulo="Valor do estoque" ativo="valorEstoque">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/v3-rel.css">
        <script defer src="${ctx}/js/v3-rel.js"></script>
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

        <nav class="v3r-abas sh-nao-imprimir" aria-label="Relatórios de estoque">
            <a class="v3r-abas__item" href="${ctx}/estoque-baixo">Reposição</a>
            <a class="v3r-abas__item" href="${ctx}/unidades">Unidades</a>
            <a class="v3r-abas__item" href="${ctx}/valor-estoque" aria-current="page">Valor</a>
        </nav>

        <div class="v3r-folha-cab">
            <p class="v3r-folha-cab__titulo">Valor do estoque</p>
            <p class="v3r-folha-cab__meta">
                <c:out value="${usuarioLogado.nome}" /> &middot; emitido em <fmt:formatDate value="${agoraData}" pattern="dd/MM/yyyy HH:mm" />
            </p>
        </div>

        <dl class="v3r-resumo">
            <div class="v3r-fig v3r-fig--destaque">
                <dt>Valor total</dt>
                <dd class="sh-mono v3r-moeda" data-contar="${valorBruto}" data-formato="moeda">R$ <fmt:formatNumber value="${valorTotal}" pattern="#,##0.00" /></dd>
                <dd class="v3r-fig__nota">preço vezes quantidade, em todos os produtos</dd>
            </div>
            <div class="v3r-fig">
                <dt>Produtos</dt>
                <dd class="sh-mono" data-contar="${totalProdutosFiltro}"><fmt:formatNumber value="${totalProdutosFiltro}" pattern="#,##0" /></dd>
                <dd class="v3r-fig__nota">listados em ordem de nome</dd>
            </div>
            <c:if test="${maior > 0}">
                <div class="v3r-fig">
                    <dt>Maior valor <c:out value="${escopo}" /></dt>
                    <dd class="sh-mono v3r-moeda" data-contar="${maiorBruto}" data-formato="moeda">R$ <fmt:formatNumber value="${maior}" pattern="#,##0.00" /></dd>
                    <dd class="v3r-fig__nota v3r-truncar"><c:out value="${nomeMaior}" /></dd>
                </div>
            </c:if>
        </dl>

        <c:choose>
            <c:when test="${empty produtos and totalProdutosFiltro > 0}">
                <div class="v3r-painel">
                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                        texto="A lista tem menos páginas do que a que você pediu."
                        acaoUrl="/valor-estoque" acaoRotulo="Voltar ao início da lista" />
                </div>
            </c:when>
            <c:when test="${empty produtos}">
                <div class="v3r-painel">
                    <sh:estadoVazio icone="box-seam" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para ver quanto o estoque vale."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </div>
            </c:when>
            <c:otherwise>
                <p class="v3r-legenda sh-nao-imprimir">A barra compara cada produto com o de maior valor <c:out value="${escopo}" />; a participação é sobre o valor total da loja.</p>
                <div class="sh-table-wrap v3r-tabela-wrap">
                    <table class="sh-table sh-table--cards v3r-tabela" role="table">
                        <caption class="visually-hidden">Valor em estoque por produto, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                        <thead role="rowgroup">
                            <tr role="row">
                                <th scope="col" role="columnheader">Produto</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Preço</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Quantidade</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Valor</th>
                                <th scope="col" role="columnheader" class="v3r-col-barra">Proporção</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Participação</th>
                            </tr>
                        </thead>
                        <tbody role="rowgroup">
                            <c:forEach var="produto" items="${produtos}">
                                <c:set var="pm" value="0" />
                                <c:if test="${maior > 0}"><c:set var="pm" value="${produto.valorTotal * 100 / maior}" /></c:if>
                                <fmt:formatNumber var="largura" value="${pm}" maxFractionDigits="0" groupingUsed="false" />
                                <c:if test="${produto.valorTotal > 0 and largura == 0}"><c:set var="largura" value="1" /></c:if>
                                <c:set var="pt" value="0" />
                                <c:if test="${valorTotal > 0}"><c:set var="pt" value="${produto.valorTotal * 100 / valorTotal}" /></c:if>
                                <tr role="row">
                                    <td role="cell" class="v3r-celula-produto">
                                        <a class="v3r-nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                        <span class="v3r-cat"><c:out value="${produto.categoria.nome}" /></span>
                                    </td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Preço">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Quantidade"><fmt:formatNumber value="${produto.quantidade}" pattern="#,##0" /> <span class="v3r-un">un.</span></td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Valor"><strong>R$ <fmt:formatNumber value="${produto.valorTotal}" pattern="#,##0.00" /></strong></td>
                                    <td role="cell" class="v3r-col-barra" data-label="Proporção">
                                        <span class="v3r-trilho sh-desenhar" aria-hidden="true"><span class="v3r-trilho__fill sh-barra sh-barra--h sh-seg-<c:out value='${largura}'/>"></span></span>
                                    </td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Participação"><fmt:formatNumber value="${pt}" pattern="#,##0.0" />%</td>
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
