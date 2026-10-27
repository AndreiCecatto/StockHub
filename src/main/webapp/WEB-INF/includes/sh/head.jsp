<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Conteúdo do <head> das telas do novo design. Incluído pelos layouts (tags/layout.tag e tags/layoutPublico.tag).
    Recebe o título pelo atributo de request "shTitulo". Tudo local: nenhum recurso de CDN.
    Ordem importa: tema-init.js e navegacao-init.js (síncronos) antes do CSS, para não piscar o tema errado e para
    registrar a transição entre páginas a tempo (o evento pagereveal dispara antes da primeira pintura).
    O <link rel="expect"> segura a primeira pintura até o <main> existir, para a transição não pegar uma página pela metade.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="color-scheme" content="light dark">
<meta name="theme-color" content="#F3EDDF" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#1A1612" media="(prefers-color-scheme: dark)">
<title><c:out value="${shTitulo}" /> - StockHub</title>
<link rel="icon" type="image/svg+xml" href="${ctx}/img/favicon.svg">
<script src="${ctx}/js/tema-init.js"></script>
<script src="${ctx}/js/navegacao-init.js"></script>
<link rel="expect" href="#conteudo" blocking="render">
<link rel="preload" href="${ctx}/vendor/inter/inter-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="preload" href="${ctx}/vendor/fraunces/fraunces-latin-soft-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="${ctx}/vendor/bootstrap-5.3.8/css/bootstrap.min.css">
<link rel="stylesheet" href="${ctx}/vendor/bootstrap-icons-1.11.3/bootstrap-icons.min.css">
<link rel="stylesheet" href="${ctx}/css/tokens.css">
<link rel="stylesheet" href="${ctx}/css/layout.css">
<link rel="stylesheet" href="${ctx}/css/componentes.css">
<link rel="stylesheet" href="${ctx}/css/identidade.css">
<link rel="stylesheet" href="${ctx}/css/paleta.css">
<link rel="stylesheet" href="${ctx}/css/impressao.css" media="print">
<script defer src="${ctx}/vendor/bootstrap-5.3.8/js/bootstrap.bundle.min.js"></script>
<script defer src="${ctx}/js/nucleo.js"></script>
<script defer src="${ctx}/js/toast.js"></script>
<script defer src="${ctx}/js/atalhos.js"></script>
<script defer src="${ctx}/js/paleta.js"></script>
<script defer src="${ctx}/js/formularios.js"></script>
<script defer src="${ctx}/js/confirmar-exclusao.js"></script>
<script defer src="${ctx}/js/numeros.js"></script>
<script defer src="${ctx}/js/humano.js"></script>
<script defer src="${ctx}/js/navegacao.js"></script>
