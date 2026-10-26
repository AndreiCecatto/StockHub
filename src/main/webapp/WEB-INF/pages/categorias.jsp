<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de categorias no novo design (lote 3). Rota: GET /categorias?busca=.
    Dados do CategoriaController: categorias (id, nome, descricao, totalProdutos = produtos ATIVOS) e busca.
    RN-09: categoria usada por qualquer produto (ativo ou inativado) não pode ser excluída. Com produtos ativos
    (totalProdutos > 0) a linha mostra "Em uso" no lugar do botão Excluir; com 0 o botão aparece e o servidor ainda
    recusa se sobrar produto inativado (a mensagem volta no flash "erro", inline e persistente).
    Exclusão: POST /categorias/{id}/excluir com CSRF e confirmação em modal. Busca ao digitar: js/produtos.js.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="total" value="${fn:length(categorias)}" />

<sh:layout titulo="Categorias" ativo="categorias">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
        <script defer src="${ctx}/js/produtos.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Categorias" descricao="Grupos que organizam o seu catálogo, como Bebidas ou Limpeza.">
            <a class="btn btn-primary" href="${ctx}/categorias/novo" data-sh-atalho-novo="pagina">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Nova categoria
            </a>
        </sh:cabecalhoPagina>

        <%-- Regra RN-09 sempre à vista, antes de a pessoa tentar excluir --%>
        <div class="sh-alert sh-alert--info">
            <i class="bi bi-info-circle-fill" aria-hidden="true"></i>
            <div class="sh-alert__corpo">
                Uma categoria usada por qualquer produto, ativo ou inativado, não pode ser excluída. Para liberar uma
                categoria, mova os produtos dela para outra categoria.
            </div>
        </div>

        <form class="sh-card sh-prod-filtros" method="get" action="${ctx}/categorias" role="search"
            aria-label="Buscar categorias" data-busca-viva>
            <div class="sh-prod-filtros__linha sh-cat-busca">
                <div class="sh-prod-busca">
                    <label class="form-label" for="busca">Buscar por nome</label>
                    <div class="sh-prod-busca__campo">
                        <i class="bi bi-search" aria-hidden="true"></i>
                        <input id="busca" class="form-control" type="search" name="busca" maxlength="120"
                            value="<c:out value='${busca}'/>" placeholder="Digite o nome da categoria" autocomplete="off"
                            aria-keyshortcuts="/" data-sh-busca-lista data-sh-limpar-esc>
                    </div>
                </div>
                <div class="sh-prod-grupo">
                    <button class="btn btn-primary w-100" type="submit">
                        <i class="bi bi-search" aria-hidden="true"></i> Buscar
                    </button>
                </div>
            </div>
        </form>

        <div class="sh-alert sh-alert--danger sh-prod-erro-busca" role="alert" hidden data-busca-erro>
            <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
            <div class="sh-alert__corpo">Não foi possível atualizar a lista. Confira a conexão ou use o botão Buscar.</div>
            <button type="button" class="btn btn-outline-secondary btn-sm" data-busca-tentar>Tentar de novo</button>
        </div>

        <div class="sh-prod-vivo">
            <div data-regiao-viva>
                <div class="sh-prod-barra">
                    <p class="sh-prod-contagem" data-contagem>
                        <c:out value="${total}" /> <c:out value="${total == 1 ? 'categoria' : 'categorias'}" />
                    </p>
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
                                        texto="Nenhum nome combina com a busca. Tente outro nome ou limpe a busca."
                                        acaoUrl="/categorias" acaoRotulo="Limpar busca" />
                                </c:when>
                                <c:otherwise>
                                    <sh:estadoVazio icone="tags" titulo="Crie categorias para agrupar seus produtos"
                                        texto="Com as categorias você acha cada produto mais rápido e filtra a lista por grupo."
                                        acaoUrl="/categorias/novo" acaoRotulo="Nova categoria" />
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="sh-table-wrap">
                            <table class="sh-table sh-table--cards" role="table">
                                <caption class="visually-hidden">Lista de categorias, <c:out value="${total}" /> no total</caption>
                                <thead role="rowgroup">
                                    <tr role="row">
                                        <th scope="col" role="columnheader">Categoria</th>
                                        <th scope="col" role="columnheader">Descrição</th>
                                        <th scope="col" role="columnheader">Produtos ativos</th>
                                        <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                                    </tr>
                                </thead>
                                <tbody role="rowgroup">
                                    <c:forEach var="categoria" items="${categorias}">
                                        <tr role="row">
                                            <td role="cell" class="sh-cat-celula-nome">
                                                <span class="sh-prod-nome"><c:out value="${categoria.nome}" /></span>
                                            </td>
                                            <td role="cell" class="sh-cat-desc" data-label="Descrição">
                                                <c:choose>
                                                    <c:when test="${empty categoria.descricao}"><span class="sh-muted">Sem descrição</span></c:when>
                                                    <c:otherwise><c:out value="${categoria.descricao}" /></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td role="cell" data-label="Produtos ativos">
                                                <c:choose>
                                                    <c:when test="${categoria.totalProdutos > 0}">
                                                        <span>
                                                            <span class="sh-cat-total"><c:out value="${categoria.totalProdutos}" /></span>
                                                            &middot;
                                                            <a href="<c:out value='${ctx}/produtos?categoriaId=${categoria.id}&tamanho=25'/>">Ver produtos<span class="visually-hidden"> da categoria <c:out value="${categoria.nome}" /></span></a>
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise><span class="sh-muted">Nenhum</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td role="cell" class="sh-prod-celula-acoes" data-label="">
                                                <div class="sh-prod-acoes">
                                                    <a class="btn btn-ghost btn-sm" href="<c:out value='${ctx}/categorias/${categoria.id}/editar'/>">
                                                        <i class="bi bi-pencil" aria-hidden="true"></i><span class="sh-prod-acao__txt">Editar</span><span class="visually-hidden"> <c:out value="${categoria.nome}" /></span>
                                                    </a>
                                                    <c:choose>
                                                        <c:when test="${categoria.totalProdutos > 0}">
                                                            <%-- RN-09: em uso por produtos ativos, não há o que confirmar --%>
                                                            <span class="sh-cat-uso">
                                                                <i class="bi bi-lock" aria-hidden="true"></i>
                                                                Em uso: não pode ser excluída
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <form method="post" action="<c:out value='${ctx}/categorias/${categoria.id}/excluir'/>"
                                                                data-confirmar-exclusao data-tipo="a categoria" data-nome="<c:out value='${categoria.nome}'/>"
                                                                data-aviso="Só é possível excluir uma categoria que nenhum produto usa, ativo ou inativado. Se algum produto inativado ainda aponta para ela, o sistema recusa e explica, para preservar o histórico.">
                                                                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                                <button type="submit" class="btn btn-ghost btn-ghost--perigo btn-sm">
                                                                    <i class="bi bi-trash3" aria-hidden="true"></i><span class="sh-prod-acao__txt">Excluir</span><span class="visually-hidden"> <c:out value="${categoria.nome}" /></span>
                                                                </button>
                                                            </form>
                                                        </c:otherwise>
                                                    </c:choose>
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

            <div class="sh-prod-esqueleto" data-busca-esqueleto hidden aria-hidden="true">
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
            </div>
        </div>
    </jsp:body>
</sh:layout>
