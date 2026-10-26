<%@ tag description="Cabeçalho da página: h1, uma linha de descrição e as ações da página à direita" body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:cabecalhoPagina titulo="Produtos" descricao="Encontre e movimente seus itens.">
             <a class="btn btn-primary" href="...">Novo produto</a>
         </sh:cabecalhoPagina>
    O corpo do tag são os botões de ação (alinhados à direita; no celular ocupam a largura toda).
    Um h1 por página: este é o único.
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<%@ attribute name="descricao" required="false" type="java.lang.String" %>
<div class="sh-page-header">
    <div class="sh-page-header__texto">
        <h1 class="sh-page-header__titulo"><c:out value="${titulo}" /></h1>
        <c:if test="${not empty descricao}">
            <p class="sh-page-header__desc"><c:out value="${descricao}" /></p>
        </c:if>
    </div>
    <div class="sh-page-header__acoes">
        <jsp:doBody />
    </div>
</div>
