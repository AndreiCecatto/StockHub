/*
 * StockHub: aplica o tema e o estado da barra lateral ANTES de a página ser pintada.
 * Carregado de forma síncrona no <head>, antes do CSS, para não piscar o tema errado.
 * Escolha salva em localStorage ("sh-tema": sistema | claro | escuro; "sh-lateral": aberta | recolhida).
 * Sem JavaScript, vale prefers-color-scheme por CSS (ver css/tokens.css).
 */
(function () {
  'use strict';
  var raiz = document.documentElement;
  var tema = 'sistema';
  var lateral = null;
  try {
    tema = localStorage.getItem('sh-tema') || 'sistema';
    lateral = localStorage.getItem('sh-lateral');
  } catch (erro) {
    // armazenamento bloqueado (janela privada, cookies desligados): segue com os padrões
  }
  if (tema !== 'claro' && tema !== 'escuro') {
    tema = 'sistema';
  }
  var escuro = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  var efetivo = tema === 'escuro' || (tema === 'sistema' && escuro) ? 'dark' : 'light';
  raiz.setAttribute('data-bs-theme', efetivo);
  raiz.setAttribute('data-sh-tema', tema);
  // Sem preferência salva, a barra lateral abre recolhida abaixo de 1200 px
  var recolhida = lateral === 'recolhida' || (lateral !== 'aberta' && window.innerWidth < 1200);
  raiz.setAttribute('data-sh-lateral', recolhida ? 'recolhida' : 'aberta');
})();
