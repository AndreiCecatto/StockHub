<%@ tag description="Mini-gráfico de barras: SVG desenhado no servidor, com texto alternativo" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Uso: <sh:miniBarras valores="4,9,14,22,31,17,3" rotulo="Saídas por dia, última semana" />
         <sh:miniBarras valores="..." largura="200" altura="48" destaque="maior" tom="baixo" />
    valores  : números (0 ou mais) separados por vírgula, do mais antigo ao mais recente (com ponto decimal).
    rotulo   : texto alternativo (role="img"). Sem ele, a tag escreve "Barras (N valores, máximo M)".
    largura / altura : tamanho em pixels (padrão 120 x 32).
    destaque : "ultimo" (padrão), "maior" ou "nenhum": a barra em destaque fica na cor cheia; as demais, esmaecidas.
    tom      : "" (acento), normal, baixo ou sem (classes sh-tom--*).
    anima    : true (padrão) as barras sobem uma vez ao aparecer, em sequência. rolagem="true": sobem conforme a rolagem
               (kit de rolagem: contêiner .sh-desenhar, barras .sh-barra). Com "menos movimento": sem animação.
    classe   : classes extras no <svg> (por exemplo sh-minibarras--bloco).
--%>
<%@ attribute name="valores" required="true" type="java.lang.String" %>
<%@ attribute name="rotulo" required="false" type="java.lang.String" %>
<%@ attribute name="largura" required="false" type="java.lang.Integer" %>
<%@ attribute name="altura" required="false" type="java.lang.Integer" %>
<%@ attribute name="destaque" required="false" type="java.lang.String" %>
<%@ attribute name="tom" required="false" type="java.lang.String" %>
<%@ attribute name="anima" required="false" type="java.lang.Boolean" %>
<%@ attribute name="rolagem" required="false" type="java.lang.Boolean" %>
<%@ attribute name="classe" required="false" type="java.lang.String" %>
<c:set var="w" value="${empty largura ? 120 : largura}" />
<c:set var="h" value="${empty altura ? 32 : altura}" />
<c:set var="vs" value="${fn:split(valores, ',')}" />
<c:set var="n" value="${fn:length(vs)}" />
<c:if test="${n >= 1}">
    <c:set var="folga" value="2" />
    <c:set var="mx" value="0" />
    <c:set var="idxMaior" value="0" />
    <c:forEach var="v" items="${vs}" varStatus="st">
        <c:set var="x" value="${fn:trim(v) + 0}" />
        <c:if test="${x > mx}"><c:set var="mx" value="${x}" /><c:set var="idxMaior" value="${st.index}" /></c:if>
    </c:forEach>
    <c:set var="larguraBarra" value="${(w - folga * (n - 1)) / n}" />
    <fmt:formatNumber var="lb" value="${larguraBarra}" pattern="0.0" groupingUsed="false" />
    <c:set var="lb" value="${fn:replace(lb, ',', '.')}" />
    <c:set var="modoRolagem" value="${rolagem == true}" />
    <c:set var="modoAnima" value="${empty anima or anima}" />
    <c:set var="modoDestaque" value="${empty destaque ? 'ultimo' : destaque}" />
    <c:set var="classeTom" value="${empty tom ? '' : 'sh-tom--'}${tom}" />
    <c:set var="classeModo" value="${modoRolagem ? 'sh-desenhar' : (modoAnima ? 'sh-minibarras--anima' : '')}" />
    <c:set var="textoAlt" value="${not empty rotulo ? rotulo : 'Barras'} (${n} valores, máximo ${mx})" />
    <svg class="sh-minibarras <c:out value='${classeTom} ${classeModo} ${classe}'/>"
        width="<c:out value='${w}'/>" height="<c:out value='${h}'/>" viewBox="0 0 <c:out value='${w}'/> <c:out value='${h}'/>"
        role="img" focusable="false" aria-label="<c:out value='${textoAlt}'/>">
        <c:forEach var="v" items="${vs}" varStatus="st">
            <c:set var="x" value="${fn:trim(v) + 0}" />
            <c:set var="alt" value="${mx > 0 ? (x / mx) * h : 0}" />
            <c:if test="${alt < 1.5}"><c:set var="alt" value="1.5" /></c:if>
            <fmt:formatNumber var="fh" value="${alt}" pattern="0.0" groupingUsed="false" />
            <fmt:formatNumber var="fy" value="${h - alt}" pattern="0.0" groupingUsed="false" />
            <fmt:formatNumber var="fx" value="${st.index * (larguraBarra + folga)}" pattern="0.0" groupingUsed="false" />
            <c:set var="destacada" value="${modoDestaque == 'maior' ? st.index == idxMaior : (modoDestaque == 'ultimo' ? st.last : false)}" />
            <rect class="sh-minibarras__barra ${destacada ? 'sh-minibarras__barra--destaque' : ''} ${modoRolagem ? 'sh-barra' : ''}"
                x="<c:out value='${fn:replace(fx, ",", ".")}'/>" y="<c:out value='${fn:replace(fy, ",", ".")}'/>"
                width="<c:out value='${lb}'/>" height="<c:out value='${fn:replace(fh, ",", ".")}'/>" rx="1.5"/>
        </c:forEach>
    </svg>
</c:if>
