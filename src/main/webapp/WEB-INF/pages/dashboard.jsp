<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <c:set var="activePage" value="dashboard" />
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
                <title>StockHub - Visão geral</title>
            </head>

            <body>
                <div class="estrutura-sistema d-flex min-vh-100">
                    <jsp:include page="../includes/sidebar.jsp" />
                    <div class="conteudo-sistema flex-grow-1">
                        <jsp:include page="../includes/navbar.jsp" />
                        <main class="conteudo-pagina">
                            <section
                                class="cabecalho-painel-principal d-flex align-items-start justify-content-between gap-3 flex-wrap">
                                <div>
                                    <span class="rotulo-pagina">Dashboard</span>
                                    <h1>Visão geral</h1>
                                    <p>Acompanhe os principais números do estoque e os itens que precisam de atenção.
                                    </p>
                                </div>
                                <div class="d-flex gap-2 flex-wrap">
                                    <a class="btn btn-primary" href="${pageContext.request.contextPath}/produtos/novo">
                                        <i class="bi bi-plus-square"></i> Novo produto
                                    </a>
                                    <a class="btn btn-outline-secondary"
                                        href="${pageContext.request.contextPath}/categorias/novo">
                                        <i class="bi bi-folder-plus"></i> Nova categoria
                                    </a>
                                </div>
                            </section>
                            <section class="row row-cols-1 row-cols-md-2 row-cols-xl-5 g-3 mb-4">
                                <div class="col"><a class="link-cartao text-decoration-none"
                                        href="${pageContext.request.contextPath}/produtos">
                                        <article class="cartao-metrica borda-azul card h-100">
                                            <div class="card-body">
                                                <div
                                                    class="d-flex align-items-center justify-content-between gap-2 mb-3">
                                                    <span class="nome-metrica">Produtos</span><span
                                                        class="icone-metrica destaque-azul"><i
                                                            class="bi bi-box"></i></span></div>
                                                <strong>${resumo.totalProdutos}</strong><small>itens cadastrados</small>
                                            </div>
                                        </article>
                                    </a></div>
                                <div class="col"><a class="link-cartao text-decoration-none"
                                        href="${pageContext.request.contextPath}/categorias">
                                        <article class="cartao-metrica borda-roxa card h-100">
                                            <div class="card-body">
                                                <div
                                                    class="d-flex align-items-center justify-content-between gap-2 mb-3">
                                                    <span class="nome-metrica">Categorias</span><span
                                                        class="icone-metrica destaque-roxo"><i
                                                            class="bi bi-tags"></i></span></div>
                                                <strong>${resumo.totalCategorias}</strong><small>grupos ativos</small>
                                            </div>
                                        </article>
                                    </a></div>
                                <div class="col"><a class="link-cartao text-decoration-none"
                                        href="${pageContext.request.contextPath}/estoque-baixo">
                                        <article class="cartao-metrica borda-vermelha card h-100">
                                            <div class="card-body">
                                                <div
                                                    class="d-flex align-items-center justify-content-between gap-2 mb-3">
                                                    <span class="nome-metrica">Estoque baixo</span><span
                                                        class="icone-metrica destaque-vermelho"><i
                                                            class="bi bi-exclamation-triangle"></i></span></div>
                                                <strong>${resumo.produtosEstoqueBaixo}</strong><small>produtos em
                                                    alerta</small>
                                            </div>
                                        </article>
                                    </a></div>
                                <div class="col"><a class="link-cartao text-decoration-none"
                                        href="${pageContext.request.contextPath}/unidades">
                                        <article class="cartao-metrica borda-verde card h-100">
                                            <div class="card-body">
                                                <div
                                                    class="d-flex align-items-center justify-content-between gap-2 mb-3">
                                                    <span class="nome-metrica">Unidades</span><span
                                                        class="icone-metrica destaque-verde"><i
                                                            class="bi bi-stack"></i></span></div>
                                                <strong>${resumo.quantidadeTotalItens}</strong><small>quantidade
                                                    total</small>
                                            </div>
                                        </article>
                                    </a></div>
                                <div class="col"><a class="link-cartao text-decoration-none"
                                        href="${pageContext.request.contextPath}/valor-estoque">
                                        <article class="cartao-metrica borda-amarela card h-100">
                                            <div class="card-body">
                                                <div
                                                    class="d-flex align-items-center justify-content-between gap-2 mb-3">
                                                    <span class="nome-metrica">Valor total</span><span
                                                        class="icone-metrica destaque-amarelo"><i
                                                            class="bi bi-cash-coin"></i></span></div><strong>R$
                                                    <fmt:formatNumber value="${resumo.valorTotalEstoque}"
                                                        pattern="#,##0.00" />
                                                </strong><small>preço x quantidade</small>
                                            </div>
                                        </article>
                                    </a></div>
                            </section>
                            <section class="row g-4">
                                <div class="col-12 col-xl-8">
                                    <section class="painel card h-100">
                                        <div
                                            class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3">
                                            <div>
                                                <h2><i class="bi bi-clock-history"></i> Produtos recentes</h2>
                                                <p>Últimos itens adicionados ao catálogo.</p>
                                            </div><a class="btn btn-outline-secondary btn-sm"
                                                href="${pageContext.request.contextPath}/produtos">Abrir catálogo</a>
                                        </div>
                                        <div class="card-body">
                                            <div class="d-grid gap-2">
                                                <c:forEach var="produto" items="${produtosRecentes}">
                                                    <a class="linha-estoque text-decoration-none"
                                                        href="${pageContext.request.contextPath}/produtos/${produto.id}">
                                                        <span class="miniatura-vazia"><i
                                                                class="bi bi-box-seam"></i></span>
                                                        <div><span class="titulo-linha">${produto.nome}</span><span
                                                                class="subtitulo-linha">${produto.categoria.nome}</span>
                                                        </div>
                                                        <div
                                                            class="meta-linha d-flex align-items-center gap-2 flex-wrap">
                                                            <span class="etiqueta">R$
                                                                <fmt:formatNumber value="${produto.preco}"
                                                                    pattern="#,##0.00" />
                                                            </span><span
                                                                class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : (produto.estoqueBaixo ? 'etiqueta-alerta' : 'etiqueta-ok')}">${produto.quantidade}
                                                                un.</span></div>
                                                    </a>
                                                </c:forEach>
                                            </div>
                                            <c:if test="${empty produtosRecentes}">
                                                <div class="estado-vazio">Nenhum produto cadastrado.</div>
                                            </c:if>
                                        </div>
                                    </section>
                                </div>
                                <div class="col-12 col-xl-4">
                                    <section class="painel painel-alerta card mb-4">
                                        <div
                                            class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3">
                                            <div>
                                                <h2><i class="bi bi-exclamation-circle"></i> Estoque baixo</h2>
                                                <p>Produtos abaixo do mínimo definido.</p>
                                            </div><a class="btn btn-outline-secondary btn-sm"
                                                href="${pageContext.request.contextPath}/estoque-baixo">Ver lista</a>
                                        </div>
                                        <div class="card-body">
                                            <div class="lista-estoque-baixo-dashboard d-grid gap-2">
                                                <c:forEach var="produto" items="${produtosEstoqueBaixo}">
                                                    <a class="linha-estoque ${produto.quantidade == 0 ? 'linha-sem-estoque' : 'linha-alerta'} text-decoration-none"
                                                        href="${pageContext.request.contextPath}/produtos/${produto.id}">
                                                        <span class="miniatura-vazia"><i
                                                                class="bi bi-exclamation-triangle"></i></span>
                                                        <div><span class="titulo-linha">${produto.nome}</span><span
                                                                class="subtitulo-linha">${produto.categoria.nome}</span>
                                                        </div>
                                                        <div
                                                            class="meta-linha d-flex align-items-center gap-2 flex-wrap">
                                                            <span
                                                                class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : 'etiqueta-alerta'}">${produto.quantidade}/${produto.estoqueMinimo}</span>
                                                        </div>
                                                    </a>
                                                </c:forEach>
                                            </div>
                                            <c:if test="${empty produtosEstoqueBaixo}">
                                                <div class="estado-vazio">Nenhum alerta de estoque.</div>
                                            </c:if>
                                        </div>
                                    </section>
                                    <section class="painel card">
                                        <div class="cabecalho-painel card-header">
                                            <h2><i class="bi bi-lightning-charge"></i> Ações rápidas</h2>
                                            <p>Atalhos para as rotinas principais.</p>
                                        </div>
                                        <div class="card-body">
                                            <div class="row row-cols-1 g-2">
                                                <div class="col"><a
                                                        class="acao-rapida d-flex align-items-center gap-3 text-decoration-none"
                                                        href="${pageContext.request.contextPath}/produtos/novo"><i
                                                            class="bi bi-plus-square"></i><span><strong>Cadastrar
                                                                produto</strong><small>Adicionar um novo item ao
                                                                estoque</small></span></a></div>
                                                <div class="col"><a
                                                        class="acao-rapida d-flex align-items-center gap-3 text-decoration-none"
                                                        href="${pageContext.request.contextPath}/categorias/novo"><i
                                                            class="bi bi-folder-plus"></i><span><strong>Cadastrar
                                                                categoria</strong><small>Organizar produtos por
                                                                grupo</small></span></a></div>
                                                <div class="col"><a
                                                        class="acao-rapida d-flex align-items-center gap-3 text-decoration-none"
                                                        href="${pageContext.request.contextPath}/estoque"><i
                                                            class="bi bi-arrow-left-right"></i><span><strong>Movimentar
                                                                estoque</strong><small>Registrar entrada ou
                                                                saída</small></span></a></div>
                                            </div>
                                        </div>
                                    </section>
                                </div>
                            </section>
                        </main>
                    </div>
                </div>
            </body>

            </html>