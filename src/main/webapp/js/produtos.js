/*
 * StockHub v3: JavaScript de produtos e categorias (lista, formulários). Vanilla, sem build.
 * Carregado por produtos.jsp, categorias.jsp, produto-form.jsp e categoria-form.jsp (fragmento "scripts" do layout.tag).
 * O ajuste rápido de saldo fica em js/v3-prod-ajuste.js (só a lista de produtos).
 *
 * 1) Busca ao digitar: form[data-busca-viva] troca só [data-regiao-viva] (o contêiner é permanente, só os filhos mudam).
 *    Enter envia pela mesma rota (sem recarregar a página). Feedback: indicador "Atualizando" no campo, fio de progresso,
 *    região esmaecida, esqueleto se demorar, alerta com "Tentar de novo" se falhar.
 *    Depois de trocar, dispara "sh:lista-trocada" na região (o ajuste rápido e a seleção se religam).
 * 2) Lista de produtos: clique na linha abre o painel peek (o link do nome), seleção de linhas com barra de lote
 *    (única ação: exportar o CSV dos filtros) e densidade compacta/confortável (preferência salva).
 * 3) Máscara leve do preço; prévia ao vivo do formulário de produto (selo, medidor, valor em estoque) e de categoria.
 * O HTML trocado vem do próprio servidor (já escapado pelas JSPs); nada aqui é montado com dado do usuário e não se usa innerHTML.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var ATRASO_DIGITACAO = 300;
  var ATRASO_ESQUELETO = 350;
  var ATRASO_INDICADOR = 150;

  function reduz() {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  }

  /* ======================== 1) Busca ao digitar ======================== */
  function iniciarBuscaViva(form, regiao) {
    var esqueleto = document.querySelector('[data-busca-esqueleto]');
    var erro = document.querySelector('[data-busca-erro]');
    var tentar = document.querySelector('[data-busca-tentar]');
    var indicador = document.querySelector('[data-busca-indicador]');
    var progresso = document.querySelector('[data-busca-progresso]');
    var temporizador = null;
    var temporizadorEsqueleto = null;
    var temporizadorIndicador = null;
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
      if (progresso) {
        progresso.classList.add('is-ativo');
      }
      if (erro) {
        erro.hidden = true;
      }
      window.clearTimeout(temporizadorEsqueleto);
      window.clearTimeout(temporizadorIndicador);
      temporizadorIndicador = window.setTimeout(function () {
        if (indicador) {
          indicador.hidden = false;
        }
      }, ATRASO_INDICADOR);
      temporizadorEsqueleto = window.setTimeout(function () {
        if (esqueleto) {
          esqueleto.hidden = false;
        }
      }, ATRASO_ESQUELETO);
    }

    function terminouCarregar() {
      window.clearTimeout(temporizadorEsqueleto);
      window.clearTimeout(temporizadorIndicador);
      regiao.classList.remove('is-carregando');
      regiao.removeAttribute('aria-busy');
      if (progresso) {
        progresso.classList.remove('is-ativo');
      }
      if (indicador) {
        indicador.hidden = true;
      }
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

    function carregar(forcar) {
      window.clearTimeout(temporizador);
      var url = urlDoFormulario();
      if (forcar !== true && url === ultimaUrl && !regiao.classList.contains('is-falhou')) {
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
          // Sessão expirada: o servidor manda para o login. Deixa o navegador seguir o caminho normal.
          if (resposta.redirected && /\/login(\?|$)/.test(resposta.url)) {
            window.location.href = resposta.url;
            throw new Error('sessão expirada');
          }
          if (!resposta.ok) {
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
          terminouCarregar();
          aplicarDensidade();
          regiao.dispatchEvent(new window.CustomEvent('sh:lista-trocada', { bubbles: true }));
          var contagem = regiao.querySelector('[data-contagem]');
          if (contagem && SH.anunciar) {
            SH.anunciar('Lista atualizada: ' + contagem.textContent.replace(/\s+/g, ' ').trim() + '.');
          }
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
    // Enter e botão Buscar: busca na hora, sem recarregar a página
    form.addEventListener('submit', function (evento) {
      evento.preventDefault();
      carregar(true);
    });
    if (tentar) {
      tentar.addEventListener('click', function () {
        carregar(true);
      });
    }
  }

  /* ======================== 2) Lista de produtos ======================== */
  var raiz = document.documentElement;

  function aplicarDensidade() {
    var d = raiz.getAttribute('data-v3-densidade') === 'compacta' ? 'compacta' : 'confortavel';
    var botoes = document.querySelectorAll('button[data-v3-densidade]');
    Array.prototype.forEach.call(botoes, function (b) {
      b.setAttribute('aria-pressed', b.getAttribute('data-v3-densidade') === d ? 'true' : 'false');
    });
  }

  function iniciarLista(regiao) {
    aplicarDensidade();

    // Densidade (preferência salva)
    regiao.addEventListener('click', function (evento) {
      var botao = evento.target.closest ? evento.target.closest('button[data-v3-densidade]') : null;
      if (!botao) {
        return;
      }
      var d = botao.getAttribute('data-v3-densidade');
      raiz.setAttribute('data-v3-densidade', d);
      if (SH.armazenar) {
        SH.armazenar.gravar('sh-prod-densidade', d);
      }
      aplicarDensidade();
    });

    // Clique na linha (fora de controles) abre o painel pelo link do nome
    regiao.addEventListener('click', function (evento) {
      if (evento.defaultPrevented || evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
        return;
      }
      var alvo = evento.target;
      if (!alvo.closest || alvo.closest('a, button, input, select, textarea, label, form, [data-v3-aj]')) {
        return;
      }
      var linha = alvo.closest('tr[data-v3-linha]');
      var link = linha ? linha.querySelector('[data-sh-peek]') : null;
      if (link) {
        link.click();
      }
    });

    // Seleção e barra de lote
    function selecionadas() {
      return regiao.querySelectorAll('[data-v3-sel]:checked');
    }

    function atualizarLote() {
      var lote = regiao.querySelector('[data-v3-lote]');
      var todos = regiao.querySelector('[data-v3-sel-todos]');
      var caixas = regiao.querySelectorAll('[data-v3-sel]');
      var n = selecionadas().length;
      Array.prototype.forEach.call(caixas, function (c) {
        var linha = c.closest('tr');
        if (linha) {
          linha.classList.toggle('is-selecionada', c.checked);
        }
      });
      if (todos) {
        todos.checked = n > 0 && n === caixas.length;
        todos.indeterminate = n > 0 && n < caixas.length;
      }
      if (!lote) {
        return;
      }
      var antes = lote.hidden;
      lote.hidden = n === 0;
      var num = lote.querySelector('[data-v3-lote-n]');
      var rot = lote.querySelector('[data-v3-lote-rotulo]');
      if (num) {
        num.textContent = String(n);
      }
      if (rot) {
        rot.textContent = n === 1 ? 'selecionado' : 'selecionados';
      }
      // com seleção, o CSV traz só os marcados (o servidor ainda restringe à loja); sem ela, o filtro inteiro
      var csv = lote.querySelector('[data-v3-lote-csv]');
      var nota = lote.querySelector('[data-v3-lote-nota]');
      if (csv) {
        if (!csv.hasAttribute('data-v3-csv-base')) {
          csv.setAttribute('data-v3-csv-base', csv.getAttribute('href'));
        }
        var base = csv.getAttribute('data-v3-csv-base');
        var ids = Array.prototype.map.call(selecionadas(), function (c) {
          return encodeURIComponent(c.value);
        });
        csv.setAttribute('href', ids.length ? base + (base.indexOf('?') < 0 ? '?' : '&') + 'ids=' + ids.join(',') : base);
      }
      if (nota) {
        if (!nota.hasAttribute('data-v3-nota-base')) {
          nota.setAttribute('data-v3-nota-base', nota.textContent);
        }
        nota.textContent = n > 0
          ? 'A exportação traz só ' + (n === 1 ? 'o produto marcado.' : 'os ' + n + ' produtos marcados.')
          : nota.getAttribute('data-v3-nota-base');
      }
      if (antes !== lote.hidden && SH.anunciar) {
        SH.anunciar(n === 0 ? 'Seleção limpa.' : n + (n === 1 ? ' produto selecionado.' : ' produtos selecionados.'));
      }
    }

    regiao.addEventListener('change', function (evento) {
      var alvo = evento.target;
      if (!alvo || !alvo.matches) {
        return;
      }
      if (alvo.matches('[data-v3-sel-todos]')) {
        Array.prototype.forEach.call(regiao.querySelectorAll('[data-v3-sel]'), function (c) {
          c.checked = alvo.checked;
        });
        atualizarLote();
      } else if (alvo.matches('[data-v3-sel]')) {
        atualizarLote();
      }
    });

    regiao.addEventListener('click', function (evento) {
      var limpar = evento.target.closest ? evento.target.closest('[data-v3-lote-limpar]') : null;
      if (limpar) {
        Array.prototype.forEach.call(regiao.querySelectorAll('[data-v3-sel], [data-v3-sel-todos]'), function (c) {
          c.checked = false;
        });
        atualizarLote();
      }
    });
  }

  /* ======================== 3) Formulários ======================== */
  function numero(campo) {
    if (!campo) {
      return 0;
    }
    var n = parseInt(String(campo.value).replace(/\D/g, ''), 10);
    return isNaN(n) ? 0 : n;
  }

  function nivelDe(saldo, minimo) {
    return saldo <= 0 ? 'SEM_ESTOQUE' : saldo <= minimo ? 'BAIXO' : 'NORMAL';
  }

  var NIVEIS = {
    NORMAL: { classe: 'sh-badge--normal', icone: 'bi-check-circle-fill', texto: 'Normal', tom: 'normal' },
    BAIXO: { classe: 'sh-badge--baixo', icone: 'bi-exclamation-triangle-fill', texto: 'Baixo', tom: 'baixo' },
    SEM_ESTOQUE: { classe: 'sh-badge--sem', icone: 'bi-x-octagon-fill', texto: 'Sem estoque', tom: 'sem' }
  };

  function iniciarPreviaProduto(previa) {
    var saldoEl = document.getElementById('quantidade');
    var minEl = document.getElementById('estoqueMinimo');
    var precoEl = document.getElementById('preco');
    var selo = previa.querySelector('[data-v3-prev-selo]');
    var medidor = previa.querySelector('[data-v3-prev-medidor]');
    var enche = previa.querySelector('[data-v3-prev-enche]');
    var texto = previa.querySelector('[data-v3-prev-texto]');
    var valor = previa.querySelector('[data-v3-prev-valor]');
    var saldoNum = previa.querySelector('[data-v3-prev-saldo]');
    var fmt = window.Intl ? new window.Intl.NumberFormat('pt-BR') : { format: String };
    var moeda = window.Intl ? new window.Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }) : { format: String };
    var nivelAnterior = previa.getAttribute('data-v3-nivel');
    var fala = null;

    function degrau(saldo, minimo) {
      var pct = Math.max(0, Math.min(100, (saldo * 50) / minimo));
      var passo = Math.round(pct / 5) * 5;
      return saldo > 0 && passo === 0 ? 5 : passo;
    }

    function atualizar() {
      var saldo = numero(saldoEl);
      var minimo = numero(minEl);
      var nivel = nivelDe(saldo, minimo);
      var cfg = NIVEIS[nivel];
      if (selo && nivel !== nivelAnterior) {
        selo.textContent = '';
        var b = document.createElement('span');
        b.className = 'sh-badge ' + cfg.classe + ' v3-troca';
        var i = document.createElement('i');
        i.className = 'bi ' + cfg.icone;
        i.setAttribute('aria-hidden', 'true');
        b.appendChild(i);
        b.appendChild(document.createTextNode(cfg.texto));
        selo.appendChild(b);
        nivelAnterior = nivel;
      }
      if (medidor) {
        medidor.hidden = minimo <= 0;
        medidor.className = 'sh-meter sh-meter--' + cfg.tom;
        if (enche && minimo > 0) {
          enche.className = 'sh-meter__fill sh-w-' + degrau(saldo, minimo);
        }
      }
      if (saldoNum) {
        saldoNum.textContent = fmt.format(saldo);
      }
      var frase;
      if (nivel === 'SEM_ESTOQUE') {
        frase = 'Com saldo 0, o produto fica Sem estoque.';
      } else if (minimo <= 0) {
        frase = 'Sem estoque mínimo definido, o produto não passa para Baixo.';
      } else if (nivel === 'BAIXO') {
        frase = 'Com saldo ' + fmt.format(saldo) + ' e mínimo ' + fmt.format(minimo) + ', o produto aparece como Baixo. Acima de ' + fmt.format(minimo) + ' un. ele fica Normal.';
      } else {
        frase = 'Com saldo ' + fmt.format(saldo) + ' e mínimo ' + fmt.format(minimo) + ', o produto fica Normal.';
      }
      if (texto) {
        texto.textContent = frase;
      }
      if (valor && precoEl) {
        var preco = parseFloat(precoEl.value);
        valor.textContent = isNaN(preco) ? '—' : moeda.format(preco * saldo);
      }
    }

    [saldoEl, minEl, precoEl].forEach(function (c) {
      if (c) {
        c.addEventListener('input', atualizar);
      }
    });
    atualizar();
    fala = null;
    return fala;
  }

  function iniciarPreviaCategoria(previa) {
    var nomeEl = document.getElementById('nome');
    var descEl = document.getElementById('descricao');
    var nomeSaida = previa.querySelector('[data-v3-cat-nome]');
    var descSaida = previa.querySelector('[data-v3-cat-desc]');
    function atualizar() {
      var nome = nomeEl ? nomeEl.value.trim() : '';
      var desc = descEl ? descEl.value.trim() : '';
      if (nomeSaida) {
        nomeSaida.textContent = nome || 'Nome da categoria';
        nomeSaida.classList.toggle('is-vazio', !nome);
      }
      if (descSaida) {
        descSaida.textContent = desc || 'Sem descrição';
        descSaida.classList.toggle('is-vazio', !desc);
      }
    }
    [nomeEl, descEl].forEach(function (c) {
      if (c) {
        c.addEventListener('input', atualizar);
      }
    });
    atualizar();
  }

  /* ======================== Início ======================== */
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
  if (regiaoViva && document.querySelector('[data-v3-lista-produtos]')) {
    iniciarLista(regiaoViva);
  }

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

  var previaProduto = document.querySelector('[data-v3-previa-produto]');
  if (previaProduto) {
    iniciarPreviaProduto(previaProduto);
  }
  var previaCategoria = document.querySelector('[data-v3-previa-categoria]');
  if (previaCategoria) {
    iniciarPreviaCategoria(previaCategoria);
  }
})();
