<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="activePage" value="categorias" />
        <!DOCTYPE html>
        <html lang="pt-BR">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
            <title>StockHub - Categorias</title>
        </head>

        <body>
            <div class="estrutura-sistema d-flex min-vh-100">
                <jsp:include page="../includes/sidebar.jsp" />
                <div class="conteudo-sistema flex-grow-1">
                    <jsp:include page="../includes/navbar.jsp" />
                    <main class="conteudo-pagina">
                        <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                            <div class="titulo-pagina">
                                <h1>Categorias</h1>
                                <p>Grupos usados para organizar o estoque.</p>
                            </div><a class="btn btn-primary"
                                href="${pageContext.request.contextPath}/categorias/novo"><i
                                    class="bi bi-folder-plus"></i> Nova categoria</a>
                        </section>
                        <form class="row g-2 mb-3" method="get" action="${pageContext.request.contextPath}/categorias">
                            <div class="col-12 col-lg"><input class="form-control" type="search" name="busca"
                                    value="${busca}" placeholder="Buscar por nome"></div>
                            <div class="col-12 col-lg-auto"><button class="btn btn-outline-light w-100" type="submit"><i
                                        class="bi bi-search"></i> Buscar</button></div>
                        </form>
                        <c:if test="${not empty erro}">
                            <div class="alert alert-danger">${erro}</div>
                        </c:if>
                        <section class="row row-cols-1 row-cols-md-2 row-cols-xl-4 g-3">
                            <c:forEach var="categoria" items="${categorias}">
                                <div class="col">
                                    <article class="cartao-catalogo card h-100">
                                        <div class="card-body">
                                            <h3>${categoria.nome}</h3>
                                            <p class="descricao-catalogo">${categoria.descricao}</p>
                                            <div class="d-flex gap-2 flex-wrap mb-3"><span class="etiqueta"><i
                                                        class="bi bi-boxes"></i> ${categoria.totalProdutos}
                                                    produtos</span></div>
                                            <div class="d-flex gap-2 flex-wrap pt-3 border-top border-secondary"><a
                                                    class="btn btn-outline-light btn-sm"
                                                    href="${pageContext.request.contextPath}/categorias/${categoria.id}/editar"><i
                                                        class="bi bi-pencil"></i> Editar</a>
                                                <form method="post"
                                                    action="${pageContext.request.contextPath}/categorias/${categoria.id}/excluir"
                                                    onsubmit="return confirm('Excluir esta categoria?')"><button
                                                        class="btn btn-outline-danger btn-sm" type="submit"><i
                                                            class="bi bi-trash"></i> Excluir</button></form>
                                            </div>
                                        </div>
                                    </article>
                                </div>
                            </c:forEach>
                        </section>
                        <c:if test="${empty categorias}">
                            <div class="estado-vazio">Nenhuma categoria encontrada.</div>
                        </c:if>
                    </main>
                </div>
            </div>
        </body>

        </html>