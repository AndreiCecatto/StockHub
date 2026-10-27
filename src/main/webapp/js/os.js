/*
 * StockHub: ordens de serviço. Pede confirmação antes de cancelar (form[data-os-confirmar]) e imprime
 * (botão [data-os-imprimir]). Sem JavaScript o cancelamento envia direto e o navegador imprime pelo menu.
 */
(function () {
  'use strict';

  document.addEventListener('submit', function (evento) {
    var form = evento.target;
    if (!form || !form.hasAttribute || !form.hasAttribute('data-os-confirmar')) {
      return;
    }
    if (!window.confirm(form.getAttribute('data-os-confirmar'))) {
      evento.preventDefault();
    }
  });

  document.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[data-os-imprimir]') : null;
    if (alvo) {
      window.print();
    }
  });
})();
