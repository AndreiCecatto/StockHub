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
                <title>StockHub - Valor em estoque</title>
            </head>

            <body>
                <div class="estrutura-sistema d-flex min-vh-100">
                    <jsp:include page="../includes/sidebar.jsp" />
                    <div class="conteudo-sistema flex-grow-1">
                        <jsp:include page="../includes/navbar.jsp" />
                        <main class="conteudo-pagina">
                            <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-4">
                                <div class="titulo-pagina">
                                    <h1>Valor em estoque</h1>
                                    <p>Visão financeira do estoque com valor unitário, quantidade e total por produto.
                                    </p>
                                </div><a class="btn btn-outline-secondary"
                                    href="${pageContext.request.contextPath}/dashboard"><i class="bi bi-arrow-left"></i>
                                    Voltar ao dashboard</a>
                            </section>
                            <section class="row g-4">
                                <div class="col-12 col-xl-4">
                                    <article class="cartao-metrica borda-amarela card h-100">
                                        <div class="card-body"><span class="nome-metrica">Valor total</span><strong>R$
                                                <fmt:formatNumber value="${resumo.valorTotalEstoque}"
                                                    pattern="#,##0.00" />
                                            </strong><small>preço x quantidade</small></div>
                                    </article>
                                </div>
                                <div class="col-12 col-xl-8">
                                    <section class="painel card">
                                        <div class="cabecalho-painel card-header">
                                            <h2><i class="bi bi-cash-coin"></i> Valor por produto</h2>
                                            <p>Quanto cada item representa dentro do estoque.</p>
                                        </div>
                                        <div class="table-responsive">
                                            <table class="table tabela-sistema align-middle">
                                                <thead>
                                                    <tr>
                                                        <th>Produto</th>
                                                        <th>Categoria</th>
                                                        <th>Preço</th>
                                                        <th>Quantidade</th>
                                                        <th>Total</th>
                                                        <th>Ações</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <c:forEach var="produto" items="${produtos}">
                                                        <tr>
                                                            <td><strong>${produto.nome}</strong></td>
                                                            <td>${produto.categoria.nome}</td>
                                                            <td>R$
                                                                <fmt:formatNumber value="${produto.preco}"
                                                                    pattern="#,##0.00" />
                                                            </td>
                                                            <td>${produto.quantidade} un.</td>
                                                            <td><span class="etiqueta">R$
                                                                    <fmt:formatNumber value="${produto.valorTotal}"
                                                                        pattern="#,##0.00" />
                                                                </span></td>
                                                            <td><a class="btn btn-outline-secondary btn-sm"
                                                                    href="${pageContext.request.contextPath}/produtos/${produto.id}"><i
                                                                        class="bi bi-eye"></i> Ver</a></td>
                                                        </tr>
                                                    </c:forEach>
                                                    <c:if test="${empty produtos}">
                                                        <tr>
                                                            <td colspan="6">
                                                                <div class="estado-vazio">Nenhum produto cadastrado.
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    </c:if>
                                                </tbody>
                                            </table>
                                        </div>
                                    </section>
                                </div>
                            </section>
                        </main>
                    </div>
                </div>
            </body>

            </html>