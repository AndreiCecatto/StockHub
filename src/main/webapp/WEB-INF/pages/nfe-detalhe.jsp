<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Detalhe de uma nota importada. Rota: GET /compras/nfe/{id}.
    Dados do NfeController: nota (NotaFiscal: numero, serie, modelo, chaveAcesso, naturezaOperacao, emitidaEm, importadaEm,
    cnpjDestinatario, valorProdutos, valorFrete, valorDesconto, valorTotal, parceiro.nome),
    itens (NotaFiscalItem: numeroItem, codigoFornecedor, descricao, cfop, cfopEntrada, unidade, quantidade, valorTotal,
    situacao IMPORTADO|IGNORADO|FRACIONADO, produto.id/nome, movimentacao.id), itensImportados, itensIgnorados, itensFracionados.
    As movimentações do produto ficam em /estoque?produto={produto.id}.
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="totalItens" value="${fn:length(itens)}" />

<sh:layout titulo="Nota fiscal ${nota.numero}" ativo="compras" largura="larga"
    voltarUrl="/compras/nfe" voltarRotulo="Compras (NF-e)">
    <jsp:attribute name="migalhas">
        <li class="breadcrumb-item"><a href="${ctx}/dashboard">Início</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/compras/nfe">Compras (NF-e)</a></li>
        <li class="breadcrumb-item active" aria-current="page">NF <c:out value="${nota.numero}" /></li>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/nfe.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Nota fiscal ${nota.numero}" descricao="Nota importada: veja o que entrou no estoque e o que ficou de fora.">
            <a class="btn btn-outline-secondary" href="${ctx}/compras/nfe">
                <i class="bi bi-arrow-left" aria-hidden="true"></i> Voltar às notas
            </a>
        </sh:cabecalhoPagina>

        <dl class="nfe-resumo__lista nfe-resumo__lista--solta">
            <div class="nfe-resumo__fig nfe-resumo__fig--entra">
                <dt>Entraram no estoque</dt>
                <dd class="sh-mono"><c:out value="${itensImportados}" /></dd>
                <dd class="nfe-resumo__nota">de <c:out value="${totalItens}" /> itens da nota</dd>
            </div>
            <div class="nfe-resumo__fig">
                <dt>Ignorados</dt>
                <dd class="sh-mono"><c:out value="${itensIgnorados}" /></dd>
                <dd class="nfe-resumo__nota">não mudaram o estoque</dd>
            </div>
            <div class="nfe-resumo__fig">
                <dt>Fracionados</dt>
                <dd class="sh-mono"><c:out value="${itensFracionados}" /></dd>
                <dd class="nfe-resumo__nota">para lançar à mão</dd>
            </div>
        </dl>

        <c:if test="${itensFracionados > 0}">
            <div class="sh-alert sh-alert--warning" role="note">
                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                <div class="sh-alert__corpo">
                    <span class="sh-alert__titulo">Há itens com quantidade fracionada</span>
                    O estoque aceita só quantidades inteiras, então estes itens ficaram registrados na nota, mas não entraram. Lance a entrada deles à mão em Movimentações.
                </div>
            </div>
        </c:if>

        <section class="sh-card sh-secao" aria-labelledby="nfe-titulo-dados">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="nfe-titulo-dados">Dados da nota</h2>
            </div>
            <div class="sh-card__corpo">
                <dl class="nfe-dados">
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Fornecedor</dt>
                        <dd><strong><c:out value="${nota.parceiro.nome}" /></strong></dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Número da nota</dt>
                        <dd class="sh-mono"><c:out value="${nota.numero}" /></dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Data da compra</dt>
                        <dd><sh:dataHora valor="${nota.emitidaEm}" /></dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Importada no sistema em</dt>
                        <dd><sh:dataHora valor="${nota.importadaEm}" /></dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Comprador (documento)</dt>
                        <dd><sh:documentoBr valor="${nota.cnpjDestinatario}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Tipo da operação</dt>
                        <dd><c:out value="${nota.naturezaOperacao}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Código da nota</dt>
                        <dd><sh:chaveNfe valor="${nota.chaveAcesso}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Detalhes fiscais</dt>
                        <dd><details class="nfe-detalhes"><summary>Ver série e modelo</summary>
                            <p class="nfe-detalhes__linha">Série <span class="sh-mono"><c:out value="${nota.serie}" /></span> &middot; modelo <span class="sh-mono"><c:out value="${nota.modelo}" /></span></p></details></dd>
                    </div>
                </dl>
                <dl class="nfe-valores">
                    <div><dt>Produtos</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorProdutos}" pattern="#,##0.00" /></dd></div>
                    <div><dt>Frete</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorFrete}" pattern="#,##0.00" /></dd></div>
                    <c:if test="${nota.valorSeguro != null and nota.valorSeguro > 0}"><div><dt>Seguro</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorSeguro}" pattern="#,##0.00" /></dd></div></c:if>
                    <c:if test="${nota.valorOutras != null and nota.valorOutras > 0}"><div><dt>Outras despesas</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorOutras}" pattern="#,##0.00" /></dd></div></c:if>
                    <div><dt>Desconto</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorDesconto}" pattern="#,##0.00" /></dd></div>
                    <div class="nfe-valores__total"><dt>Total da nota</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorTotal}" pattern="#,##0.00" /></dd></div>
                </dl>
                <p class="nfe-nota-rodape">A assinatura digital do arquivo e a situação da nota na Receita (SEFAZ) não foram verificadas pelo StockHub.</p>
            </div>
        </section>

        <section class="sh-secao" aria-labelledby="nfe-titulo-itens">
            <h2 class="nfe-titulo-secao" id="nfe-titulo-itens">Itens da nota <span class="nfe-contagem">(<c:out value="${totalItens}" />)</span></h2>
            <div class="sh-table-wrap">
                <table class="sh-table sh-table--cards nfe-tabela nfe-tabela--itens" role="table">
                    <caption class="visually-hidden">Itens da nota fiscal <c:out value="${nota.numero}" /> e o que aconteceu com cada um</caption>
                    <thead role="rowgroup">
                        <tr role="row">
                            <th scope="col" role="columnheader">Item</th>
                            <th scope="col" role="columnheader">Descrição</th>
                            <th scope="col" role="columnheader" class="sh-col-num">Quantidade</th>
                            <th scope="col" role="columnheader" class="sh-col-num">Valor</th>
                            <th scope="col" role="columnheader">Situação</th>
                            <th scope="col" role="columnheader">Produto no estoque</th>
                        </tr>
                    </thead>
                    <tbody role="rowgroup">
                        <c:forEach var="it" items="${itens}">
                            <tr role="row" class="${it.situacao == 'IMPORTADO' ? '' : 'nfe-linha--fora'}">
                                <td role="cell" class="sh-mono" data-label="Item"><c:out value="${it.numeroItem}" /></td>
                                <td role="cell" data-label="Descrição">
                                    <span class="nfe-desc"><c:out value="${it.descricao}" /></span>
                                    <span class="nfe-desc__meta">
                                        <c:if test="${not empty it.codigoFornecedor}">Cód. <span class="sh-mono"><c:out value="${it.codigoFornecedor}" /></span></c:if>
                                        <c:if test="${not empty it.cfop}"><c:if test="${not empty it.codigoFornecedor}"> &middot; </c:if><sh:cfop codigo="${it.cfop}" /></c:if>
                                        <c:if test="${it.custoUnitario != null}"><br>Custo de cada unidade: <span class="sh-mono">R$ <fmt:formatNumber value="${it.custoUnitario}" pattern="#,##0.00##" /></span><c:if test="${it.rateio > 0}"> (com R$ <fmt:formatNumber value="${it.rateio}" pattern="#,##0.00" /> de frete e outras despesas)</c:if></c:if>
                                    </span>
                                </td>
                                <td role="cell" class="sh-col-num sh-mono" data-label="Quantidade"><fmt:formatNumber value="${it.quantidade}" pattern="#,##0.####" /> <c:out value="${it.unidade}" /></td>
                                <td role="cell" class="sh-col-num sh-mono" data-label="Valor">R$ <fmt:formatNumber value="${it.valorTotal}" pattern="#,##0.00" /></td>
                                <td role="cell" data-label="Situação">
                                    <c:choose>
                                        <c:when test="${it.situacao == 'IMPORTADO'}"><span class="sh-badge sh-badge--entrada"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Entrou no estoque</span></c:when>
                                        <c:when test="${it.situacao == 'FRACIONADO'}"><span class="sh-badge sh-badge--baixo"><i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i> Fracionado: lançar à mão</span></c:when>
                                        <c:otherwise><span class="sh-badge sh-badge--ajuste"><i class="bi bi-slash-circle" aria-hidden="true"></i> Ignorado</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td role="cell" data-label="Produto no estoque">
                                    <c:choose>
                                        <c:when test="${it.produto != null}">
                                            <a class="nfe-produto-link" href="<c:out value='${ctx}/produtos/${it.produto.id}'/>"><c:out value="${it.produto.nome}" /></a>
                                            <a class="nfe-mov-link" href="<c:out value='${ctx}/estoque?produto=${it.produto.id}'/>">
                                                <i class="bi bi-arrow-left-right" aria-hidden="true"></i> Ver movimentações<span class="visually-hidden"> de <c:out value="${it.produto.nome}" /></span>
                                            </a>
                                        </c:when>
                                        <c:otherwise><span class="sh-muted">&mdash;<span class="visually-hidden"> sem produto vinculado</span></span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </jsp:body>
</sh:layout>
