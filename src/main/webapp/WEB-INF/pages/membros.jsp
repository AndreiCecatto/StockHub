<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Equipe da loja (RN-61 a RN-65). Rotas: GET /membros, POST /membros (nome, email, senha, papel),
    POST /membros/{id}/desativar e /reativar. Só dono e gerente chegam aqui (SecurityConfig).
    Dados do MembroController: membros (MembroLoja: id, nome, email, papel, ativo), membroAtualId,
    papeisEscolhiveis; flash: mensagem, erro, nomeDigitado, emailDigitado, papelDigitado (a senha nunca volta).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Equipe" ativo="membros" largura="form">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/membros.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Equipe" descricao="As pessoas que usam a loja. Cada uma entra com o próprio e-mail e senha." />

        <section class="sh-card sh-secao sh-lista" aria-labelledby="titulo-equipe">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-equipe">Pessoas da loja</h2>
            </div>
            <div class="sh-table-wrap">
                <table class="sh-table sh-table--cards" role="table">
                    <caption class="visually-hidden">Pessoas da loja, <c:out value="${fn:length(membros)}" /> no total</caption>
                    <thead role="rowgroup">
                        <tr role="row">
                            <th scope="col" role="columnheader">Pessoa</th>
                            <th scope="col" role="columnheader">Papel</th>
                            <th scope="col" role="columnheader">Situação</th>
                            <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                        </tr>
                    </thead>
                    <tbody role="rowgroup">
                        <c:forEach var="m" items="${membros}">
                            <tr role="row">
                                <td role="cell">
                                    <span class="mb-nome"><c:out value="${m.nome}" /><c:if test="${m.id == membroAtualId}"> <span class="mb-voce">(você)</span></c:if></span>
                                    <span class="mb-email"><c:out value="${m.email}" /></span>
                                </td>
                                <td role="cell" data-label="Papel"><c:out value="${m.papel.rotulo}" /></td>
                                <td role="cell" data-label="Situação">
                                    <c:choose>
                                        <c:when test="${m.ativo}"><span class="sh-badge sh-badge--normal">Ativa</span></c:when>
                                        <c:otherwise><span class="sh-badge sh-badge--sem">Desativada</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td role="cell" data-label="" class="mb-acoes">
                                    <c:if test="${m.papel != 'DONO' and m.id != membroAtualId and (m.papel != 'GERENTE' or papelLogado == 'DONO')}">
                                        <c:choose>
                                            <c:when test="${m.ativo}">
                                                <form method="post" action="<c:out value='${ctx}/membros/${m.id}/desativar'/>" class="m-0">
                                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                    <button type="submit" class="btn btn-outline-secondary btn-sm">
                                                        Desativar<span class="visually-hidden"> <c:out value="${m.nome}" /></span>
                                                    </button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post" action="<c:out value='${ctx}/membros/${m.id}/reativar'/>" class="m-0">
                                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                    <button type="submit" class="btn btn-outline-secondary btn-sm">
                                                        Ativar de novo<span class="visually-hidden"> <c:out value="${m.nome}" /></span>
                                                    </button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <p class="mb-nota">Quem é desativado deixa de entrar na hora, mas o histórico dele continua guardado. Ninguém é excluído.</p>
        </section>

        <section class="sh-card sh-secao" aria-labelledby="titulo-nova">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-nova">Adicionar pessoa</h2>
            </div>
            <div class="sh-card__corpo">
                <form method="post" action="${ctx}/membros" data-sh-enviando="Adicionando…">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="nome">Nome</label>
                            <input id="nome" class="form-control" type="text" name="nome" maxlength="255" required
                                autocomplete="off" value="<c:out value='${nomeDigitado}'/>" placeholder="Ex.: Ana Souza">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="email">E-mail</label>
                            <input id="email" class="form-control" type="email" name="email" maxlength="255" required
                                autocomplete="off" value="<c:out value='${emailDigitado}'/>" placeholder="ana@loja.com.br">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="senha">Senha inicial</label>
                            <input id="senha" class="form-control" type="password" name="senha" minlength="8" maxlength="72"
                                required autocomplete="new-password" aria-describedby="ajuda-senha">
                            <div id="ajuda-senha" class="form-text">Pelo menos 8 caracteres. Combine com a pessoa e peça para ela guardar.</div>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="papel">Papel</label>
                            <select id="papel" class="form-select" name="papel" required>
                                <c:forEach var="p" items="${papeisEscolhiveis}">
                                    <option value="${p.name()}" ${papelDigitado == p.name() ? 'selected' : ''}><c:out value="${p.rotulo}" /></option>
                                </c:forEach>
                            </select>
                            <div class="form-text">Gerente pode tudo, menos mexer em outro gerente. Os demais consultam e movimentam o estoque, sem excluir.</div>
                        </div>
                    </div>
                    <div class="sh-form-actions mb-acoes-form">
                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-person-plus" aria-hidden="true"></i> Adicionar pessoa
                        </button>
                    </div>
                </form>
            </div>
        </section>
    </jsp:body>
</sh:layout>
