<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="dashboard" scope="request" />
<fmt:setLocale value="pt_BR" />
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>Valor em estoque - StockHub</title>
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
                        <p>Visão financeira do estoque com valor unitário, quantidade e total por produto.</p>
                    </div>
                    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/dashboard">
                        <i class="bi bi-arrow-left" aria-hidden="true"></i> Voltar ao dashboard
                    </a>
                </section>

                <jsp:include page="../includes/alertas.jsp" />

                <section class="row g-4">
                    <div class="col-12 col-xl-4">
                        <article class="cartao-metrica borda-amarela card h-100">
                            <div class="card-body">
                                <span class="nome-metrica">Valor total</span>
                                <strong>R$ <fmt:formatNumber value="${resumo.valorTotalEstoque}" pattern="#,##0.00" /></strong>
                                <small>preço x quantidade</small>
                            </div>
                        </article>
                    </div>
                    <div class="col-12 col-xl-8">
                        <section class="painel card">
                            <div class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3 flex-wrap">
                                <div>
                                    <h2><i class="bi bi-cash-coin" aria-hidden="true"></i> Valor por produto</h2>
                                    <p>Quanto cada item representa dentro do estoque.</p>
                                </div>
                                <span class="etiqueta"><c:out value="${totalProdutosFiltro}" /> produto(s)</span>
                            </div>
                            <div class="table-responsive">
                                <table class="table tabela-sistema align-middle">
                                    <thead>
                                        <tr>
                                            <th scope="col">Produto</th>
                                            <th scope="col">Categoria</th>
                                            <th scope="col">Preço</th>
                                            <th scope="col">Quantidade</th>
                                            <th scope="col">Total</th>
                                            <th scope="col">Ações</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="produto" items="${produtos}">
                                            <tr>
                                                <td><strong><c:out value="${produto.nome}" /></strong></td>
                                                <td><c:out value="${produto.categoria.nome}" /></td>
                                                <td>R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></td>
                                                <td><c:out value="${produto.quantidade}" /> un.</td>
                                                <td><span class="etiqueta">R$ <fmt:formatNumber value="${produto.valorTotal}" pattern="#,##0.00" /></span></td>
                                                <td>
                                                    <a class="btn btn-outline-secondary btn-sm"
                                                        href="${pageContext.request.contextPath}/produtos/<c:out value='${produto.id}'/>"
                                                        aria-label="Ver produto <c:out value='${produto.nome}'/>">
                                                        <i class="bi bi-eye" aria-hidden="true"></i> Ver
                                                    </a>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty produtos}">
                                            <tr>
                                                <td colspan="6"><div class="estado-vazio">Nenhum produto cadastrado.</div></td>
                                            </tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                            <jsp:include page="../includes/paginacao.jsp">
                                <jsp:param name="base" value="/valor-estoque" />
                                <jsp:param name="rotulo" value="Paginação da lista de valor em estoque" />
                            </jsp:include>
                        </section>
                    </div>
                </section>
            </main>
        </div>
    </div>
    <jsp:include page="../includes/scripts.jsp" />
</body>

</html>
