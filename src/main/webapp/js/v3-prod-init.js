/*
 * StockHub v3: marca a página de produtos como "com JavaScript" e aplica a densidade salva ANTES da primeira pintura.
 * Síncrono e minúsculo (como tema-init.js). Classe html.v3-js: mostra o que é só de JS (.v3-so-js) e esconde o que o JS substitui
 * (.v3-so-semjs). Atributo html[data-v3-densidade]: "compacta" ou "confortavel" (preferência em localStorage "sh-prod-densidade").
 */
(function () {
  'use strict';
  var raiz = document.documentElement;
  raiz.classList.add('v3-js');
  var d = 'confortavel';
  try {
    if (window.localStorage.getItem('sh-prod-densidade') === 'compacta') {
      d = 'compacta';
    }
  } catch (erro) {
    // sem armazenamento: usa o padrão
  }
  raiz.setAttribute('data-v3-densidade', d);
})();
