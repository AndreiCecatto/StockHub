/*
 * Cadastro: mostra no seletor de ramo só os grupos que combinam com o tipo de loja escolhido (RN-44).
 * Sem JavaScript o seletor mostra todos os ramos e o servidor recusa combinações erradas com mensagem.
 */
(function () {
  'use strict';
  var bloco = document.querySelector('[data-sh-tipos]');
  if (!bloco) return;
  var select = bloco.querySelector('select[name="subtipo"]');
  var radios = bloco.querySelectorAll('input[name="tipoLoja"]');
  var inicial = select.getAttribute('data-selecionado') || '';

  function tipoAtual() {
    var marcado = bloco.querySelector('input[name="tipoLoja"]:checked');
    return marcado ? marcado.value : 'ESTOQUE';
  }

  function filtrar() {
    var tipo = tipoAtual();
    var valorAtual = select.value;
    Array.prototype.forEach.call(select.querySelectorAll('optgroup'), function (grupo) {
      var ok = (grupo.getAttribute('data-tipos') || '').split(' ').indexOf(tipo) !== -1;
      grupo.hidden = !ok;
      grupo.disabled = !ok;
    });
    var opcao = select.options[select.selectedIndex];
    if (opcao && opcao.parentNode.disabled) select.value = '';
    else if (valorAtual === '' && inicial) select.value = inicial;
  }

  Array.prototype.forEach.call(radios, function (r) { r.addEventListener('change', filtrar); });
  if (inicial) select.value = inicial;
  filtrar();
})();
