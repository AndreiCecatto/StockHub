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
            <title>StockHub - Categoria</title>
        </head>

        <body>
            <div class="estrutura-sistema d-flex min-vh-100">
                <jsp:include page="../includes/sidebar.jsp" />
                <div class="conteudo-sistema flex-grow-1">
                    <jsp:include page="../includes/navbar.jsp" />
                    <main class="conteudo-pagina">
                        <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                            <div class="titulo-pagina">
                                <h1>${categoria.id != null ? 'Editar categoria' : 'Cadastrar categoria'}</h1>
                                <p>Grupo usado para organizar produtos no catálogo.</p>
                            </div><a class="btn btn-outline-light"
                                href="${pageContext.request.contextPath}/categorias"><i class="bi bi-arrow-left"></i>
                                Voltar</a>
                        </section>
                        <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/categorias">
                            <input type="hidden" name="id" value="${categoria.id}">
                            <section class="painel card">
                                <div class="card-body">
                                    <div class="titulo-secao d-flex align-items-center gap-2 mb-3"><i
                                            class="bi bi-tag"></i> Dados da categoria</div>
                                    <div class="row g-3">
                                        <div class="col-12"><label class="form-label" for="nome">Nome da
                                                categoria</label><input id="nome" class="form-control" type="text"
                                                name="nome" value="${categoria.nome}" placeholder="Ex: Eletrônicos"
                                                required></div>
                                        <div class="col-12"><label class="form-label"
                                                for="descricao">Descrição</label><textarea id="descricao"
                                                class="form-control" name="descricao" rows="6"
                                                placeholder="Resumo do tipo de produto desta categoria">${categoria.descricao}</textarea>
                                        </div>
                                    </div>
                                </div>
                            </section>
                            <div class="acoes-formulario d-flex justify-content-end gap-2 p-3"><a
                                    class="btn btn-outline-light"
                                    href="${pageContext.request.contextPath}/categorias">Cancelar</a><button
                                    class="btn btn-primary" type="submit"><i class="bi bi-check2-circle"></i> Salvar
                                    categoria</button></div>
                        </form>
                    </main>
                </div>
            </div>
        </body>

        </html>