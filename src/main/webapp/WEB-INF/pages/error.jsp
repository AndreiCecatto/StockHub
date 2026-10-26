<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Pagina de erro (view "error" do Spring Boot). O model traz status, error e path.
    Funciona sem usuario logado: nao usa sidebar/navbar nem usuarioLogado.
    Nunca mostra stack trace nem mensagem interna da excecao.
--%>
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
        <c:set var="textoErro" value="Esta página não aceita esse tipo de ação. Volte ao painel e tente novamente." />
    </c:when>
    <c:otherwise>
        <c:set var="tituloErro" value="Algo deu errado" />
        <c:set var="textoErro" value="Tivemos um problema inesperado. Tente novamente em instantes; se continuar, avise o suporte." />
    </c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title><c:out value="${tituloErro}" /> - StockHub</title>
</head>

<body class="pagina-login min-vh-100 d-flex align-items-center justify-content-center p-4">
    <main class="cartao-login cartao-erro card p-4">
        <span class="logo-marca mx-auto"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
        <p class="codigo-erro mt-3" aria-hidden="true"><c:out value="${empty status ? 'Erro' : status}" /></p>
        <h1 class="mt-2 mb-2"><c:out value="${tituloErro}" /></h1>
        <p><c:out value="${textoErro}" /></p>
        <c:if test="${not empty path}">
            <p class="small mb-3">Endereço: <code><c:out value="${path}" /></code></p>
        </c:if>
        <div class="d-grid gap-2">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">
                <i class="bi bi-house" aria-hidden="true"></i> Voltar ao painel
            </a>
        </div>
    </main>
</body>

</html>
