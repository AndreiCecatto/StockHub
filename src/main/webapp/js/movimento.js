/*
 * StockHub: movimento opcional (seção 17 de docs/design-system.md). Progressivo: sem este script, a página aparece
 * inteira e funciona igual.
 *  1. Revela ao rolar: cartões e grades que estão ABAIXO da dobra começam escondidos e entram (fade + subida curta,
 *     escalonados) quando chegam à tela. O que já está visível ao carregar é animado só por CSS (css/movimento.css).
 *  2. Números de métrica (.sh-metric__valor) com inteiro puro ganham a contagem animada de js/numeros.js.
 * Com "menos movimento" (prefers-reduced-motion) nada disso roda.
 */
(function () {
  'use strict';

  var SH = window.SH || (window.SH = {});
  var reduz = SH.reduzMovimento
    ? SH.reduzMovimento()
    : !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  if (reduz) {
    return;
  }

  var altura = window.innerHeight || document.documentElement.clientHeight;

  /* ---------- 1. Revela ao rolar ---------- */
  if ('IntersectionObserver' in window) {
    var alvos = document.querySelectorAll(
      '.sh-main .sh-metricas > *, .sh-main .sh-grade > *, .sh-main .sh-secao > .sh-card, ' +
      '.sh-main .sh-secao > .sh-table-wrap, .sh-main [data-sh-revela]'
    );
    var observador = new window.IntersectionObserver(function (entradas) {
      var ordem = 0;
      for (var i = 0; i < entradas.length; i++) {
        var el = entradas[i].target;
        if (!entradas[i].isIntersecting) {
          continue;
        }
        observador.unobserve(el);
        el.style.setProperty('--sh-i', String(ordem++));
        el.classList.remove('sh-aguarda');
        el.classList.add('sh-chega');
        el.addEventListener('animationend', function limpar(e) {
          e.currentTarget.classList.remove('sh-chega');
          e.currentTarget.style.removeProperty('--sh-i');
          e.currentTarget.removeEventListener('animationend', limpar);
        });
      }
    }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' });

    for (var k = 0; k < alvos.length; k++) {
      var caixa = alvos[k].getBoundingClientRect();
      if (caixa.top > altura * 0.96) {
        alvos[k].classList.add('sh-aguarda');
        observador.observe(alvos[k]);
      }
    }
  }

  /* ---------- 2. Contagem nos números de métrica ---------- */
  var valores = document.querySelectorAll('.sh-metric__valor:not([data-contar])');
  var marcou = false;
  for (var j = 0; j < valores.length; j++) {
    var v = valores[j];
    var texto = v.textContent.trim();
    if (v.children.length === 0 && /^\d{1,3}(\.\d{3})*$|^\d+$/.test(texto)) {
      v.setAttribute('data-contar', texto.replace(/\./g, ''));
      v.setAttribute('data-formato', 'inteiro');
      marcou = true;
    }
  }
  if (marcou && SH.animarNumeros) {
    SH.animarNumeros(document);
  }
})();
