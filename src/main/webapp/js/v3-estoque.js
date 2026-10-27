/*
 * StockHub v3: envio da movimentação sem recarregar e livro-razão (estoque.jsp).
 *
 * Melhoria progressiva: sem este arquivo o formulário faz POST normal (redirect + mensagem). Com ele:
 *  - POST /estoque/movimentar por fetch, com "Accept: application/json" EXPLÍCITO (sem ele o servidor redireciona)
 *    e o token CSRF no cabeçalho X-CSRF-TOKEN (lido do campo oculto _csrf do formulário);
 *  - sucesso (200): linha nova no topo do livro-razão (fade + deslocamento curto das demais, destaque que desbota),
 *    último lançamento no console, contagem de registros, toast, formulário limpo e foco no seletor de produto;
 *  - com filtros ativos, ou fora da página 1, a linha só entra se casar com o filtro e estiver na página 1 (senão, só toast);
 *  - erro: 400 (por campo), 422 (regra), 404, 403 (CSRF), 401 (sessão expirada: vai ao /login), falha de rede.
 *    O digitado nunca se perde; o foco vai ao campo certo ou ao alerta;
 *  - resposta que não seja o JSON esperado: cai para o envio normal.
 * A validação em português e o bloqueio de envio duplo são do js/formularios.js (este arquivo roda DEPOIS dele).
 * Tudo entra por createElement e textContent (nunca HTML montado com dado).
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var form = document.getElementById('form-movimentacao');
  var razao = document.querySelector('[data-v3e-razao]');
  var F = SH.mov && SH.mov.formulario;
  if (!form || !razao || !F || !window.fetch || !window.URLSearchParams) {
    return;
  }

  var grupos = razao.querySelector('[data-v3e-grupos]');
  var colunas = razao.querySelector('[data-v3e-colunas]');
  var recibo = form.querySelector('[data-v3e-recibo]');
  var alerta = form.querySelector('[data-v3e-alerta]');
  var alertaTexto = form.querySelector('[data-v3e-alerta-texto]');
  var resumo = form.querySelector('[data-sh-resumo-erros]');
  var campoProduto = document.getElementById('produtoId');
  var campoQuantidade = document.getElementById('quantidade');
  var campoObservacao = document.getElementById('observacao');
  var emAndamento = false;

  var TIPOS = {
    ENTRADA: { cls: 'entrada', icone: 'bi-box-arrow-in-down', rotulo: 'Entrada', sinal: '+' },
    SAIDA: { cls: 'saida', icone: 'bi-box-arrow-up', rotulo: 'Saída', sinal: '−' },
    AJUSTE: { cls: 'ajuste', icone: 'bi-sliders', rotulo: 'Ajuste', sinal: '=' }
  };

  function el(tag, classe, texto) {
    var e = document.createElement(tag);
    if (classe) {
      e.className = classe;
    }
    if (texto !== undefined && texto !== null) {
      e.textContent = texto;
    }
    return e;
  }

  function icone(classe) {
    var i = el('i', 'bi ' + classe);
    i.setAttribute('aria-hidden', 'true');
    return i;
  }

  function oculto(texto) {
    return el('span', 'visually-hidden', texto);
  }

  function fmt(n) {
    return SH.mov.formatar(n);
  }

  function dois(n) {
    return (n < 10 ? '0' : '') + n;
  }

  function reduz() {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  }

  /* ---------- Cabeçalhos da requisição ---------- */
  function token() {
    var campo = form.querySelector('input[type="hidden"][name="_csrf"]');
    return campo ? campo.value : '';
  }

  function corpo() {
    var p = new window.URLSearchParams();
    p.set('produtoId', campoProduto.value);
    var marcado = form.querySelector('input[name="tipo"]:checked');
    p.set('tipo', marcado ? marcado.value : '');
    p.set('quantidade', campoQuantidade.value.trim());
    p.set('observacao', campoObservacao.value);
    return p;
  }

  /* ---------- Erros ---------- */
  function mostrarCampo(campo, idErro, texto) {
    var erro = document.getElementById(idErro);
    if (erro) {
      var destino = erro.querySelector('.sh-erro-campo__texto') || erro;
      destino.textContent = texto;
      erro.hidden = false;
    }
    campo.classList.add('is-invalid');
    campo.setAttribute('aria-invalid', 'true');
    var combo = document.getElementById('produto-busca');
    if (campo === campoProduto && combo) {
      combo.classList.add('is-invalid');
      combo.setAttribute('aria-invalid', 'true');
    }
  }

  function focarAlerta(texto) {
    alertaTexto.textContent = texto;
    alerta.hidden = false;
    alerta.focus();
  }

  function mostrarErros(dados, status) {
    F.limparErros();
    var campos = dados && dados.campos ? dados.campos : null;
    if (status === 400 && campos) {
      var mapa = [
        ['produtoId', campoProduto, 'erro-produtoId', 'produtoId'],
        ['tipo', form.querySelector('input[name="tipo"]'), 'erro-tipo', 'tipo-ENTRADA'],
        ['quantidade', campoQuantidade, 'erro-quantidade', 'quantidade'],
        ['observacao', campoObservacao, 'erro-observacao', 'observacao']
      ];
      var lista = resumo.querySelector('ul');
      while (lista.firstChild) {
        lista.removeChild(lista.firstChild);
      }
      var n = 0;
      mapa.forEach(function (m) {
        if (campos[m[0]]) {
          n++;
          mostrarCampo(m[1], m[2], String(campos[m[0]]));
          var li = el('li');
          var a = el('a', null, String(campos[m[0]]));
          a.href = '#' + m[3];
          a.setAttribute('data-sh-ir-campo', m[3]);
          li.appendChild(a);
          lista.appendChild(li);
        }
      });
      if (n) {
        resumo.querySelector('.sh-resumo-erros__titulo').textContent =
          n === 1 ? 'Corrija 1 campo para continuar:' : 'Corrija ' + n + ' campos para continuar:';
        resumo.hidden = false;
        resumo.focus();
        return;
      }
    }
    var msg = (dados && dados.erro) || 'Não foi possível registrar agora. Tente de novo.';
    if (status === 422) {
      // Regra de negócio: marca o campo a que a mensagem se refere e leva o foco a ele
      var alvo = null;
      if (/reservad|observa|motivo/i.test(msg)) {
        alvo = ['observacao', campoObservacao, 'erro-observacao'];
      } else if (/saldo|quantidade|contagem|unidade|limite/i.test(msg)) {
        alvo = ['quantidade', campoQuantidade, 'erro-quantidade'];
      }
      if (alvo) {
        mostrarCampo(alvo[1], alvo[2], msg);
        alvo[1].focus();
        if (alvo[1].select) {
          alvo[1].select();
        }
        return;
      }
    }
    focarAlerta(msg);
  }

  /* ---------- Livro-razão ---------- */
  function hojeIso(d) {
    d = d || new Date();
    return d.getFullYear() + '-' + dois(d.getMonth() + 1) + '-' + dois(d.getDate());
  }

  function casaComFiltros(r, iso) {
    var d = razao.dataset;
    if (d.v3eFiltroProduto && d.v3eFiltroProduto !== String(r.produtoId)) {
      return false;
    }
    if (d.v3eFiltroTipo && d.v3eFiltroTipo !== r.tipo) {
      return false;
    }
    if (d.v3eFiltroDe && iso < d.v3eFiltroDe) {
      return false;
    }
    if (d.v3eFiltroAte && iso > d.v3eFiltroAte) {
      return false;
    }
    return true;
  }

  function criarLinha(r, observacao, agora) {
    var t = TIPOS[r.tipo] || TIPOS.AJUSTE;
    var li = el('li', 'v3e-linha v3e-linha--nova');
    li.setAttribute('data-sh-linha', '');
    li.setAttribute('data-v3e-id', String(r.movimentacaoId));

    var hora = el('time', 'v3e-hora sh-mono');
    hora.setAttribute('datetime', hojeIso(agora) + 'T' + dois(agora.getHours()) + ':' + dois(agora.getMinutes()));
    hora.appendChild(oculto('às '));
    hora.appendChild(document.createTextNode(dois(agora.getHours()) + ':' + dois(agora.getMinutes())));

    var tipo = el('span', 'v3e-tipo');
    var badge = el('span', 'sh-badge sh-badge--' + t.cls);
    badge.appendChild(icone(t.icone));
    badge.appendChild(document.createTextNode(t.rotulo));
    tipo.appendChild(badge);

    var prod = el('div', 'v3e-prod');
    var nome = el('a', 'v3e-prod__nome', r.nome);
    nome.href = (SH.contexto || '') + '/produtos/' + encodeURIComponent(r.produtoId);
    nome.setAttribute('data-sh-peek', '');
    nome.setAttribute('data-sh-peek-titulo', r.nome);
    prod.appendChild(nome);
    if (observacao) {
      prod.appendChild(el('p', 'v3e-obs', observacao));
    }

    var qtd = el('span', 'v3e-qtd-col v3e-qtd-col--' + t.cls + ' sh-mono');
    qtd.appendChild(oculto(t.rotulo + ' de '));
    qtd.appendChild(document.createTextNode(t.sinal + fmt(r.quantidade)));
    qtd.appendChild(oculto(' un.' + (r.tipo === 'AJUSTE' ? ' (contagem final)' : '')));

    var saldo = el('span', 'v3e-saldo sh-mono');
    saldo.appendChild(oculto('Saldo de '));
    saldo.appendChild(el('span', 'v3e-saldo__antes', fmt(r.saldoAnterior)));
    saldo.appendChild(icone('bi-arrow-right'));
    saldo.appendChild(oculto(' para '));
    saldo.appendChild(el('strong', null, fmt(r.saldoNovo)));

    // "Desfazer": mesmo desenho da linha do servidor, só quando o papel pode e a movimentação muda o saldo
    if (razao.getAttribute('data-v3e-pode-desfazer') === 'sim' && r.saldoAnterior !== r.saldoNovo) {
      prod.appendChild(criarDesfazer(r.movimentacaoId));
    }

    [hora, tipo, prod, qtd, saldo].forEach(function (c) {
      li.appendChild(c);
    });
    return li;
  }

  function criarDesfazer(id) {
    var det = el('details', 'v3e-desfazer');
    det.appendChild(el('summary', 'v3e-desfazer__abrir', 'Desfazer'));
    var f = el('form', 'v3e-desfazer__form');
    f.method = 'post';
    f.action = (SH.contexto || '') + '/estoque/' + encodeURIComponent(id) + '/estornar';
    var csrf = form.querySelector('input[type="hidden"][name="_csrf"]');
    if (csrf) {
      var oculto = el('input');
      oculto.type = 'hidden';
      oculto.name = csrf.name;
      oculto.value = csrf.value;
      f.appendChild(oculto);
    }
    var rotulo = el('label', 'visually-hidden', 'Motivo de desfazer');
    rotulo.setAttribute('for', 'motivo-estorno-' + id);
    var campo = el('input', 'form-control form-control-sm');
    campo.id = 'motivo-estorno-' + id;
    campo.type = 'text';
    campo.name = 'motivo';
    campo.maxLength = 200;
    campo.required = true;
    campo.autocomplete = 'off';
    campo.placeholder = 'Por que está desfazendo?';
    var confirmar = el('button', 'btn btn-outline-secondary btn-sm', 'Confirmar');
    confirmar.type = 'submit';
    f.appendChild(rotulo);
    f.appendChild(campo);
    f.appendChild(confirmar);
    det.appendChild(f);
    return det;
  }

  function criarGrupoHoje(iso, agora) {
    var sec = el('section', 'v3e-dia');
    sec.setAttribute('data-v3e-dia', iso);
    sec.setAttribute('aria-labelledby', 'dia-' + iso);
    var h = el('h3', 'v3e-dia__titulo');
    h.id = 'dia-' + iso;
    var time = el('time');
    time.setAttribute('datetime', iso);
    time.appendChild(el('strong', null, 'Hoje'));
    time.appendChild(document.createTextNode(' '));
    time.appendChild(el('span', 'sh-mono', dois(agora.getDate()) + '/' + dois(agora.getMonth() + 1)));
    h.appendChild(time);
    h.appendChild(el('span', 'v3e-dia__n'));
    sec.appendChild(h);
    sec.appendChild(el('ol', 'v3e-lista'));
    return sec;
  }

  function contarDias() {
    Array.prototype.forEach.call(grupos.querySelectorAll('.v3e-dia'), function (sec) {
      var n = sec.querySelectorAll('.v3e-linha:not(.v3e-linha--pendente)').length;
      var alvo = sec.querySelector('[data-v3e-dia-n]');
      if (alvo) {
        alvo.textContent = n + (n === 1 ? ' movimento' : ' movimentos');
      }
    });
  }

  /** Insere com FLIP: o que já estava na lista desliza para baixo (transform), a linha nova entra por fade. */
  function inserirNoTopo(li, iso, agora) {
    var antes = [];
    var itens = Array.prototype.slice.call(grupos.querySelectorAll('.v3e-linha, .v3e-dia__titulo'));
    itens.forEach(function (e) {
      antes.push(e.getBoundingClientRect().top);
    });

    var primeiro = grupos.querySelector('.v3e-dia');
    var grupo;
    if (primeiro && primeiro.getAttribute('data-v3e-dia') === iso) {
      grupo = primeiro;
    } else {
      grupo = criarGrupoHoje(iso, agora);
      grupos.insertBefore(grupo, primeiro);
    }
    var lista = grupo.querySelector('.v3e-lista');
    lista.insertBefore(li, lista.firstChild);

    // Página de 50: a última linha sai (o resto está na próxima página)
    var tamanho = parseInt(razao.getAttribute('data-v3e-tamanho'), 10) || 50;
    var todas = grupos.querySelectorAll('.v3e-linha');
    if (todas.length > tamanho) {
      var ultima = todas[todas.length - 1];
      var secUltima = ultima.closest('.v3e-dia');
      ultima.parentNode.removeChild(ultima);
      if (secUltima && !secUltima.querySelector('.v3e-linha')) {
        secUltima.parentNode.removeChild(secUltima);
      }
    }
    contarDias();

    if (!reduz()) {
      var altura = window.innerHeight || 800;
      itens.forEach(function (e, i) {
        if (!e.isConnected || !e.animate) {
          return;
        }
        var delta = antes[i] - e.getBoundingClientRect().top;
        if (Math.abs(delta) < 1 || antes[i] > altura + 200) {
          return;
        }
        e.animate([{ transform: 'translateY(' + delta + 'px)' }, { transform: 'none' }], {
          duration: 240,
          easing: 'cubic-bezier(.2,.8,.2,1)'
        });
      });
    }
    window.setTimeout(function () {
      li.classList.remove('v3e-linha--nova');
    }, 6000);
  }

  function atualizarTotal() {
    var total = razao.querySelector('[data-v3e-total]');
    var rotulo = razao.querySelector('[data-v3e-total-rotulo]');
    if (!total) {
      return;
    }
    var n = (parseInt(total.getAttribute('data-v3e-total'), 10) || 0) + 1;
    total.setAttribute('data-v3e-total', String(n));
    total.textContent = fmt(n);
    if (rotulo) {
      rotulo.textContent = n === 1 ? 'registro' : 'registros';
    }
  }

  /* ---------- Último lançamento ---------- */
  function mostrarRecibo(r, agora) {
    var t = TIPOS[r.tipo] || TIPOS.AJUSTE;
    while (recibo.firstChild) {
      recibo.removeChild(recibo.firstChild);
    }
    var cab = el('div', 'v3e-recibo__cab');
    cab.appendChild(el('span', 'v3e-recibo__rotulo', 'Último lançamento'));
    cab.appendChild(el('span', 'v3e-recibo__hora sh-mono', dois(agora.getHours()) + ':' + dois(agora.getMinutes())));
    var linha = el('div', 'v3e-recibo__linha');
    var badge = el('span', 'sh-badge sh-badge--' + t.cls);
    badge.appendChild(icone(t.icone));
    badge.appendChild(document.createTextNode(t.rotulo));
    linha.appendChild(badge);
    linha.appendChild(el('span', 'sh-mono v3e-qtd-col--' + t.cls, t.sinal + fmt(r.quantidade)));
    var saldo = el('span', 'v3e-recibo__saldo sh-mono');
    saldo.appendChild(document.createTextNode(fmt(r.saldoAnterior) + ' → '));
    saldo.appendChild(el('strong', null, fmt(r.saldoNovo)));
    linha.appendChild(saldo);
    linha.appendChild(SH.mov.criarBadge(r.nivel));
    recibo.appendChild(cab);
    recibo.appendChild(el('div', 'v3e-recibo__produto', r.nome));
    recibo.appendChild(linha);
    // reinicia a animação de entrada
    recibo.hidden = true;
    void recibo.offsetWidth;
    recibo.hidden = false;
  }

  /* ---------- Sucesso ---------- */
  function sucesso(r, observacao) {
    // hora gravada pelo servidor (sem fuso, horário de Brasília); sem ela, cai para a do navegador
    var agora = r.criadoEm ? new Date(r.criadoEm) : new Date();
    if (isNaN(agora.getTime())) {
      agora = new Date();
    }
    var iso = hojeIso(agora);
    if (r.observacao !== undefined && r.observacao !== null) {
      observacao = r.observacao;
    }
    F.atualizarProduto(r.produtoId, r.saldoNovo);

    var naPrimeira = (razao.getAttribute('data-v3e-pagina') || '1') === '1';
    var entra = naPrimeira && casaComFiltros(r, iso);
    if (entra) {
      var vazios = razao.querySelectorAll('[data-v3e-vazio]');
      Array.prototype.forEach.call(vazios, function (v) {
        v.hidden = true;
      });
      if (colunas) {
        colunas.hidden = false;
      }
      inserirNoTopo(criarLinha(r, observacao, agora), iso, agora);
      atualizarTotal();
    }

    mostrarRecibo(r, agora);
    F.reiniciar();
    F.focarSeletor();

    var opcoes = { tom: 'sucesso' };
    if (!entra) {
      opcoes.acao = {
        rotulo: 'Ver histórico',
        aoClicar: function () {
          window.location.assign((SH.contexto || '') + '/estoque');
        }
      };
    }
    if (SH.toast) {
      SH.toast(r.mensagem || 'Movimentação registrada.', opcoes);
    }
  }

  /* ---------- Envio ---------- */
  var pendente = null;
  var temporizadorPendente = null;

  function mostrarPendente() {
    if (!grupos.querySelector('.v3e-linha') && !grupos.querySelector('.v3e-dia')) {
      return; // histórico vazio: o estado vazio já ocupa o lugar
    }
    var primeiro = grupos.querySelector('.v3e-lista');
    if (!primeiro) {
      return;
    }
    pendente = el('li', 'v3e-linha v3e-linha--pendente');
    pendente.setAttribute('aria-hidden', 'true');
    pendente.appendChild(el('span', 'sh-esqueleto v3e-pendente__a'));
    pendente.appendChild(el('span', 'sh-esqueleto v3e-pendente__b'));
    primeiro.insertBefore(pendente, primeiro.firstChild);
  }

  function tirarPendente() {
    window.clearTimeout(temporizadorPendente);
    if (pendente && pendente.parentNode) {
      pendente.parentNode.removeChild(pendente);
    }
    pendente = null;
  }

  function terminar() {
    emAndamento = false;
    tirarPendente();
    form.removeAttribute('data-sh-enviando-agora');
    if (SH.formularios) {
      SH.formularios.desbloquear(form);
    }
  }

  function envioNormal() {
    emAndamento = false;
    if (SH.formularios) {
      SH.formularios.desbloquear(form);
    }
    form.submit(); // não dispara o evento "submit": vale o POST comum com redirect
  }

  document.addEventListener('submit', function (evento) {
    if (evento.target !== form) {
      return;
    }
    if (evento.defaultPrevented) {
      return; // a validação do formularios.js recusou: ela já mostrou os erros
    }
    evento.preventDefault();
    if (emAndamento) {
      return;
    }
    emAndamento = true;
    var observacao = campoObservacao.value.trim();
    F.limparErros();
    window.setTimeout(function () {
      if (emAndamento && SH.formularios) {
        form.setAttribute('data-sh-enviando-agora', 'sim');
        SH.formularios.bloquear(form);
      }
    }, 20);
    temporizadorPendente = window.setTimeout(mostrarPendente, 350);

    window
      .fetch(form.action, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
          'X-CSRF-TOKEN': token()
        },
        body: corpo().toString()
      })
      .then(function (resp) {
        var tipo = resp.headers.get('Content-Type') || '';
        if (resp.status === 401 || (resp.redirected && /\/login/.test(resp.url))) {
          terminar();
          window.location.assign((SH.contexto || '') + '/login');
          return null;
        }
        if (tipo.indexOf('json') === -1) {
          envioNormal();
          return null;
        }
        return resp.json().then(
          function (dados) {
            return { status: resp.status, dados: dados };
          },
          function () {
            envioNormal();
            return null;
          }
        );
      })
      .then(function (res) {
        if (!res) {
          return;
        }
        terminar();
        if (res.status === 200 && res.dados && res.dados.ok === true && res.dados.movimentacaoId !== undefined) {
          sucesso(res.dados, observacao);
        } else if (res.status === 403) {
          focarAlerta('A sessão de segurança expirou. Recarregue a página e registre de novo; o que você digitou não foi enviado.');
        } else if (res.dados && res.dados.ok === false) {
          mostrarErros(res.dados, res.status);
        } else {
          envioNormal();
        }
      })
      .catch(function () {
        terminar();
        focarAlerta('Sem conexão com o servidor. Confira a rede e tente de novo; nada foi registrado.');
      });
  });

  // Linhas já na página também entram no ritmo do livro-razão: contagem por dia
  contarDias();
  // Linha destacada depois do redirect (envio sem JavaScript): tira a marca depois de um tempo
  window.setTimeout(function () {
    var nova = grupos.querySelector('.v3e-linha--nova');
    if (nova) {
      nova.classList.remove('v3e-linha--nova');
    }
  }, 6000);
})();
