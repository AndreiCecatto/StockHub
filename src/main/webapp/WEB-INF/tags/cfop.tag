<%@ tag description="CFOP em linguagem comum (Tipo da operação), com o número como detalhe" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:cfop codigo="${it.cfop}" />   ou   <sh:cfop codigo="5102" semNumero="true" />
    Lugar único do mapeamento CFOP -> texto (compra, venda, devolução, remessa, conserto, bonificação, consignação etc.). Os 4 dígitos são o código oficial; o número aparece em letra pequena
    e no title (dica ao passar o mouse). Código desconhecido sai como "Operação código NNNN".
    Sempre escapado com c:out.
--%>
<%@ attribute name="codigo" required="false" type="java.lang.String" %>
<%@ attribute name="semNumero" required="false" type="java.lang.Boolean" %>
<c:set var="cod" value="${fn:trim(codigo)}" />
<c:choose>
    <c:when test="${empty cod}">&mdash;</c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${cod == '1102' or cod == '2102' or cod == '1101' or cod == '2101' or cod == '1403' or cod == '2403' or cod == '1401' or cod == '2401'}"><c:set var="texto" value="Compra para revenda" /></c:when>
            <c:when test="${cod == '5102' or cod == '6102' or cod == '5101' or cod == '6101' or cod == '5405' or cod == '6404' or cod == '5403' or cod == '6403'}"><c:set var="texto" value="Venda de mercadoria" /></c:when>
            <c:when test="${cod == '1556' or cod == '2556' or cod == '1407' or cod == '2407'}"><c:set var="texto" value="Compra para uso ou consumo" /></c:when>
            <c:when test="${cod == '1551' or cod == '2551' or cod == '1406' or cod == '2406'}"><c:set var="texto" value="Compra de bem para o ativo" /></c:when>
            <c:when test="${cod == '1202' or cod == '2202' or cod == '1411' or cod == '2411' or cod == '5202' or cod == '6202' or cod == '5411' or cod == '6411'}"><c:set var="texto" value="Devolução" /></c:when>
            <c:when test="${cod == '5915' or cod == '6915' or cod == '1916' or cod == '2916'}"><c:set var="texto" value="Remessa para conserto" /></c:when>
            <c:when test="${cod == '1949' or cod == '2949' or cod == '5949' or cod == '6949'}"><c:set var="texto" value="Outra operação" /></c:when>
            <c:when test="${cod == '5910' or cod == '6910' or cod == '1910' or cod == '2910'}"><c:set var="texto" value="Bonificação ou brinde" /></c:when>
            <c:when test="${cod == '1152' or cod == '2152' or cod == '5152' or cod == '6152' or cod == '5151' or cod == '6151'}"><c:set var="texto" value="Transferência entre lojas" /></c:when>
            <c:when test="${cod == '1904' or cod == '2904' or cod == '5904' or cod == '6904' or cod == '1917' or cod == '2917'}"><c:set var="texto" value="Remessa em consignação" /></c:when>
            <c:when test="${cod == '3102' or cod == '3101'}"><c:set var="texto" value="Compra para revenda (importação)" /></c:when>
            <c:when test="${cod == '5103' or cod == '6103' or cod == '5104' or cod == '6104' or cod == '5401' or cod == '6401' or cod == '5404' or cod == '6108' or cod == '5108'}"><c:set var="texto" value="Venda de mercadoria" /></c:when>
            <c:when test="${cod == '1201' or cod == '2201' or cod == '1410' or cod == '2410' or cod == '5201' or cod == '6201' or cod == '5410' or cod == '6410' or cod == '5206' or cod == '6206'}"><c:set var="texto" value="Devolução" /></c:when>
            <c:when test="${cod == '5916' or cod == '6916' or cod == '1915' or cod == '2915'}"><c:set var="texto" value="Retorno ou remessa de conserto" /></c:when>
            <c:when test="${cod == '5901' or cod == '6901' or cod == '1901' or cod == '2901'}"><c:set var="texto" value="Remessa para industrialização" /></c:when>
            <c:when test="${cod == '5902' or cod == '6902' or cod == '1902' or cod == '2902' or cod == '5903' or cod == '6903' or cod == '1903' or cod == '2903'}"><c:set var="texto" value="Retorno de industrialização" /></c:when>
            <c:when test="${cod == '5908' or cod == '6908' or cod == '1908' or cod == '2908' or cod == '5909' or cod == '6909' or cod == '1909' or cod == '2909'}"><c:set var="texto" value="Comodato (empréstimo de bem)" /></c:when>
            <c:when test="${cod == '5911' or cod == '6911' or cod == '1911' or cod == '2911'}"><c:set var="texto" value="Amostra grátis" /></c:when>
            <c:when test="${cod == '5912' or cod == '6912' or cod == '1912' or cod == '2912' or cod == '5913' or cod == '6913' or cod == '1913' or cod == '2913'}"><c:set var="texto" value="Remessa ou retorno de demonstração" /></c:when>
            <c:when test="${cod == '5917' or cod == '6917' or cod == '5918' or cod == '6918' or cod == '1918' or cod == '2918'}"><c:set var="texto" value="Remessa em consignação" /></c:when>
            <c:when test="${cod == '5920' or cod == '6920' or cod == '1920' or cod == '2920' or cod == '5921' or cod == '6921' or cod == '1921' or cod == '2921'}"><c:set var="texto" value="Vasilhame ou embalagem" /></c:when>
            <c:when test="${cod == '5927' or cod == '1927'}"><c:set var="texto" value="Perda ou quebra de mercadoria" /></c:when>
            <c:otherwise><c:set var="texto" value="Operação código ${cod}" /></c:otherwise>
        </c:choose>
        <span class="nfe-cfop" title="Código fiscal da operação (CFOP): ${fn:escapeXml(cod)}"><c:out value="${texto}" /><c:if test="${empty semNumero or not semNumero}"> <small class="nfe-cfop__num sh-mono">(<c:out value="${cod}" />)</small></c:if></span>
    </c:otherwise>
</c:choose>
