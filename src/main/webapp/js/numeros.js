/*
 * StockHub: números (seção 5 de docs/design-system.md).
 *  - Contagem animada: todo elemento com data-contar="123.45" conta de 0 até o valor em 700 ms (ease-out),
 *    só na primeira exibição da página. data-formato="inteiro" (padrão) ou "moeda" (R$ 1.234,56, pt-BR).
 *    O texto que a JSP já escreveu é o valor final: sem JS ou com "menos movimento" nada muda.
 *  - Destaque do item novo: [data-destacar-primeiro] marca o primeiro filho com a classe sh-novo.
 */
(function () {
  'use strict';

  var SH = window.SH || (window.SH = {});
  var DURACAO = 700;

  function reduz() {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  }

  /* ---------- Contagem animada ---------- */
  var formatadores = null;
  if (window.Intl && window.Intl.NumberFormat) {
    formatadores = {
      inteiro: new window.Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }),
      moeda: new window.Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
    };
  }

  function facilitarSaida(t) {
    return 1 - Math.pow(1 - t, 3); // ease-out cúbico
  }

  function contar(elemento, alvo, formatador) {
    var inicio = null;
    function quadro(agora) {
      if (inicio === null) {
        inicio = agora;
      }
      var t = Math.min(1, (agora - inicio) / DURACAO);
      elemento.textContent = formatador.format(alvo * facilitarSaida(t));
      if (t < 1) {
        window.requestAnimationFrame(quadro);
      } else {
        elemento.textContent = formatador.format(alvo);
      }
    }
    elemento.textContent = formatador.format(0);
    window.requestAnimationFrame(quadro);
  }

  /** Anima todo [data-contar] dentro de "raiz" (o documento, por padrão). Cada elemento anima uma vez só. */
  function animarNumeros(raiz) {
    if (!formatadores || reduz() || !window.requestAnimationFrame) {
      return;
    }
    var numeros = (raiz || document).querySelectorAll('[data-contar]');
    for (var i = 0; i < numeros.length; i++) {
      if (numeros[i].getAttribute('data-contado') === 'sim') {
        continue;
      }
      var alvo = parseFloat(numeros[i].getAttribute('data-contar'));
      var formato = numeros[i].getAttribute('data-formato') === 'moeda' ? 'moeda' : 'inteiro';
      numeros[i].setAttribute('data-contado', 'sim');
      if (isFinite(alvo) && alvo > 0) {
        contar(numeros[i], alvo, formatadores[formato]);
      }
    }
  }

  SH.animarNumeros = animarNumeros;
  window.SH = SH;
  animarNumeros(document);

  /* ---------- Destaque do item recém-criado ---------- */
  var listas = document.querySelectorAll('[data-destacar-primeiro]');
  for (var j = 0; j < listas.length; j++) {
    var primeiro = listas[j].firstElementChild;
    if (primeiro) {
      primeiro.classList.add('sh-novo');
    }
  }
})();
