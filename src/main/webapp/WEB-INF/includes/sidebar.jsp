<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <aside class="barra-lateral">
            <div class="marca-lateral d-flex align-items-center gap-2">
                <span class="logo-marca"><i class="bi bi-box-seam"></i></span>
                <div>
                    <strong>StockHub</strong>
                    <small>Controle de estoque</small>
                </div>
            </div>
            <div class="rotulo-menu">Principal</div>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'dashboard' ? 'active' : ''}"
                href="${pageContext.request.contextPath}/dashboard">
                <i class="bi bi-speedometer2"></i> Visão geral
            </a>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'produtos' ? 'active' : ''}"
                href="${pageContext.request.contextPath}/produtos">
                <i class="bi bi-grid-3x3-gap"></i> Produtos
            </a>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'categorias' ? 'active' : ''}"
                href="${pageContext.request.contextPath}/categorias">
                <i class="bi bi-tags"></i> Categorias
            </a>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'estoqueBaixo' ? 'active' : ''}"
                href="${pageContext.request.contextPath}/estoque-baixo">
                <i class="bi bi-exclamation-triangle"></i> Estoque baixo
            </a>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none ${activePage == 'estoque' ? 'active' : ''}"
                href="${pageContext.request.contextPath}/estoque">
                <i class="bi bi-arrow-left-right"></i> Movimentações
            </a>
            <div class="rotulo-menu">Cadastro</div>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none"
                href="${pageContext.request.contextPath}/produtos/novo">
                <i class="bi bi-plus-square"></i> Produto
            </a>
            <a class="link-menu d-flex align-items-center gap-2 text-decoration-none"
                href="${pageContext.request.contextPath}/categorias/novo">
                <i class="bi bi-folder-plus"></i> Categoria
            </a>
        </aside>