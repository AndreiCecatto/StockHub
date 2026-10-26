/*
 * StockHub: telas de relatório (lote 5). Carregado por estoque-baixo.jsp, unidades.jsp e valor-estoque.jsp.
 *   [data-sh-imprimir]   botão que abre a impressão do navegador (o visual de impressão vem de css/impressao.css)
 * Sem JavaScript o botão fica oculto (atributo hidden na JSP) e o usuário ainda pode imprimir com Ctrl+P.
 */
(function () {
  'use strict';

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
})();
