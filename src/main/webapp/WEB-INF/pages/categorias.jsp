<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activePage" value="categorias" scope="request" />
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>Categorias - StockHub</title>
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
                    </div>
                    <a class="btn btn-primary" href="${pageContext.request.contextPath}/categorias/novo">
                        <i class="bi bi-folder-plus" aria-hidden="true"></i> Nova categoria
                    </a>
                </section>

                <%-- Flash: "mensagem" (sucesso) e "erro" (ex.: categoria com produtos ativos) --%>
                <jsp:include page="../includes/alertas.jsp" />

                <form class="row g-2 mb-3" method="get" action="${pageContext.request.contextPath}/categorias" role="search">
                    <div class="col-12 col-lg">
                        <label class="visually-hidden" for="busca">Buscar categoria por nome</label>
                        <input class="form-control" id="busca" type="search" name="busca"
                            value="<c:out value='${busca}'/>" placeholder="Buscar por nome">
                    </div>
                    <div class="col-12 col-lg-auto">
                        <button class="btn btn-outline-light w-100" type="submit">
                            <i class="bi bi-search" aria-hidden="true"></i> Buscar
                        </button>
                    </div>
                </form>

                <section class="row row-cols-1 row-cols-md-2 row-cols-xl-4 g-3" aria-label="Lista de categorias">
                    <c:forEach var="categoria" items="${categorias}">
                        <div class="col">
                            <article class="cartao-catalogo card h-100">
                                <div class="card-body">
                                    <h2 class="h3"><c:out value="${categoria.nome}" /></h2>
                                    <p class="descricao-catalogo"><c:out value="${categoria.descricao}" /></p>
                                    <div class="d-flex gap-2 flex-wrap mb-3">
                                        <span class="etiqueta"><i class="bi bi-boxes" aria-hidden="true"></i>
                                            <c:out value="${categoria.totalProdutos}" /> produtos</span>
                                    </div>
                                    <div class="d-flex gap-2 flex-wrap pt-3 border-top border-secondary">
                                        <a class="btn btn-outline-light btn-sm"
                                            href="${pageContext.request.contextPath}/categorias/<c:out value='${categoria.id}'/>/editar"
                                            aria-label="Editar categoria <c:out value='${categoria.nome}'/>">
                                            <i class="bi bi-pencil" aria-hidden="true"></i> Editar
                                        </a>
                                        <form method="post" class="m-0"
                                            action="${pageContext.request.contextPath}/categorias/<c:out value='${categoria.id}'/>/excluir"
                                            data-confirmar-exclusao data-tipo="a categoria" data-nome="<c:out value='${categoria.nome}'/>"
                                            data-aviso="A categoria não pode ser excluída se algum produto a usa, ativo ou inativado (o histórico é preservado).">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                            <button class="btn btn-outline-danger btn-sm" type="submit"
                                                aria-label="Excluir categoria <c:out value='${categoria.nome}'/>">
                                                <i class="bi bi-trash" aria-hidden="true"></i> Excluir
                                            </button>
                                        </form>
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
    <jsp:include page="../includes/modal-exclusao.jsp" />
    <jsp:include page="../includes/scripts.jsp" />
</body>

</html>
