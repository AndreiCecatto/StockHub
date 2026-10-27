/*
 * StockHub v3: detalhe de usabilidade das telas de entrada (login e cadastro).
 * Avisa, em uma região aria-live, quando o Caps Lock está ligado enquanto a pessoa digita a senha.
 * É só conforto: sem JavaScript o formulário funciona igual. Texto por textContent, nunca HTML.
 */
(function () {
  'use strict';

  var senha = document.getElementById('senha');
  var aviso = document.querySelector('[data-sh-caps]');
  if (!senha || !aviso) {
    return;
  }

  var ligado = false;

  function mostrar(estado) {
    if (estado === ligado) {
      return;
    }
    ligado = estado;
    aviso.textContent = '';
    if (estado) {
      var icone = document.createElement('i');
      icone.className = 'bi bi-capslock-fill';
      icone.setAttribute('aria-hidden', 'true');
      var texto = document.createElement('span');
      texto.textContent = 'Caps Lock ativado';
      aviso.appendChild(icone);
      aviso.appendChild(texto);
    }
  }

  function checar(evento) {
    if (evento.getModifierState) {
      mostrar(evento.getModifierState('CapsLock'));
    }
  }

  senha.addEventListener('keydown', checar);
  senha.addEventListener('keyup', checar);
  senha.addEventListener('blur', function () {
    mostrar(false);
  });
})();
