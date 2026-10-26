<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    As três opções de tema (Sistema, Claro, Escuro). Quem inclui define o contêiner e a classe dos botões
    (parâmetro "classe"). js/nucleo.js marca aria-pressed no botão do tema atual, grava a escolha em
    localStorage e troca o data-bs-theme do <html>.
--%>
<button type="button" class="<c:out value='${param.classe}'/> sh-tema-opcao" data-sh-tema-opcao="sistema" aria-pressed="false">
    <i class="bi bi-circle-half" aria-hidden="true"></i> Sistema
    <i class="bi bi-check-lg sh-tema-opcao__visto" aria-hidden="true"></i>
</button>
<button type="button" class="<c:out value='${param.classe}'/> sh-tema-opcao" data-sh-tema-opcao="claro" aria-pressed="false">
    <i class="bi bi-sun" aria-hidden="true"></i> Claro
    <i class="bi bi-check-lg sh-tema-opcao__visto" aria-hidden="true"></i>
</button>
<button type="button" class="<c:out value='${param.classe}'/> sh-tema-opcao" data-sh-tema-opcao="escuro" aria-pressed="false">
    <i class="bi bi-moon-stars" aria-hidden="true"></i> Escuro
    <i class="bi bi-check-lg sh-tema-opcao__visto" aria-hidden="true"></i>
</button>
