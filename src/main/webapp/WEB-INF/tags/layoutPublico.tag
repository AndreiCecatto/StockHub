<%@ tag description="Layout das telas sem login (entrar, criar loja): sem barra lateral, com tema e toasts" body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Uso: <sh:layoutPublico titulo="Entrar"> ...conteúdo... </sh:layoutPublico>
    O conteúdo ocupa a tela inteira (ver .sh-auth em css/layout.css). Não há menu nem atalhos de teclado.
    Quem usa fornece o seu próprio botão de tema (includes/sh/tema-botao.jsp).
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="shTitulo" value="${titulo}" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <jsp:include page="/WEB-INF/includes/sh/head.jsp" />
</head>
<body data-sh-ctx="<c:out value='${ctx}'/>" data-sh-publico>
    <a class="sh-pular" href="#conteudo">Pular para o conteúdo</a>
    <main id="conteudo" tabindex="-1">
        <jsp:doBody />
    </main>
    <div id="sh-toasts" class="sh-toasts" aria-live="polite" aria-relevant="additions"></div>
    <div id="sh-anuncio" class="visually-hidden" role="status"></div>
</body>
</html>
