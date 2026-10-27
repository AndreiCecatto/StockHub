<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Clientes (RN-76). Rotas: GET /clientes e POST /clientes (documento, nome, uf). Só lojas com Vendas ou Ordens
    de serviço chegam aqui (o controller responde 404 nas demais).
    Dados do ClienteController: clientes (Parceiro: documento, nome, uf, fornecedor); flash: mensagem, erro,
    documentoDigitado, nomeDigitado, ufDigitada.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Clientes" ativo="clientes" largura="form">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/membros.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Clientes" descricao="As pessoas e empresas para quem você vende ou presta serviço." />

        <section class="sh-card sh-secao" aria-labelledby="titulo-clientes">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-clientes">Seus clientes</h2>
            </div>
            <c:choose>
                <c:when test="${empty clientes}">
                    <p class="mb-nota">Nenhum cliente ainda. Cadastre o primeiro logo abaixo.</p>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards" role="table">
                            <caption class="visually-hidden">Clientes da loja, <c:out value="${fn:length(clientes)}" /> no total</caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Cliente</th>
                                    <th scope="col" role="columnheader">CPF ou CNPJ</th>
                                    <th scope="col" role="columnheader">UF</th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="cli" items="${clientes}">
                                    <tr role="row">
                                        <td role="cell">
                                            <span class="mb-nome"><c:out value="${cli.nome}" /></span>
                                            <c:if test="${cli.fornecedor}"><span class="mb-email">Também é fornecedor</span></c:if>
                                        </td>
                                        <td role="cell" data-label="CPF ou CNPJ"><sh:documentoBr valor="${cli.documento}" /></td>
                                        <td role="cell" data-label="UF"><c:out value="${cli.uf}" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="sh-card sh-secao" aria-labelledby="titulo-novo-cliente">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-novo-cliente">Novo cliente</h2>
            </div>
            <div class="sh-card__corpo">
                <form method="post" action="${ctx}/clientes" data-sh-enviando="Salvando…">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="nome">Nome</label>
                            <input id="nome" class="form-control" type="text" name="nome" maxlength="255" required
                                autocomplete="off" value="<c:out value='${nomeDigitado}'/>" placeholder="Ex.: Maria Oliveira">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="documento">CPF ou CNPJ</label>
                            <input id="documento" class="form-control" type="text" name="documento" maxlength="18" required
                                inputmode="numeric" autocomplete="off" value="<c:out value='${documentoDigitado}'/>">
                        </div>
                        <div class="col-12 col-md-2">
                            <label class="form-label" for="uf">UF <span class="text-muted">(opcional)</span></label>
                            <input id="uf" class="form-control" type="text" name="uf" maxlength="2" autocomplete="off"
                                value="<c:out value='${ufDigitada}'/>" placeholder="RS">
                        </div>
                    </div>
                    <div class="sh-form-actions mb-acoes-form">
                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-person-plus" aria-hidden="true"></i> Cadastrar cliente
                        </button>
                    </div>
                </form>
            </div>
        </section>
    </jsp:body>
</sh:layout>
