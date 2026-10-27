<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Reposição v3 (estoque baixo): lista de compras priorizada. Rota: GET /estoque-baixo?pagina=N (100 por página).
    Dados do DashboardController: produtos (Produto com categoria.nome, quantidade, estoqueMinimo, nivelEstoque; já em ordem
    de menor saldo, então os sem estoque vêm primeiro), paginaAtual, totalPaginas, totalProdutosFiltro.
    "Comprar" sugere repor até o mínimo (mínimo - saldo). Os totais do resumo (sem estoque, abaixo do mínimo, unidades)
    são da lista exibida (a página); só "Para repor" é o total geral. Nada é inventado.
    Exportar CSV reaproveita /produtos/exportar.csv: o filtro aceita UM nível por vez, então são dois links.
    Impressão: css/v3-rel.css monta a folha de compras (caixa de marcar, coluna "Comprar" em branco para anotar).
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

<%-- Totais da lista exibida --%>
<c:set var="qSem" value="0" />
<c:set var="qBaixo" value="0" />
<c:set var="somaFalta" value="0" />
<c:forEach var="p" items="${produtos}">
    <c:if test="${p.nivelEstoque == 'SEM_ESTOQUE'}"><c:set var="qSem" value="${qSem + 1}" /></c:if>
    <c:if test="${p.nivelEstoque == 'BAIXO'}"><c:set var="qBaixo" value="${qBaixo + 1}" /></c:if>
    <c:if test="${p.estoqueMinimo > p.quantidade}"><c:set var="somaFalta" value="${somaFalta + p.estoqueMinimo - p.quantidade}" /></c:if>
</c:forEach>
<c:set var="escopo" value="${totalPaginas > 1 ? 'nesta página' : 'na lista'}" />
<jsp:useBean id="agoraData" class="java.util.Date" />

<sh:layout titulo="Estoque baixo" ativo="estoqueBaixo">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/v3-rel.css">
        <script defer src="${ctx}/js/v3-rel.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Reposição" descricao="Produtos sem estoque ou no mínimo ou abaixo dele, do mais urgente ao menos urgente.">
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

        <nav class="v3r-abas sh-nao-imprimir" aria-label="Relatórios de estoque">
            <a class="v3r-abas__item" href="${ctx}/estoque-baixo" aria-current="page">Reposição</a>
            <a class="v3r-abas__item" href="${ctx}/unidades">Unidades</a>
            <a class="v3r-abas__item" href="${ctx}/valor-estoque">Valor</a>
        </nav>

        <%-- Cabeçalho que só existe no papel --%>
        <div class="v3r-folha-cab">
            <p class="v3r-folha-cab__titulo">Lista de reposição</p>
            <p class="v3r-folha-cab__meta">
                <c:out value="${usuarioLogado.nome}" /> &middot; emitida em <fmt:formatDate value="${agoraData}" pattern="dd/MM/yyyy HH:mm" />
                &middot; <c:out value="${totalProdutosFiltro}" /> produto(s) para repor
            </p>
        </div>

        <c:choose>
            <c:when test="${empty produtos and totalProdutosFiltro > 0}">
                <div class="v3r-painel">
                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                        texto="A lista tem menos páginas do que a que você pediu."
                        acaoUrl="/estoque-baixo" acaoRotulo="Voltar ao início da lista" />
                </div>
            </c:when>
            <c:when test="${empty produtos}">
                <div class="v3r-painel">
                    <sh:estadoVazio tom="sucesso" titulo="Tudo em dia"
                        texto="Nenhum produto está no mínimo ou abaixo dele."
                        acaoUrl="/produtos?tamanho=25" acaoRotulo="Ver produtos" />
                </div>
            </c:when>
            <c:otherwise>
                <dl class="v3r-resumo sh-nao-imprimir">
                    <div class="v3r-fig">
                        <dt>Para repor</dt>
                        <dd class="sh-mono" data-contar="${totalProdutosFiltro}"><fmt:formatNumber value="${totalProdutosFiltro}" pattern="#,##0" /></dd>
                        <dd class="v3r-fig__nota"><c:out value="${totalProdutosFiltro == 1 ? 'produto' : 'produtos'}" /> no total</dd>
                    </div>
                    <div class="v3r-fig v3r-fig--sem">
                        <dt>Sem estoque</dt>
                        <dd class="sh-mono" data-contar="${qSem}"><fmt:formatNumber value="${qSem}" pattern="#,##0" /></dd>
                        <dd class="v3r-fig__nota"><c:out value="${escopo}" /></dd>
                    </div>
                    <div class="v3r-fig v3r-fig--baixo">
                        <dt>Abaixo do mínimo</dt>
                        <dd class="sh-mono" data-contar="${qBaixo}"><fmt:formatNumber value="${qBaixo}" pattern="#,##0" /></dd>
                        <dd class="v3r-fig__nota"><c:out value="${escopo}" /></dd>
                    </div>
                    <div class="v3r-fig">
                        <dt>Unidades até o mínimo</dt>
                        <dd class="sh-mono" data-contar="${somaFalta}"><fmt:formatNumber value="${somaFalta}" pattern="#,##0" /></dd>
                        <dd class="v3r-fig__nota">soma do que falta <c:out value="${escopo}" /></dd>
                    </div>
                </dl>

                <div class="sh-table-wrap v3r-tabela-wrap">
                    <table class="sh-table sh-table--cards v3r-tabela" role="table">
                        <caption class="visually-hidden">Produtos sem estoque ou com estoque baixo, por prioridade, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                        <thead role="rowgroup">
                            <tr role="row">
                                <th scope="col" role="columnheader" class="v3r-papel v3r-col-marca"><span class="visually-hidden">Comprado</span></th>
                                <th scope="col" role="columnheader" class="v3r-tela v3r-col-rank">Prioridade</th>
                                <th scope="col" role="columnheader">Produto</th>
                                <th scope="col" role="columnheader" class="v3r-tela">Situação</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Saldo</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Mínimo</th>
                                <th scope="col" role="columnheader" class="sh-col-num">Falta</th>
                                <th scope="col" role="columnheader" class="v3r-col-comprar">Comprar</th>
                                <th scope="col" role="columnheader" class="v3r-tela"><span class="visually-hidden">Ações</span></th>
                            </tr>
                        </thead>
                        <tbody role="rowgroup">
                            <c:forEach var="produto" items="${produtos}" varStatus="st">
                                <c:set var="falta" value="${produto.estoqueMinimo - produto.quantidade}" />
                                <tr role="row" class="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sh-linha--sem' : 'sh-linha--baixo'}">
                                    <td role="cell" class="v3r-papel v3r-col-marca"><span class="v3r-caixa" aria-hidden="true"></span></td>
                                    <td role="cell" class="v3r-tela v3r-col-rank" data-label="Prioridade">
                                        <span class="v3r-rank sh-mono"><c:out value="${(paginaAtual - 1) * 100 + st.count}" /></span>
                                    </td>
                                    <td role="cell" class="v3r-celula-produto">
                                        <a class="v3r-nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>" data-sh-peek data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                        <span class="v3r-cat"><c:out value="${produto.categoria.nome}" /></span>
                                    </td>
                                    <td role="cell" class="v3r-tela" data-label="Situação"><sh:badgeEstoque nivel="${produto.nivelEstoque}" /></td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Saldo">
                                        <div class="v3r-saldo">
                                            <strong><c:out value="${produto.quantidade}" /></strong>
                                            <span class="v3r-tela"><sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" /></span>
                                        </div>
                                    </td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Mínimo"><c:out value="${produto.estoqueMinimo}" /></td>
                                    <td role="cell" class="sh-col-num sh-mono" data-label="Falta">
                                        <c:choose>
                                            <c:when test="${falta > 0}"><c:out value="${falta}" /></c:when>
                                            <c:otherwise><span aria-label="Nada falta para o mínimo">&mdash;</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td role="cell" class="v3r-col-comprar" data-label="Comprar">
                                        <%-- Na tela: sugestão (repor até o mínimo). No papel: espaço em branco para anotar, com a sugestão pequena --%>
                                        <span class="v3r-comprar">
                                            <span class="v3r-comprar__sugestao ${falta > 0 ? '' : 'v3r-comprar__sugestao--nenhuma'} sh-mono">
                                                <c:choose>
                                                    <c:when test="${falta > 0}"><span class="visually-hidden">Sugestão: </span>+<c:out value="${falta}" /></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </span>
                                        </span>
                                    </td>
                                    <td role="cell" class="v3r-tela v3r-celula-acoes" data-label="">
                                        <a class="btn btn-primary btn-sm"
                                            href="<c:out value='${ctx}/estoque?movProduto=${produto.id}&movTipo=ENTRADA'/>">
                                            <i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Repor<span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                        </a>
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
