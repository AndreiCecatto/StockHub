<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <c:set var="activePage" value="produtos" />
            <fmt:setLocale value="pt_BR" />
            <!DOCTYPE html>
            <html lang="pt-BR">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
                    rel="stylesheet">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
                <title>StockHub - Produtos</title>
            </head>

            <body>
                <div class="estrutura-sistema d-flex min-vh-100">
                    <jsp:include page="../includes/sidebar.jsp" />
                    <div class="conteudo-sistema flex-grow-1">
                        <jsp:include page="../includes/navbar.jsp" />
                        <main class="conteudo-pagina">
                            <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                                <div class="titulo-pagina">
                                    <h1>Produtos</h1>
                                    <p>Catálogo do estoque da loja.</p>
                                </div><a class="btn btn-primary"
                                    href="${pageContext.request.contextPath}/produtos/novo"><i
                                        class="bi bi-plus-square"></i> Novo produto</a>
                            </section>
                            <section class="painel-filtros card mb-4">
                                <div class="card-body">
                                    <form class="row g-3 align-items-end" method="get"
                                        action="${pageContext.request.contextPath}/produtos">
                                        <div class="col-12 col-lg-3"><label class="form-label" for="busca">Nome</label>
                                            <div class="input-group"><span class="input-group-text"><i
                                                        class="bi bi-search"></i></span><input class="form-control"
                                                    id="busca" type="search" name="busca" value="${busca}"
                                                    placeholder="Buscar por nome"></div>
                                        </div>
                                        <div class="col-12 col-lg-2"><label class="form-label"
                                                for="categoriaId">Categoria</label><select class="form-select"
                                                id="categoriaId" name="categoriaId">
                                                <option value="">Todas</option>
                                                <c:forEach var="categoria" items="${categorias}">
                                                    <option value="${categoria.id}" ${categoriaId==categoria.id
                                                        ? 'selected' : '' }>${categoria.nome}</option>
                                                </c:forEach>
                                            </select></div>
                                        <div class="col-12 col-lg-2"><label class="form-label" for="nivelEstoque">Nível
                                                de estoque</label><select class="form-select" id="nivelEstoque"
                                                name="nivelEstoque">
                                                <option value="">Todos</option>
                                                <option value="ok" ${nivelEstoque=='ok' ? 'selected' : '' }>Estoque ok
                                                </option>
                                                <option value="baixo" ${nivelEstoque=='baixo' ? 'selected' : '' }>
                                                    Estoque baixo</option>
                                                <option value="semEstoque" ${nivelEstoque=='semEstoque' ? 'selected'
                                                    : '' }>Sem estoque</option>
                                            </select></div>
                                        <div class="col-12 col-lg-2"><label class="form-label"
                                                for="ordenacao">Ordenar</label><select class="form-select"
                                                id="ordenacao" name="ordenacao">
                                                <option value="id" ${ordenacao=='id' ? 'selected' : '' }>Recentes
                                                </option>
                                                <option value="nome" ${ordenacao=='nome' ? 'selected' : '' }>Nome
                                                </option>
                                                <option value="preco" ${ordenacao=='preco' ? 'selected' : '' }>Preço
                                                </option>
                                                <option value="quantidade" ${ordenacao=='quantidade' ? 'selected' : ''
                                                    }>Quantidade</option>
                                                <option value="categoria" ${ordenacao=='categoria' ? 'selected' : '' }>
                                                    Categoria</option>
                                            </select></div>
                                        <div class="col-12 col-lg-1"><label class="form-label"
                                                for="direcao">Ordem</label><select class="form-select" id="direcao"
                                                name="direcao">
                                                <option value="asc" ${direcao=='asc' ? 'selected' : '' }>Cresc.</option>
                                                <option value="desc" ${direcao=='desc' ? 'selected' : '' }>Decresc.
                                                </option>
                                            </select></div>
                                        <div class="col-12 col-lg-2">
                                            <div class="d-flex gap-2"><input type="hidden" name="tamanho"
                                                    value="${tamanho}"><button class="btn btn-primary flex-fill"
                                                    type="submit"><i class="bi bi-funnel"></i> Filtrar</button><a
                                                    class="btn btn-outline-secondary"
                                                    href="${pageContext.request.contextPath}/produtos"
                                                    aria-label="Limpar filtros"><i class="bi bi-x-lg"></i></a></div>
                                        </div>
                                    </form>
                                </div>
                            </section>
                            <div class="d-flex align-items-center justify-content-between gap-3 flex-wrap mb-3"><span
                                    class="etiqueta"><i class="bi bi-list-check"></i> ${totalProdutosFiltro}
                                    produto(s)</span><a class="btn btn-outline-secondary btn-sm"
                                    href="${pageContext.request.contextPath}/produtos?pagina=${paginaAtual}&direcao=${direcaoInversa}${parametrosFiltros}"><i
                                        class="bi bi-arrow-down-up"></i> Inverter ordem</a></div>
                            <section class="row row-cols-1 row-cols-md-2 row-cols-xl-4 g-3">
                                <c:forEach var="produto" items="${produtos}">
                                    <div class="col">
                                        <article class="cartao-catalogo card h-100">
                                            <div class="card-body">
                                                <h3>${produto.nome}</h3><span class="preco-produto">R$
                                                    <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" />
                                                </span>
                                                <p class="descricao-catalogo">${produto.descricao}</p>
                                                <div class="d-flex gap-2 flex-wrap mb-3"><span class="etiqueta"><i
                                                            class="bi bi-tag"></i> ${produto.categoria.nome}</span><span
                                                        class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : (produto.estoqueBaixo ? 'etiqueta-alerta' : 'etiqueta-ok')}"><i
                                                            class="bi bi-box"></i> ${produto.quantidade} un.</span>
                                                </div>
                                                <div class="d-flex gap-2 flex-wrap pt-3 border-top border-secondary"><a
                                                        class="btn btn-outline-secondary btn-sm"
                                                        href="${pageContext.request.contextPath}/produtos/${produto.id}"><i
                                                            class="bi bi-eye"></i> Ver</a><a
                                                        class="btn btn-outline-light btn-sm"
                                                        href="${pageContext.request.contextPath}/produtos/${produto.id}/editar"><i
                                                            class="bi bi-pencil"></i> Editar</a>
                                                    <form method="post"
                                                        action="${pageContext.request.contextPath}/produtos/${produto.id}/excluir"
                                                        onsubmit="return confirm('Excluir este produto?')"><button
                                                            class="btn btn-outline-danger btn-sm" type="submit"><i
                                                                class="bi bi-trash"></i> Excluir</button></form>
                                                </div>
                                            </div>
                                        </article>
                                    </div>
                                </c:forEach>
                            </section>
                            <c:if test="${empty produtos}">
                                <div class="estado-vazio">Nenhum produto encontrado.</div>
                            </c:if>
                            <c:if test="${totalPaginas > 1}">
                                <nav class="mt-3" aria-label="Paginação de produtos">
                                    <ul class="pagination pagination-sm mb-0">
                                        <li class="page-item ${paginaAtual == 1 ? 'disabled' : ''}"><a class="page-link"
                                                href="${pageContext.request.contextPath}/produtos?pagina=${paginaAtual - 1}&direcao=${direcao}${parametrosFiltros}">Anterior</a>
                                        </li>
                                        <c:forEach var="pagina" begin="1" end="${totalPaginas}">
                                            <li class="page-item ${pagina == paginaAtual ? 'active' : ''}"><a
                                                    class="page-link"
                                                    href="${pageContext.request.contextPath}/produtos?pagina=${pagina}&direcao=${direcao}${parametrosFiltros}">${pagina}</a>
                                            </li>
                                        </c:forEach>
                                        <li class="page-item ${paginaAtual == totalPaginas ? 'disabled' : ''}"><a
                                                class="page-link"
                                                href="${pageContext.request.contextPath}/produtos?pagina=${paginaAtual + 1}&direcao=${direcao}${parametrosFiltros}">Próxima</a>
                                        </li>
                                    </ul>
                                </nav>
                            </c:if>
                        </main>
                    </div>
                </div>
            </body>

            </html>