<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Entrar e criar loja (novo design, lote 2). Rotas e campos inalterados:
      GET /login, GET /cadastro, POST /login (email, senha: tratado pelo Spring Security),
      POST /cadastro (nome, email, senha).
    Dados do model: erro, sucesso, modoCadastro, errosCampos (nome, email, senha, senhaDentroDoLimite), nome, email.
    A senha nunca volta preenchida (o input de senha não tem atributo value).
    Erro de login: uma mensagem única e genérica (não revela se o e-mail existe), em role="alert".
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Erros por campo (só existem no cadastro). A regra dos 72 bytes chega com o nome "senhaDentroDoLimite". --%>
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eEmail" value="${errosCampos['email']}" />
<c:set var="eSenha" value="${not empty errosCampos['senha'] ? errosCampos['senha'] : errosCampos['senhaDentroDoLimite']}" />
<c:set var="temErrosCampos" value="${not empty eNome or not empty eEmail or not empty eSenha}" />
<c:set var="rotuloEnviar" value="${modoCadastro ? 'Criar minha loja' : 'Entrar'}" />

<sh:layoutPublico titulo="${modoCadastro ? 'Criar loja' : 'Entrar'}">
    <div class="sh-auth">
        <%-- Painel de marca: só em telas largas (no celular fica só o formulário) --%>
        <section class="sh-auth__marca" aria-label="Sobre o StockHub">
            <a class="sh-marca" href="${ctx}/login">
                <span class="sh-marca__logo"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
                <span class="sh-marca__nome">StockHub</span>
            </a>
            <div>
                <p class="sh-auth__frase">Saiba o que tem, o que falta e o que saiu.</p>
                <ul class="sh-auth__beneficios">
                    <li><i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                        <span>Veja o que está acabando antes de faltar.</span></li>
                    <li><i class="bi bi-arrow-left-right" aria-hidden="true"></i>
                        <span>Registre entradas, saídas e ajustes em segundos.</span></li>
                    <li><i class="bi bi-clock-history" aria-hidden="true"></i>
                        <span>Todo o histórico guardado: o que mudou e quando.</span></li>
                </ul>
            </div>
            <p class="sh-auth__rodape-marca">Controle de estoque para lojas pequenas.</p>
        </section>

        <div class="sh-auth__lado">
            <div class="sh-auth__tema">
                <jsp:include page="/WEB-INF/includes/sh/tema-botao.jsp" />
            </div>

            <div class="sh-auth__caixa">
                <div class="sh-auth__logo-mobile">
                    <span class="sh-marca__logo"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
                    <span>StockHub</span>
                </div>

                <h1 class="sh-auth__titulo"><c:choose>
                    <c:when test="${modoCadastro}">Criar sua loja</c:when>
                    <c:otherwise>Entrar</c:otherwise>
                </c:choose></h1>
                <p class="sh-auth__sub"><c:choose>
                    <c:when test="${modoCadastro}">Crie o estoque da sua loja em poucos segundos.</c:when>
                    <c:otherwise>Acesse o painel da sua loja.</c:otherwise>
                </c:choose></p>

                <c:if test="${not empty sucesso}">
                    <div class="sh-alert sh-alert--success" role="status">
                        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                        <div class="sh-alert__corpo"><c:out value="${sucesso}" /></div>
                    </div>
                </c:if>
                <c:if test="${not empty erro and not temErrosCampos}">
                    <div class="sh-alert sh-alert--danger" role="alert" tabindex="-1">
                        <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
                        <div class="sh-alert__corpo"><c:out value="${erro}" /></div>
                    </div>
                </c:if>

                <form action="${ctx}${modoCadastro ? '/cadastro' : '/login'}" method="post" data-sh-validar
                    data-sh-enviando="${modoCadastro ? 'Criando…' : 'Entrando…'}">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

                    <%-- Resumo de erros: o servidor preenche quando há erro por campo; o JS preenche ao enviar --%>
                    <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                        <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para continuar:</c:if></p>
                        <ul>
                            <c:if test="${not empty eNome}">
                                <li><a href="#nome" data-sh-ir-campo="nome"><c:out value="${eNome}" /></a></li>
                            </c:if>
                            <c:if test="${not empty eEmail}">
                                <li><a href="#email" data-sh-ir-campo="email"><c:out value="${eEmail}" /></a></li>
                            </c:if>
                            <c:if test="${not empty eSenha}">
                                <li><a href="#senha" data-sh-ir-campo="senha"><c:out value="${eSenha}" /></a></li>
                            </c:if>
                        </ul>
                    </div>

                    <c:if test="${modoCadastro}">
                        <div class="mb-3">
                            <label class="form-label" for="nome">Nome da loja <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="nome" type="text" class="form-control ${not empty eNome ? 'is-invalid' : ''}" name="nome"
                                value="<c:out value='${nome}'/>" autocomplete="organization" maxlength="255" required
                                ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                            <c:if test="${not empty eNome}">
                                <div id="erro-nome" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eNome}" /></span>
                                </div>
                            </c:if>
                        </div>
                    </c:if>

                    <div class="mb-3">
                        <label class="form-label" for="email">E-mail <span class="sh-obrigatorio">(obrigatório)</span></label>
                        <input id="email" type="email" class="form-control ${not empty eEmail ? 'is-invalid' : ''}" name="email"
                            value="<c:out value='${email}'/>" placeholder="voce@email.com" autocomplete="username"
                            inputmode="email" autocapitalize="none" spellcheck="false" required
                            ${not empty eEmail ? 'aria-invalid="true" aria-describedby="erro-email"' : ''}>
                        <c:if test="${not empty eEmail}">
                            <div id="erro-email" class="sh-erro-campo">
                                <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                <span class="sh-erro-campo__texto"><c:out value="${eEmail}" /></span>
                            </div>
                        </c:if>
                    </div>

                    <div class="mb-3">
                        <label class="form-label" for="senha">Senha <span class="sh-obrigatorio">(obrigatório)</span></label>
                        <div class="sh-senha">
                            <c:choose>
                                <c:when test="${modoCadastro}">
                                    <%-- A senha nunca volta preenchida depois de um erro (sem atributo value) --%>
                                    <input id="senha" type="password" class="form-control ${not empty eSenha ? 'is-invalid' : ''}" name="senha"
                                        minlength="8" autocomplete="new-password"
                                        aria-describedby="regras-senha${not empty eSenha ? ' erro-senha' : ''}"
                                        ${not empty eSenha ? 'aria-invalid="true"' : ''} required>
                                </c:when>
                                <c:otherwise>
                                    <input id="senha" type="password" class="form-control" name="senha"
                                        autocomplete="current-password" required>
                                </c:otherwise>
                            </c:choose>
                            <button type="button" class="sh-senha__alternar" aria-controls="senha" aria-pressed="false"
                                aria-label="Mostrar senha">
                                <i class="bi bi-eye" aria-hidden="true"></i>
                            </button>
                        </div>
                        <c:if test="${not empty eSenha}">
                            <div id="erro-senha" class="sh-erro-campo">
                                <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                <span class="sh-erro-campo__texto"><c:out value="${eSenha}" /></span>
                            </div>
                        </c:if>
                        <c:if test="${modoCadastro}">
                            <ul class="sh-regras" id="regras-senha" data-sh-regras-senha data-campo="senha">
                                <li data-regra="minimo">
                                    <i class="bi bi-circle sh-regras__pendente" aria-hidden="true"></i>
                                    <i class="bi bi-check-circle-fill sh-regras__ok" aria-hidden="true"></i>
                                    <span>Pelo menos 8 caracteres</span>
                                    <span class="visually-hidden sh-regras__estado">(pendente)</span>
                                </li>
                                <li data-regra="maximo">
                                    <i class="bi bi-circle sh-regras__pendente" aria-hidden="true"></i>
                                    <i class="bi bi-check-circle-fill sh-regras__ok" aria-hidden="true"></i>
                                    <span>No máximo 72 bytes (acentos e símbolos contam mais de um)</span>
                                    <span class="visually-hidden sh-regras__estado">(pendente)</span>
                                </li>
                            </ul>
                        </c:if>
                    </div>

                    <button type="submit" class="btn btn-primary btn-lg w-100 mt-2">
                        <i class="bi ${modoCadastro ? 'bi-shop' : 'bi-door-open'}" aria-hidden="true"></i>
                        <span><c:out value="${rotuloEnviar}" /></span>
                    </button>
                </form>

                <p class="sh-auth__troca">
                    <c:choose>
                        <c:when test="${modoCadastro}">
                            Já tem uma loja? <a href="${ctx}/login">Entrar</a>
                        </c:when>
                        <c:otherwise>
                            Ainda não tem uma loja? <a href="${ctx}/cadastro">Criar minha loja</a>
                        </c:otherwise>
                    </c:choose>
                </p>
            </div>
        </div>
    </div>
</sh:layoutPublico>
