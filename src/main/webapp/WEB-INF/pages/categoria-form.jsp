<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activePage" value="categorias" scope="request" />
<c:set var="edicao" value="${categoria.id != null}" />
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eDescricao" value="${errosCampos['descricao']}" />
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>${edicao ? 'Editar categoria' : 'Nova categoria'} - StockHub</title>
</head>

<body>
    <div class="estrutura-sistema d-flex min-vh-100">
        <jsp:include page="../includes/sidebar.jsp" />
        <div class="conteudo-sistema flex-grow-1">
            <jsp:include page="../includes/navbar.jsp" />
            <main class="conteudo-pagina">
                <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                    <div class="titulo-pagina">
                        <h1>${edicao ? 'Editar categoria' : 'Cadastrar categoria'}</h1>
                        <p>Grupo usado para organizar produtos no catálogo.</p>
                    </div>
                    <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/categorias">
                        <i class="bi bi-arrow-left" aria-hidden="true"></i> Voltar
                    </a>
                </section>

                <c:if test="${not empty erro}">
                    <div class="alert alert-danger" role="alert"><c:out value="${erro}" /></div>
                </c:if>

                <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/categorias">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <input type="hidden" name="id" value="<c:out value='${categoria.id}'/>">
                    <section class="painel card">
                        <div class="card-body">
                            <div class="titulo-secao d-flex align-items-center gap-2 mb-3">
                                <i class="bi bi-tag" aria-hidden="true"></i> Dados da categoria
                            </div>
                            <div class="row g-3">
                                <div class="col-12">
                                    <label class="form-label" for="nome">Nome da categoria</label>
                                    <input id="nome" class="form-control ${not empty eNome ? 'is-invalid' : ''}" type="text"
                                        name="nome" value="<c:out value='${categoria.nome}'/>" placeholder="Ex: Eletrônicos"
                                        maxlength="120" required
                                        ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                                    <c:if test="${not empty eNome}">
                                        <div id="erro-nome" class="invalid-feedback"><c:out value="${eNome}" /></div>
                                    </c:if>
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="descricao">Descrição</label>
                                    <textarea id="descricao" class="form-control ${not empty eDescricao ? 'is-invalid' : ''}"
                                        name="descricao" rows="6" maxlength="500"
                                        placeholder="Resumo do tipo de produto desta categoria"
                                        ${not empty eDescricao ? 'aria-invalid="true" aria-describedby="erro-descricao"' : ''}><c:out value="${categoria.descricao}" /></textarea>
                                    <c:if test="${not empty eDescricao}">
                                        <div id="erro-descricao" class="invalid-feedback"><c:out value="${eDescricao}" /></div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </section>
                    <div class="acoes-formulario d-flex justify-content-end gap-2 p-3">
                        <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/categorias">Cancelar</a>
                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar categoria
                        </button>
                    </div>
                </form>
            </main>
        </div>
    </div>
    <jsp:include page="../includes/scripts.jsp" />
</body>

</html>
