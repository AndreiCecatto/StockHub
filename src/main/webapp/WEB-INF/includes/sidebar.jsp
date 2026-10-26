<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Telas grandes: barra lateral fixa --%>
<aside class="barra-lateral d-none d-lg-block">
    <div class="marca-lateral d-flex align-items-center gap-2">
        <span class="logo-marca"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
        <div>
            <strong>StockHub</strong>
            <small>Controle de estoque</small>
        </div>
    </div>
    <jsp:include page="menu.jsp" />
</aside>

<%-- Celular/tablet: o mesmo menu dentro de um painel deslizante (Bootstrap offcanvas) --%>
<div class="offcanvas offcanvas-start menu-offcanvas d-lg-none" tabindex="-1" id="menuMobile"
    aria-labelledby="menuMobileTitulo">
    <div class="offcanvas-header">
        <div class="d-flex align-items-center gap-2" id="menuMobileTitulo">
            <span class="logo-marca"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
            <strong>StockHub</strong>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Fechar menu"></button>
    </div>
    <div class="offcanvas-body pt-0">
        <jsp:include page="menu.jsp" />
    </div>
</div>
