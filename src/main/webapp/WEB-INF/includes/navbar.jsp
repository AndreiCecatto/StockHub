<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <header class="barra-superior d-flex align-items-center justify-content-between gap-3">
        <div class="titulo-superior d-flex align-items-center gap-2">
            <i class="bi bi-shop-window"></i>
            <strong>Painel de estoque</strong>
            <span>/ StockHub</span>
        </div>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="usuario-pilula d-inline-flex align-items-center gap-2">
                <i class="bi bi-person-circle"></i>
                ${usuarioLogado.nome}
                <small>Minha loja</small>
            </span>
            <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/logout">
                <i class="bi bi-box-arrow-right"></i> Sair
            </a>
        </div>
    </header>