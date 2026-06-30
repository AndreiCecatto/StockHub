<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <c:set var="activePage" value="estoqueBaixo" />
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
                <title>StockHub - Estoque baixo</title>
            </head>

            <body>
                <div class="estrutura-sistema d-flex min-vh-100">
                    <jsp:include page="../includes/sidebar.jsp" />
                    <div class="conteudo-sistema flex-grow-1">
                        <jsp:include page="../includes/navbar.jsp" />
                        <main class="conteudo-pagina">
                            <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                                <div class="titulo-pagina">
                                    <h1>Estoque baixo</h1>
                                    <p>Itens com quantidade menor ou igual ao mínimo.</p>
                                </div><a class="btn btn-primary"
                                    href="${pageContext.request.contextPath}/produtos/novo"><i
                                        class="bi bi-plus-square"></i> Novo produto</a>
                            </section>
                            <section class="painel card">
                                <div
                                    class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3">
                                    <h2><i class="bi bi-exclamation-triangle"></i> Alertas</h2><a
                                        class="btn btn-outline-light btn-sm"
                                        href="${pageContext.request.contextPath}/produtos">Abrir catálogo</a>
                                </div>
                                <div class="table-responsive">
                                    <table class="table tabela-sistema align-middle">
                                        <thead>
                                            <tr>
                                                <th>Produto</th>
                                                <th>Categoria</th>
                                                <th>Preço</th>
                                                <th>Quantidade</th>
                                                <th>Mínimo</th>
                                                <th>Ações</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="produto" items="${produtos}">
                                                <tr>
                                                    <td><strong>${produto.nome}</strong><span
                                                            class="subtitulo-linha">${produto.descricao}</span></td>
                                                    <td>${produto.categoria.nome}</td>
                                                    <td>R$
                                                        <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" />
                                                    </td>
                                                    <td><span
                                                            class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : 'etiqueta-alerta'}">${produto.quantidade
                                                            == 0 ? 'Sem estoque' : produto.quantidade}<c:if
                                                                test="${produto.quantidade != 0}"> un.</c:if></span>
                                                    </td>
                                                    <td>${produto.estoqueMinimo}</td>
                                                    <td><a class="btn btn-outline-secondary btn-sm"
                                                            href="${pageContext.request.contextPath}/produtos/${produto.id}"><i
                                                                class="bi bi-eye"></i> Ver</a> <a
                                                            class="btn btn-outline-light btn-sm"
                                                            href="${pageContext.request.contextPath}/produtos/${produto.id}/editar"><i
                                                                class="bi bi-pencil"></i> Editar</a></td>
                                                </tr>
                                            </c:forEach>
                                            <c:if test="${empty produtos}">
                                                <tr>
                                                    <td colspan="6">
                                                        <div class="estado-vazio">Nenhum produto com estoque baixo.
                                                        </div>
                                                    </td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>
                                </div>
                            </section>
                        </main>
                    </div>
                </div>
            </body>

            </html>
