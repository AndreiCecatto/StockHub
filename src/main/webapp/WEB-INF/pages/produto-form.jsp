<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activePage" value="produtos" scope="request" />
<c:set var="edicao" value="${produto.id != null}" />
<%-- Erros por campo (errosCampos vem do controller; pode nao existir no primeiro GET) --%>
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eDescricao" value="${errosCampos['descricao']}" />
<c:set var="eCategoria" value="${errosCampos['categoriaId']}" />
<c:set var="ePreco" value="${errosCampos['preco']}" />
<c:set var="eQuantidade" value="${errosCampos['quantidade']}" />
<c:set var="eMinimo" value="${errosCampos['estoqueMinimo']}" />
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>${edicao ? 'Editar produto' : 'Novo produto'} - StockHub</title>
</head>

<body>
    <div class="estrutura-sistema d-flex min-vh-100">
        <jsp:include page="../includes/sidebar.jsp" />
        <div class="conteudo-sistema flex-grow-1">
            <jsp:include page="../includes/navbar.jsp" />
            <main class="conteudo-pagina">
                <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                    <div class="titulo-pagina">
                        <h1>${edicao ? 'Editar produto' : 'Cadastrar produto'}</h1>
                        <p>Dados do item, preço, estoque e categoria.</p>
                    </div>
                    <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/produtos">
                        <i class="bi bi-arrow-left" aria-hidden="true"></i> Voltar
                    </a>
                </section>

                <c:if test="${not empty erro}">
                    <div class="alert alert-danger" role="alert"><c:out value="${erro}" /></div>
                </c:if>

                <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/produtos">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <input type="hidden" name="id" value="<c:out value='${produto.id}'/>">
                    <section class="painel card">
                        <div class="card-body">
                            <div class="titulo-secao d-flex align-items-center gap-2 mb-3">
                                <i class="bi bi-card-text" aria-hidden="true"></i> Informações
                            </div>
                            <div class="row g-3">
                                <div class="col-12">
                                    <label class="form-label" for="nome">Nome do produto</label>
                                    <input id="nome" class="form-control ${not empty eNome ? 'is-invalid' : ''}" type="text"
                                        name="nome" value="<c:out value='${produto.nome}'/>" placeholder="Ex: Mouse sem fio"
                                        minlength="2" maxlength="120" required
                                        ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                                    <c:if test="${not empty eNome}">
                                        <div id="erro-nome" class="invalid-feedback"><c:out value="${eNome}" /></div>
                                    </c:if>
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="descricao">Descrição</label>
                                    <textarea id="descricao" class="form-control ${not empty eDescricao ? 'is-invalid' : ''}"
                                        name="descricao" rows="5" maxlength="500" placeholder="Detalhes do produto"
                                        ${not empty eDescricao ? 'aria-invalid="true" aria-describedby="erro-descricao"' : ''}><c:out value="${produto.descricao}" /></textarea>
                                    <c:if test="${not empty eDescricao}">
                                        <div id="erro-descricao" class="invalid-feedback"><c:out value="${eDescricao}" /></div>
                                    </c:if>
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="categoriaId">Categoria</label>
                                    <select id="categoriaId" class="form-select ${not empty eCategoria ? 'is-invalid' : ''}"
                                        name="categoriaId" required
                                        ${not empty eCategoria ? 'aria-invalid="true" aria-describedby="erro-categoriaId"' : ''}>
                                        <option value="">Selecione uma categoria</option>
                                        <c:forEach var="categoria" items="${categorias}">
                                            <option value="<c:out value='${categoria.id}'/>" ${produto.categoria.id == categoria.id ? 'selected' : ''}><c:out value="${categoria.nome}" /></option>
                                        </c:forEach>
                                    </select>
                                    <c:if test="${not empty eCategoria}">
                                        <div id="erro-categoriaId" class="invalid-feedback"><c:out value="${eCategoria}" /></div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                        <div class="card-body border-top border-secondary">
                            <div class="titulo-secao d-flex align-items-center gap-2 mb-3">
                                <i class="bi bi-boxes" aria-hidden="true"></i> Estoque e preço
                            </div>
                            <div class="row g-3">
                                <div class="col-12 col-md-4">
                                    <label class="form-label" for="preco">Preço (R$)</label>
                                    <input id="preco" class="form-control ${not empty ePreco ? 'is-invalid' : ''}" type="number"
                                        step="0.01" min="0" max="9999999.99" name="preco" value="<c:out value='${produto.preco}'/>"
                                        required ${not empty ePreco ? 'aria-invalid="true" aria-describedby="erro-preco"' : ''}>
                                    <c:if test="${not empty ePreco}">
                                        <div id="erro-preco" class="invalid-feedback"><c:out value="${ePreco}" /></div>
                                    </c:if>
                                </div>
                                <div class="col-12 col-md-4">
                                    <c:choose>
                                        <c:when test="${edicao}">
                                            <%-- Edicao: saldo so muda por movimentacao (RN-03). Campo somente leitura. --%>
                                            <label class="form-label" for="quantidade">Quantidade em estoque</label>
                                            <input id="quantidade" class="form-control ${not empty eQuantidade ? 'is-invalid' : ''}"
                                                type="number" name="quantidade" value="<c:out value='${produto.quantidade}'/>"
                                                readonly aria-readonly="true" aria-describedby="dica-quantidade">
                                            <div id="dica-quantidade" class="form-text">
                                                Para alterar o saldo use
                                                <a href="${pageContext.request.contextPath}/estoque">Movimentar estoque</a>.
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <label class="form-label" for="quantidade">Estoque inicial</label>
                                            <input id="quantidade" class="form-control ${not empty eQuantidade ? 'is-invalid' : ''}"
                                                type="number" min="0" max="1000000" name="quantidade"
                                                value="<c:out value='${produto.quantidade}'/>" required
                                                aria-describedby="dica-quantidade${not empty eQuantidade ? ' erro-quantidade' : ''}"
                                                ${not empty eQuantidade ? 'aria-invalid="true"' : ''}>
                                            <div id="dica-quantidade" class="form-text">
                                                Saldo de partida; fica registrado como uma entrada.
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                    <c:if test="${not empty eQuantidade}">
                                        <div id="erro-quantidade" class="invalid-feedback"><c:out value="${eQuantidade}" /></div>
                                    </c:if>
                                </div>
                                <div class="col-12 col-md-4">
                                    <label class="form-label" for="estoqueMinimo">Estoque mínimo</label>
                                    <input id="estoqueMinimo" class="form-control ${not empty eMinimo ? 'is-invalid' : ''}"
                                        type="number" min="0" max="2000000000" name="estoqueMinimo"
                                        value="<c:out value='${produto.estoqueMinimo}'/>" required
                                        ${not empty eMinimo ? 'aria-invalid="true" aria-describedby="erro-estoqueMinimo"' : ''}>
                                    <c:if test="${not empty eMinimo}">
                                        <div id="erro-estoqueMinimo" class="invalid-feedback"><c:out value="${eMinimo}" /></div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </section>
                    <div class="acoes-formulario d-flex justify-content-end gap-2 p-3">
                        <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/produtos">Cancelar</a>
                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar produto
                        </button>
                    </div>
                </form>
            </main>
        </div>
    </div>
    <jsp:include page="../includes/scripts.jsp" />
</body>

</html>
