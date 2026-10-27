<%@ tag description="Sparkline: linha de tendência em SVG, desenhada no servidor, com texto alternativo" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Uso: <sh:sparkline valores="${fn:join(...)}" rotulo="Saldo nos últimos 14 dias" />
         <sh:sparkline valores="3,5,4,8,12,9,14" tom="normal" area="true" largura="160" altura="40" />
    valores : números separados por vírgula, do mais antigo ao mais recente (com ponto decimal). Menos de 2 valores: não desenha.
    rotulo  : texto alternativo (role="img"). Sem ele, a tag escreve "Tendência com N pontos, de A até B".
    largura / altura : tamanho em pixels (padrão 120 x 32). O SVG é escalável: com a classe sh-spark--bloco ocupa a largura toda.
    tom     : "" (acento), normal, baixo ou sem (cores de estoque, classes sh-tom--*). Cor sempre por token.
    area    : true preenche suavemente sob a linha.
    anima   : true (padrão) a linha se desenha uma vez ao aparecer. rolagem="true": em vez disso, a linha se desenha conforme
              a rolagem (kit de rolagem: contêiner .sh-desenhar, linha .sh-traco). Com "menos movimento": sem animação.
    classe  : classes extras no <svg> (por exemplo sh-spark--bloco).
    Sem style inline: só atributos SVG e classes.
--%>
<%@ attribute name="valores" required="true" type="java.lang.String" %>
<%@ attribute name="rotulo" required="false" type="java.lang.String" %>
<%@ attribute name="largura" required="false" type="java.lang.Integer" %>
<%@ attribute name="altura" required="false" type="java.lang.Integer" %>
<%@ attribute name="tom" required="false" type="java.lang.String" %>
<%@ attribute name="area" required="false" type="java.lang.Boolean" %>
<%@ attribute name="anima" required="false" type="java.lang.Boolean" %>
<%@ attribute name="rolagem" required="false" type="java.lang.Boolean" %>
<%@ attribute name="classe" required="false" type="java.lang.String" %>
<c:set var="w" value="${empty largura ? 120 : largura}" />
<c:set var="h" value="${empty altura ? 32 : altura}" />
<c:set var="vs" value="${fn:split(valores, ',')}" />
<c:set var="n" value="${fn:length(vs)}" />
<c:if test="${n >= 2}">
    <c:set var="pad" value="3" />
    <c:set var="mn" value="0" />
    <c:set var="mx" value="0" />
    <c:set var="primeiro" value="0" />
    <c:set var="ultimo" value="0" />
    <c:forEach var="v" items="${vs}" varStatus="st">
        <c:set var="x" value="${fn:trim(v) + 0}" />
        <c:if test="${st.first}"><c:set var="mn" value="${x}" /><c:set var="mx" value="${x}" /><c:set var="primeiro" value="${x}" /></c:if>
        <c:if test="${x < mn}"><c:set var="mn" value="${x}" /></c:if>
        <c:if test="${x > mx}"><c:set var="mx" value="${x}" /></c:if>
        <c:if test="${st.last}"><c:set var="ultimo" value="${x}" /></c:if>
    </c:forEach>
    <c:set var="faixa" value="${mx - mn}" />
    <c:set var="pts" value="" />
    <c:set var="cxFinal" value="0" />
    <c:set var="cyFinal" value="0" />
    <c:forEach var="v" items="${vs}" varStatus="st">
        <c:set var="x" value="${fn:trim(v) + 0}" />
        <c:set var="px" value="${pad + st.index * (w - 2 * pad) / (n - 1)}" />
        <c:choose>
            <c:when test="${faixa > 0}"><c:set var="py" value="${pad + (h - 2 * pad) * (1 - (x - mn) / faixa)}" /></c:when>
            <c:otherwise><c:set var="py" value="${h / 2}" /></c:otherwise>
        </c:choose>
        <fmt:formatNumber var="fx" value="${px}" pattern="0.0" groupingUsed="false" />
        <fmt:formatNumber var="fy" value="${py}" pattern="0.0" groupingUsed="false" />
        <c:set var="fx" value="${fn:replace(fx, ',', '.')}" />
        <c:set var="fy" value="${fn:replace(fy, ',', '.')}" />
        <c:set var="pts" value="${pts}${empty pts ? '' : ' '}${fx},${fy}" />
        <c:set var="cxFinal" value="${fx}" />
        <c:set var="cyFinal" value="${fy}" />
    </c:forEach>
    <c:set var="modoRolagem" value="${rolagem == true}" />
    <c:set var="modoAnima" value="${empty anima or anima}" />
    <c:set var="classeTom" value="${empty tom ? '' : 'sh-tom--'}${tom}" />
    <c:set var="classeModo" value="${modoRolagem ? 'sh-desenhar' : (modoAnima ? 'sh-spark--anima' : '')}" />
    <c:set var="textoAlt" value="${not empty rotulo ? rotulo : 'Tendência'} (${n} pontos, de ${primeiro} até ${ultimo})" />
    <svg class="sh-spark <c:out value='${classeTom} ${classeModo} ${classe}'/>"
        width="<c:out value='${w}'/>" height="<c:out value='${h}'/>" viewBox="0 0 <c:out value='${w}'/> <c:out value='${h}'/>"
        role="img" focusable="false" aria-label="<c:out value='${textoAlt}'/>">
        <c:if test="${area == true}">
            <polygon class="sh-spark__area" points="<c:out value='${pts}'/> <c:out value='${w - pad}'/>,<c:out value='${h}'/> <c:out value='${pad}'/>,<c:out value='${h}'/>"/>
        </c:if>
        <polyline class="sh-spark__linha ${modoRolagem ? 'sh-traco' : ''}" pathLength="1" points="<c:out value='${pts}'/>"/>
        <circle class="sh-spark__ponto" cx="<c:out value='${cxFinal}'/>" cy="<c:out value='${cyFinal}'/>" r="2.4"/>
    </svg>
</c:if>
