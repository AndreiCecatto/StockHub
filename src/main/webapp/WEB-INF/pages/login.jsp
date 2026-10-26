<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <!DOCTYPE html>
        <html lang="pt-BR">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
            <title>StockHub - Login</title>
        </head>

        <body class="pagina-login min-vh-100 d-flex align-items-center justify-content-center p-4">
            <main class="cartao-login card p-4">
                <span class="logo-marca"><i class="bi bi-box-seam"></i></span>
                <h1 class="mt-3 mb-1">StockHub</h1>
                <p>${modoCadastro ? 'Crie o estoque da sua loja.' : 'Acesse o painel da sua loja.'}</p>
                <%-- Alertas acima do formulario --%>
                <c:if test="${not empty sucesso}">
                    <div class="alert alert-success mt-3 mb-0" role="status"><c:out value="${sucesso}" /></div>
                </c:if>
                <c:if test="${not empty erro}">
                    <div class="alert alert-danger mt-3 mb-0" role="alert"><c:out value="${erro}" /></div>
                </c:if>
                <form action="${pageContext.request.contextPath}${modoCadastro ? '/cadastro' : '/login'}" method="post"
                    class="mt-3">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <c:if test="${modoCadastro}">
                        <div class="mb-3">
                            <label class="form-label" for="nome">Nome da loja</label>
                            <input id="nome" type="text" class="form-control" name="nome"
                                value="<c:out value='${nome}'/>" placeholder="Minha loja" autocomplete="organization"
                                required>
                        </div>
                    </c:if>
                    <div class="mb-3">
                        <label class="form-label" for="email">E-mail</label>
                        <input id="email" type="email" class="form-control" name="email"
                            value="<c:out value='${email}'/>" placeholder="voce@email.com" autocomplete="username"
                            required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="senha">Senha</label>
                        <c:choose>
                            <c:when test="${modoCadastro}">
                                <input id="senha" type="password" class="form-control" name="senha"
                                    placeholder="Sua senha" minlength="8" autocomplete="new-password"
                                    aria-describedby="dica-senha" required>
                                <div id="dica-senha" class="form-text">Mínimo de 8 caracteres.</div>
                            </c:when>
                            <c:otherwise>
                                <input id="senha" type="password" class="form-control" name="senha"
                                    placeholder="Sua senha" autocomplete="current-password" required>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">
                        <i class="bi ${modoCadastro ? 'bi-shop' : 'bi-door-open'}" aria-hidden="true"></i>
                        <c:out value="${modoCadastro ? 'Criar minha loja' : 'Entrar'}" />
                    </button>
                </form>
                <div class="texto-login-secundario mt-3 text-center">
                    <c:choose>
                        <c:when test="${modoCadastro}">
                            <a href="${pageContext.request.contextPath}/login">Já tenho uma loja</a>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/cadastro">Criar minha loja</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </main>
        </body>

        </html>