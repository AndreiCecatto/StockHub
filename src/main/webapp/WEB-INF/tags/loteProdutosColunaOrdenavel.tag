<%@ tag description="Cabeçalho de coluna ordenável da lista de produtos: link com aria-sort e seta (lote 3)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso (só em produtos.jsp):
      <sh:loteProdutosColunaOrdenavel campo="nome" rotulo="Produto" ordenacaoAtual="${ordenacao}"
          direcaoAtual="${direcao}" urlBase="${urlOrdem}" />

    campo:         valor do parâmetro "ordenacao" (nome, categoria, preco, quantidade).
    ordenacaoAtual / direcaoAtual: o que o ProdutoController devolveu no model (já normalizados: asc ou desc).
    urlBase:       URL pronta (com o contexto e já com "?"), com os filtros e o tamanho, SEM ordenacao e direcao.
    numerico:      true alinha à direita (preço).
    Clicar na coluna ativa inverte a direção; em outra coluna começa crescente. A página volta à primeira.
    Arquivo do lote 3: ao crescer, pode ir para o conjunto compartilhado de tags no lote 5.
--%>
<%@ attribute name="campo" required="true" type="java.lang.String" %>
<%@ attribute name="rotulo" required="true" type="java.lang.String" %>
<%@ attribute name="ordenacaoAtual" required="true" type="java.lang.String" %>
<%@ attribute name="direcaoAtual" required="true" type="java.lang.String" %>
<%@ attribute name="urlBase" required="true" type="java.lang.String" %>
<%@ attribute name="numerico" required="false" type="java.lang.Boolean" %>
<c:set var="ativa" value="${ordenacaoAtual == campo}" />
<c:set var="crescente" value="${direcaoAtual == 'asc'}" />
<c:set var="proxima" value="${ativa ? (crescente ? 'desc' : 'asc') : 'asc'}" />
<th scope="col" role="columnheader" class="${numerico ? 'sh-col-num' : ''}"
    aria-sort="${ativa ? (crescente ? 'ascending' : 'descending') : 'none'}">
    <a class="sh-prod-ordenar ${ativa ? 'is-ativa' : ''}"
        href="<c:out value='${urlBase}&ordenacao=${campo}&direcao=${proxima}'/>">
        <span><c:out value="${rotulo}" /></span>
        <i class="bi ${ativa ? (crescente ? 'bi-arrow-up' : 'bi-arrow-down') : 'bi-arrow-down-up'}" aria-hidden="true"></i>
        <span class="visually-hidden">, ordenar de forma <c:out value="${proxima == 'asc' ? 'crescente' : 'decrescente'}" /></span>
    </a>
</th>
