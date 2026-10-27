<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de categorias na v3. Rota: GET /categorias?busca=.
    Dados do CategoriaController: categorias (id, nome, descricao, totalProdutos = produtos ATIVOS) e busca.
    RN-09: categoria usada por qualquer produto (ativo ou inativado) não pode ser excluída. Com produtos ativos
    (totalProdutos > 0) a linha mostra "Protegida" no lugar do botão Excluir; com 0 o botão aparece e o servidor ainda
    recusa se sobrar produto inativado (a mensagem volta no flash "erro", inline e persistente).
    Exclusão: POST /categorias/{id}/excluir com CSRF e confirmação em modal. Busca ao digitar: js/produtos.js.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="total" value="${fn:length(categorias)}" />
<c:set var="somaProdutos" value="0" />
<c:set var="maiorTotal" value="0" />
<c:forEach var="cat" items="${categorias}">
    <c:set var="somaProdutos" value="${somaProdutos + cat.totalProdutos}" />
    <c:if test="${cat.totalProdutos > maiorTotal}"><c:set var="maiorTotal" value="${cat.totalProdutos}" /></c:if>
</c:forEach>

<sh:layout titulo="Categorias" ativo="categorias">
    <jsp:attribute name="scripts">
        <script src="${ctx}/js/v3-prod-init.js"></script>
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
        <script defer src="${ctx}/js/produtos.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Categorias" descricao="Grupos que organizam o catálogo, como Bebidas ou Limpeza.">
            <a class="btn btn-primary" href="${ctx}/categorias/novo" data-sh-atalho-novo="pagina">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Nova categoria
            </a>
        </sh:cabecalhoPagina>

        <%-- Regra RN-09 sempre à vista, antes de a pessoa tentar excluir --%>
        <div class="v3-regra" role="note">
            <i class="bi bi-lock" aria-hidden="true"></i>
            <p class="v3-regra__txt">
                <strong>Categoria em uso não pode ser excluída.</strong>
                Vale para qualquer produto que a use, ativo ou inativado. Para liberá-la, mova os produtos para outra categoria.
            </p>
        </div>

        <form class="v3-filtros" method="get" action="${ctx}/categorias" role="search"
            aria-label="Buscar categorias" data-busca-viva>
            <div class="v3-filtros__busca">
                <label class="visually-hidden" for="busca">Buscar por nome</label>
                <div class="v3-busca">
                    <i class="bi bi-search v3-busca__lupa" aria-hidden="true"></i>
                    <input id="busca" class="form-control" type="search" name="busca" maxlength="120"
                        value="<c:out value='${busca}'/>" placeholder="Buscar por nome" autocomplete="off"
                        aria-keyshortcuts="/" data-sh-busca-lista data-sh-limpar-esc>
                    <span class="v3-busca__estado v3-so-js" data-busca-indicador hidden>
                        <span class="v3-giro" aria-hidden="true"></span>
                        <span>Atualizando</span>
                    </span>
                </div>
                <button class="btn btn-outline-secondary v3-so-semjs" type="submit">
                    <i class="bi bi-search" aria-hidden="true"></i> Buscar
                </button>
            </div>
        </form>

        <div class="sh-alert sh-alert--danger v3-erro-busca" role="alert" hidden data-busca-erro>
            <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
            <div class="sh-alert__corpo">Não foi possível atualizar a lista. Confira a conexão e tente de novo.</div>
            <button type="button" class="btn btn-outline-secondary btn-sm" data-busca-tentar>Tentar de novo</button>
        </div>

        <div class="v3-vivo">
            <div class="v3-progresso" data-busca-progresso aria-hidden="true"></div>
            <div data-regiao-viva>
                <div class="v3-barra">
                    <div class="v3-barra__esq">
                        <p class="v3-contagem" data-contagem><strong class="v3-mono"><c:out value="${total}" /></strong> <c:out value="${total == 1 ? 'categoria' : 'categorias'}" /></p>
                        <c:if test="${total > 0}">
                            <span class="v3-ordem-texto"><span class="v3-mono"><fmt:formatNumber value="${somaProdutos}" pattern="#,##0" /></span> <c:out value="${somaProdutos == 1 ? 'produto ativo' : 'produtos ativos'}" /> no total</span>
                        </c:if>
                    </div>
                    <c:if test="${not empty busca}">
                        <a class="btn btn-ghost btn-sm" href="${ctx}/categorias">Limpar busca</a>
                    </c:if>
                </div>

                <c:choose>
                    <c:when test="${empty categorias}">
                        <div class="sh-card">
                            <c:choose>
                                <c:when test="${not empty busca}">
                                    <sh:estadoVazio icone="search" titulo="Nenhuma categoria encontrada"
                                        texto="Nenhum nome combina com a busca. Ajuste o nome ou limpe a busca."
                                        acaoUrl="/categorias" acaoRotulo="Limpar busca" />
                                </c:when>
                                <c:otherwise>
                                    <sh:estadoVazio icone="tags" titulo="Nenhuma categoria cadastrada"
                                        texto="Com categorias, cada produto fica no seu grupo e a lista de produtos pode ser filtrada por elas."
                                        acaoUrl="/categorias/novo" acaoRotulo="Nova categoria" />
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="sh-table-wrap">
                            <table class="sh-table sh-table--cards v3-tabela v3-tabela--cat" role="table">
                                <caption class="visually-hidden">Lista de categorias, <c:out value="${total}" /> no total</caption>
                                <thead role="rowgroup">
                                    <tr role="row">
                                        <th scope="col" role="columnheader" class="v3-cc-nome">Categoria</th>
                                        <th scope="col" role="columnheader" class="v3-cc-prod">Produtos ativos</th>
                                        <th scope="col" role="columnheader" class="v3-cc-excl">Exclusão</th>
                                        <th scope="col" role="columnheader" class="v3-c-acoes"><span class="visually-hidden">Ações</span></th>
                                    </tr>
                                </thead>
                                <tbody role="rowgroup">
                                    <c:forEach var="categoria" items="${categorias}">
                                        <%-- Participação no catálogo: barra em degraus de 5 % (classes sh-w-N, sem style inline) --%>
                                        <c:set var="pctCat" value="${maiorTotal > 0 ? categoria.totalProdutos * 100 / maiorTotal : 0}" />
                                        <fmt:formatNumber var="passoCat" value="${pctCat / 5}" maxFractionDigits="0" groupingUsed="false" />
                                        <c:set var="degrauCat" value="${passoCat * 5}" />
                                        <c:if test="${categoria.totalProdutos > 0 and degrauCat == 0}"><c:set var="degrauCat" value="5" /></c:if>
                                        <tr role="row">
                                            <td role="cell" class="v3-cc-nome">
                                                <span class="v3-prod-nome"><c:out value="${categoria.nome}" /></span>
                                                <span class="sh-prod-desc">
                                                    <c:choose>
                                                        <c:when test="${empty categoria.descricao}">Sem descrição</c:when>
                                                        <c:otherwise><c:out value="${categoria.descricao}" /></c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </td>
                                            <td role="cell" class="v3-cc-prod" data-label="Produtos ativos">
                                                <div class="v3-cat-qtd">
                                                    <span class="v3-cat-qtd__num v3-mono"><c:out value="${categoria.totalProdutos}" /></span>
                                                    <c:if test="${categoria.totalProdutos > 0}">
                                                        <span class="v3-cat-barra" aria-hidden="true"><span class="v3-cat-barra__fill sh-w-<c:out value='${degrauCat}'/>"></span></span>
                                                        <a href="<c:out value='${ctx}/produtos?categoriaId=${categoria.id}&tamanho=25'/>">Ver produtos<span class="visually-hidden"> da categoria <c:out value="${categoria.nome}" /></span></a>
                                                    </c:if>
                                                </div>
                                            </td>
                                            <td role="cell" class="v3-cc-excl" data-label="Exclusão">
                                                <c:choose>
                                                    <c:when test="${categoria.totalProdutos > 0}">
                                                        <span class="v3-cat-uso"><i class="bi bi-lock" aria-hidden="true"></i> Em uso, protegida</span>
                                                    </c:when>
                                                    <c:otherwise><span class="v3-cat-livre">Sem produtos ativos</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td role="cell" class="v3-c-acoes" data-label="">
                                                <div class="v3-acoes">
                                                    <a class="btn btn-ghost btn-sm v3-icone" href="<c:out value='${ctx}/categorias/${categoria.id}/editar'/>" title="Editar">
                                                        <i class="bi bi-pencil" aria-hidden="true"></i><span class="v3-acao__txt">Editar</span><span class="visually-hidden"> <c:out value="${categoria.nome}" /></span>
                                                    </a>
                                                    <c:if test="${categoria.totalProdutos <= 0 and podeGerenciar}">
                                                        <form method="post" action="<c:out value='${ctx}/categorias/${categoria.id}/excluir'/>"
                                                            data-confirmar-exclusao data-tipo="a categoria" data-nome="<c:out value='${categoria.nome}'/>"
                                                            data-aviso="Só é possível excluir uma categoria que nenhum produto usa, ativo ou inativado. Se algum produto inativado ainda aponta para ela, o sistema recusa e explica, para preservar o histórico.">
                                                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                            <button type="submit" class="btn btn-ghost btn-ghost--perigo btn-sm v3-icone" title="Excluir">
                                                                <i class="bi bi-trash3" aria-hidden="true"></i><span class="v3-acao__txt">Excluir</span><span class="visually-hidden"> <c:out value="${categoria.nome}" /></span>
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="v3-esqueleto" data-busca-esqueleto hidden aria-hidden="true">
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
            </div>
        </div>
    </jsp:body>
</sh:layout>
