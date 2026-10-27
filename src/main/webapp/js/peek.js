/*
 * StockHub v3: painel lateral deslizante ("peek"). Abre os detalhes de um item sem sair da lista.
 * Esqueleto em WEB-INF/includes/sh/peek.jsp (já incluído por tags/layout.tag); estilo em css/peek.css.
 *
 * Contrato:
 *  - Gatilho: <a href="/produtos/5" data-sh-peek data-sh-peek-titulo="Nome do produto">...</a>
 *    (ou qualquer elemento com data-sh-peek-url="/produtos/5"). Clique simples abre o painel; Ctrl/Cmd/Shift/botão do
 *    meio, nova aba e links de outra origem continuam navegando normalmente. Sem JavaScript, o link abre a página inteira.
 *  - Conteúdo: o JS faz GET na URL (cabeçalhos Accept: text/html e X-SH-Peek: 1) e extrai da resposta a região marcada
 *    com data-peek (a página completa precisa ter esse elemento). Título do painel: data-sh-peek-titulo do gatilho, ou
 *    data-peek-titulo da região, ou "Detalhes". Se a região não existir ou a resposta falhar, o painel mostra um estado de erro
 *    com "Tentar de novo" e o link para a página completa.
 *  - Segurança: o HTML vem da própria aplicação (já escapado no servidor); mesmo assim, script, style, link, meta, base,
 *    iframe, object, embed, atributos on* e URLs javascript: são removidos antes de inserir. Nenhum dado vira HTML por concatenação.
 *  - Acessibilidade: <dialog> modal com aria-labelledby (título), foco preso (Tab e Shift+Tab), Esc fecha, clique no fundo
 *    fecha, o foco volta ao gatilho, aria-busy enquanto carrega e anúncio ao terminar. Menos movimento: sem deslize.
 * API: SH.peek.abrir(url, { titulo, gatilho }), SH.peek.fechar(), SH.peek.aberto().
 * Eventos (disparados no <dialog> #sh-peek, com bolha): sh:peek-abrir, sh:peek-carregado ({ url, corpo }),
 *   sh:peek-erro ({ url }), sh:peek-fechar.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var dialogo = document.getElementById('sh-peek');
  if (!dialogo || typeof dialogo.showModal !== 'function') {
    return; // sem <dialog> (ou telas públicas): os links seguem como links comuns
  }

  var corpo = dialogo.querySelector('[data-sh-peek-corpo]');
  var titulo = dialogo.querySelector('[data-sh-peek-titulo-saida]');
  var linkPagina = dialogo.querySelector('[data-sh-peek-pagina]');

  var gatilho = null;
  var controlador = null;
  var urlAtual = '';
  var opcoesAtuais = {};
  var DURACAO_SAIDA = 180;

  var reduz = function () {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  };

  function emitir(nome, detalhe) {
    dialogo.dispatchEvent(new window.CustomEvent(nome, { bubbles: true, detail: detalhe || {} }));
  }

  function mesmaOrigem(url) {
    try {
      return new URL(url, window.location.href).origin === window.location.origin;
    } catch (erro) {
      return false;
    }
  }

  /* ---------- Limpeza do HTML recebido ---------- */
  var PROIBIDOS = 'script, style, link, meta, base, iframe, object, embed, noscript';

  function limpar(no) {
    var lixo = no.querySelectorAll(PROIBIDOS);
    for (var i = 0; i < lixo.length; i++) {
      lixo[i].parentNode.removeChild(lixo[i]);
    }
    var todos = no.querySelectorAll('*');
    for (var j = 0; j < todos.length; j++) {
      var atributos = Array.prototype.slice.call(todos[j].attributes);
      for (var k = 0; k < atributos.length; k++) {
        var nome = atributos[k].name.toLowerCase();
        var valor = String(atributos[k].value || '').replace(/[\u0000- ]/g, '').toLowerCase();
        if (nome.indexOf('on') === 0 || ((nome === 'href' || nome === 'src' || nome === 'action' || nome === 'formaction') && valor.indexOf('javascript:') === 0)) {
          todos[j].removeAttribute(atributos[k].name);
        }
      }
    }
  }

  /* ---------- Estados do corpo ---------- */
  function criar(etiqueta, classe, texto) {
    var el = document.createElement(etiqueta);
    if (classe) {
      el.className = classe;
    }
    if (texto) {
      el.textContent = texto;
    }
    return el;
  }

  function mostrarCarregando() {
    corpo.textContent = '';
    var bloco = criar('div', 'sh-carregando');
    bloco.setAttribute('aria-hidden', 'true');
    bloco.appendChild(criar('span', 'sh-esqueleto sh-esq-linha'));
    bloco.appendChild(criar('span', 'sh-esqueleto sh-esq-linha sh-esq-linha--curta'));
    bloco.appendChild(criar('span', 'sh-esqueleto sh-esq-cartao'));
    bloco.appendChild(criar('span', 'sh-esqueleto sh-esq-linha'));
    bloco.appendChild(criar('span', 'sh-esqueleto sh-esq-linha sh-esq-linha--curta'));
    corpo.appendChild(bloco);
    corpo.setAttribute('aria-busy', 'true');
  }

  function mostrarErro(url) {
    corpo.textContent = '';
    corpo.setAttribute('aria-busy', 'false');
    var caixa = criar('div', 'sh-vazio sh-vazio--erro');
    caixa.setAttribute('role', 'alert');
    var icone = criar('i', 'bi bi-exclamation-triangle sh-vazio__icone');
    icone.setAttribute('aria-hidden', 'true');
    caixa.appendChild(icone);
    caixa.appendChild(criar('p', 'sh-vazio__titulo', 'Não foi possível carregar os detalhes'));
    caixa.appendChild(criar('p', 'sh-vazio__texto', 'Verifique a conexão e tente de novo, ou abra a página completa.'));
    var tentar = criar('button', 'btn btn-outline-secondary btn-sm', 'Tentar de novo');
    tentar.type = 'button';
    tentar.addEventListener('click', function () {
      carregar(url);
    });
    caixa.appendChild(tentar);
    corpo.appendChild(caixa);
    tentar.focus();
    emitir('sh:peek-erro', { url: url });
  }

  /* ---------- Carga ---------- */
  function carregar(url) {
    if (controlador) {
      controlador.abort();
    }
    controlador = window.AbortController ? new window.AbortController() : null;
    mostrarCarregando();
    var opcoes = { credentials: 'same-origin', headers: { Accept: 'text/html', 'X-SH-Peek': '1' } };
    if (controlador) {
      opcoes.signal = controlador.signal;
    }
    window.fetch(url, opcoes)
      .then(function (resposta) {
        if (!resposta.ok) {
          throw new Error('http-' + resposta.status);
        }
        return resposta.text();
      })
      .then(function (html) {
        if (url !== urlAtual) {
          return; // chegou depois de o usuário abrir outro item
        }
        var doc = new window.DOMParser().parseFromString(html, 'text/html');
        var regiao = doc.querySelector('[data-peek]');
        if (!regiao) {
          throw new Error('sem-regiao');
        }
        limpar(regiao);
        corpo.textContent = '';
        var filhos = Array.prototype.slice.call(regiao.childNodes);
        for (var i = 0; i < filhos.length; i++) {
          corpo.appendChild(document.importNode(filhos[i], true));
        }
        if (!opcoesAtuais.titulo) {
          titulo.textContent = regiao.getAttribute('data-peek-titulo') || 'Detalhes';
        }
        corpo.setAttribute('aria-busy', 'false');
        if (SH.animarNumeros) {
          SH.animarNumeros(corpo);
        }
        corpo.focus({ preventScroll: true });
        if (SH.anunciar) {
          SH.anunciar('Detalhes carregados: ' + titulo.textContent);
        }
        emitir('sh:peek-carregado', { url: url, corpo: corpo });
      })
      .catch(function (erro) {
        if (erro && erro.name === 'AbortError') {
          return;
        }
        if (url === urlAtual) {
          mostrarErro(url);
        }
      });
  }

  /* ---------- Abrir e fechar ---------- */
  function abrir(url, opcoes) {
    if (!url) {
      return;
    }
    opcoesAtuais = opcoes || {};
    urlAtual = new URL(url, window.location.href).href;
    if (opcoesAtuais.gatilho) {
      gatilho = opcoesAtuais.gatilho;
    }
    titulo.textContent = opcoesAtuais.titulo || 'Detalhes';
    linkPagina.href = urlAtual;
    linkPagina.hidden = false;
    dialogo.classList.remove('is-fechando');
    if (!dialogo.open) {
      dialogo.showModal();
      document.documentElement.classList.add('sh-peek-aberto');
    }
    emitir('sh:peek-abrir', { url: urlAtual, gatilho: gatilho });
    carregar(urlAtual);
  }

  function terminarFechamento() {
    dialogo.classList.remove('is-fechando');
    if (dialogo.open) {
      dialogo.close();
    }
    document.documentElement.classList.remove('sh-peek-aberto');
    corpo.textContent = '';
    corpo.setAttribute('aria-busy', 'false');
    var volta = gatilho;
    gatilho = null;
    if (volta && document.contains(volta) && volta.focus) {
      volta.focus();
    }
    emitir('sh:peek-fechar', {});
  }

  function fechar() {
    if (!dialogo.open || dialogo.classList.contains('is-fechando')) {
      return;
    }
    if (controlador) {
      controlador.abort();
    }
    urlAtual = '';
    if (reduz()) {
      terminarFechamento();
      return;
    }
    dialogo.classList.add('is-fechando');
    window.setTimeout(terminarFechamento, DURACAO_SAIDA);
  }

  SH.peek = {
    abrir: abrir,
    fechar: fechar,
    aberto: function () {
      return dialogo.open;
    }
  };

  /* ---------- Eventos ---------- */
  document.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[data-sh-peek], [data-sh-peek-url]') : null;
    if (!alvo || evento.defaultPrevented) {
      return;
    }
    if (evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
      return;
    }
    var url = alvo.getAttribute('data-sh-peek-url') || alvo.getAttribute('href');
    var alvoAba = alvo.getAttribute('target');
    if (!url || !mesmaOrigem(url) || (alvoAba && alvoAba !== '_self') || alvo.hasAttribute('download')) {
      return;
    }
    evento.preventDefault();
    abrir(url, { titulo: alvo.getAttribute('data-sh-peek-titulo') || '', gatilho: alvo });
  });

  dialogo.addEventListener('click', function (evento) {
    // clique no fundo (o próprio <dialog>, fora da caixa) ou em [data-sh-peek-fechar]
    var botao = evento.target.closest ? evento.target.closest('[data-sh-peek-fechar]') : null;
    if (botao || evento.target === dialogo) {
      fechar();
    }
  });

  // Esc: fecha com a animação de saída
  dialogo.addEventListener('cancel', function (evento) {
    evento.preventDefault();
    fechar();
  });

  // Foco preso: Tab e Shift+Tab giram dentro do painel
  dialogo.addEventListener('keydown', function (evento) {
    if (evento.key !== 'Tab') {
      return;
    }
    var candidatos = dialogo.querySelectorAll(
      'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    var visiveis = [];
    for (var i = 0; i < candidatos.length; i++) {
      if (!candidatos[i].hidden && candidatos[i].offsetParent !== null) {
        visiveis.push(candidatos[i]);
      }
    }
    if (!visiveis.length) {
      evento.preventDefault();
      corpo.focus();
      return;
    }
    var primeiro = visiveis[0];
    var ultimo = visiveis[visiveis.length - 1];
    var ativo = document.activeElement;
    if (evento.shiftKey && (ativo === primeiro || ativo === corpo || ativo === dialogo)) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && ativo === ultimo) {
      evento.preventDefault();
      primeiro.focus();
    }
  });
})();
