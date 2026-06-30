<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="activePage" value="dashboard" />
        <!DOCTYPE html>
        <html lang="pt-BR">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
            <title>StockHub - Unidades</title>
        </head>

        <body>
            <div class="estrutura-sistema d-flex min-vh-100">
                <jsp:include page="../includes/sidebar.jsp" />
                <div class="conteudo-sistema flex-grow-1">
                    <jsp:include page="../includes/navbar.jsp" />
                    <main class="conteudo-pagina">
                        <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-4">
                            <div class="titulo-pagina">
                                <h1>Unidades em estoque</h1>
                                <p>Distribuição da quantidade total entre os produtos cadastrados.</p>
                            </div><a class="btn btn-outline-secondary"
                                href="${pageContext.request.contextPath}/dashboard"><i class="bi bi-arrow-left"></i>
                                Voltar ao dashboard</a>
                        </section>
                        <section class="row g-4">
                            <div class="col-12 col-xl-4">
                                <article class="cartao-metrica borda-verde card h-100">
                                    <div class="card-body"><span class="nome-metrica">Total de
                                            unidades</span><strong>${resumo.quantidadeTotalItens}</strong><small>soma de
                                            todos os produtos</small></div>
                                </article>
                            </div>
                            <div class="col-12 col-xl-8">
                                <section class="painel card">
                                    <div class="cabecalho-painel card-header">
                                        <h2><i class="bi bi-stack"></i> Quantidade por produto</h2>
                                        <p>Lista com o saldo atual de cada item.</p>
                                    </div>
                                    <div class="table-responsive">
                                        <table class="table tabela-sistema align-middle">
                                            <thead>
                                                <tr>
                                                    <th>Produto</th>
                                                    <th>Categoria</th>
                                                    <th>Quantidade</th>
                                                    <th>Mínimo</th>
                                                    <th>Status</th>
                                                    <th>Ações</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach var="produto" items="${produtos}">
                                                    <tr>
                                                        <td><strong>${produto.nome}</strong></td>
                                                        <td>${produto.categoria.nome}</td>
                                                        <td>${produto.quantidade} un.</td>
                                                        <td>${produto.estoqueMinimo}</td>
                                                        <td><span
                                                                class="etiqueta ${produto.quantidade == 0 ? 'etiqueta-sem-estoque' : (produto.estoqueBaixo ? 'etiqueta-alerta' : 'etiqueta-ok')}">${produto.quantidade
                                                                == 0 ? 'Sem estoque' : (produto.estoqueBaixo ? 'Baixo' :
                                                                'Ok')}</span></td>
                                                        <td><a class="btn btn-outline-secondary btn-sm"
                                                                href="${pageContext.request.contextPath}/produtos/${produto.id}"><i
                                                                    class="bi bi-eye"></i> Ver</a></td>
                                                    </tr>
                                                </c:forEach>
                                                <c:if test="${empty produtos}">
                                                    <tr>
                                                        <td colspan="6">
                                                            <div class="estado-vazio">Nenhum produto cadastrado.</div>
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