/*
 * StockHub v3: números (seção "v3: central de operações" de docs/design-system.md).
 *  - Contagem animada: todo elemento com data-contar="123.45" conta de 0 até o valor UMA vez, na primeira vez em que
 *    aparece na viewport (já visível ao carregar: conta na hora; mais abaixo na página: conta quando a rolagem chegar).
 *    Formatos (data-formato): "inteiro" (padrão), "decimal" (1 casa), "moeda" (R$ 1.234,56) e "percentual" (42 %),
 *    sempre em pt-BR. Duração: data-contar-duracao em ms (padrão 900; telas de apresentação podem usar 1400).
 *    O texto que a JSP já escreveu é o valor final: sem JS, sem IntersectionObserver ou com "menos movimento"
 *    nada muda. Na impressão, os que ainda não contaram mostram o valor final.
 *  - Destaque do item novo: [data-destacar-primeiro] marca o primeiro filho com a classe sh-novo.
 * API: SH.animarNumeros(raiz) (para conteúdo inserido depois, como os cartões que o início carrega por fetch).
 */
(function () {
  'use strict';

  var SH = window.SH || (window.SH = {});
  var DURACAO_PADRAO = 900;

  function reduz() {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  }

  /* ---------- Contagem animada ---------- */
  var formatadores = null;
  if (window.Intl && window.Intl.NumberFormat) {
    var decimal = new window.Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    var inteiro = new window.Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
    formatadores = {
      inteiro: inteiro,
      decimal: decimal,
      moeda: new window.Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }),
      percentual: { format: function (n) { return inteiro.format(n) + ' %'; } }
    };
  }

  function facilitarSaida(t) {
    return 1 - Math.pow(1 - t, 4); // desaceleração forte, sem overshoot
  }

  function contar(elemento, alvo, formatador, duracao) {
    var inicio = null;
    function quadro(agora) {
      if (inicio === null) {
        inicio = agora;
      }
      var t = Math.min(1, (agora - inicio) / duracao);
      elemento.textContent = formatador.format(alvo * facilitarSaida(t));
      if (t < 1) {
        window.requestAnimationFrame(quadro);
      } else {
        elemento.textContent = formatador.format(alvo);
      }
    }
    window.requestAnimationFrame(quadro);
  }

  function nomeDoFormato(elemento) {
    var f = elemento.getAttribute('data-formato');
    return f === 'moeda' || f === 'decimal' || f === 'percentual' ? f : 'inteiro';
  }

  function comecar(elemento) {
    if (elemento.getAttribute('data-contado') === 'sim') {
      return;
    }
    elemento.setAttribute('data-contado', 'sim');
    var alvo = parseFloat(elemento.getAttribute('data-contar'));
    var duracao = parseInt(elemento.getAttribute('data-contar-duracao'), 10) || DURACAO_PADRAO;
    contar(elemento, alvo, formatadores[nomeDoFormato(elemento)], duracao);
  }

  var observador = null;
  if (window.IntersectionObserver) {
    observador = new window.IntersectionObserver(
      function (entradas) {
        for (var i = 0; i < entradas.length; i++) {
          if (entradas[i].isIntersecting) {
            observador.unobserve(entradas[i].target);
            comecar(entradas[i].target);
          }
        }
      },
      { threshold: 0.4 }
    );
  }

  function visivelAgora(elemento) {
    var caixa = elemento.getBoundingClientRect();
    var altura = window.innerHeight || document.documentElement.clientHeight;
    return caixa.top < altura * 0.92 && caixa.bottom > 0;
  }

  /** Prepara todo [data-contar] dentro de "raiz" (o documento, por padrão). Cada elemento conta uma vez só. */
  function animarNumeros(raiz) {
    if (!formatadores || reduz() || !window.requestAnimationFrame) {
      return;
    }
    var numeros = (raiz || document).querySelectorAll('[data-contar]');
    for (var i = 0; i < numeros.length; i++) {
      var el = numeros[i];
      if (el.hasAttribute('data-contado')) {
        continue;
      }
      var alvo = parseFloat(el.getAttribute('data-contar'));
      if (!isFinite(alvo) || alvo <= 0) {
        el.setAttribute('data-contado', 'sim');
        continue;
      }
      var formatador = formatadores[nomeDoFormato(el)];
      el.textContent = formatador.format(0);
      if (!observador || visivelAgora(el)) {
        comecar(el);
      } else {
        el.setAttribute('data-contado', 'aguardando');
        observador.observe(el);
      }
    }
  }

  // Impressão: o que ainda não contou mostra o valor final
  window.addEventListener('beforeprint', function () {
    if (!formatadores) {
      return;
    }
    var pendentes = document.querySelectorAll('[data-contar][data-contado="aguardando"]');
    for (var i = 0; i < pendentes.length; i++) {
      var alvo = parseFloat(pendentes[i].getAttribute('data-contar'));
      pendentes[i].textContent = formatadores[nomeDoFormato(pendentes[i])].format(alvo);
    }
  });

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
