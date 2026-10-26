/*
 * StockHub: núcleo do JavaScript próprio (vanilla, sem build).
 * Cria o objeto global SH com utilitários que os outros arquivos usam:
 *   SH.armazenar     leitura e gravação segura em localStorage
 *   SH.reduzMovimento()  true se o usuário pediu menos animação (prefers-reduced-motion)
 *   SH.contexto      caminho da aplicação (context path)
 *   SH.anunciar(t)   fala um texto para leitores de tela, sem mexer no foco
 * e cuida do tema (claro, escuro, sistema) e da barra lateral recolhível.
 * Regra do projeto: nunca montar HTML com dado do usuário (usar textContent e createElement).
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var raiz = document.documentElement;

  /* ---------- Armazenamento (sempre dentro de try/catch) ---------- */
  SH.armazenar = {
    ler: function (chave) {
      try {
        return window.localStorage.getItem(chave);
      } catch (erro) {
        return null;
      }
    },
    gravar: function (chave, valor) {
      try {
        window.localStorage.setItem(chave, valor);
      } catch (erro) {
        // sem armazenamento: a preferência vale só até recarregar
      }
    }
  };

  /* ---------- Menos movimento ---------- */
  var consultaMovimento = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
  SH.reduzMovimento = function () {
    return !!(consultaMovimento && consultaMovimento.matches);
  };

  SH.contexto = document.body ? document.body.getAttribute('data-sh-ctx') || '' : '';

  /* ---------- Anúncio para leitores de tela ---------- */
  SH.anunciar = function (texto) {
    var regiao = document.getElementById('sh-anuncio');
    if (!regiao) {
      return;
    }
    regiao.textContent = '';
    // o intervalo faz o leitor perceber a troca mesmo se o texto for igual ao anterior
    window.setTimeout(function () {
      regiao.textContent = texto;
    }, 60);
  };

  /* ---------- Tema ---------- */
  var consultaEscuro = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function temaEfetivo(escolha) {
    if (escolha === 'claro') {
      return 'light';
    }
    if (escolha === 'escuro') {
      return 'dark';
    }
    return consultaEscuro && consultaEscuro.matches ? 'dark' : 'light';
  }

  function marcarOpcoesDeTema(escolha) {
    var botoes = document.querySelectorAll('[data-sh-tema-opcao]');
    for (var i = 0; i < botoes.length; i++) {
      var ativo = botoes[i].getAttribute('data-sh-tema-opcao') === escolha;
      botoes[i].setAttribute('aria-pressed', ativo ? 'true' : 'false');
    }
  }

  function aplicarTema(escolha, salvar) {
    raiz.setAttribute('data-bs-theme', temaEfetivo(escolha));
    raiz.setAttribute('data-sh-tema', escolha);
    marcarOpcoesDeTema(escolha);
    if (salvar) {
      SH.armazenar.gravar('sh-tema', escolha);
    }
  }

  SH.tema = {
    atual: function () {
      return raiz.getAttribute('data-sh-tema') || 'sistema';
    },
    aplicar: function (escolha) {
      aplicarTema(escolha, true);
    }
  };

  marcarOpcoesDeTema(SH.tema.atual());

  if (consultaEscuro) {
    var seguirSistema = function () {
      if (SH.tema.atual() === 'sistema') {
        aplicarTema('sistema', false);
      }
    };
    if (consultaEscuro.addEventListener) {
      consultaEscuro.addEventListener('change', seguirSistema);
    } else if (consultaEscuro.addListener) {
      consultaEscuro.addListener(seguirSistema); // navegadores antigos
    }
  }

  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-tema-opcao]') : null;
    if (!botao) {
      return;
    }
    var escolha = botao.getAttribute('data-sh-tema-opcao');
    if (escolha === 'claro' || escolha === 'escuro' || escolha === 'sistema') {
      SH.tema.aplicar(escolha);
      var nomes = { claro: 'Tema claro', escuro: 'Tema escuro', sistema: 'Tema do sistema' };
      SH.anunciar(nomes[escolha] + ' ativado.');
    }
  });

  /* ---------- Barra lateral recolhível ---------- */
  function atualizarBotaoLateral() {
    var recolhida = raiz.getAttribute('data-sh-lateral') === 'recolhida';
    var botoes = document.querySelectorAll('[data-sh-alternar-lateral]');
    for (var i = 0; i < botoes.length; i++) {
      var rotulo = botoes[i].querySelector('.sh-lateral__alternar-rotulo');
      var texto = recolhida ? 'Expandir menu' : 'Recolher menu';
      if (rotulo) {
        rotulo.textContent = texto;
      }
      botoes[i].setAttribute('title', texto);
    }
  }

  atualizarBotaoLateral();

  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-alternar-lateral]') : null;
    if (!botao) {
      return;
    }
    var vaiRecolher = raiz.getAttribute('data-sh-lateral') !== 'recolhida';
    var novo = vaiRecolher ? 'recolhida' : 'aberta';
    raiz.setAttribute('data-sh-lateral', novo);
    SH.armazenar.gravar('sh-lateral', novo);
    atualizarBotaoLateral();
  });
})();
