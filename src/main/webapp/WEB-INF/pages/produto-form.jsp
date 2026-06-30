<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="activePage" value="produtos" />
        <!DOCTYPE html>
        <html lang="pt-BR">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
            <title>StockHub - Produto</title>
        </head>

        <body>
            <div class="estrutura-sistema d-flex min-vh-100">
                <jsp:include page="../includes/sidebar.jsp" />
                <div class="conteudo-sistema flex-grow-1">
                    <jsp:include page="../includes/navbar.jsp" />
                    <main class="conteudo-pagina">
                        <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                            <div class="titulo-pagina">
                                <h1>${produto.id != null ? 'Editar produto' : 'Cadastrar produto'}</h1>
                                <p>Dados do item, preço, quantidade e categoria.</p>
                            </div><a class="btn btn-outline-light" href="${pageContext.request.contextPath}/produtos"><i
                                    class="bi bi-arrow-left"></i> Voltar</a>
                        </section>
                        <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/produtos">
                            <input type="hidden" name="id" value="${produto.id}">
                            <section class="painel card">
                                <div class="card-body">
                                    <div class="titulo-secao d-flex align-items-center gap-2 mb-3"><i
                                            class="bi bi-card-text"></i> Informações</div>
                                    <div class="row g-3">
                                        <div class="col-12"><label class="form-label" for="nome">Nome do
                                                produto</label><input id="nome" class="form-control" type="text"
                                                name="nome" value="${produto.nome}" placeholder="Ex: Mouse sem fio"
                                                required></div>
                                        <div class="col-12"><label class="form-label"
                                                for="descricao">Descrição</label><textarea id="descricao"
                                                class="form-control" name="descricao" rows="5"
                                                placeholder="Detalhes do produto">${produto.descricao}</textarea></div>
                                        <div class="col-12"><label class="form-label"
                                                for="categoriaId">Categoria</label><select id="categoriaId"
                                                class="form-select" name="categoriaId" required>
                                                <option value="">Selecione uma categoria</option>
                                                <c:forEach var="categoria" items="${categorias}">
                                                    <option value="${categoria.id}" ${produto.categoria.id==categoria.id
                                                        ? 'selected' : '' }>${categoria.nome}</option>
                                                </c:forEach>
                                            </select></div>
                                    </div>
                                </div>
                                <div class="card-body border-top border-secondary">
                                    <div class="titulo-secao d-flex align-items-center gap-2 mb-3"><i
                                            class="bi bi-boxes"></i> Estoque e preço</div>
                                    <div class="row g-3">
                                        <div class="col-12 col-md-4"><label class="form-label"
                                                for="preco">Preço</label><input id="preco" class="form-control"
                                                type="number" step="0.01" min="0" name="preco" value="${produto.preco}"
                                                required></div>
                                        <div class="col-12 col-md-4"><label class="form-label"
                                                for="quantidade">Quantidade</label><input id="quantidade"
                                                class="form-control" type="number" min="0" name="quantidade"
                                                value="${produto.quantidade}" required></div>
                                        <div class="col-12 col-md-4"><label class="form-label"
                                                for="estoqueMinimo">Estoque mínimo</label><input id="estoqueMinimo"
                                                class="form-control" type="number" min="0" name="estoqueMinimo"
                                                value="${produto.estoqueMinimo}" required></div>
                                    </div>
                                </div>
                            </section>
                            <div class="acoes-formulario d-flex justify-content-end gap-2 p-3"><a
                                    class="btn btn-outline-light"
                                    href="${pageContext.request.contextPath}/produtos">Cancelar</a><button
                                    class="btn btn-primary" type="submit"><i class="bi bi-check2-circle"></i> Salvar
                                    produto</button></div>
                        </form>
                    </main>
                </div>
            </div>
        </body>

        </html>