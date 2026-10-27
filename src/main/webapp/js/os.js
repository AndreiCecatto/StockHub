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

  // Máscara leve da placa: maiúsculas, só letras/números, hífen opcional (ABC-1234 ou ABC1D23).
  document.addEventListener('input', function (evento) {
    var campo = evento.target;
    if (!campo || !campo.hasAttribute || !campo.hasAttribute('data-os-placa')) {
      return;
    }
    var limpo = campo.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
    if (/^[A-Z]{3}[0-9]{4}$/.test(limpo) && campo.value.indexOf('-') !== -1) {
      limpo = limpo.slice(0, 3) + '-' + limpo.slice(3);
    }
    campo.value = limpo;
  });

  document.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[data-os-imprimir]') : null;
    if (alvo) {
      window.print();
    }
  });
})();
