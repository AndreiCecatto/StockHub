/*
 * StockHub v3: motor de rolagem da apresentação pública, feito com GSAP + ScrollTrigger (vendor/gsap-3.15.0).
 * Entrega a mesma aparência do kit (css/rolagem.css), com o mesmo comportamento em todos os navegadores e um
 * pequeno suavizamento (inércia) no avanço das cenas:
 *  1. Cenas fixas (.sh-cena, a partir de 992 px de largura e 600 px de altura): escreve --sh-progresso (0 a 1) na cena,
 *     de quando o topo encosta embaixo da barra até a base encostar no fim da tela. O CSS da página faz o resto.
 *  2. Revelar (.sh-revelar e filhos de .sh-revelar-grupo): entram uma vez, em lote e escalonados, ao chegar na tela.
 * Não sequestra a rolagem (sem pin, sem wheel, sem preventDefault). Sem GSAP ou com "menos movimento", este arquivo não
 * faz nada: a classe html.ap-gsap só é ligada aqui, e sem ela o kit de CSS continua valendo (estado final sem JS).
 */
(function () {
  'use strict';

  var gsap = window.gsap;
  var ScrollTrigger = window.ScrollTrigger;
  if (!gsap || !ScrollTrigger || !window.matchMedia) {
    return;
  }
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }

  gsap.registerPlugin(ScrollTrigger);
  var raiz = document.documentElement;
  raiz.classList.add('ap-gsap'); // tira de cena as animações por timeline do CSS (ver css/apresentacao.css)

  /* ---------- 1. Cenas fixas ---------- */
  gsap.matchMedia().add('(min-width: 992px) and (min-height: 600px)', function () {
    var cenas = document.querySelectorAll('.sh-cena');
    for (var i = 0; i < cenas.length; i++) {
      conduzir(cenas[i]);
    }
    return function () {
      for (var j = 0; j < cenas.length; j++) {
        cenas[j].style.removeProperty('--sh-progresso');
      }
    };
  });

  function conduzir(cena) {
    var topo = parseFloat(window.getComputedStyle(cena).getPropertyValue('--sh-cena-topo')) || 0;
    var proxy = { p: 0 };
    gsap.to(proxy, {
      p: 1,
      ease: 'none',
      onUpdate: function () {
        cena.style.setProperty('--sh-progresso', proxy.p.toFixed(3));
      },
      scrollTrigger: { trigger: cena, start: 'top top+=' + topo, end: 'bottom bottom', scrub: 0.5 }
    });
  }

  /* ---------- 2. Revelar ao entrar na tela ---------- */
  var distancia = parseFloat(window.getComputedStyle(raiz).getPropertyValue('--sh-revelar-dist')) || 12;
  var alvos = document.querySelectorAll('.sh-revelar, .sh-revelar-grupo > *');

  function origem(el) {
    var c = el.classList;
    var lado = c.contains('sh-revelar--esquerda') ? -2 : c.contains('sh-revelar--direita') ? 2 : 0;
    var parado = lado !== 0 || c.contains('sh-revelar--escala') || c.contains('sh-revelar--fade');
    return { opacity: 0, x: lado * distancia, y: parado ? 0 : distancia, scale: c.contains('sh-revelar--escala') ? 0.96 : 1 };
  }

  for (var k = 0; k < alvos.length; k++) {
    gsap.set(alvos[k], origem(alvos[k]));
  }
  ScrollTrigger.batch(alvos, {
    start: 'top 90%',
    once: true,
    onEnter: function (lote) {
      gsap.to(lote, { opacity: 1, x: 0, y: 0, scale: 1, duration: 0.8, ease: 'power3.out', stagger: 0.09, clearProps: 'opacity,transform' });
    }
  });
})();
