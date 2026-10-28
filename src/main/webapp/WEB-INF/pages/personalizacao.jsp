<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Personalizar a loja (AD-19 e AD-20, RN-109 a RN-120). Rotas: GET /conta/personalizacao, POST /conta/personalizacao/inicio
    e POST /conta/personalizacao/produtos (campo "blocos", um valor por caixa marcada; um formulário por seção).
    Só dono e gerente chegam aqui (SecurityConfig).
    Dados do PersonalizacaoController: blocosInicio e blocosProdutos (BlocoTela: name, rotulo, descricao, moduloExigido; só os
    aplicáveis à loja), estadoBlocos e estadoColunas (Map código -> visível); flash: mensagem, erro (saem pelo layout).
    Sem CSS próprio: usa só classes já existentes (sh-*, db-*, form-check, btn).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Personalizar a loja" ativo="dashboard" largura="form">
    <jsp:body>
        <sh:cabecalhoPagina titulo="Personalizar a loja" descricao="Escolha o que aparece no início e na lista de produtos. Cada seção tem o seu botão Salvar." />

        <form method="post" action="${ctx}/conta/personalizacao/inicio" data-sh-bloquear data-sh-enviando="Salvando…">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

            <section class="sh-card sh-secao" aria-labelledby="titulo-blocos">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-blocos">Blocos do início</h2>
                </div>
                <div class="sh-card__corpo">
                    <p class="db-sub">O que não estiver marcado fica oculto do início, para todas as pessoas da loja. A frase do dia e os primeiros passos sempre aparecem.</p>
                    <c:forEach var="b" items="${blocosInicio}">
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" name="blocos" id="bloco-<c:out value='${b.name()}'/>"
                                value="<c:out value='${b.name()}'/>" aria-describedby="desc-<c:out value='${b.name()}'/>"
                                ${estadoBlocos[b.name()] ? 'checked' : ''}>
                            <label class="form-check-label fw-semibold" for="bloco-<c:out value='${b.name()}'/>"><c:out value="${b.rotulo}" /></label>
                            <div class="db-sub" id="desc-<c:out value='${b.name()}'/>"><c:out value="${b.descricao}" /></div>
                        </div>
                    </c:forEach>
                </div>
            </section>

            <div class="sh-form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/dashboard">Voltar ao início</a>
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar o início
                </button>
            </div>
        </form>

        <%-- Segunda seção: formulário próprio (cada tela salva sozinha) --%>
        <form method="post" action="${ctx}/conta/personalizacao/produtos" data-sh-bloquear data-sh-enviando="Salvando…">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

            <section class="sh-card sh-secao" aria-labelledby="titulo-produtos">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-produtos">Lista de produtos</h2>
                </div>
                <div class="sh-card__corpo">
                    <p class="db-sub">Escolha quais colunas aparecem na lista de produtos, para todas as pessoas da loja. Nome, saldo e ações sempre aparecem.</p>
                    <c:forEach var="b" items="${blocosProdutos}">
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" name="blocos" id="col-<c:out value='${b.name()}'/>"
                                value="<c:out value='${b.name()}'/>" aria-describedby="desc-col-<c:out value='${b.name()}'/>"
                                ${estadoColunas[b.name()] ? 'checked' : ''}>
                            <label class="form-check-label fw-semibold" for="col-<c:out value='${b.name()}'/>"><c:out value="${b.rotulo}" /></label>
                            <div class="db-sub" id="desc-col-<c:out value='${b.name()}'/>"><c:out value="${b.descricao}" /></div>
                        </div>
                    </c:forEach>
                </div>
            </section>

            <div class="sh-form-actions">
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar a lista de produtos
                </button>
            </div>
        </form>
    </jsp:body>
</sh:layout>
