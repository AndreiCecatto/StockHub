<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Links do menu lateral. Usado na barra fixa (telas grandes) e no offcanvas (celular).
    O destaque da pagina atual vem de "activePage" (escopo request, definido em cada JSP).
--%>
<nav aria-label="Menu principal">
    <div class="rotulo-menu">Principal</div>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'dashboard' ? 'active' : ''}"
        href="${pageContext.request.contextPath}/dashboard" ${activePage == 'dashboard' ? 'aria-current="page"' : ''}>
        <i class="bi bi-speedometer2" aria-hidden="true"></i> Visão geral
    </a>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'produtos' ? 'active' : ''}"
        href="${pageContext.request.contextPath}/produtos" ${activePage == 'produtos' ? 'aria-current="page"' : ''}>
        <i class="bi bi-grid-3x3-gap" aria-hidden="true"></i> Produtos
    </a>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'categorias' ? 'active' : ''}"
        href="${pageContext.request.contextPath}/categorias" ${activePage == 'categorias' ? 'aria-current="page"' : ''}>
        <i class="bi bi-tags" aria-hidden="true"></i> Categorias
    </a>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'estoqueBaixo' ? 'active' : ''}"
        href="${pageContext.request.contextPath}/estoque-baixo" ${activePage == 'estoqueBaixo' ? 'aria-current="page"' : ''}>
        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i> Estoque baixo
    </a>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'estoque' ? 'active' : ''}"
        href="${pageContext.request.contextPath}/estoque" ${activePage == 'estoque' ? 'aria-current="page"' : ''}>
        <i class="bi bi-arrow-left-right" aria-hidden="true"></i> Movimentações
    </a>
    <div class="rotulo-menu">Cadastro</div>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none"
        href="${pageContext.request.contextPath}/produtos/novo">
        <i class="bi bi-plus-square" aria-hidden="true"></i> Produto
    </a>
    <a class="link-menu d-flex align-items-center gap-2 text-decoration-none"
        href="${pageContext.request.contextPath}/categorias/novo">
        <i class="bi bi-folder-plus" aria-hidden="true"></i> Categoria
    </a>
</nav>
