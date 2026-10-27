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

  // Máscara leve da placa. Ao digitar: maiúsculas e só letras, números e hífen (o valor só é reescrito
  // se mudou, preservando o cursor). Ao sair do campo (blur): ABC1234 vira ABC-1234; ABC1D23 não muda.
  function ehPlaca(campo) {
    return campo && campo.hasAttribute && campo.hasAttribute('data-os-placa');
  }

  document.addEventListener('input', function (evento) {
    var campo = evento.target;
    if (!ehPlaca(campo)) {
      return;
    }
    var original = campo.value;
    var ajustado = original.toUpperCase().replace(/[^A-Z0-9-]/g, '');
    if (ajustado === original) {
      return;
    }
    var inicio = campo.selectionStart;
    var fim = campo.selectionEnd;
    campo.value = ajustado;
    if (inicio !== null && fim !== null) {
      var diferenca = original.length - ajustado.length;
      campo.setSelectionRange(Math.max(0, inicio - diferenca), Math.max(0, fim - diferenca));
    }
  });

  document.addEventListener('focusout', function (evento) {
    var campo = evento.target;
    if (!ehPlaca(campo)) {
      return;
    }
    var limpo = campo.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    var novo = /^[A-Z]{3}[0-9]{4}$/.test(limpo) ? limpo.slice(0, 3) + '-' + limpo.slice(3) : campo.value;
    if (novo !== campo.value) {
      campo.value = novo;
    }
  });

  document.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[data-os-imprimir]') : null;
    if (alvo) {
      window.print();
    }
  });
})();
