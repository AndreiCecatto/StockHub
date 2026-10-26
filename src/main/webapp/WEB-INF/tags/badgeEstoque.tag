<%@ tag description="Badge do nível de estoque: ícone + palavra + cor (nunca só a cor)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
    nivel: NORMAL, BAIXO ou SEM_ESTOQUE (o enum NivelEstoque vira texto pelo próprio EL). Outro valor não mostra nada.
    Três formas diferentes (círculo com visto, triângulo, octógono) + a palavra: o estado não depende de cor.
--%>
<%@ attribute name="nivel" required="true" type="java.lang.String" %>
<c:choose>
    <c:when test="${nivel == 'NORMAL'}">
        <span class="sh-badge sh-badge--normal"><i class="bi bi-check-circle-fill" aria-hidden="true"></i>Normal</span>
    </c:when>
    <c:when test="${nivel == 'BAIXO'}">
        <span class="sh-badge sh-badge--baixo"><i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>Baixo</span>
    </c:when>
    <c:when test="${nivel == 'SEM_ESTOQUE'}">
        <span class="sh-badge sh-badge--sem"><i class="bi bi-x-octagon-fill" aria-hidden="true"></i>Sem estoque</span>
    </c:when>
</c:choose>
