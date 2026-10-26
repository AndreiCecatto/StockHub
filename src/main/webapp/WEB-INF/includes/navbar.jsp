<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <header class="barra-superior d-flex align-items-center justify-content-between gap-3">
        <div class="titulo-superior d-flex align-items-center gap-2">
            <%-- Botao do menu: so aparece em telas pequenas --%>
            <button type="button" class="btn btn-outline-secondary btn-sm d-lg-none" data-bs-toggle="offcanvas"
                data-bs-target="#menuMobile" aria-controls="menuMobile" aria-label="Abrir menu">
                <i class="bi bi-list" aria-hidden="true"></i>
            </button>
            <i class="bi bi-shop-window" aria-hidden="true"></i>
            <strong>Painel de estoque</strong>
            <span>/ StockHub</span>
        </div>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="usuario-pilula d-inline-flex align-items-center gap-2">
                <i class="bi bi-person-circle" aria-hidden="true"></i>
                <c:out value="${usuarioLogado.nome}" />
                <small>Minha loja</small>
            </span>
            <%-- Logout agora e POST com token CSRF (Spring Security) --%>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <button type="submit" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Sair
                </button>
            </form>
        </div>
    </header>
