/*
 * StockHub v3: ajuste rápido de saldo na lista de produtos (−, + e número digitado), sem recarregar a página.
 * Carregado só por produtos.jsp. Cada linha tem tr[data-v3-linha][data-produto-id][data-minimo][data-saldo] com um grupo
 * [data-v3-aj] (botões [data-v3-menos] e [data-v3-mais], campo [data-v3-num] e a faixa de confirmação [data-v3-confirma]).
 *
 * Contrato do servidor: POST /estoque/movimentar com "Accept: application/json" e o token CSRF em X-CSRF-TOKEN (lido do
 * campo oculto _csrf do layout ou de <meta name="_csrf">); corpo form-urlencoded: produtoId, tipo (ENTRADA|SAIDA|AJUSTE),
 * quantidade, observacao. 200 {ok, saldoAnterior, saldoNovo, nivel, ...}; 400/404/422 {ok:false, erro}; 401 vai ao login; 403 = CSRF.
 *
 * Comportamento:
 *  - − e + (e as setas do campo, Shift = 10) somam ao saldo na hora (otimista) e AGRUPAM cliques rápidos: depois de
 *    600 ms sem novo clique sai UMA movimentação (ENTRADA ou SAIDA com a soma). SAIDA com saldo 0 é bloqueada no cliente.
 *  - Digitar um número e tecla Enter (ou Confirmar) registra um AJUSTE (contagem final, observação "Ajuste rápido na lista").
 *    Esc ou sair do campo cancela a digitação.
 *  - Cada produto tem uma fila: uma requisição por vez, então não há duplo envio. Se o servidor recusar ou a rede falhar,
 *    a fila é descartada e o saldo volta ao último valor confirmado, com aviso.
 *  - Depois de gravar: toast com "Desfazer" (movimentação contrária; AJUSTE volta ao saldo anterior, observação
 *    "Desfeito: ajuste rápido") e link "Ver no histórico".
 *  - Atualiza número, medidor, selo e a marca da linha, com animações curtas (só transform e opacity; sem elas com menos movimento).
 * Tudo entra por textContent e createElement.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var regiao = document.querySelector('[data-regiao-viva]');
  if (!regiao || !document.querySelector('[data-v3-lista-produtos]') || !window.fetch) {
    return;
  }

  var ATRASO_AGRUPAR = 600;
  var MAX_MOV = 1000000;
  var MAX_SALDO = 2000000000;
  var OBS_AJUSTE = 'Ajuste rápido na lista';
  var OBS_DESFEITO = 'Desfeito: ajuste rápido';
  var fmt = window.Intl ? new window.Intl.NumberFormat('pt-BR') : { format: String };

  var NIVEIS = {
    NORMAL: { classe: 'sh-badge--normal', icone: 'bi-check-circle-fill', texto: 'Normal', tom: 'normal' },
    BAIXO: { classe: 'sh-badge--baixo', icone: 'bi-exclamation-triangle-fill', texto: 'Baixo', tom: 'baixo' },
    SEM_ESTOQUE: { classe: 'sh-badge--sem', icone: 'bi-x-octagon-fill', texto: 'Sem estoque', tom: 'sem' }
  };

  var estados = {};

  function reduz() {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  }

  function nivelDe(saldo, minimo) {
    return saldo <= 0 ? 'SEM_ESTOQUE' : saldo <= minimo ? 'BAIXO' : 'NORMAL';
  }

  function token() {
    var meta = document.querySelector('meta[name="_csrf"]');
    if (meta && meta.getAttribute('content')) {
      return meta.getAttribute('content');
    }
    var campo = document.querySelector('input[type="hidden"][name="_csrf"]');
    return campo ? campo.value : '';
  }

  function meio(n) {
    // arredondamento "par" (o mesmo do fmt:formatNumber da JSP), para a barra bater com a do servidor
    var f = Math.floor(n);
    var d = n - f;
    if (d > 0.5) {
      return f + 1;
    }
    if (d < 0.5) {
      return f;
    }
    return f % 2 === 0 ? f : f + 1;
  }

  function degrauDe(saldo, minimo) {
    var pct = Math.max(0, Math.min(100, (saldo * 50) / minimo));
    var passo = meio(pct / 5) * 5;
    return saldo > 0 && passo === 0 ? 5 : passo;
  }

  function ativo(e) {
    return e.emVoo || e.pendente !== 0 || e.fila.length > 0 || e.editando;
  }

  function linhaDe(e) {
    return regiao.querySelector('tr[data-produto-id="' + e.id + '"]');
  }

  function obter(tr) {
    var id = tr.getAttribute('data-produto-id');
    var e = estados[id];
    if (!e) {
      e = estados[id] = {
        id: id,
        nome: tr.getAttribute('data-nome') || 'produto',
        minimo: parseInt(tr.getAttribute('data-minimo'), 10) || 0,
        confirmado: parseInt(tr.getAttribute('data-saldo'), 10) || 0,
        exibido: 0,
        pendente: 0,
        fila: [],
        emVoo: false,
        editando: false,
        timer: null,
        toast: null
      };
      e.exibido = e.confirmado;
    } else if (!ativo(e)) {
      e.confirmado = parseInt(tr.getAttribute('data-saldo'), 10) || 0;
      e.exibido = e.confirmado;
    }
    return e;
  }

  function doElemento(alvo) {
    var tr = alvo.closest ? alvo.closest('tr[data-v3-linha]') : null;
    return tr ? obter(tr) : null;
  }

  /* ---------- Desenho ---------- */
  function definirEstado(e, estado) {
    var tr = linhaDe(e);
    var aj = tr ? tr.querySelector('[data-v3-aj]') : null;
    if (!aj) {
      return;
    }
    if (estado) {
      aj.setAttribute('data-estado', estado);
    } else {
      aj.removeAttribute('data-estado');
    }
    aj.setAttribute('aria-busy', estado === 'enviando' ? 'true' : 'false');
  }

  function trocarSelo(tr, nivel) {
    var selo = tr.querySelector('[data-v3-selo]');
    var cfg = NIVEIS[nivel];
    if (!selo || !cfg || selo.getAttribute('data-nivel') === nivel) {
      return;
    }
    selo.setAttribute('data-nivel', nivel);
    selo.textContent = '';
    var b = document.createElement('span');
    b.className = 'sh-badge ' + cfg.classe + (reduz() ? '' : ' v3-troca');
    var i = document.createElement('i');
    i.className = 'bi ' + cfg.icone;
    i.setAttribute('aria-hidden', 'true');
    b.appendChild(i);
    b.appendChild(document.createTextNode(cfg.texto));
    selo.appendChild(b);
  }

  function pintarMedidor(tr, saldo, minimo, nivel) {
    var medidor = tr.querySelector('.sh-meter');
    var enche = medidor ? medidor.querySelector('.sh-meter__fill') : null;
    if (!medidor || !enche || minimo <= 0) {
      return;
    }
    var antes = /sh-w-(\d+)/.exec(enche.className);
    var larguraAntes = antes ? parseInt(antes[1], 10) : 0;
    var larguraDepois = degrauDe(saldo, minimo);
    medidor.className = 'sh-meter sh-meter--' + NIVEIS[nivel].tom;
    medidor.setAttribute('aria-label', 'Saldo ' + saldo + ' de mínimo ' + minimo);
    if (larguraAntes === larguraDepois) {
      return;
    }
    enche.className = 'sh-meter__fill sh-w-' + larguraDepois;
    if (!reduz() && enche.animate && larguraDepois > 0) {
      // A largura muda de uma vez; a transformação "desfaz" a diferença e desliza até o valor final (só transform)
      enche.animate(
        [{ transform: 'scaleX(' + larguraAntes / larguraDepois + ')' }, { transform: 'scaleX(1)' }],
        { duration: 240, easing: 'cubic-bezier(.2,.8,.2,1)' }
      );
    }
  }

  function pintar(e, direcao) {
    var tr = linhaDe(e);
    if (!tr) {
      return;
    }
    var nivel = nivelDe(e.exibido, e.minimo);
    var num = tr.querySelector('[data-v3-num]');
    if (num && !e.editando) {
      var novoTexto = fmt.format(e.exibido);
      if (num.value !== novoTexto) {
        num.value = novoTexto;
        if (direcao && !reduz()) {
          num.classList.remove('is-sobe', 'is-desce');
          void num.offsetWidth;
          num.classList.add(direcao > 0 ? 'is-sobe' : 'is-desce');
        }
      }
    }
    tr.setAttribute('data-saldo', String(e.exibido));
    var menos = tr.querySelector('[data-v3-menos]');
    if (menos) {
      menos.setAttribute('aria-disabled', e.exibido <= 0 ? 'true' : 'false');
    }
    pintarMedidor(tr, e.exibido, e.minimo, nivel);
    trocarSelo(tr, nivel);
    tr.classList.toggle('sh-linha--sem', nivel === 'SEM_ESTOQUE');
    tr.classList.toggle('sh-linha--baixo', nivel === 'BAIXO');
  }

  function destacar(e) {
    var tr = linhaDe(e);
    if (!tr) {
      return;
    }
    if (reduz() || !tr.animate) {
      tr.classList.add('is-mudou');
      window.setTimeout(function () {
        tr.classList.remove('is-mudou');
      }, 1600);
      return;
    }
    var cor = window.getComputedStyle(document.documentElement).getPropertyValue('--sh-accent-soft').trim() || '#E8EFFC';
    tr.animate([{ backgroundColor: cor }, { backgroundColor: 'transparent' }], { duration: 1600, easing: 'cubic-bezier(.2,.8,.2,1)' });
  }

  /* ---------- Requisição ---------- */
  function enviar(e, op) {
    var corpo = new window.URLSearchParams();
    corpo.append('produtoId', e.id);
    corpo.append('tipo', op.tipo);
    corpo.append('quantidade', String(op.qtd));
    corpo.append('observacao', op.obs || '');
    return window
      .fetch((SH.contexto || '') + '/estoque/movimentar', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
          'X-CSRF-TOKEN': token()
        },
        body: corpo.toString()
      })
      .then(function (resposta) {
        if (resposta.status === 401) {
          window.location.href = (SH.contexto || '') + '/login';
          return { ok: false, erro: 'Sessão expirada. Entre de novo.', redirecionou: true };
        }
        return resposta
          .json()
          .catch(function () {
            return null;
          })
          .then(function (dados) {
            if (resposta.ok && dados && dados.ok) {
              return { ok: true, dados: dados };
            }
            var msg = dados && dados.erro ? dados.erro : null;
            if (dados && dados.campos && !msg) {
              msg = dados.campos[Object.keys(dados.campos)[0]];
            }
            if (resposta.status === 403) {
              msg = 'A página perdeu a validade de segurança. Recarregue e tente de novo.';
            }
            return { ok: false, erro: msg || 'O servidor não conseguiu registrar a movimentação.' };
          });
      })
      .catch(function () {
        return { ok: false, erro: 'Sem conexão com o servidor.' };
      });
  }

  function textoDoToast(e, op, d) {
    var nome = e.nome;
    var un = fmt.format(d.quantidade) + ' un.';
    var base;
    if (op.desfazendo) {
      base = 'Desfeito em ' + nome + '. Saldo: ' + fmt.format(d.saldoAnterior) + ' → ' + fmt.format(d.saldoNovo) + ' un.';
    } else if (d.tipo === 'ENTRADA') {
      base = 'Entrada de ' + un + ' em ' + nome + '. Saldo: ' + fmt.format(d.saldoAnterior) + ' → ' + fmt.format(d.saldoNovo) + ' un.';
    } else if (d.tipo === 'SAIDA') {
      base = 'Saída de ' + un + ' em ' + nome + '. Saldo: ' + fmt.format(d.saldoAnterior) + ' → ' + fmt.format(d.saldoNovo) + ' un.';
    } else {
      base = 'Saldo de ' + nome + ' ajustado para ' + fmt.format(d.saldoNovo) + ' un. (era ' + fmt.format(d.saldoAnterior) + ').';
    }
    if (nivelDe(d.saldoAnterior, e.minimo) !== d.nivel && NIVEIS[d.nivel]) {
      base += ' Situação: ' + NIVEIS[d.nivel].texto + '.';
    }
    return base;
  }

  function avisarSucesso(e, op, d) {
    if (e.toast && e.toast.parentNode) {
      var fechar = e.toast.querySelector('.btn-ghost');
      if (fechar) {
        fechar.click();
      }
    }
    if (!SH.toast) {
      return;
    }
    var opcoes = { tom: op.desfazendo ? 'info' : 'sucesso' };
    if (!op.desfazendo) {
      opcoes.desfazer = function () {
        desfazer(e, d);
      };
    }
    var toast = SH.toast(textoDoToast(e, op, d), opcoes);
    e.toast = toast;
    if (toast) {
      // o link entra dentro do texto do toast (numa linha própria), para não disputar largura com o botão Desfazer
      var link = document.createElement('a');
      link.className = 'v3-toast-link';
      link.href = (SH.contexto || '') + '/estoque?produto=' + encodeURIComponent(e.id);
      link.textContent = 'Ver no histórico';
      var texto = toast.querySelector('.sh-toast__texto');
      if (texto) {
        texto.appendChild(link);
      }
    }
  }

  function falhar(e, mensagem) {
    e.fila = [];
    e.pendente = 0;
    e.emVoo = false;
    window.clearTimeout(e.timer);
    var antes = e.exibido;
    e.exibido = e.confirmado;
    pintar(e, e.exibido > antes ? 1 : -1);
    definirEstado(e, 'erro');
    window.setTimeout(function () {
      if (!ativo(e)) {
        definirEstado(e, '');
      }
    }, 2400);
    if (SH.toast) {
      SH.toast(mensagem + ' O saldo de ' + e.nome + ' voltou para ' + fmt.format(e.confirmado) + ' un.', { tom: 'erro' });
    }
  }

  function bombear(e) {
    if (e.emVoo || !e.fila.length) {
      if (!e.emVoo && !e.fila.length && e.pendente === 0) {
        definirEstado(e, '');
      }
      return;
    }
    var op = e.fila.shift();
    e.emVoo = true;
    definirEstado(e, 'enviando');
    enviar(e, op).then(function (r) {
      if (r.redirecionou) {
        return;
      }
      if (!r.ok) {
        falhar(e, r.erro);
        return;
      }
      var d = r.dados;
      e.emVoo = false;
      e.confirmado = d.saldoNovo;
      if (!e.fila.length && e.pendente === 0) {
        var antes = e.exibido;
        e.exibido = d.saldoNovo;
        pintar(e, e.exibido > antes ? 1 : e.exibido < antes ? -1 : 0);
        var tr = linhaDe(e);
        if (tr) {
          tr.setAttribute('data-saldo', String(d.saldoNovo));
        }
      }
      destacar(e);
      avisarSucesso(e, op, d);
      bombear(e);
    });
  }

  /* ---------- Operações ---------- */
  function descarregar(e) {
    window.clearTimeout(e.timer);
    e.timer = null;
    if (e.pendente === 0) {
      return;
    }
    var delta = e.pendente;
    e.pendente = 0;
    e.fila.push({ tipo: delta > 0 ? 'ENTRADA' : 'SAIDA', qtd: Math.abs(delta), obs: '' });
    bombear(e);
  }

  function somar(e, delta) {
    if (e.exibido + delta < 0) {
      return; // saída a partir de saldo 0 é bloqueada aqui; o servidor também recusa
    }
    if (Math.abs(e.pendente + delta) > MAX_MOV || e.exibido + delta > MAX_SALDO) {
      return;
    }
    e.pendente += delta;
    e.exibido += delta;
    pintar(e, delta);
    window.clearTimeout(e.timer);
    if (e.pendente === 0) {
      definirEstado(e, e.emVoo ? 'enviando' : '');
      return;
    }
    definirEstado(e, e.emVoo ? 'enviando' : 'pendente');
    e.timer = window.setTimeout(function () {
      descarregar(e);
    }, ATRASO_AGRUPAR);
  }

  function ajustar(e, alvo) {
    window.clearTimeout(e.timer);
    e.pendente = 0;
    var antes = e.exibido;
    e.exibido = alvo;
    e.fila.push({ tipo: 'AJUSTE', qtd: alvo, obs: OBS_AJUSTE });
    pintar(e, alvo > antes ? 1 : -1);
    bombear(e);
  }

  function desfazer(e, d) {
    var op;
    var antes = e.exibido;
    if (d.tipo === 'ENTRADA') {
      op = { tipo: 'SAIDA', qtd: d.quantidade, obs: OBS_DESFEITO, desfazendo: true };
      e.exibido = Math.max(0, e.exibido - d.quantidade);
    } else if (d.tipo === 'SAIDA') {
      op = { tipo: 'ENTRADA', qtd: d.quantidade, obs: OBS_DESFEITO, desfazendo: true };
      e.exibido = e.exibido + d.quantidade;
    } else {
      op = { tipo: 'AJUSTE', qtd: d.saldoAnterior, obs: OBS_DESFEITO, desfazendo: true };
      e.exibido = d.saldoAnterior;
    }
    e.fila.push(op);
    pintar(e, e.exibido > antes ? 1 : -1);
    bombear(e);
  }

  /* ---------- Digitação (contagem final) ---------- */
  function lerDigitado(num) {
    var limpo = num.value.replace(/\D/g, '');
    return limpo === '' ? null : parseInt(limpo, 10);
  }

  function mostrarConfirma(e, num, valido) {
    var tr = linhaDe(e);
    var aj = tr ? tr.querySelector('[data-v3-aj]') : null;
    var faixa = aj ? aj.querySelector('[data-v3-confirma]') : null;
    if (!faixa) {
      return;
    }
    var valor = lerDigitado(num);
    var alvo = faixa.querySelector('[data-v3-alvo]');
    faixa.hidden = !valido;
    aj.classList.toggle('is-editando', valido);
    if (alvo) {
      alvo.textContent = valor === null ? '' : fmt.format(valor);
    }
  }

  function cancelarEdicao(e) {
    if (!e.editando) {
      return;
    }
    e.editando = false;
    var tr = linhaDe(e);
    var num = tr ? tr.querySelector('[data-v3-num]') : null;
    if (num) {
      num.value = fmt.format(e.exibido);
      num.removeAttribute('aria-invalid');
      mostrarConfirma(e, num, false);
    }
  }

  function confirmarEdicao(e, num) {
    var valor = lerDigitado(num);
    if (valor === null || valor > MAX_SALDO) {
      num.setAttribute('aria-invalid', 'true');
      if (SH.toast) {
        SH.toast('Informe uma contagem de 0 a ' + fmt.format(MAX_SALDO) + ' un.', { tom: 'aviso' });
      }
      return;
    }
    e.editando = false;
    num.removeAttribute('aria-invalid');
    mostrarConfirma(e, num, false);
    if (valor === e.exibido) {
      num.value = fmt.format(e.exibido);
      return;
    }
    ajustar(e, valor);
  }

  /* ---------- Eventos (delegados na região permanente) ---------- */
  regiao.addEventListener('click', function (evento) {
    var alvo = evento.target.closest ? evento.target.closest('[data-v3-menos], [data-v3-mais], [data-v3-ok], [data-v3-cancelar]') : null;
    if (!alvo) {
      return;
    }
    var e = doElemento(alvo);
    if (!e) {
      return;
    }
    if (alvo.hasAttribute('data-v3-menos')) {
      if (e.editando) {
        cancelarEdicao(e);
      }
      somar(e, -1);
    } else if (alvo.hasAttribute('data-v3-mais')) {
      if (e.editando) {
        cancelarEdicao(e);
      }
      somar(e, 1);
    } else {
      var num = linhaDe(e).querySelector('[data-v3-num]');
      if (alvo.hasAttribute('data-v3-ok')) {
        confirmarEdicao(e, num);
      } else {
        cancelarEdicao(e);
      }
      num.focus();
    }
  });

  // Os botões da faixa de confirmação não tiram o foco do campo (assim o "sair do campo" não cancela antes do clique)
  regiao.addEventListener('mousedown', function (evento) {
    if (evento.target.closest && evento.target.closest('[data-v3-confirma] button')) {
      evento.preventDefault();
    }
  });

  regiao.addEventListener('input', function (evento) {
    var num = evento.target;
    if (!num.matches || !num.matches('[data-v3-num]')) {
      return;
    }
    var e = doElemento(num);
    var limpo = num.value.replace(/\D/g, '');
    if (limpo !== num.value) {
      num.value = limpo;
    }
    e.editando = true;
    var valor = lerDigitado(num);
    mostrarConfirma(e, num, valor === null || valor !== e.exibido);
    if (valor === e.exibido) {
      e.editando = false;
    }
  });

  regiao.addEventListener('focusin', function (evento) {
    var num = evento.target;
    if (num.matches && num.matches('[data-v3-num]')) {
      var e = doElemento(num);
      if (!e.editando) {
        num.value = String(e.exibido);
        num.select();
      }
    }
  });

  regiao.addEventListener('focusout', function (evento) {
    var num = evento.target;
    if (!num.matches || !num.matches('[data-v3-num]')) {
      return;
    }
    var e = doElemento(num);
    var aj = num.closest('[data-v3-aj]');
    if (evento.relatedTarget && aj && aj.contains(evento.relatedTarget)) {
      return;
    }
    if (e.editando) {
      cancelarEdicao(e);
    } else {
      num.value = fmt.format(e.exibido);
    }
  });

  regiao.addEventListener('keydown', function (evento) {
    var num = evento.target;
    if (!num.matches || !num.matches('[data-v3-num]')) {
      return;
    }
    var e = doElemento(num);
    var passo = evento.shiftKey ? 10 : 1;
    if (evento.key === 'ArrowUp' || evento.key === 'ArrowDown') {
      evento.preventDefault();
      var sinal = evento.key === 'ArrowUp' ? 1 : -1;
      if (e.editando) {
        var atual = lerDigitado(num);
        var novo = Math.max(0, Math.min(MAX_SALDO, (atual === null ? e.exibido : atual) + sinal * passo));
        num.value = String(novo);
        mostrarConfirma(e, num, novo !== e.exibido);
        e.editando = novo !== e.exibido;
      } else {
        somar(e, sinal * passo);
        num.value = String(e.exibido);
        num.select();
      }
    } else if (evento.key === 'Enter') {
      evento.preventDefault();
      if (e.editando) {
        confirmarEdicao(e, num);
        if (!e.editando) {
          num.value = String(e.exibido);
          num.select();
        }
      }
    } else if (evento.key === 'Escape') {
      if (e.editando) {
        evento.preventDefault();
        evento.stopPropagation();
        cancelarEdicao(e);
        num.value = String(e.exibido);
        num.select();
      }
    }
  });

  // Depois de a busca trocar a lista: reaplica o que ainda está em andamento e esquece o resto
  regiao.addEventListener('sh:lista-trocada', function () {
    Object.keys(estados).forEach(function (id) {
      var e = estados[id];
      if (ativo(e) && !e.editando) {
        pintar(e, 0);
        if (e.emVoo) {
          definirEstado(e, 'enviando');
        } else if (e.pendente !== 0) {
          definirEstado(e, 'pendente');
        }
      } else {
        delete estados[id];
      }
    });
  });

  // Sair da página com saldo ainda agrupado: envia já (o navegador conclui o que puder)
  window.addEventListener('pagehide', function () {
    Object.keys(estados).forEach(function (id) {
      if (estados[id].pendente !== 0) {
        descarregar(estados[id]);
      }
    });
  });
})();
