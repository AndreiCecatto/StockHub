<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Formulário de categoria no novo design (lote 3). Rotas: GET /categorias/novo, GET /categorias/{id}/editar,
    POST /categorias (campos id, nome, descricao: inalterados).
    Dados do CategoriaController: categoria (CategoriaForm), erro (regra de negócio, ex.: nome repetido, RN-10),
    errosCampos (nome, descricao). Erros por campo e o resumo seguem o mesmo desenho do formulário de produto.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="edicao" value="${categoria.id != null}" />
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eDescricao" value="${errosCampos['descricao']}" />
<c:set var="temErrosCampos" value="${not empty eNome or not empty eDescricao}" />

<sh:layout titulo="${edicao ? 'Editar categoria' : 'Nova categoria'}" ativo="categorias" largura="form" formulario="true"
    voltarUrl="/categorias" voltarRotulo="Categorias">
    <jsp:attribute name="migalhas">
        <li class="breadcrumb-item"><a href="${ctx}/dashboard">Início</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/categorias">Categorias</a></li>
        <li class="breadcrumb-item active" aria-current="page"><c:out value="${edicao ? 'Editar' : 'Nova categoria'}" /></li>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="${edicao ? 'Editar categoria' : 'Nova categoria'}"
            descricao="Um grupo para organizar produtos no catálogo." />

        <form method="post" action="${ctx}/categorias" data-sh-validar data-sh-avisar-alteracoes data-sh-enviando="Salvando…">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
            <input type="hidden" name="id" value="<c:out value='${categoria.id}'/>">

            <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para salvar a categoria:</c:if></p>
                <ul>
                    <c:if test="${not empty eNome}"><li><a href="#nome" data-sh-ir-campo="nome"><c:out value="${eNome}" /></a></li></c:if>
                    <c:if test="${not empty eDescricao}"><li><a href="#descricao" data-sh-ir-campo="descricao"><c:out value="${eDescricao}" /></a></li></c:if>
                </ul>
            </div>

            <section class="sh-card sh-secao" aria-labelledby="titulo-dados">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-dados">Dados da categoria</h2>
                </div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label" for="nome">Nome da categoria <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="nome" class="form-control ${not empty eNome ? 'is-invalid' : ''}" type="text" name="nome"
                                value="<c:out value='${categoria.nome}'/>" placeholder="Ex.: Bebidas" minlength="2" maxlength="120"
                                autocomplete="off" required
                                ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                            <c:if test="${not empty eNome}">
                                <div id="erro-nome" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eNome}" /></span>
                                </div>
                            </c:if>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="descricao">Descrição</label>
                            <textarea id="descricao" class="form-control ${not empty eDescricao ? 'is-invalid' : ''}" name="descricao"
                                rows="5" maxlength="500" placeholder="Resumo do tipo de produto desta categoria (opcional)"
                                data-sh-contador="contador-descricao"
                                ${not empty eDescricao ? 'aria-invalid="true"' : ''}
                                aria-describedby="contador-descricao${not empty eDescricao ? ' erro-descricao' : ''}"><c:out value="${categoria.descricao}" /></textarea>
                            <c:if test="${not empty eDescricao}">
                                <div id="erro-descricao" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eDescricao}" /></span>
                                </div>
                            </c:if>
                            <div id="contador-descricao" class="sh-contador">Até 500 caracteres</div>
                        </div>
                    </div>
                </div>
            </section>

            <div class="sh-form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/categorias">Cancelar</a>
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar categoria
                </button>
            </div>
        </form>
    </jsp:body>
</sh:layout>
