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
                <title>StockHub - Produto</title>
            </head>

            <body>
                <div class="estrutura-sistema d-flex min-vh-100">
                    <jsp:include page="../includes/sidebar.jsp" />
                    <div class="conteudo-sistema flex-grow-1">
                        <jsp:include page="../includes/navbar.jsp" />
                        <main class="conteudo-pagina">
                            <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-4">
                                <div class="titulo-pagina">
                                    <h1>${produto.nome}</h1>
                                    <p>Visualização completa do produto cadastrado no estoque.</p>
                                </div>
                                <div class="d-flex gap-2 flex-wrap"><a class="btn btn-outline-secondary"
                                        href="${pageContext.request.contextPath}/produtos"><i
                                            class="bi bi-arrow-left"></i> Voltar</a><a class="btn btn-primary"
                                        href="${pageContext.request.contextPath}/produtos/${produto.id}/editar"><i
                                            class="bi bi-pencil"></i> Editar produto</a></div>
                            </section>
                            <section class="row g-4">
                                <div class="col-12 col-xl-8">
                                    <article class="painel detalhe-produto card">
                                        <div class="card-body">
                                            <div
                                                class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-4">
                                                <div><span class="rotulo-pagina">Produto #${produto.id}</span>
                                                    <h2>${produto.nome}</h2><span class="etiqueta"><i
                                                            class="bi bi-tag"></i> ${produto.categoria.nome}</span>
                                                </div><span
                                                    class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : (produto.estoqueBaixo ? 'etiqueta-alerta' : 'etiqueta-ok')}"><i
                                                        class="bi ${produto.quantidade == 0 ? 'bi-x-circle' : (produto.estoqueBaixo ? 'bi-exclamation-triangle' : 'bi-check2-circle')}"></i>${produto.quantidade
                                                    == 0 ? 'Sem estoque' : (produto.estoqueBaixo ? 'Estoque baixo' :
                                                    'Estoque ok')}</span>
                                            </div>
                                            <div class="bloco-descricao">
                                                <h3>Descrição</h3>
                                                <p>${empty produto.descricao ? 'Sem descrição cadastrada.' :
                                                    produto.descricao}</p>
                                            </div>
                                        </div>
                                    </article>
                                </div>
                                <div class="col-12 col-xl-4">
                                    <section class="row row-cols-1 g-3">
                                        <div class="col">
                                            <article class="cartao-metrica borda-azul card">
                                                <div class="card-body"><span class="nome-metrica">Preço
                                                        unitário</span><strong>R$
                                                        <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" />
                                                    </strong><small>valor de venda do item</small></div>
                                            </article>
                                        </div>
                                        <div class="col">
                                            <article class="cartao-metrica borda-verde card">
                                                <div class="card-body"><span
                                                        class="nome-metrica">Quantidade</span><strong>${produto.quantidade}</strong><small>unidades
                                                        disponíveis</small></div>
                                            </article>
                                        </div>
                                        <div class="col">
                                            <article class="cartao-metrica borda-amarela card">
                                                <div class="card-body"><span class="nome-metrica">Valor em
                                                        estoque</span><strong>R$
                                                        <fmt:formatNumber value="${produto.valorTotal}"
                                                            pattern="#,##0.00" />
                                                    </strong><small>preço x quantidade</small></div>
                                            </article>
                                        </div>
                                        <div class="col">
                                            <article class="cartao-metrica borda-vermelha card">
                                                <div class="card-body"><span class="nome-metrica">Estoque
                                                        mínimo</span><strong>${produto.estoqueMinimo}</strong><small>limite
                                                        para alerta</small></div>
                                            </article>
                                        </div>
                                    </section>
                                </div>
                            </section>
                        </main>
                    </div>
                </div>
            </body>

            </html>