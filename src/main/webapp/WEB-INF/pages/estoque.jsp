<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="estoque" scope="request" />
<%-- Erros por campo do formulario de movimentacao --%>
<c:set var="eProduto" value="${errosCampos['produtoId']}" />
<c:set var="eTipo" value="${errosCampos['tipo']}" />
<c:set var="eQuantidade" value="${errosCampos['quantidade']}" />
<c:set var="eObservacao" value="${errosCampos['observacao']}" />
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>Movimentações - StockHub</title>
</head>
<body>
<div class="estrutura-sistema d-flex min-vh-100">
    <jsp:include page="../includes/sidebar.jsp" />
    <div class="conteudo-sistema flex-grow-1">
        <jsp:include page="../includes/navbar.jsp" />
        <main class="conteudo-pagina">
            <section class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-3">
                <div class="titulo-pagina">
                    <h1>Movimentações de estoque</h1>
                    <p>Registre entradas, saídas e ajustes para manter histórico do saldo dos produtos.</p>
                </div>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/produtos">
                    <i class="bi bi-grid-3x3-gap" aria-hidden="true"></i> Ver produtos
                </a>
            </section>

            <jsp:include page="../includes/alertas.jsp" />

            <section class="row g-4">
                <div class="col-12 col-xl-4">
                    <section class="painel card h-100">
                        <div class="cabecalho-painel card-header">
                            <h2><i class="bi bi-arrow-left-right" aria-hidden="true"></i> Registrar movimento</h2>
                            <p>Entrada soma, saída subtrai e ajuste define o saldo final.</p>
                        </div>
                        <div class="card-body">
                            <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/estoque/movimentar">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                <div>
                                    <label class="form-label" for="produtoId">Produto</label>
                                    <select class="form-select ${not empty eProduto ? 'is-invalid' : ''}" id="produtoId" name="produtoId" required
                                        ${not empty eProduto ? 'aria-invalid="true" aria-describedby="erro-produtoId"' : ''}>
                                        <option value="">Selecione um produto</option>
                                        <c:forEach var="produto" items="${produtos}">
                                            <option value="<c:out value='${produto.id}'/>" ${movimentacaoForm.produtoId == produto.id ? 'selected' : ''}><c:out value="${produto.nome}" /> - atual: <c:out value="${produto.quantidade}" /> un.</option>
                                        </c:forEach>
                                    </select>
                                    <c:if test="${not empty eProduto}">
                                        <div id="erro-produtoId" class="invalid-feedback"><c:out value="${eProduto}" /></div>
                                    </c:if>
                                </div>
                                <div>
                                    <label class="form-label" for="tipo">Tipo</label>
                                    <select class="form-select ${not empty eTipo ? 'is-invalid' : ''}" id="tipo" name="tipo" required
                                        ${not empty eTipo ? 'aria-invalid="true" aria-describedby="erro-tipo"' : ''}>
                                        <c:forEach var="tipo" items="${tipos}">
                                            <option value="<c:out value='${tipo}'/>" ${movimentacaoForm.tipo == tipo ? 'selected' : ''}>
                                                <c:choose>
                                                    <c:when test="${tipo == 'ENTRADA'}">Entrada (soma ao saldo)</c:when>
                                                    <c:when test="${tipo == 'SAIDA'}">Saída (subtrai do saldo)</c:when>
                                                    <c:when test="${tipo == 'AJUSTE'}">Ajuste (define o saldo final)</c:when>
                                                    <c:otherwise><c:out value="${tipo}" /></c:otherwise>
                                                </c:choose>
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <c:if test="${not empty eTipo}">
                                        <div id="erro-tipo" class="invalid-feedback"><c:out value="${eTipo}" /></div>
                                    </c:if>
                                </div>
                                <div>
                                    <%-- Rotulo, min/max e dica mudam conforme o tipo (js/estoque.js) --%>
                                    <label class="form-label" for="quantidade" id="rotulo-quantidade">Quantidade</label>
                                    <input class="form-control ${not empty eQuantidade ? 'is-invalid' : ''}" id="quantidade" type="number"
                                        min="0" max="1000000" name="quantidade" value="<c:out value='${movimentacaoForm.quantidade}'/>" required
                                        aria-describedby="dica-quantidade${not empty eQuantidade ? ' erro-quantidade' : ''}"
                                        ${not empty eQuantidade ? 'aria-invalid="true"' : ''}>
                                    <div id="dica-quantidade" class="form-text">Mínimo 1 unidade.</div>
                                    <c:if test="${not empty eQuantidade}">
                                        <div id="erro-quantidade" class="invalid-feedback"><c:out value="${eQuantidade}" /></div>
                                    </c:if>
                                </div>
                                <div>
                                    <label class="form-label" for="observacao">Observação
                                        <span id="obs-obrigatoria" class="text-secondary fw-normal" hidden>(obrigatória no ajuste)</span>
                                    </label>
                                    <textarea class="form-control ${not empty eObservacao ? 'is-invalid' : ''}" id="observacao" name="observacao" rows="4"
                                        maxlength="255" placeholder="Ex: compra de fornecedor, venda, perda ou correção"
                                        ${not empty eObservacao ? 'aria-invalid="true" aria-describedby="erro-observacao"' : ''}><c:out value="${movimentacaoForm.observacao}" /></textarea>
                                    <c:if test="${not empty eObservacao}">
                                        <div id="erro-observacao" class="invalid-feedback"><c:out value="${eObservacao}" /></div>
                                    </c:if>
                                </div>
                                <button class="btn btn-primary" type="submit">
                                    <i class="bi bi-check2-circle" aria-hidden="true"></i> Registrar movimentação
                                </button>
                            </form>
                        </div>
                    </section>
                </div>

                <div class="col-12 col-xl-8">
                    <section class="painel card">
                        <div class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3">
                            <div>
                                <h2><i class="bi bi-clock-history" aria-hidden="true"></i> Histórico</h2>
                                <p>Entradas, saídas e ajustes registrados, do mais recente ao mais antigo.</p>
                            </div>
                            <span class="etiqueta"><i class="bi bi-list-check" aria-hidden="true"></i> <c:out value="${totalMovimentacoes}" /> registro(s)</span>
                        </div>
                        <div class="table-responsive">
                            <table class="table tabela-sistema align-middle">
                                <thead>
                                <tr>
                                    <th scope="col">Data</th>
                                    <th scope="col">Produto</th>
                                    <th scope="col">Tipo</th>
                                    <th scope="col">Quantidade</th>
                                    <th scope="col">Saldo (antes → depois)</th>
                                    <th scope="col">Observação</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="mov" items="${movimentacoes}">
                                    <%--
                                        criadoEm e LocalDateTime (nao e java.util.Date, entao fmt:formatDate nao serve).
                                        toString() = "aaaa-MM-ddTHH:mm[:ss[.n]]", sempre com ao menos 16 caracteres,
                                        entao fn:substring monta dd/MM/aaaa HH:mm de forma segura.
                                    --%>
                                    <c:set var="dt" value="${mov.criadoEm}" />
                                    <tr>
                                        <td>
                                            <time datetime="<c:out value='${fn:substring(dt, 0, 16)}'/>"><c:out value="${fn:substring(dt, 8, 10)}/${fn:substring(dt, 5, 7)}/${fn:substring(dt, 0, 4)} ${fn:substring(dt, 11, 16)}" /></time>
                                        </td>
                                        <td><strong><c:out value="${mov.produto.nome}" /></strong><span class="subtitulo-linha"><c:out value="${mov.produto.categoria.nome}" /></span></td>
                                        <td>
                                            <span class="etiqueta ${mov.tipo == 'ENTRADA' ? 'etiqueta-ok' : (mov.tipo == 'SAIDA' ? 'etiqueta-sem-estoque' : 'etiqueta-alerta')}">
                                                <c:choose>
                                                    <c:when test="${mov.tipo == 'ENTRADA'}"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Entrada</c:when>
                                                    <c:when test="${mov.tipo == 'SAIDA'}"><i class="bi bi-box-arrow-up" aria-hidden="true"></i> Saída</c:when>
                                                    <c:otherwise><i class="bi bi-sliders" aria-hidden="true"></i> Ajuste</c:otherwise>
                                                </c:choose>
                                            </span>
                                        </td>
                                        <td>
                                            <c:out value="${mov.quantidade}" /> un.
                                            <c:if test="${mov.tipo == 'AJUSTE'}"><span class="subtitulo-linha">contagem final</span></c:if>
                                        </td>
                                        <td><c:out value="${mov.quantidadeAnterior}" /> &rarr; <strong><c:out value="${mov.quantidadeNova}" /></strong></td>
                                        <td><c:out value="${mov.observacao}" /></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty movimentacoes}">
                                    <tr>
                                        <td colspan="6"><div class="estado-vazio">Nenhuma movimentação registrada ainda.</div></td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                        <jsp:include page="../includes/paginacao.jsp">
                            <jsp:param name="base" value="/estoque" />
                            <jsp:param name="rotulo" value="Paginação do histórico de movimentações" />
                        </jsp:include>
                    </section>
                </div>
            </section>
        </main>
    </div>
</div>
<jsp:include page="../includes/scripts.jsp" />
<script src="${pageContext.request.contextPath}/js/estoque.js"></script>
</body>
</html>
