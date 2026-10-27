<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de clientes (RN-76, RN-97). Rota: GET /clientes?busca=&situacao=&os=&ordem=&pagina=. Só lojas com Vendas ou
    Ordens de serviço chegam aqui (o controller responde 404 nas demais).
    Dados do ClienteController: clientes (ClienteLinha: cliente, totalOs), filtro (ClienteFiltro), paginaAtual,
    totalPaginas, totalClientes, tamanho, parametrosFiltros (já codificado, começa com "&"), temOs (módulo OS ligado).
    Funciona sem JavaScript: é um GET comum; com js/clientes.js os filtros se aplicam ao mudar.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Clientes" ativo="clientes">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/membros.css">
        <link rel="stylesheet" href="${ctx}/css/clientes.css">
        <script defer src="${ctx}/js/clientes.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Clientes" descricao="As pessoas e empresas para quem você vende ou presta serviço.">
            <a class="btn btn-primary" href="${ctx}/clientes/novo"><i class="bi bi-person-plus" aria-hidden="true"></i> Novo cliente</a>
        </sh:cabecalhoPagina>

        <form class="cli-filtros" method="get" action="${ctx}/clientes" role="search" aria-label="Buscar e filtrar clientes" data-cliente-filtros>
            <div class="cli-filtros__busca">
                <label class="visually-hidden" for="busca">Buscar cliente</label>
                <input id="busca" class="form-control" type="search" name="busca" maxlength="120" autocomplete="off"
                    value="<c:out value='${filtro.busca}'/>" placeholder="Buscar por nome, CPF/CNPJ, telefone ou e-mail">
                <button class="btn btn-outline-secondary" type="submit"><i class="bi bi-search" aria-hidden="true"></i> Buscar</button>
            </div>
            <div class="cli-filtros__linha">
                <div class="cli-filtros__grupo">
                    <span class="cli-filtros__rotulo" id="rot-situacao">Situação</span>
                    <div class="sh-seg" role="radiogroup" aria-labelledby="rot-situacao">
                        <input type="radio" id="sit-ativos" name="situacao" value="ativos" ${filtro.situacao == 'ATIVOS' ? 'checked' : ''}>
                        <label for="sit-ativos">Ativos</label>
                        <input type="radio" id="sit-inativos" name="situacao" value="inativos" ${filtro.situacao == 'INATIVOS' ? 'checked' : ''}>
                        <label for="sit-inativos">Inativos</label>
                        <input type="radio" id="sit-todos" name="situacao" value="todos" ${filtro.situacao == 'TODOS' ? 'checked' : ''}>
                        <label for="sit-todos">Todos</label>
                    </div>
                </div>
                <c:if test="${temOs}">
                    <div class="cli-filtros__grupo">
                        <span class="cli-filtros__rotulo" id="rot-os">Ordens de serviço</span>
                        <div class="sh-seg" role="radiogroup" aria-labelledby="rot-os">
                            <input type="radio" id="os-todos" name="os" value="todos" ${filtro.comOs == 'TODOS' ? 'checked' : ''}>
                            <label for="os-todos">Todos</label>
                            <input type="radio" id="os-com" name="os" value="com" ${filtro.comOs == 'COM' ? 'checked' : ''}>
                            <label for="os-com">Com OS</label>
                            <input type="radio" id="os-sem" name="os" value="sem" ${filtro.comOs == 'SEM' ? 'checked' : ''}>
                            <label for="os-sem">Sem OS</label>
                        </div>
                    </div>
                </c:if>
                <div class="cli-filtros__grupo cli-filtros__grupo--ordem">
                    <label class="cli-filtros__rotulo" for="ordem">Ordenar por</label>
                    <select id="ordem" class="form-select" name="ordem">
                        <option value="nome" ${filtro.ordem == 'NOME' ? 'selected' : ''}>Nome (A a Z)</option>
                        <option value="recentes" ${filtro.ordem == 'RECENTES' ? 'selected' : ''}>Mais recentes</option>
                    </select>
                </div>
                <c:if test="${filtro.temFiltros()}">
                    <a class="btn btn-ghost cli-filtros__limpar" href="${ctx}/clientes"><i class="bi bi-x-lg" aria-hidden="true"></i> Limpar filtros</a>
                </c:if>
            </div>
        </form>

        <section class="sh-card sh-secao" aria-labelledby="titulo-clientes">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-clientes">
                    <c:out value="${totalClientes}" /> <c:out value="${totalClientes == 1 ? 'cliente' : 'clientes'}" />
                </h2>
            </div>
            <c:choose>
                <c:when test="${empty clientes}">
                    <p class="mb-nota">
                        <c:choose>
                            <c:when test="${filtro.temFiltros()}">Nenhum cliente encontrado com essa busca. <a href="${ctx}/clientes">Limpar filtros</a>.</c:when>
                            <c:otherwise>Nenhum cliente ainda. Use o botão "Novo cliente" para cadastrar o primeiro.</c:otherwise>
                        </c:choose>
                    </p>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards" role="table">
                            <caption class="visually-hidden">Clientes da loja, <c:out value="${fn:length(clientes)}" /> nesta página</caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Cliente</th>
                                    <th scope="col" role="columnheader">CPF ou CNPJ</th>
                                    <th scope="col" role="columnheader">Contato</th>
                                    <th scope="col" role="columnheader">Cidade</th>
                                    <c:if test="${temOs}"><th scope="col" role="columnheader" class="text-end">Ordens</th></c:if>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="linha" items="${clientes}">
                                    <c:set var="cli" value="${linha.cliente}" />
                                    <tr role="row">
                                        <td role="cell">
                                            <a class="mb-nome cli-nome" href="${ctx}/clientes/${cli.id}"><c:out value="${cli.nome}" /></a>
                                            <c:if test="${not empty cli.nomeFantasia}"><span class="mb-email"><c:out value="${cli.nomeFantasia}" /></span></c:if>
                                            <c:if test="${not cli.ativo}"><span class="cli-selo cli-selo--inativo">Inativo</span></c:if>
                                            <c:if test="${cli.fornecedor}"><span class="mb-email">Também é fornecedor</span></c:if>
                                        </td>
                                        <td role="cell" data-label="CPF ou CNPJ"><sh:documentoBr valor="${cli.documento}" /></td>
                                        <td role="cell" data-label="Contato">
                                            <c:choose>
                                                <c:when test="${not empty cli.telefone or not empty cli.whatsapp or not empty cli.email}">
                                                    <c:if test="${not empty cli.whatsapp or not empty cli.telefone}">
                                                        <span class="mb-nome cli-fone"><sh:telefoneBr valor="${empty cli.whatsapp ? cli.telefone : cli.whatsapp}" /><c:if test="${not empty cli.whatsapp}"> <span class="mb-email cli-inline">WhatsApp</span></c:if></span>
                                                    </c:if>
                                                    <c:if test="${not empty cli.email}"><span class="mb-email"><c:out value="${cli.email}" /></span></c:if>
                                                </c:when>
                                                <c:otherwise>&mdash;</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td role="cell" data-label="Cidade">
                                            <c:choose>
                                                <c:when test="${not empty cli.cidade}"><c:out value="${cli.cidade}" /><c:if test="${not empty cli.uf}">/<c:out value="${cli.uf}" /></c:if></c:when>
                                                <c:when test="${not empty cli.uf}"><c:out value="${cli.uf}" /></c:when>
                                                <c:otherwise>&mdash;</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <c:if test="${temOs}"><td role="cell" data-label="Ordens" class="text-end"><c:out value="${linha.totalOs}" /></td></c:if>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalClientes}"
                        tamanho="${tamanho}" base="/clientes" extra="${parametrosFiltros}" rotulo="Paginação de clientes" />
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
