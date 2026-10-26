/*
 * StockHub: formulário "Registrar movimentação" (seções 4.9 e 4.10 de docs/design-system.md).
 * Substitui, na tela nova, o js/estoque.js legado (que continua existindo só para a tela antiga).
 *
 * O que este arquivo faz, tudo como melhoria: sem ele o formulário funciona com os atributos nativos.
 *  - tipo (Entrada, Saída, Ajuste): rótulo, min e max da quantidade, dica, texto do botão e obrigatoriedade
 *    da observação no Ajuste (RN-05). ENTRADA e SAIDA: 1 a 1.000.000. AJUSTE ("contagem final"): 0 a 2.000.000.000.
 *  - seletor de produto com busca (js/combobox-produto.js) e pré-seleção por ?movProduto=ID&movTipo=ENTRADA
 *    (o servidor ignora esses parâmetros: aqui o ID é validado contra as opções e o tipo contra a lista fixa);
 *  - botões − e + da quantidade;
 *  - prévia "saldo atual → saldo depois", com aviso (sem bloquear o envio) quando a saída excede o saldo:
 *    quem decide é o servidor, porque o saldo da página pode estar desatualizado;
 *  - motivos rápidos que preenchem a observação. O texto reservado "Estoque inicial" nunca é oferecido
 *    e, se digitado, é recusado aqui com uma explicação (o servidor também recusa).
 * A validação e o bloqueio de envio duplo são do js/formularios.js (form[data-sh-validar]); Ctrl+Enter, do atalhos.js.
 * Regra do projeto: nada de HTML montado com dado do usuário (só createElement e textContent).
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var MOV = SH.mov;
  var form = document.getElementById('form-movimentacao');
  if (!form || !MOV) {
    return;
  }

  var select = document.getElementById('produtoId');
  var quantidade = document.getElementById('quantidade');
  var observacao = document.getElementById('observacao');
  var rotuloQuantidade = document.getElementById('rotulo-quantidade-texto');
  var dicaQuantidade = document.getElementById('dica-quantidade');
  var marcaObrigatoria = document.getElementById('obs-obrigatoria');
  var rotuloBotao = document.getElementById('rotulo-botao');
  var previa = document.getElementById('previa-saldo');
  var previaVisual = document.getElementById('previa-visual');
  var previaAviso = document.getElementById('previa-aviso');
  var previaLeitor = document.getElementById('previa-leitor');
  var motivos = document.getElementById('motivos');
  var radios = Array.prototype.slice.call(form.querySelectorAll('input[name="tipo"]'));
  var botoesQuantidade = Array.prototype.slice.call(form.querySelectorAll('[data-sh-qtd]'));
  var chips = Array.prototype.slice.call(form.querySelectorAll('.sh-chip[data-motivo]'));
  if (!select || !quantidade || !observacao || !radios.length) {
    return;
  }

  var RESERVADO = 'estoque inicial'; // motivo do sistema (RN-04 e RN-08): nunca vira chip nem passa no formulário
  var SALDO_MAXIMO = 2000000000;

  var TIPOS = {
    ENTRADA: {
      rotulo: 'Quantidade',
      min: 1,
      max: 1000000,
      dica: 'Quantidade que entrou no estoque. Mínimo 1.',
      obrigatorio: 'Informe a quantidade.',
      botao: 'Registrar entrada',
      obsObrigatoria: false
    },
    SAIDA: {
      rotulo: 'Quantidade',
      min: 1,
      max: 1000000,
      dica: 'Quantidade que saiu do estoque. Mínimo 1.',
      obrigatorio: 'Informe a quantidade.',
      botao: 'Registrar saída',
      obsObrigatoria: false
    },
    AJUSTE: {
      rotulo: 'Contagem final',
      min: 0,
      max: SALDO_MAXIMO,
      dica: 'Saldo que o produto deve ter depois do ajuste (pode ser 0).',
      obrigatorio: 'Informe a contagem final.',
      botao: 'Registrar ajuste',
      obsObrigatoria: true
    }
  };

  function tem(objeto, chave) {
    return Object.prototype.hasOwnProperty.call(objeto, chave);
  }

  function elemento(tag, classe, texto) {
    var el = document.createElement(tag);
    if (classe) {
      el.className = classe;
    }
    if (texto !== undefined && texto !== null) {
      el.textContent = texto;
    }
    return el;
  }

  function limpar(no) {
    while (no.firstChild) {
      no.removeChild(no.firstChild);
    }
  }

  function avisarMudanca(campo) {
    campo.dispatchEvent(new window.Event('input', { bubbles: true }));
  }

  function tipoAtual() {
    for (var i = 0; i < radios.length; i++) {
      if (radios[i].checked && tem(TIPOS, radios[i].value)) {
        return radios[i].value;
      }
    }
    return 'ENTRADA';
  }

  /** Produto escolhido, lido da opção do select (serve com ou sem o seletor com busca). */
  function produtoAtual() {
    var opcao = select.options[select.selectedIndex];
    if (!opcao || !opcao.value) {
      return null;
    }
    var saldo = parseInt(opcao.getAttribute('data-saldo'), 10);
    var minimo = parseInt(opcao.getAttribute('data-minimo'), 10);
    return {
      id: opcao.value,
      nome: opcao.getAttribute('data-nome') || opcao.textContent,
      saldo: isFinite(saldo) ? saldo : 0,
      minimo: isFinite(minimo) ? minimo : 0
    };
  }

  function lerQuantidade() {
    var texto = quantidade.value.trim();
    if (texto === '') {
      return null;
    }
    var numero = Number(texto);
    if (!isFinite(numero) || Math.floor(numero) !== numero || numero < 0) {
      return null;
    }
    return numero;
  }

  /* ---------- Prévia: saldo atual -> saldo depois ---------- */
  var temporizadorLeitor = null;

  function falar(texto) {
    if (!previaLeitor) {
      return;
    }
    window.clearTimeout(temporizadorLeitor);
    // Espera a pessoa parar de digitar: o leitor de tela não repete a cada tecla
    temporizadorLeitor = window.setTimeout(function () {
      previaLeitor.textContent = texto;
    }, 700);
  }

  function mostrarAviso(texto) {
    limpar(previaAviso);
    var icone = elemento('i', 'bi bi-exclamation-triangle-fill');
    icone.setAttribute('aria-hidden', 'true');
    previaAviso.appendChild(icone);
    previaAviso.appendChild(elemento('span', null, texto));
    previaAviso.hidden = false;
  }

  function atualizarPrevia() {
    if (!previa) {
      return;
    }
    var produto = produtoAtual();
    var tipo = tipoAtual();
    var cfg = TIPOS[tipo];
    var valor = lerQuantidade();
    var fmt = MOV.formatar;

    limpar(previaVisual);
    previaAviso.hidden = true;

    if (!produto) {
      var semProduto = 'Escolha um produto para ver o saldo depois.';
      previaVisual.appendChild(elemento('p', 'sh-previa__texto', semProduto));
      falar(semProduto);
      return;
    }
    if (valor === null || valor < cfg.min || valor > cfg.max) {
      var pedido =
        'Saldo atual: ' + fmt(produto.saldo) + ' un. ' +
        (tipo === 'AJUSTE' ? 'Informe a contagem final' : 'Informe a quantidade') + ' para ver o saldo depois.';
      previaVisual.appendChild(elemento('p', 'sh-previa__texto', pedido));
      falar(pedido);
      return;
    }

    var depois;
    if (tipo === 'ENTRADA') {
      depois = produto.saldo + valor;
    } else if (tipo === 'SAIDA') {
      depois = produto.saldo - valor;
    } else {
      depois = valor;
    }

    var fala;
    if (depois < 0) {
      // Saída maior que o saldo: avisa, não bloqueia (o servidor decide, RN-02 e RN-07)
      var recusa = 'Saída maior que o saldo (' + fmt(produto.saldo) + ' un.): o sistema vai recusar.';
      previaVisual.appendChild(elemento('p', 'sh-previa__texto', 'Saldo atual: ' + fmt(produto.saldo) + ' un.'));
      mostrarAviso(recusa);
      falar(recusa);
      return;
    }

    previaVisual.appendChild(elemento('span', 'sh-previa__num sh-previa__num--antes', fmt(produto.saldo)));
    var seta = elemento('span', 'sh-previa__seta', '→');
    seta.setAttribute('aria-hidden', 'true');
    previaVisual.appendChild(seta);
    previaVisual.appendChild(elemento('span', 'sh-previa__num', fmt(depois)));
    previaVisual.appendChild(elemento('span', 'sh-previa__un', 'un.'));
    previaVisual.appendChild(MOV.criarBadge(MOV.nivel(depois, produto.minimo)));
    fala = 'Saldo de ' + fmt(produto.saldo) + ' para ' + fmt(depois) + ' unidades.';

    if (tipo === 'AJUSTE') {
      var diferenca = depois - produto.saldo;
      var texto =
        diferenca === 0
          ? 'Sem diferença em relação ao saldo atual.'
          : (diferenca > 0 ? '+' : '−') + fmt(Math.abs(diferenca)) + ' em relação ao saldo atual.';
      previaVisual.appendChild(elemento('span', 'sh-previa__diferenca', texto));
      fala += ' ' + texto;
    }
    if (depois > SALDO_MAXIMO) {
      var limite = 'O saldo passaria do limite de ' + fmt(SALDO_MAXIMO) + ' un.: o sistema vai recusar.';
      mostrarAviso(limite);
      fala += ' ' + limite;
    }
    falar(fala);
  }

  /* ---------- Motivos rápidos ---------- */
  function motivoValido(chip) {
    return MOV.simplificar(chip.getAttribute('data-motivo')) !== RESERVADO;
  }

  function textoDo(chip) {
    return chip.getAttribute('data-motivo');
  }

  function chipMarcado() {
    for (var i = 0; i < chips.length; i++) {
      if (chips[i].getAttribute('aria-pressed') === 'true') {
        return chips[i];
      }
    }
    return null;
  }

  function marcarChip(chip) {
    chips.forEach(function (outro) {
      outro.setAttribute('aria-pressed', outro === chip ? 'true' : 'false');
    });
  }

  function comecaCom(texto, prefixo) {
    return texto.slice(0, prefixo.length).toLowerCase() === prefixo.toLowerCase();
  }

  function alternarMotivo(chip) {
    if (!motivoValido(chip)) {
      return;
    }
    var anterior = chipMarcado();
    var atual = observacao.value;
    var motivo = textoDo(chip);
    if (anterior === chip) {
      // desmarcar: tira o texto do motivo e mantém o que a pessoa escreveu depois
      var resto = comecaCom(atual, motivo) ? atual.slice(motivo.length) : atual;
      observacao.value = resto.replace(/^\s*[:\-–—]?\s*/, '');
      marcarChip(null);
    } else {
      var complemento = '';
      if (anterior && comecaCom(atual, textoDo(anterior))) {
        complemento = atual.slice(textoDo(anterior).length); // troca só o motivo, mantém o complemento
      } else if (atual.trim()) {
        complemento = ': ' + atual; // a pessoa já tinha escrito algo: o motivo vai na frente
      }
      var max = parseInt(observacao.getAttribute('maxlength'), 10) || 255;
      observacao.value = (motivo + complemento).slice(0, max);
      marcarChip(chip);
    }
    avisarMudanca(observacao);
  }

  /** Mantém os chips coerentes quando a pessoa edita a observação à mão. */
  function sincronizarChips() {
    var texto = observacao.value;
    var achado = null;
    chips.forEach(function (chip) {
      if (!chip.hidden && comecaCom(texto, textoDo(chip)) && (!achado || textoDo(chip).length > textoDo(achado).length)) {
        achado = chip;
      }
    });
    marcarChip(achado);
  }

  function conferirReservado() {
    var reservado = MOV.simplificar(observacao.value.trim()) === RESERVADO;
    observacao.setCustomValidity(
      reservado ? 'Este texto é reservado ao sistema. Descreva o motivo com outras palavras.' : ''
    );
  }

  /* ---------- Tipo ---------- */
  function atualizarTipo(trocou) {
    var tipo = tipoAtual();
    var cfg = TIPOS[tipo];

    quantidade.min = String(cfg.min);
    quantidade.max = String(cfg.max);
    quantidade.setAttribute('data-sh-msg-obrigatorio', cfg.obrigatorio);
    if (rotuloQuantidade) {
      rotuloQuantidade.textContent = cfg.rotulo;
    }
    if (dicaQuantidade) {
      dicaQuantidade.textContent = cfg.dica;
    }
    observacao.required = cfg.obsObrigatoria;
    if (marcaObrigatoria) {
      marcaObrigatoria.hidden = !cfg.obsObrigatoria;
    }
    if (rotuloBotao) {
      rotuloBotao.textContent = cfg.botao;
    }

    if (trocou) {
      // O motivo do tipo anterior não vale mais: se a observação é só ele, limpa
      var marcado = chipMarcado();
      if (marcado && observacao.value.trim() === textoDo(marcado)) {
        observacao.value = '';
        avisarMudanca(observacao);
      }
    }
    chips.forEach(function (chip) {
      var doTipo = chip.getAttribute('data-tipo') === tipo && motivoValido(chip);
      chip.hidden = !doTipo;
    });
    sincronizarChips();

    // Se os campos já mostravam erro, reavalia com a nova regra
    if (trocou && SH.formularios && SH.formularios.validarCampo) {
      [quantidade, observacao].forEach(function (campo) {
        if (campo.getAttribute('aria-invalid') === 'true') {
          SH.formularios.validarCampo(campo);
        }
      });
    }
    atualizarPrevia();
  }

  /* ---------- Quantidade com − e + ---------- */
  function passo(delta) {
    var cfg = TIPOS[tipoAtual()];
    var atual = lerQuantidade();
    var novo = atual === null ? cfg.min : atual + delta;
    novo = Math.max(cfg.min, Math.min(cfg.max, novo));
    quantidade.value = String(novo);
    avisarMudanca(quantidade);
  }

  /* ---------- Seletor de produto ---------- */
  var combo = MOV.criarCombobox
    ? MOV.criarCombobox(select, {
        aoMudar: function (produto, origem) {
          atualizarPrevia();
          // Escolheu com o dedo ou o mouse: o próximo passo é a quantidade
          if (produto && origem === 'ponteiro') {
            quantidade.focus();
          }
        },
        aoConfirmar: function () {
          quantidade.focus();
          if (quantidade.select) {
            quantidade.select();
          }
        }
      })
    : null;

  /** Sem o seletor com busca (ou se ele não montou): escolhe direto no select. */
  function escolherNoSelect(id) {
    for (var i = 0; i < select.options.length; i++) {
      if (select.options[i].value === id) {
        select.value = id;
        select.dispatchEvent(new window.Event('change', { bubbles: true }));
        atualizarPrevia();
        return true;
      }
    }
    return false;
  }

  /* ---------- Ligações ---------- */
  form.classList.add('sh-mov--js');
  botoesQuantidade.forEach(function (botao) {
    botao.hidden = false;
    botao.addEventListener('click', function () {
      passo(parseInt(botao.getAttribute('data-sh-qtd'), 10) || 0);
    });
  });
  if (previa) {
    previa.hidden = false;
  }
  if (motivos) {
    motivos.hidden = false;
  }

  radios.forEach(function (radio) {
    radio.addEventListener('change', function () {
      atualizarTipo(true);
    });
  });
  quantidade.addEventListener('input', atualizarPrevia);
  select.addEventListener('change', atualizarPrevia);
  observacao.addEventListener('input', function () {
    conferirReservado();
    sincronizarChips();
  });
  chips.forEach(function (chip) {
    chip.addEventListener('click', function () {
      alternarMotivo(chip);
      observacao.focus();
    });
  });

  conferirReservado();
  atualizarTipo(false);

  /* ---------- Pré-seleção por link (?movProduto=ID&movTipo=ENTRADA) e foco ---------- */
  // Depois de um erro do servidor o foco já foi para o resumo de erros ou para o alerta: não o roubamos
  var erroDoServidor = document.querySelector('[data-sh-resumo-erros]:not([hidden]), .sh-alert--danger[role="alert"]');
  var escolheuProduto = false;
  var escolheuTipo = false;

  if (window.URLSearchParams && !select.value) {
    var parametros = new window.URLSearchParams(window.location.search);

    var tipoPedido = String(parametros.get('movTipo') || '').toUpperCase();
    if (tem(TIPOS, tipoPedido)) {
      radios.forEach(function (radio) {
        radio.checked = radio.value === tipoPedido;
      });
      atualizarTipo(false);
      escolheuTipo = true;
    }

    var idPedido = parametros.get('movProduto');
    if (idPedido && /^[1-9][0-9]{0,9}$/.test(idPedido)) {
      escolheuProduto = combo ? combo.selecionarPorId(idPedido, 'inicio') : escolherNoSelect(idPedido);
    }
  }

  function focarSeletor() {
    if (combo) {
      combo.focar();
    } else {
      select.focus();
    }
  }

  if (!erroDoServidor) {
    if (escolheuProduto) {
      quantidade.focus(); // veio de "Repor" ou "Movimentar": falta só a quantidade
    } else if (escolheuTipo || window.location.hash === '#registrar') {
      focarSeletor();
    }
  }
  // Abrir já em /estoque#registrar (atalho "m" vindo de outra tela): o navegador pode tirar o foco ao rolar
  // até o trecho depois dos scripts, então se o foco ficou sem dono, devolve ao seletor
  if (!erroDoServidor && !escolheuProduto && window.location.hash === '#registrar') {
    window.addEventListener('load', function () {
      window.setTimeout(function () {
        if (document.activeElement === document.body || !document.activeElement) {
          focarSeletor();
        }
      }, 0);
    });
  }
  window.addEventListener('hashchange', function () {
    if (window.location.hash === '#registrar') {
      focarSeletor();
    }
  });
})();
