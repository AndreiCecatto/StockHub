<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activePage" value="estoque" />
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <title>StockHub - Movimentações</title>
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
                    <i class="bi bi-grid-3x3-gap"></i> Ver produtos
                </a>
            </section>

            <c:if test="${not empty mensagem}"><div class="alert alert-success">${mensagem}</div></c:if>
            <c:if test="${not empty erro}"><div class="alert alert-danger">${erro}</div></c:if>

            <section class="row g-4">
                <div class="col-12 col-xl-4">
                    <section class="painel card h-100">
                        <div class="cabecalho-painel card-header">
                            <h2><i class="bi bi-arrow-left-right"></i> Registrar movimento</h2>
                            <p>Entrada soma, saída subtrai e ajuste define o saldo final.</p>
                        </div>
                        <div class="card-body">
                            <form class="d-grid gap-3" method="post" action="${pageContext.request.contextPath}/estoque/movimentar">
                                <div>
                                    <label class="form-label" for="produtoId">Produto</label>
                                    <select class="form-select" id="produtoId" name="produtoId" required>
                                        <option value="">Selecione um produto</option>
                                        <c:forEach var="produto" items="${produtos}">
                                            <option value="${produto.id}">${produto.nome} - atual: ${produto.quantidade} un.</option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div>
                                    <label class="form-label" for="tipo">Tipo</label>
                                    <select class="form-select" id="tipo" name="tipo" required>
                                        <c:forEach var="tipo" items="${tipos}">
                                            <option value="${tipo}">${tipo}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div>
                                    <label class="form-label" for="quantidade">Quantidade</label>
                                    <input class="form-control" id="quantidade" type="number" min="1" name="quantidade" required>
                                </div>
                                <div>
                                    <label class="form-label" for="observacao">Observação</label>
                                    <textarea class="form-control" id="observacao" name="observacao" rows="4" placeholder="Ex: compra de fornecedor, venda, perda ou correção"></textarea>
                                </div>
                                <button class="btn btn-primary" type="submit">
                                    <i class="bi bi-check2-circle"></i> Registrar movimentação
                                </button>
                            </form>
                        </div>
                    </section>
                </div>

                <div class="col-12 col-xl-8">
                    <section class="painel card">
                        <div class="cabecalho-painel card-header d-flex align-items-center justify-content-between gap-3">
                            <div>
                                <h2><i class="bi bi-clock-history"></i> Histórico</h2>
                                <p>Últimas entradas, saídas e ajustes registrados.</p>
                            </div>
                            <span class="etiqueta"><i class="bi bi-list-check"></i> ${movimentacoes.size()} registro(s)</span>
                        </div>
                        <div class="table-responsive">
                            <table class="table tabela-sistema align-middle">
                                <thead>
                                <tr>
                                    <th>Data</th>
                                    <th>Produto</th>
                                    <th>Tipo</th>
                                    <th>Quantidade</th>
                                    <th>Observação</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="mov" items="${movimentacoes}">
                                    <tr>
                                        <td>${mov.criadoEm}</td>
                                        <td><strong>${mov.produto.nome}</strong><span class="subtitulo-linha">${mov.produto.categoria.nome}</span></td>
                                        <td>
                                            <span class="etiqueta ${mov.tipo == 'ENTRADA' ? 'etiqueta-ok' : (mov.tipo == 'SAIDA' ? 'etiqueta-sem-estoque' : 'etiqueta-alerta')}">${mov.tipo}</span>
                                        </td>
                                        <td>${mov.quantidade} un.</td>
                                        <td>${mov.observacao}</td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty movimentacoes}">
                                    <tr>
                                        <td colspan="5"><div class="estado-vazio">Nenhuma movimentação registrada ainda.</div></td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                    </section>
                </div>
            </section>
        </main>
    </div>
</div>
</body>
</html>