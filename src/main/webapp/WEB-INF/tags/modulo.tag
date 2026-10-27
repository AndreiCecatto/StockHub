<%@ tag description="Mostra o conteúdo só se o módulo estiver ligado para o tipo da loja (ESTOQUE, VENDAS ou OS)" body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:modulo nome="VENDAS"> ...item de menu... </sh:modulo>
    ESTOQUE é sempre ligado; VENDAS para COMERCIO e MISTO; OS para OFICINA e MISTO (RN-43).
    Lê o mapa "modulos" que o GlobalModelAdvice põe em toda requisição. Sem o mapa (página sem controller), só ESTOQUE aparece.
    Esconde apenas o item de menu: a rota continua existindo (o tipo não é controle de acesso).
--%>
<%@ attribute name="nome" required="true" type="java.lang.String" %>
<c:if test="${nome == 'ESTOQUE' or modulos[nome]}"><jsp:doBody /></c:if>
