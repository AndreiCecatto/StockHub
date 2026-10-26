<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Página de erro (view "error" do Spring Boot, novo design, lote 5). O model traz status, error e path, mas só o
    status é usado: o texto é fixo por status, então nunca aparece stack trace, mensagem interna nem o caminho pedido.
    Funciona sem usuário logado e sem atributos do app: usa layoutPublico (sem menu) e não lê usuarioLogado.
    Rota /error liberada no SecurityConfig. Sem script nem style inline.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:choose>
    <c:when test="${status == 404}">
        <c:set var="tituloErro" value="Página não encontrada" />
        <c:set var="textoErro" value="O endereço que você abriu não existe ou o item foi removido." />
    </c:when>
    <c:when test="${status == 403}">
        <c:set var="tituloErro" value="Acesso negado" />
        <c:set var="textoErro" value="Você não tem permissão para abrir esta página." />
    </c:when>
    <c:when test="${status == 400}">
        <c:set var="tituloErro" value="Pedido inválido" />
        <c:set var="textoErro" value="Não foi possível entender o pedido. Volte e tente novamente." />
    </c:when>
    <c:when test="${status == 405}">
        <c:set var="tituloErro" value="Ação não permitida" />
        <c:set var="textoErro" value="Esta página não aceita esse tipo de ação. Volte ao início e tente novamente." />
    </c:when>
    <c:otherwise>
        <c:set var="tituloErro" value="Algo deu errado" />
        <c:set var="textoErro" value="Tivemos um problema inesperado. Tente novamente em instantes; se continuar, avise o suporte." />
    </c:otherwise>
</c:choose>

<sh:layoutPublico titulo="${tituloErro}">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/relatorios.css">
    </jsp:attribute>
    <jsp:body>
        <div class="sh-erro">
            <div class="sh-erro__tema">
                <jsp:include page="/WEB-INF/includes/sh/tema-botao.jsp" />
            </div>
            <section class="sh-card sh-erro__caixa" aria-labelledby="titulo-erro">
                <a class="sh-erro__marca" href="${ctx}/dashboard">
                    <span class="sh-marca__logo"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
                    <span>StockHub</span>
                </a>
                <p class="sh-erro__codigo" aria-hidden="true"><c:out value="${empty status ? 'Erro' : status}" /></p>
                <h1 class="sh-erro__titulo" id="titulo-erro"><c:out value="${tituloErro}" /></h1>
                <p class="sh-erro__texto"><c:out value="${textoErro}" /></p>
                <div class="sh-erro__acoes">
                    <a class="btn btn-primary" href="${ctx}/dashboard">
                        <i class="bi bi-house" aria-hidden="true"></i> Voltar ao início
                    </a>
                    <c:if test="${status == 403}">
                        <a class="btn btn-outline-secondary" href="${ctx}/login">
                            <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i> Entrar
                        </a>
                    </c:if>
                </div>
            </section>
        </div>
    </jsp:body>
</sh:layoutPublico>
