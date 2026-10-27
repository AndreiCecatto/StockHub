<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Página de erro v3 (view "error" do Spring Boot). O model traz status, error e path, mas só o status é usado:
    o texto é fixo por status, então nunca aparece stack trace, mensagem interna nem o caminho pedido.
    Funciona sem usuário logado e sem atributos do app: usa layoutPublico (sem menu) e não lê usuarioLogado.
    A raiz "/" é pública; "Ir para o painel" passa pelo login se não houver sessão.
    Rota /error liberada no SecurityConfig. Sem script nem style inline.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:choose>
    <c:when test="${status == 404}">
        <c:set var="tituloErro" value="Página não encontrada" />
        <c:set var="textoErro" value="O endereço não existe ou o item foi removido. Confira o endereço ou volte ao início." />
    </c:when>
    <c:when test="${status == 403}">
        <c:set var="tituloErro" value="Acesso negado" />
        <c:set var="textoErro" value="Você não tem permissão para abrir esta página. Se a sessão expirou, entre de novo." />
    </c:when>
    <c:when test="${status == 400}">
        <c:set var="tituloErro" value="Pedido inválido" />
        <c:set var="textoErro" value="Não foi possível entender o pedido. Volte e tente de novo." />
    </c:when>
    <c:when test="${status == 405}">
        <c:set var="tituloErro" value="Ação não permitida" />
        <c:set var="textoErro" value="Esta página não aceita esse tipo de ação. Volte ao início e tente de novo." />
    </c:when>
    <c:otherwise>
        <c:set var="tituloErro" value="Erro inesperado" />
        <c:set var="textoErro" value="Algo falhou do nosso lado. Tente de novo em instantes; se continuar, avise o suporte." />
    </c:otherwise>
</c:choose>

<sh:layoutPublico titulo="${tituloErro}">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/v3-erro.css">
    </jsp:attribute>
    <jsp:body>
        <div class="v3-erro">
            <div class="v3-erro__topo">
                <a class="v3-erro__marca" href="${ctx}/">
                    <sh:marca tamanho="28" />
                    <span>StockHub</span>
                </a>
                <jsp:include page="/WEB-INF/includes/sh/tema-botao.jsp" />
            </div>
            <section class="v3-erro__corpo" aria-labelledby="titulo-erro">
                <svg class="v3-erro__traco" viewBox="0 0 320 64" aria-hidden="true" focusable="false">
                    <path class="v3-erro__grade" d="M0 56 H320 M0 32 H320 M0 8 H320" />
                    <path class="v3-erro__nivel il-desenha" pathLength="1" d="M0 20 L60 26 L120 38 L170 46 L170 12 L230 20 L262 30" />
                    <circle class="v3-erro__ponto" cx="262" cy="30" r="3.5" />
                </svg>
                <p class="sh-eyebrow">Erro <span class="sh-mono"><c:out value="${empty status ? '' : status}" /></span></p>
                <h1 class="v3-erro__titulo" id="titulo-erro"><c:out value="${tituloErro}" /></h1>
                <p class="v3-erro__texto"><c:out value="${textoErro}" /></p>
                <div class="v3-erro__acoes">
                    <a class="btn btn-primary" href="${ctx}/dashboard">
                        <i class="bi bi-house-door" aria-hidden="true"></i> Ir para o painel
                    </a>
                    <a class="btn btn-outline-secondary" href="${ctx}/">Página inicial</a>
                    <c:if test="${status == 403}">
                        <a class="btn btn-ghost" href="${ctx}/login">Entrar</a>
                    </c:if>
                </div>
            </section>
        </div>
    </jsp:body>
</sh:layoutPublico>
