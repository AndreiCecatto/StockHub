/*
 * StockHub v2: paleta de comandos (Ctrl+K / Cmd+K ou botão da barra superior).
 *  - telas e ações locais, com busca sem acento e destaque do trecho encontrado;
 *  - produtos via GET /api/busca?q=termo&limite=8 -> [{id, nome, categoria, saldo, minimo, nivel, url}].
 *    Se o endpoint não existir ou falhar, a paleta segue funcionando (telas, ações e "Ver na lista de produtos")
 *    e mostra um aviso discreto no lugar dos produtos;
 *  - ARIA: dialog modal, input role="combobox" com aria-activedescendant, listbox com grupos, resultado anunciado;
 *  - teclado: Ctrl/Cmd+K abre e fecha, setas movem, Enter abre, Esc fecha, Tab fica preso entre o campo e "Esc".
 * Nunca monta HTML a partir de dado: tudo por createElement e textContent.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var dialogo = document.getElementById('sh-paleta');
  if (!dialogo) {
    return; // telas públicas não têm paleta
  }

  var entrada = document.getElementById('sh-paleta-entrada');
  var lista = document.getElementById('sh-paleta-lista');
  var status = document.getElementById('sh-paleta-status');
  var botaoFechar = dialogo.querySelector('[data-sh-fechar-paleta]');
  var suportaDialog = typeof dialogo.showModal === 'function';

  var ctx = function () {
    return SH.contexto || '';
  };

  var NIVEIS = {
    NORMAL: { classe: 'sh-badge--normal', icone: 'bi-check-circle-fill', texto: 'Normal' },
    BAIXO: { classe: 'sh-badge--baixo', icone: 'bi-exclamation-triangle-fill', texto: 'Baixo' },
    SEM_ESTOQUE: { classe: 'sh-badge--sem', icone: 'bi-x-octagon-fill', texto: 'Sem estoque' }
  };

  /* ---------- Comandos locais ---------- */
  function alternarTema() {
    var escuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
    if (SH.tema) {
      SH.tema.aplicar(escuro ? 'claro' : 'escuro');
    }
    var texto = escuro ? 'Tema claro ativado.' : 'Tema escuro ativado.';
    if (SH.toast) {
      SH.toast(texto);
    }
    if (SH.anunciar) {
      SH.anunciar(texto);
    }
  }

  function abrirAtalhos() {
    var botao = document.querySelector('[data-sh-abrir-atalhos]');
    if (botao) {
      botao.click();
    }
  }

  var COMANDOS = [
    { grupo: 'acoes', icone: 'bi-box-arrow-in-down', nome: 'Registrar entrada', dica: 'chegou mercadoria, repor, comprar', url: '/estoque?movTipo=ENTRADA' },
    { grupo: 'acoes', icone: 'bi-box-arrow-up', nome: 'Registrar saída', dica: 'venda, baixa, saiu', url: '/estoque?movTipo=SAIDA' },
    { grupo: 'acoes', icone: 'bi-plus-lg', nome: 'Novo produto', dica: 'cadastrar item adicionar', url: '/produtos/novo' },
    { grupo: 'acoes', icone: 'bi-tag', nome: 'Nova categoria', dica: 'cadastrar grupo adicionar', url: '/categorias/novo' },
    { grupo: 'acoes', icone: 'bi-circle-half', nome: 'Alternar tema', dica: 'claro escuro noite dia aparência', executar: alternarTema },
    { grupo: 'acoes', icone: 'bi-keyboard', nome: 'Atalhos de teclado', dica: 'ajuda teclas', executar: abrirAtalhos },
    { grupo: 'telas', icone: 'bi-house-door', nome: 'Início', dica: 'painel home resumo', url: '/dashboard' },
    { grupo: 'telas', icone: 'bi-box-seam', nome: 'Produtos', dica: 'lista itens catálogo', url: '/produtos?tamanho=25' },
    { grupo: 'telas', icone: 'bi-tags', nome: 'Categorias', dica: 'grupos', url: '/categorias' },
    { grupo: 'telas', icone: 'bi-arrow-left-right', nome: 'Movimentações', dica: 'histórico entradas saídas ajustes estoque', url: '/estoque' },
    { grupo: 'telas', icone: 'bi-receipt', nome: 'Compras (NF-e)', dica: 'nota fiscal xml importar fornecedor compra entrada', url: '/compras/nfe' },
    { grupo: 'telas', icone: 'bi-exclamation-triangle', nome: 'Estoque baixo', dica: 'alertas reposição acabando faltando', url: '/estoque-baixo' },
    { grupo: 'telas', icone: 'bi-stack', nome: 'Unidades', dica: 'quantidade relatório', url: '/unidades' },
    { grupo: 'telas', icone: 'bi-cash-coin', nome: 'Valor do estoque', dica: 'dinheiro financeiro relatório', url: '/valor-estoque' }
  ];

  var TITULOS = { produtos: 'Produtos', acoes: 'Ações', telas: 'Ir para', busca: 'Buscar' };

  /* ---------- Texto sem acento, para comparar ---------- */
  function normalizar(texto) {
    return String(texto || '')
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase();
  }

  /** Divide o nome em [antes, trecho, depois] pelo primeiro trecho que casa com o termo (sem acento). */
  function localizar(nome, termo) {
    if (!termo) {
      return null;
    }
    var mapa = [];
    var plano = '';
    for (var i = 0; i < nome.length; i++) {
      var parte = normalizar(nome.charAt(i));
      for (var j = 0; j < parte.length; j++) {
        mapa.push(i);
      }
      plano += parte;
    }
    var pos = plano.indexOf(termo);
    if (pos < 0 || !mapa.length) {
      return null;
    }
    var inicio = mapa[pos];
    var fim = mapa[Math.min(pos + termo.length - 1, mapa.length - 1)] + 1;
    return [nome.slice(0, inicio), nome.slice(inicio, fim), nome.slice(fim)];
  }

  function escreverComDestaque(elemento, texto, termo) {
    elemento.textContent = '';
    var partes = localizar(texto, termo);
    if (!partes) {
      elemento.textContent = texto;
      return;
    }
    elemento.appendChild(document.createTextNode(partes[0]));
    var marca = document.createElement('mark');
    marca.textContent = partes[1];
    elemento.appendChild(marca);
    elemento.appendChild(document.createTextNode(partes[2]));
  }

  function pontuar(comando, tokens) {
    var nome = normalizar(comando.nome);
    var dica = normalizar(comando.dica || '');
    var total = 0;
    for (var i = 0; i < tokens.length; i++) {
      var t = tokens[i];
      if (nome.indexOf(t) === 0) {
        total += 4;
      } else if (nome.indexOf(' ' + t) >= 0) {
        total += 3;
      } else if (nome.indexOf(t) >= 0) {
        total += 2;
      } else if (dica.indexOf(t) >= 0) {
        total += 1;
      } else {
        return 0; // todos os termos precisam casar
      }
    }
    return total;
  }

  /* ---------- Estado ---------- */
  var opcoes = []; // [{elemento, acao()}]
  var ativo = -1;
  var moveu = false; // o usuário já mexeu na seleção: resultados que chegam depois não a roubam
  var termoAtual = '';
  var produtos = { estado: 'ocioso', itens: [] }; // ocioso | buscando | ok | erro
  var temporizadorBusca = null;
  var controlador = null;
  var cache = {};
  var temporizadorAnuncio = null;
  var abertoEm = 0;

  /* ---------- Montagem da lista ---------- */
  function criarOpcao(id, icone) {
    var el = document.createElement('div');
    el.className = 'sh-paleta__opcao';
    el.id = id;
    el.setAttribute('role', 'option');
    el.setAttribute('aria-selected', 'false');
    var i = document.createElement('i');
    i.className = 'bi ' + icone;
    i.setAttribute('aria-hidden', 'true');
    el.appendChild(i);
    return el;
  }

  function criarGrupo(chave) {
    var grupo = document.createElement('div');
    grupo.setAttribute('role', 'group');
    var titulo = document.createElement('p');
    titulo.className = 'sh-paleta__grupo-titulo';
    titulo.id = 'sh-paleta-grupo-' + chave;
    titulo.textContent = TITULOS[chave];
    grupo.setAttribute('aria-labelledby', titulo.id);
    grupo.appendChild(titulo);
    lista.appendChild(grupo);
    return grupo;
  }

  function irPara(caminho) {
    var destino = caminho;
    if (ctx() && caminho.indexOf(ctx() + '/') !== 0 && caminho.charAt(0) === '/') {
      destino = ctx() + caminho;
    }
    fechar(function () {
      window.location.assign(destino);
    });
  }

  function adicionarComando(grupo, comando, termo, indice) {
    var el = criarOpcao('sh-paleta-op-' + indice, comando.icone);
    var texto = document.createElement('span');
    texto.className = 'sh-paleta__texto';
    var nome = document.createElement('span');
    nome.className = 'sh-paleta__nome';
    escreverComDestaque(nome, comando.nome, termo);
    texto.appendChild(nome);
    el.appendChild(texto);
    var tipo = document.createElement('span');
    tipo.className = 'sh-paleta__lado';
    tipo.textContent = comando.grupo === 'telas' ? 'Ir para' : 'Ação';
    el.appendChild(tipo);
    grupo.appendChild(el);
    opcoes.push({
      elemento: el,
      acao: function () {
        if (comando.executar) {
          fechar(comando.executar);
        } else {
          irPara(comando.url);
        }
      }
    });
  }

  function adicionarProduto(grupo, item, termo, indice) {
    var el = criarOpcao('sh-paleta-op-' + indice, 'bi-box-seam');
    var texto = document.createElement('span');
    texto.className = 'sh-paleta__texto';
    var nome = document.createElement('span');
    nome.className = 'sh-paleta__nome';
    escreverComDestaque(nome, item.nome, termo);
    texto.appendChild(nome);
    if (item.categoria) {
      var sub = document.createElement('span');
      sub.className = 'sh-paleta__sub';
      sub.textContent = item.categoria;
      texto.appendChild(sub);
    }
    el.appendChild(texto);

    var lado = document.createElement('span');
    lado.className = 'sh-paleta__lado';
    if (item.aproximado) {
      var parecido = document.createElement('span');
      parecido.className = 'sh-parecido';
      parecido.textContent = 'parecido';
      lado.appendChild(parecido);
    }
    if (isFinite(item.saldo)) {
      var saldo = document.createElement('span');
      saldo.textContent = item.saldo + ' un.';
      lado.appendChild(saldo);
    }
    var nivel = NIVEIS[item.nivel];
    if (nivel) {
      var selo = document.createElement('span');
      selo.className = 'sh-badge ' + nivel.classe;
      var icone = document.createElement('i');
      icone.className = 'bi ' + nivel.icone;
      icone.setAttribute('aria-hidden', 'true');
      selo.appendChild(icone);
      selo.appendChild(document.createTextNode(nivel.texto));
      lado.appendChild(selo);
    }
    el.appendChild(lado);
    grupo.appendChild(el);
    opcoes.push({
      elemento: el,
      acao: function () {
        irPara(item.url);
      }
    });
  }

  function aviso(grupo, texto, comPontos) {
    var p = document.createElement('div');
    p.className = 'sh-paleta__aviso';
    p.setAttribute('role', 'presentation');
    if (comPontos) {
      for (var i = 0; i < 3; i++) {
        var ponto = document.createElement('span');
        ponto.className = 'sh-paleta__ponto';
        p.appendChild(ponto);
      }
    }
    var t = document.createElement('span');
    t.textContent = texto;
    p.appendChild(t);
    grupo.appendChild(p);
  }

  function comandosDoGrupo(grupo, tokens) {
    var saida = [];
    for (var i = 0; i < COMANDOS.length; i++) {
      if (COMANDOS[i].grupo !== grupo) {
        continue;
      }
      if (!tokens.length) {
        saida.push({ c: COMANDOS[i], p: 0, i: i });
      } else {
        var p = pontuar(COMANDOS[i], tokens);
        if (p > 0) {
          saida.push({ c: COMANDOS[i], p: p, i: i });
        }
      }
    }
    saida.sort(function (a, b) {
      return b.p - a.p || a.i - b.i;
    });
    return saida;
  }

  function renderizar() {
    var termoBruto = entrada.value.replace(/\s+/g, ' ').trim();
    termoAtual = normalizar(termoBruto);
    var tokens = termoAtual ? termoAtual.split(' ') : [];
    var anterior = ativo >= 0 && opcoes[ativo] ? opcoes[ativo].elemento.id : null;
    lista.textContent = '';
    opcoes = [];
    ativo = -1;
    var indice = 0;

    // Produtos (só com 2 ou mais letras)
    if (termoAtual.length >= 2) {
      var gp = criarGrupo('produtos');
      if (produtos.estado === 'buscando') {
        aviso(gp, 'Buscando produtos…', true);
      } else if (produtos.estado === 'erro') {
        aviso(gp, 'Não consegui buscar os produtos agora. Dá para tentar a lista completa abaixo.', false);
      } else if (produtos.estado === 'ok' && !produtos.itens.length) {
        aviso(gp, 'Nenhum produto com “' + termoBruto + '”.', false);
      } else {
        for (var k = 0; k < produtos.itens.length; k++) {
          adicionarProduto(gp, produtos.itens[k], termoAtual, indice++);
        }
      }
    }

    // Ações e telas
    var ordem = ['acoes', 'telas'];
    for (var g = 0; g < ordem.length; g++) {
      var achados = comandosDoGrupo(ordem[g], tokens);
      if (!achados.length) {
        continue;
      }
      var grupo = criarGrupo(ordem[g]);
      for (var m = 0; m < achados.length; m++) {
        adicionarComando(grupo, achados[m].c, termoAtual, indice++);
      }
    }

    // Saída sempre disponível: a lista completa de produtos (funciona mesmo sem a API)
    if (termoBruto) {
      var gb = criarGrupo('busca');
      var op = criarOpcao('sh-paleta-op-' + indice++, 'bi-list-ul');
      var tx = document.createElement('span');
      tx.className = 'sh-paleta__texto';
      var nm = document.createElement('span');
      nm.className = 'sh-paleta__nome';
      nm.textContent = 'Ver “' + termoBruto + '” na lista de produtos';
      tx.appendChild(nm);
      op.appendChild(tx);
      gb.appendChild(op);
      opcoes.push({
        elemento: op,
        acao: function () {
          irPara('/produtos?busca=' + encodeURIComponent(termoBruto));
        }
      });
    }

    if (!opcoes.length) {
      var vazio = document.createElement('div');
      vazio.className = 'sh-paleta__aviso';
      vazio.setAttribute('role', 'presentation');
      vazio.textContent = 'Nada encontrado.';
      lista.appendChild(vazio);
    }

    // mantém a opção que estava ativa (se ainda existir); senão a primeira
    var novo = 0;
    if (moveu && anterior) {
      for (var n = 0; n < opcoes.length; n++) {
        if (opcoes[n].elemento.id === anterior) {
          novo = n;
        }
      }
    }
    definirAtivo(opcoes.length ? novo : -1, false);
    anunciarResultado();
  }

  function definirAtivo(indice, rolar) {
    if (ativo >= 0 && opcoes[ativo]) {
      opcoes[ativo].elemento.setAttribute('aria-selected', 'false');
    }
    ativo = indice;
    if (ativo >= 0 && opcoes[ativo]) {
      var el = opcoes[ativo].elemento;
      el.setAttribute('aria-selected', 'true');
      entrada.setAttribute('aria-activedescendant', el.id);
      if (rolar && el.scrollIntoView) {
        el.scrollIntoView({ block: 'nearest' });
      }
    } else {
      entrada.removeAttribute('aria-activedescendant');
    }
  }

  function anunciarResultado() {
    window.clearTimeout(temporizadorAnuncio);
    temporizadorAnuncio = window.setTimeout(function () {
      var n = opcoes.length;
      var texto = n === 0 ? 'Nenhum resultado.' : n === 1 ? '1 resultado.' : n + ' resultados.';
      if (produtos.estado === 'buscando') {
        texto = 'Buscando produtos. ' + texto;
      }
      status.textContent = '';
      window.setTimeout(function () {
        status.textContent = texto;
      }, 30);
    }, 350);
  }

  /* ---------- Busca de produtos ---------- */
  function urlSegura(url, id) {
    if (typeof url === 'string' && url.charAt(0) === '/' && url.charAt(1) !== '/' && url.indexOf('\\') < 0) {
      return url;
    }
    return '/produtos/' + encodeURIComponent(id);
  }

  function limpar(itens) {
    var saida = [];
    if (!Array.isArray(itens)) {
      throw new Error('formato');
    }
    for (var i = 0; i < itens.length && saida.length < 8; i++) {
      var it = itens[i];
      if (!it || typeof it.nome !== 'string' || it.id === undefined || it.id === null) {
        continue;
      }
      saida.push({
        nome: it.nome,
        categoria: typeof it.categoria === 'string' ? it.categoria : '',
        saldo: Number(it.saldo),
        nivel: typeof it.nivel === 'string' ? it.nivel : '',
        aproximado: it.aproximado === true,
        url: urlSegura(it.url, it.id)
      });
    }
    return saida;
  }

  function buscarProdutos(termoBruto) {
    window.clearTimeout(temporizadorBusca);
    if (controlador) {
      controlador.abort();
      controlador = null;
    }
    if (termoBruto.length < 2) {
      produtos = { estado: 'ocioso', itens: [] };
      return;
    }
    if (cache[termoBruto]) {
      produtos = { estado: 'ok', itens: cache[termoBruto] };
      return;
    }
    produtos = { estado: 'buscando', itens: [] };
    temporizadorBusca = window.setTimeout(function () {
      if (!window.fetch) {
        produtos = { estado: 'erro', itens: [] };
        renderizar();
        return;
      }
      controlador = window.AbortController ? new window.AbortController() : null;
      var tempo = window.setTimeout(function () {
        if (controlador) {
          controlador.abort();
        }
      }, 6000);
      window
        .fetch(ctx() + '/api/busca?q=' + encodeURIComponent(termoBruto) + '&limite=8', {
          credentials: 'same-origin',
          headers: { Accept: 'application/json' },
          signal: controlador ? controlador.signal : undefined
        })
        .then(function (resposta) {
          var tipo = resposta.headers.get('content-type') || '';
          if (!resposta.ok || tipo.indexOf('json') < 0) {
            throw new Error('http');
          }
          return resposta.json();
        })
        .then(function (dados) {
          window.clearTimeout(tempo);
          var itens = limpar(dados);
          cache[termoBruto] = itens;
          if (entrada.value.replace(/\s+/g, ' ').trim() === termoBruto) {
            produtos = { estado: 'ok', itens: itens };
            renderizar();
          }
        })
        .catch(function (erro) {
          window.clearTimeout(tempo);
          if (erro && erro.name === 'AbortError' && entrada.value.replace(/\s+/g, ' ').trim() !== termoBruto) {
            return; // trocada por uma busca mais nova
          }
          if (entrada.value.replace(/\s+/g, ' ').trim() === termoBruto) {
            produtos = { estado: 'erro', itens: [] };
            renderizar();
          }
        });
    }, 180);
  }

  /* ---------- Abrir e fechar ---------- */
  function aberta() {
    return dialogo.hasAttribute('open');
  }

  function abrir(textoInicial) {
    if (!suportaDialog) {
      window.location.assign(ctx() + '/produtos?tamanho=25');
      return;
    }
    if (aberta()) {
      return;
    }
    // fecha menus do Bootstrap abertos, para não ficarem por baixo do fundo escuro
    var menus = document.querySelectorAll('.dropdown-menu.show');
    for (var i = 0; i < menus.length; i++) {
      menus[i].classList.remove('show');
    }
    entrada.value = textoInicial || '';
    moveu = false;
    produtos = { estado: 'ocioso', itens: [] };
    dialogo.classList.remove('is-fechando');
    dialogo.showModal();
    abertoEm = Date.now();
    buscarProdutos(entrada.value.trim());
    renderizar();
    entrada.focus();
  }

  var fechando = false;

  function fechar(depois) {
    if (!aberta() || fechando) {
      if (depois) {
        depois();
      }
      return;
    }
    fechando = true;
    if (controlador) {
      controlador.abort();
    }
    window.clearTimeout(temporizadorBusca);
    var concluir = function () {
      dialogo.classList.remove('is-fechando');
      dialogo.close();
      fechando = false;
      if (depois) {
        depois();
      }
    };
    if (SH.reduzMovimento && SH.reduzMovimento()) {
      concluir();
    } else {
      dialogo.classList.add('is-fechando');
      window.setTimeout(concluir, 130);
    }
  }

  SH.paleta = {
    abrir: abrir,
    fechar: fechar,
    estaAberta: aberta
  };

  /* ---------- Eventos ---------- */
  entrada.addEventListener('input', function () {
    moveu = false;
    buscarProdutos(entrada.value.replace(/\s+/g, ' ').trim());
    renderizar();
  });

  entrada.addEventListener('keydown', function (evento) {
    var n = opcoes.length;
    if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
      evento.preventDefault();
      if (!n) {
        return;
      }
      moveu = true;
      var passo = evento.key === 'ArrowDown' ? 1 : -1;
      definirAtivo((ativo + passo + n) % n, true);
    } else if (evento.key === 'Enter') {
      evento.preventDefault();
      if (ativo >= 0 && opcoes[ativo]) {
        opcoes[ativo].acao();
      }
    } else if (evento.key === 'PageDown' || evento.key === 'PageUp') {
      evento.preventDefault();
      if (n) {
        moveu = true;
        definirAtivo(Math.max(0, Math.min(n - 1, ativo + (evento.key === 'PageDown' ? 5 : -5))), true);
      }
    }
  });

  lista.addEventListener('mousemove', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[role="option"]') : null;
    if (!alvo) {
      return;
    }
    for (var i = 0; i < opcoes.length; i++) {
      if (opcoes[i].elemento === alvo && i !== ativo) {
        moveu = true;
        definirAtivo(i, false);
        break;
      }
    }
  });

  lista.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[role="option"]') : null;
    if (!alvo) {
      return;
    }
    for (var i = 0; i < opcoes.length; i++) {
      if (opcoes[i].elemento === alvo) {
        opcoes[i].acao();
        break;
      }
    }
  });

  // Esc (nativo do dialog) passa pela animação de saída
  dialogo.addEventListener('cancel', function (evento) {
    evento.preventDefault();
    fechar();
  });

  // Clique no fundo escuro fecha (o alvo do clique é o próprio <dialog>)
  dialogo.addEventListener('click', function (evento) {
    if (evento.target === dialogo && Date.now() - abertoEm > 200) {
      fechar();
    }
  });

  botaoFechar.addEventListener('click', function () {
    fechar();
  });

  // Foco preso: Tab alterna entre o campo e o botão "Esc"
  dialogo.addEventListener('keydown', function (evento) {
    if (evento.key !== 'Tab') {
      return;
    }
    var alvo = document.activeElement;
    if (evento.shiftKey && alvo === entrada) {
      evento.preventDefault();
      botaoFechar.focus();
    } else if (!evento.shiftKey && alvo === botaoFechar) {
      evento.preventDefault();
      entrada.focus();
    } else if (!evento.shiftKey && alvo === entrada) {
      evento.preventDefault();
      botaoFechar.focus();
    } else if (evento.shiftKey && alvo === botaoFechar) {
      evento.preventDefault();
      entrada.focus();
    }
  });

  // Ctrl+K / Cmd+K em qualquer lugar (inclusive dentro de campos de texto)
  document.addEventListener('keydown', function (evento) {
    if ((evento.ctrlKey || evento.metaKey) && !evento.altKey && !evento.shiftKey && (evento.key === 'k' || evento.key === 'K')) {
      evento.preventDefault();
      if (aberta()) {
        fechar();
      } else {
        abrir('');
      }
    }
  });

  // Botões que abrem a paleta
  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-abrir-paleta]') : null;
    if (botao) {
      evento.preventDefault();
      abrir('');
    }
  });

  // No Mac a dica mostra o símbolo do Command
  var eMac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent || '');
  if (eMac) {
    var teclas = document.querySelectorAll('[data-sh-tecla-paleta]');
    for (var t = 0; t < teclas.length; t++) {
      teclas[t].textContent = '⌘ K';
    }
  }
})();
