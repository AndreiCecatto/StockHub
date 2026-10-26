<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Paginador reutilizavel (janela de 5 paginas + primeira/ultima).
    Le do request: paginaAtual e totalPaginas (base 1).
    Parametros: base (caminho da tela, ex.: /estoque) e rotulo (aria-label do nav).
    Os links usam c:url/c:param, que codificam o valor e acrescentam o contexto da aplicacao.
--%>
<c:if test="${totalPaginas > 1}">
    <c:set var="janelaIni" value="${paginaAtual - 2 < 1 ? 1 : paginaAtual - 2}" />
    <c:set var="janelaFim" value="${paginaAtual + 2 > totalPaginas ? totalPaginas : paginaAtual + 2}" />
    <nav class="d-flex align-items-center justify-content-between gap-3 flex-wrap p-3" aria-label="<c:out value='${param.rotulo}'/>">
        <span class="text-secondary small">Página <c:out value="${paginaAtual}" /> de <c:out value="${totalPaginas}" /></span>
        <ul class="pagination pagination-sm mb-0 flex-wrap">
            <c:choose>
                <c:when test="${paginaAtual > 1}">
                    <c:url var="urlAnterior" value="${param.base}"><c:param name="pagina" value="${paginaAtual - 1}" /></c:url>
                    <li class="page-item"><a class="page-link" href="<c:out value='${urlAnterior}'/>">Anterior</a></li>
                </c:when>
                <c:otherwise>
                    <li class="page-item disabled"><span class="page-link">Anterior</span></li>
                </c:otherwise>
            </c:choose>

            <c:if test="${janelaIni > 1}">
                <c:url var="urlPrimeira" value="${param.base}"><c:param name="pagina" value="1" /></c:url>
                <li class="page-item"><a class="page-link" href="<c:out value='${urlPrimeira}'/>" aria-label="Página 1">1</a></li>
                <c:if test="${janelaIni > 2}">
                    <li class="page-item disabled"><span class="page-link" aria-hidden="true">&hellip;</span></li>
                </c:if>
            </c:if>

            <c:forEach var="p" begin="${janelaIni}" end="${janelaFim}">
                <c:url var="urlPagina" value="${param.base}"><c:param name="pagina" value="${p}" /></c:url>
                <c:choose>
                    <c:when test="${p == paginaAtual}">
                        <li class="page-item active" aria-current="page"><span class="page-link"><c:out value="${p}" /></span></li>
                    </c:when>
                    <c:otherwise>
                        <li class="page-item"><a class="page-link" href="<c:out value='${urlPagina}'/>" aria-label="Página <c:out value='${p}'/>"><c:out value="${p}" /></a></li>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <c:if test="${janelaFim < totalPaginas}">
                <c:if test="${janelaFim < totalPaginas - 1}">
                    <li class="page-item disabled"><span class="page-link" aria-hidden="true">&hellip;</span></li>
                </c:if>
                <c:url var="urlUltima" value="${param.base}"><c:param name="pagina" value="${totalPaginas}" /></c:url>
                <li class="page-item"><a class="page-link" href="<c:out value='${urlUltima}'/>" aria-label="Página <c:out value='${totalPaginas}'/>"><c:out value="${totalPaginas}" /></a></li>
            </c:if>

            <c:choose>
                <c:when test="${paginaAtual < totalPaginas}">
                    <c:url var="urlProxima" value="${param.base}"><c:param name="pagina" value="${paginaAtual + 1}" /></c:url>
                    <li class="page-item"><a class="page-link" href="<c:out value='${urlProxima}'/>">Próxima</a></li>
                </c:when>
                <c:otherwise>
                    <li class="page-item disabled"><span class="page-link">Próxima</span></li>
                </c:otherwise>
            </c:choose>
        </ul>
    </nav>
</c:if>
