<%@ tag description="Como foi pago e parcelas de uma nota (somente leitura)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Uso: <sh:cobrancaNfe pagamentos="${nota.pagamentos}" parcelas="${nota.parcelas}" />
    Funciona com a nota lida do XML (revisão) e com a nota importada (detalhe): as duas têm descricao/valor nos
    pagamentos e numero/vencimento/valor nas parcelas. Não mostra nada se a nota não trouxe esses dados.
    Todo dado sai por c:out; sem script nem style inline.
--%>
<%@ attribute name="pagamentos" required="false" type="java.util.Collection" %>
<%@ attribute name="parcelas" required="false" type="java.util.Collection" %>
<c:if test="${not empty pagamentos or not empty parcelas}">
    <div class="nfe-cobranca">
        <c:if test="${not empty pagamentos}">
            <section aria-labelledby="nfe-titulo-pago">
                <h3 class="nfe-cobranca__titulo" id="nfe-titulo-pago">Como foi pago</h3>
                <ul class="nfe-cobranca__lista">
                    <c:forEach var="pg" items="${pagamentos}">
                        <li><span><c:out value="${pg.descricao}" /></span><span class="sh-mono">R$ <fmt:formatNumber value="${pg.valor}" pattern="#,##0.00" /></span></li>
                    </c:forEach>
                </ul>
            </section>
        </c:if>
        <c:if test="${not empty parcelas}">
            <section aria-labelledby="nfe-titulo-parcelas">
                <h3 class="nfe-cobranca__titulo" id="nfe-titulo-parcelas">Parcelas</h3>
                <ul class="nfe-cobranca__lista">
                    <c:forEach var="pc" items="${parcelas}">
                        <li>
                            <span>Parcela <c:out value="${pc.numero}" /><c:choose>
                                <c:when test="${pc.vencimento != null}"> &middot; vence em <span class="sh-mono"><fmt:formatNumber value="${pc.vencimento.dayOfMonth}" minIntegerDigits="2" />/<fmt:formatNumber value="${pc.vencimento.monthValue}" minIntegerDigits="2" />/<fmt:formatNumber value="${pc.vencimento.year}" groupingUsed="false" /></span></c:when>
                                <c:otherwise> &middot; sem data de vencimento</c:otherwise>
                            </c:choose></span>
                            <span class="sh-mono">R$ <fmt:formatNumber value="${pc.valor}" pattern="#,##0.00" /></span>
                        </li>
                    </c:forEach>
                </ul>
            </section>
        </c:if>
    </div>
</c:if>
