/*
 * StockHub v3: relatórios (estoque-baixo, unidades, valor-estoque).
 *   [data-sh-imprimir]   botão que abre a impressão do navegador (o visual do papel vem de css/v3-rel.css)
 * Sem JavaScript o botão fica oculto (atributo hidden na JSP) e o usuário ainda pode imprimir com Ctrl+P.
 * Atalho: P imprime (só fora de campos, sem Ctrl/Alt/Meta e respeitando a chave de atalhos do sistema).
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var botoes = document.querySelectorAll('[data-sh-imprimir]');
  for (var i = 0; i < botoes.length; i++) {
    botoes[i].hidden = false;
  }

  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-imprimir]') : null;
    if (botao) {
      window.print();
    }
  });

  document.addEventListener('keydown', function (evento) {
    if (evento.defaultPrevented || evento.repeat || evento.ctrlKey || evento.altKey || evento.metaKey || evento.shiftKey) {
      return;
    }
    if (evento.key !== 'p' || !botoes.length) {
      return;
    }
    if (SH.armazenar && SH.armazenar.ler('sh-atalhos') === 'desligados') {
      return;
    }
    var alvo = evento.target;
    var t = alvo && alvo.tagName ? alvo.tagName.toLowerCase() : '';
    if (t === 'input' || t === 'textarea' || t === 'select' || (alvo && alvo.isContentEditable)) {
      return;
    }
    if (document.body.classList.contains('modal-open') || document.querySelector('dialog[open]')) {
      return;
    }
    evento.preventDefault();
    window.print();
  });
})();
