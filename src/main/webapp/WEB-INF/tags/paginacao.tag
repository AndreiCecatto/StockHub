<%@ tag description="Paginação: 'Mostrando 26-50 de 140', janela de 5 páginas, Anterior e Próxima" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalProdutosFiltro}"
             tamanho="${tamanho}" base="/produtos" extra="${parametrosFiltros}" rotulo="Paginação de produtos" />

    base:   caminho da tela SEM o contexto da aplicação (o tag acrescenta), ex.: /estoque.
    extra:  texto já codificado que começa com "&", ex.: "&busca=cafe&tamanho=25" (é o atributo
            parametrosFiltros que o ProdutoController monta). Pode ficar vazio.
    totalItens e tamanho (opcionais): com eles aparece "Mostrando 26-50 de 140"; sem eles, "Página 2 de 6".
    Fica oculta quando só há uma página. Anterior na primeira página e Próxima na última ficam
    aria-disabled e fora da ordem de tabulação. No celular os números somem e sobra "‹ Anterior · 2 de 6 · Próxima ›".
    Todo href é escapado com c:out.
--%>
<%@ attribute name="paginaAtual" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalPaginas" required="true" type="java.lang.Integer" %>
<%@ attribute name="base" required="true" type="java.lang.String" %>
<%@ attribute name="extra" required="false" type="java.lang.String" %>
<%@ attribute name="totalItens" required="false" type="java.lang.Long" %>
<%@ attribute name="tamanho" required="false" type="java.lang.Integer" %>
<%@ attribute name="rotulo" required="false" type="java.lang.String" %>
<c:if test="${totalPaginas > 1}">
    <c:set var="ctx" value="${pageContext.request.contextPath}" />
    <c:set var="janelaIni" value="${paginaAtual - 2 < 1 ? 1 : paginaAtual - 2}" />
    <c:set var="janelaFim" value="${paginaAtual + 2 > totalPaginas ? totalPaginas : paginaAtual + 2}" />
    <nav class="sh-paginacao" aria-label="<c:out value='${empty rotulo ? "Paginação" : rotulo}'/>">
        <p class="sh-paginacao__info">
            <c:choose>
                <c:when test="${not empty totalItens and not empty tamanho and tamanho > 0}">
                    <c:set var="de" value="${(paginaAtual - 1) * tamanho + 1}" />
                    <c:set var="ate" value="${paginaAtual * tamanho > totalItens ? totalItens : paginaAtual * tamanho}" />
                    Mostrando <c:out value="${de}" />&ndash;<c:out value="${ate}" /> de <c:out value="${totalItens}" />
                </c:when>
                <c:otherwise>
                    Página <c:out value="${paginaAtual}" /> de <c:out value="${totalPaginas}" />
                </c:otherwise>
            </c:choose>
        </p>
        <ul class="pagination">
            <c:choose>
                <c:when test="${paginaAtual > 1}">
                    <li class="page-item">
                        <a class="page-link" href="<c:out value='${ctx}${base}?pagina=${paginaAtual - 1}${extra}'/>">
                            <i class="bi bi-chevron-left" aria-hidden="true"></i> Anterior
                        </a>
                    </li>
                </c:when>
                <c:otherwise>
                    <li class="page-item disabled">
                        <span class="page-link" aria-disabled="true"><i class="bi bi-chevron-left" aria-hidden="true"></i> Anterior</span>
                    </li>
                </c:otherwise>
            </c:choose>

            <li class="sh-paginacao__compacta" aria-hidden="true"><c:out value="${paginaAtual}" /> de <c:out value="${totalPaginas}" /></li>

            <c:if test="${janelaIni > 1}">
                <li class="page-item sh-pag-numero">
                    <a class="page-link" href="<c:out value='${ctx}${base}?pagina=1${extra}'/>" aria-label="Página 1">1</a>
                </li>
                <c:if test="${janelaIni > 2}">
                    <li class="page-item disabled sh-pag-numero"><span class="page-link" aria-hidden="true">&hellip;</span></li>
                </c:if>
            </c:if>

            <c:forEach var="p" begin="${janelaIni}" end="${janelaFim}">
                <c:choose>
                    <c:when test="${p == paginaAtual}">
                        <li class="page-item active sh-pag-numero" aria-current="page">
                            <span class="page-link"><c:out value="${p}" /></span>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="page-item sh-pag-numero">
                            <a class="page-link" href="<c:out value='${ctx}${base}?pagina=${p}${extra}'/>"
                                aria-label="Página <c:out value='${p}'/>"><c:out value="${p}" /></a>
                        </li>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <c:if test="${janelaFim < totalPaginas}">
                <c:if test="${janelaFim < totalPaginas - 1}">
                    <li class="page-item disabled sh-pag-numero"><span class="page-link" aria-hidden="true">&hellip;</span></li>
                </c:if>
                <li class="page-item sh-pag-numero">
                    <a class="page-link" href="<c:out value='${ctx}${base}?pagina=${totalPaginas}${extra}'/>"
                        aria-label="Página <c:out value='${totalPaginas}'/>"><c:out value="${totalPaginas}" /></a>
                </li>
            </c:if>

            <c:choose>
                <c:when test="${paginaAtual < totalPaginas}">
                    <li class="page-item">
                        <a class="page-link" href="<c:out value='${ctx}${base}?pagina=${paginaAtual + 1}${extra}'/>">
                            Próxima <i class="bi bi-chevron-right" aria-hidden="true"></i>
                        </a>
                    </li>
                </c:when>
                <c:otherwise>
                    <li class="page-item disabled">
                        <span class="page-link" aria-disabled="true">Próxima <i class="bi bi-chevron-right" aria-hidden="true"></i></span>
                    </li>
                </c:otherwise>
            </c:choose>
        </ul>
    </nav>
</c:if>
