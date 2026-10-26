/*
 * StockHub: JavaScript do lote 3 (produtos e categorias). Vanilla, sem build.
 * Carregado só por produtos.jsp, categorias.jsp e produto-form.jsp (fragmento "scripts" do layout.tag).
 *
 * 1) Busca ao digitar (seção 4.8 de docs/design-system.md)
 *    form[data-busca-viva]       formulário GET de filtros; sem JS ele envia normalmente (Enter e botão Buscar)
 *    [data-regiao-viva]          trecho da página que é trocado pelo mesmo trecho da resposta do servidor
 *    [data-busca-esqueleto]      esqueleto mostrado só se a resposta demorar
 *    [data-busca-erro]           alerta "Não foi possível atualizar a lista" (com [data-busca-tentar])
 *    [data-contagem]             texto "N produtos" dentro da região, anunciado para leitores de tela
 *    Depois de 300 ms sem digitar (ou ao mudar um select ou chip) faz fetch GET na mesma URL e com os mesmos
 *    parâmetros do formulário, troca só a região, atualiza o endereço com history.replaceState e anuncia o total.
 *    O HTML trocado vem do próprio servidor (já escapado pelas JSPs); aqui nada é montado com dado do usuário
 *    e não se usa innerHTML: os nós são importados com DOMParser e importNode.
 *
 * 2) Máscara leve do preço: input[data-prod-preco] (type=number) ganha duas casas decimais ao sair do campo.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});

  /* ======================== 1) Busca ao digitar ======================== */
  var ATRASO_DIGITACAO = 300;
  var ATRASO_ESQUELETO = 350;

  function iniciarBuscaViva(form, regiao) {
    var esqueleto = document.querySelector('[data-busca-esqueleto]');
    var erro = document.querySelector('[data-busca-erro]');
    var tentar = document.querySelector('[data-busca-tentar]');
    var temporizador = null;
    var temporizadorEsqueleto = null;
    var controle = null;
    var sequencia = 0;

    function urlDoFormulario() {
      var params = new window.URLSearchParams();
      new window.FormData(form).forEach(function (valor, nome) {
        if (typeof valor === 'string') {
          params.append(nome, valor);
        }
      });
      return form.getAttribute('action') + '?' + params.toString();
    }

    var ultimaUrl = urlDoFormulario();

    function comecouCarregar() {
      regiao.classList.add('is-carregando');
      regiao.setAttribute('aria-busy', 'true');
      if (erro) {
        erro.hidden = true;
      }
      window.clearTimeout(temporizadorEsqueleto);
      temporizadorEsqueleto = window.setTimeout(function () {
        if (esqueleto) {
          esqueleto.hidden = false;
        }
      }, ATRASO_ESQUELETO);
    }

    function terminouCarregar() {
      window.clearTimeout(temporizadorEsqueleto);
      regiao.classList.remove('is-carregando');
      regiao.removeAttribute('aria-busy');
      if (esqueleto) {
        esqueleto.hidden = true;
      }
    }

    function trocarRegiao(nova) {
      while (regiao.firstChild) {
        regiao.removeChild(regiao.firstChild);
      }
      Array.prototype.slice.call(nova.childNodes).forEach(function (no) {
        regiao.appendChild(document.importNode(no, true));
      });
    }

    function carregar() {
      window.clearTimeout(temporizador);
      var url = urlDoFormulario();
      if (url === ultimaUrl && !regiao.classList.contains('is-falhou')) {
        return; // nada mudou desde a última lista mostrada
      }
      var minha = ++sequencia;
      if (controle && controle.abort) {
        controle.abort();
      }
      controle = window.AbortController ? new window.AbortController() : null;
      comecouCarregar();

      window
        .fetch(url, {
          credentials: 'same-origin',
          headers: { Accept: 'text/html' },
          signal: controle ? controle.signal : undefined
        })
        .then(function (resposta) {
          // Sessão expirada: o servidor manda para o login. Deixa o formulário seguir o caminho normal.
          if (!resposta.ok || (resposta.redirected && /\/login(\?|$)/.test(resposta.url))) {
            throw new Error('resposta inválida');
          }
          return resposta.text();
        })
        .then(function (texto) {
          if (minha !== sequencia) {
            return; // chegou uma resposta mais nova
          }
          var documento = new window.DOMParser().parseFromString(texto, 'text/html');
          var nova = documento.querySelector('[data-regiao-viva]');
          if (!nova) {
            throw new Error('região ausente');
          }
          trocarRegiao(nova);
          regiao.classList.remove('is-falhou');
          ultimaUrl = url;
          if (window.history && window.history.replaceState) {
            window.history.replaceState(null, '', url);
          }
          var contagem = regiao.querySelector('[data-contagem]');
          if (contagem && SH.anunciar) {
            SH.anunciar('Lista atualizada: ' + contagem.textContent.replace(/\s+/g, ' ').trim() + '.');
          }
          terminouCarregar();
        })
        .catch(function (falha) {
          if ((falha && falha.name === 'AbortError') || minha !== sequencia) {
            return;
          }
          terminouCarregar();
          regiao.classList.add('is-falhou');
          if (erro) {
            erro.hidden = false;
          }
        });
    }

    function agendar() {
      window.clearTimeout(temporizador);
      temporizador = window.setTimeout(carregar, ATRASO_DIGITACAO);
    }

    // Digitar no campo de busca espera 300 ms; select e chips trocam na hora
    form.addEventListener('input', function (evento) {
      var alvo = evento.target;
      if (alvo && alvo.matches && alvo.matches('input[type="search"], input[type="text"]')) {
        agendar();
      }
    });
    form.addEventListener('change', function (evento) {
      var alvo = evento.target;
      if (alvo && alvo.matches && alvo.matches('select, input[type="radio"]')) {
        carregar();
      }
    });
    // Esc limpa o campo (atalhos.js) sem disparar "input": procura de novo
    form.addEventListener('keyup', function (evento) {
      if (evento.key === 'Escape') {
        agendar();
      }
    });
    // Enter e botão Buscar: envio normal do formulário (a página inteira). Cancela o que estiver pendente.
    form.addEventListener('submit', function () {
      window.clearTimeout(temporizador);
      sequencia++;
      if (controle && controle.abort) {
        controle.abort();
      }
    });
    if (tentar) {
      tentar.addEventListener('click', carregar);
    }
  }

  var formulario = document.querySelector('form[data-busca-viva]');
  var regiaoViva = document.querySelector('[data-regiao-viva]');
  if (
    formulario &&
    regiaoViva &&
    window.fetch &&
    window.DOMParser &&
    window.URLSearchParams &&
    window.FormData &&
    Element.prototype.matches
  ) {
    iniciarBuscaViva(formulario, regiaoViva);
  }

  /* ======================== 2) Máscara leve do preço ======================== */
  var precos = document.querySelectorAll('input[data-prod-preco]');
  Array.prototype.forEach.call(precos, function (campo) {
    campo.addEventListener('blur', function () {
      var texto = campo.value;
      // Só formata quando já está num formato aceito (até 2 casas): não "corrige" em silêncio o que o servidor recusaria
      if (/^\d+(\.\d{1,2})?$/.test(texto)) {
        campo.value = Number(texto).toFixed(2);
      }
    });
  });
})();
