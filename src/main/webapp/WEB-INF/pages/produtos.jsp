<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de produtos no novo design (lote 3). Rota: GET /produtos. Parâmetros: busca, categoriaId, nivelEstoque
    (ok, baixo, semEstoque), ordenacao (id, nome, preco, quantidade, categoria), direcao (asc, desc), tamanho, pagina.
    Dados do ProdutoController: produtos, categorias, busca, categoriaId, nivelEstoque, ordenacao, direcao,
    direcaoInversa, paginaAtual, totalPaginas, totalProdutosFiltro, tamanho, parametrosFiltros (que NÃO traz direcao:
    a paginação acrescenta). Flash: mensagem (toast) e erro (inline), pelo alertas.jsp do layout.
    Funciona sem JavaScript: o formulário de filtros é um GET comum. Com js/produtos.js a lista troca ao digitar
    (só a região [data-regiao-viva]). Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="temFiltros" value="${not empty busca or not empty categoriaId or not empty nivelEstoque}" />

<%-- Rótulos dos filtros ativos --%>
<c:set var="categoriaNome" value="" />
<c:if test="${not empty categoriaId}">
    <c:forEach var="cat" items="${categorias}">
        <c:if test="${cat.id == categoriaId}"><c:set var="categoriaNome" value="${cat.nome}" /></c:if>
    </c:forEach>
</c:if>
<c:set var="nivelRotulo" value="${nivelEstoque == 'ok' ? 'Normal' : (nivelEstoque == 'baixo' ? 'Baixo' : 'Sem estoque')}" />
<c:set var="ordemRotulo" value="${ordenacao == 'nome' ? 'nome' : (ordenacao == 'preco' ? 'preço' : (ordenacao == 'quantidade' ? 'saldo' : (ordenacao == 'categoria' ? 'categoria' : 'data de cadastro')))}" />

<%-- URLs montadas com c:url/c:param (codificação automática). Todas começam em /produtos e já têm "?" --%>
<c:url var="urlOrdem" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemBusca" value="/produtos">
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemCategoria" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemNivel" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlLimpar" value="/produtos">
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlRecentes" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="id" />
    <c:param name="direcao" value="desc" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<%-- Exportação CSV com os mesmos filtros e a mesma ordem (sem paginação) --%>
<c:url var="urlCsv" value="/produtos/exportar.csv">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
</c:url>
<%-- A paginação precisa do filtro inteiro, inclusive direcao (parametrosFiltros já vem codificado) --%>
<c:set var="extraPaginacao" value="${parametrosFiltros}&direcao=${direcao}" />

<sh:layout titulo="Produtos" ativo="produtos">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
        <script defer src="${ctx}/js/produtos.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Produtos" descricao="Encontre um produto, veja o saldo e registre uma reposição.">
            <a class="btn btn-primary" href="${ctx}/produtos/novo" data-sh-atalho-novo="pagina">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Novo produto
            </a>
        </sh:cabecalhoPagina>

        <%-- ===== Filtros (GET comum; com JS a lista troca ao digitar) ===== --%>
        <form class="sh-card sh-prod-filtros" method="get" action="${ctx}/produtos" role="search"
            aria-label="Filtrar produtos" data-busca-viva>
            <div class="sh-prod-busca">
                <label class="form-label" for="busca">Buscar por nome</label>
                <div class="sh-prod-busca__campo">
                    <i class="bi bi-search" aria-hidden="true"></i>
                    <input id="busca" class="form-control" type="search" name="busca" maxlength="120"
                        value="<c:out value='${busca}'/>" placeholder="Digite o nome do produto" autocomplete="off"
                        aria-keyshortcuts="/" data-sh-busca-lista data-sh-limpar-esc>
                </div>
            </div>

            <div class="sh-prod-filtros__linha">
                <fieldset class="sh-prod-grupo">
                    <legend class="sh-prod-legenda">Situação do estoque</legend>
                    <div class="sh-chips">
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-todos" name="nivelEstoque"
                                value="" ${empty nivelEstoque ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-todos"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i>Todos</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-ok" name="nivelEstoque"
                                value="ok" ${nivelEstoque == 'ok' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-ok"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i>Normal</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-baixo" name="nivelEstoque"
                                value="baixo" ${nivelEstoque == 'baixo' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-baixo"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i>Baixo</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-sem" name="nivelEstoque"
                                value="semEstoque" ${nivelEstoque == 'semEstoque' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-sem"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i>Sem estoque</label>
                        </span>
                    </div>
                </fieldset>

                <div class="sh-prod-grupo">
                    <label class="form-label" for="categoriaId">Categoria</label>
                    <select id="categoriaId" class="form-select" name="categoriaId">
                        <option value="">Todas</option>
                        <c:forEach var="categoria" items="${categorias}">
                            <option value="<c:out value='${categoria.id}'/>" ${categoriaId == categoria.id ? 'selected' : ''}><c:out value="${categoria.nome}" /></option>
                        </c:forEach>
                    </select>
                </div>

                <div class="sh-prod-grupo">
                    <label class="form-label" for="tamanho">Itens por página</label>
                    <select id="tamanho" class="form-select" name="tamanho">
                        <c:if test="${tamanho != 10 and tamanho != 25 and tamanho != 50 and tamanho != 100}">
                            <option value="<c:out value='${tamanho}'/>" selected><c:out value="${tamanho}" /></option>
                        </c:if>
                        <option value="10" ${tamanho == 10 ? 'selected' : ''}>10</option>
                        <option value="25" ${tamanho == 25 ? 'selected' : ''}>25</option>
                        <option value="50" ${tamanho == 50 ? 'selected' : ''}>50</option>
                        <option value="100" ${tamanho == 100 ? 'selected' : ''}>100</option>
                    </select>
                </div>

                <%-- Ordenação: no computador sai do cabeçalho da tabela; no celular (cabeçalho oculto) vira dois selects.
                     Escondidos por CSS no computador, continuam enviando a ordem atual junto com o formulário. --%>
                <div class="sh-prod-ordem-movel">
                    <div class="sh-prod-grupo">
                        <label class="form-label" for="ordenacao">Ordenar por</label>
                        <select id="ordenacao" class="form-select" name="ordenacao">
                            <option value="id" ${ordenacao == 'id' ? 'selected' : ''}>Mais recentes</option>
                            <option value="nome" ${ordenacao == 'nome' ? 'selected' : ''}>Nome</option>
                            <option value="preco" ${ordenacao == 'preco' ? 'selected' : ''}>Preço</option>
                            <option value="quantidade" ${ordenacao == 'quantidade' ? 'selected' : ''}>Saldo</option>
                            <option value="categoria" ${ordenacao == 'categoria' ? 'selected' : ''}>Categoria</option>
                        </select>
                    </div>
                    <div class="sh-prod-grupo">
                        <label class="form-label" for="direcao">Ordem</label>
                        <select id="direcao" class="form-select" name="direcao">
                            <option value="asc" ${direcao == 'asc' ? 'selected' : ''}>Crescente</option>
                            <option value="desc" ${direcao == 'desc' ? 'selected' : ''}>Decrescente</option>
                        </select>
                    </div>
                </div>

                <div class="sh-prod-grupo">
                    <button class="btn btn-primary w-100" type="submit">
                        <i class="bi bi-search" aria-hidden="true"></i> Buscar
                    </button>
                </div>
            </div>
        </form>

        <%-- Falha ao atualizar a lista ao digitar (o JS mostra; Buscar continua funcionando) --%>
        <div class="sh-alert sh-alert--danger sh-prod-erro-busca" role="alert" hidden data-busca-erro>
            <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
            <div class="sh-alert__corpo">Não foi possível atualizar a lista. Confira a conexão ou use o botão Buscar.</div>
            <button type="button" class="btn btn-outline-secondary btn-sm" data-busca-tentar>Tentar de novo</button>
        </div>

        <div class="sh-prod-vivo">
            <%-- ===== Região trocada pela busca ao digitar ===== --%>
            <div data-regiao-viva>
                <div class="sh-prod-barra">
                    <p class="sh-prod-contagem" data-contagem>
                        <c:out value="${totalProdutosFiltro}" /> <c:out value="${totalProdutosFiltro == 1 ? 'produto' : 'produtos'}" />
                    </p>
                    <div class="sh-prod-barra__acoes">
                        <span class="sh-prod-ordem-texto">
                            Ordenado por <c:out value="${ordemRotulo}" />,
                            <c:out value="${direcao == 'asc' ? 'crescente' : 'decrescente'}" />
                            <c:if test="${ordenacao != 'id'}">
                                &middot; <a href="<c:out value='${urlRecentes}'/>">Mais recentes primeiro</a>
                            </c:if>
                        </span>
                        <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${urlCsv}'/>">
                            <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
                        </a>
                    </div>
                </div>

                <c:if test="${temFiltros}">
                    <ul class="sh-chips sh-prod-ativos" aria-label="Filtros ativos">
                        <c:if test="${not empty busca}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemBusca}'/>">
                                    Nome: <c:out value="${busca}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de nome</span>
                                </a>
                            </li>
                        </c:if>
                        <c:if test="${not empty categoriaId}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemCategoria}'/>">
                                    Categoria: <c:out value="${empty categoriaNome ? 'indisponível' : categoriaNome}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de categoria</span>
                                </a>
                            </li>
                        </c:if>
                        <c:if test="${not empty nivelEstoque}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemNivel}'/>">
                                    Situação: <c:out value="${nivelRotulo}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de situação do estoque</span>
                                </a>
                            </li>
                        </c:if>
                        <li><a class="btn btn-ghost btn-sm" href="<c:out value='${urlLimpar}'/>">Limpar filtros</a></li>
                    </ul>
                </c:if>

                <c:choose>
                    <%-- ===== Vazio ===== --%>
                    <c:when test="${empty produtos}">
                        <div class="sh-card">
                            <c:choose>
                                <c:when test="${totalProdutosFiltro > 0}">
                                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                                        texto="A lista tem menos páginas do que a que você pediu."
                                        acaoUrl="/produtos?tamanho=${tamanho}" acaoRotulo="Voltar ao início da lista" />
                                </c:when>
                                <c:when test="${temFiltros}">
                                    <sh:estadoVazio icone="search" titulo="Nenhum produto encontrado"
                                        texto="Nada combina com a busca e os filtros atuais. Tente outro nome ou limpe os filtros."
                                        acaoUrl="/produtos?tamanho=${tamanho}" acaoRotulo="Limpar filtros" />
                                </c:when>
                                <c:when test="${empty categorias}">
                                    <sh:estadoVazio icone="tags" titulo="Comece criando uma categoria"
                                        texto="Todo produto pertence a uma categoria, como Bebidas ou Limpeza. Crie a primeira e depois cadastre seus produtos."
                                        acaoUrl="/categorias/novo" acaoRotulo="Criar categoria" />
                                </c:when>
                                <c:otherwise>
                                    <sh:estadoVazio icone="box-seam" titulo="Cadastre seu primeiro produto"
                                        texto="Com os produtos cadastrados você acompanha o saldo e recebe o aviso de reposição."
                                        acaoUrl="/produtos/novo" acaoRotulo="Novo produto" />
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>

                    <%-- ===== Tabela (vira cartões no celular) ===== --%>
                    <c:otherwise>
                        <div class="sh-table-wrap">
                            <table class="sh-table sh-table--cards" role="table">
                                <caption class="visually-hidden">Lista de produtos, <c:out value="${totalProdutosFiltro}" /> no total</caption>
                                <thead role="rowgroup">
                                    <tr role="row">
                                        <sh:colunaOrdenavel campo="nome" rotulo="Produto" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" />
                                        <sh:colunaOrdenavel campo="categoria" rotulo="Categoria" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" />
                                        <sh:colunaOrdenavel campo="preco" rotulo="Preço" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" numerico="true" />
                                        <sh:colunaOrdenavel campo="quantidade" rotulo="Saldo" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" />
                                        <th scope="col" role="columnheader">Situação</th>
                                        <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                                    </tr>
                                </thead>
                                <tbody role="rowgroup">
                                    <c:forEach var="produto" items="${produtos}">
                                        <tr role="row" class="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sh-linha--sem' : (produto.nivelEstoque == 'BAIXO' ? 'sh-linha--baixo' : '')}">
                                            <td role="cell" class="sh-prod-celula-produto">
                                                <a class="sh-prod-nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                                <c:if test="${not empty produto.descricao}">
                                                    <span class="sh-prod-desc"><c:out value="${produto.descricao}" /></span>
                                                </c:if>
                                            </td>
                                            <td role="cell" data-label="Categoria"><c:out value="${produto.categoria.nome}" /></td>
                                            <td role="cell" class="sh-col-num" data-label="Preço">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></td>
                                            <td role="cell" data-label="Saldo">
                                                <div class="sh-saldo sh-prod-saldo">
                                                    <span class="sh-saldo__texto">
                                                        <strong><c:out value="${produto.quantidade}" /></strong> un.
                                                        <c:if test="${produto.estoqueMinimo > 0}">
                                                            <span class="sh-saldo__minimo">(mínimo <c:out value="${produto.estoqueMinimo}" />)</span>
                                                        </c:if>
                                                    </span>
                                                    <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                                </div>
                                            </td>
                                            <td role="cell" data-label="Situação"><sh:badgeEstoque nivel="${produto.nivelEstoque}" /></td>
                                            <td role="cell" class="sh-prod-celula-acoes" data-label="">
                                                <div class="sh-prod-acoes">
                                                    <c:choose>
                                                        <c:when test="${produto.nivelEstoque == 'NORMAL'}">
                                                            <a class="btn btn-outline-secondary btn-sm"
                                                                href="<c:out value='${ctx}/estoque?movProduto=${produto.id}'/>">
                                                                <i class="bi bi-arrow-left-right" aria-hidden="true"></i> Movimentar<span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                            </a>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a class="btn btn-primary btn-sm"
                                                                href="<c:out value='${ctx}/estoque?movProduto=${produto.id}&movTipo=ENTRADA'/>">
                                                                <i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Repor<span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                            </a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    <a class="btn btn-ghost btn-sm" href="<c:out value='${ctx}/produtos/${produto.id}/editar'/>">
                                                        <i class="bi bi-pencil" aria-hidden="true"></i><span class="sh-prod-acao__txt">Editar</span><span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                    </a>
                                                    <form method="post" action="<c:out value='${ctx}/produtos/${produto.id}/excluir'/>"
                                                        data-confirmar-exclusao data-tipo="o produto" data-nome="<c:out value='${produto.nome}'/>"
                                                        data-aviso="Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.">
                                                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                        <button type="submit" class="btn btn-ghost btn-ghost--perigo btn-sm">
                                                            <i class="bi bi-trash3" aria-hidden="true"></i><span class="sh-prod-acao__txt">Excluir</span><span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                        </button>
                                                    </form>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                            <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalProdutosFiltro}"
                                tamanho="${tamanho}" base="/produtos" extra="${extraPaginacao}" rotulo="Paginação de produtos" />
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- Esqueleto: só aparece se a atualização ao digitar demorar --%>
            <div class="sh-prod-esqueleto" data-busca-esqueleto hidden aria-hidden="true">
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
                <span class="sh-skeleton"></span>
            </div>
        </div>
    </jsp:body>
</sh:layout>
