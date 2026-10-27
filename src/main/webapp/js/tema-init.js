/*
 * StockHub: aplica o tema e o estado da barra lateral ANTES de a página ser pintada.
 * Carregado de forma síncrona no <head>, antes do CSS, para não piscar o tema errado.
 * Escolha salva em localStorage ("sh-tema": sistema | claro | escuro; "sh-lateral": aberta | recolhida).
 * Sem JavaScript, vale prefers-color-scheme por CSS (ver css/tokens.css).
 *
 * v3: também decide o modo do kit de rolagem (css/rolagem.css). Sem suporte a animation-timeline (Firefox), liga a
 * classe html.sh-rolagem-js, que esconde os elementos .sh-revelar até js/rolagem.js revelá-los. Se o script não chegar a
 * tempo (3 s), a classe sai e tudo aparece: nenhum conteúdo fica escondido por falha.
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

  // Kit de rolagem: plano B em JavaScript quando o navegador não tem animation-timeline
  var temTimeline = !!(window.CSS && window.CSS.supports && window.CSS.supports('animation-timeline: view()'));
  var reduz = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!temTimeline && !reduz && 'IntersectionObserver' in window) {
    raiz.classList.add('sh-rolagem-js');
    window.setTimeout(function () {
      if (raiz.getAttribute('data-sh-rolagem') !== 'pronta') {
        raiz.classList.remove('sh-rolagem-js');
      }
    }, 3000);
  }
})();
