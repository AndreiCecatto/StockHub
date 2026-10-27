/*
 * StockHub v3: console "Registrar movimentação" (seção 12 de docs/design-system.md).
 *
 * O que este arquivo faz, tudo como melhoria: sem ele o formulário funciona com os atributos nativos.
 *  - tipo (Entrada, Saída, Ajuste): rótulo, min e max da quantidade, dica, texto do botão e obrigatoriedade
 *    da observação no Ajuste (RN-05). ENTRADA e SAIDA: 1 a 1.000.000. AJUSTE ("contagem final"): 0 a 2.000.000.000.
 *  - seletor de produto com busca (js/combobox-produto.js) e pré-seleção por ?movProduto=ID&movTipo=ENTRADA
 *    (o servidor ignora esses parâmetros: aqui o ID é validado contra as opções e o tipo contra a lista fixa);
 *  - botões − e + da quantidade;
 *  - prévia "saldo atual → saldo depois" com selo e medidor, e aviso (sem bloquear o envio) quando a saída excede o
 *    saldo: quem decide é o servidor, porque o saldo da página pode estar desatualizado. Os nós da prévia são fixos
 *    (JSP): o JS só troca texto e classe, então nada pisca a cada tecla;
 *  - motivos rápidos que preenchem a observação. O texto reservado "Estoque inicial" nunca é oferecido
 *    e, se digitado, é recusado aqui com uma explicação (o servidor também recusa);
 *  - atalhos de uma tecla (E, S, A escolhem o tipo; F vai ao filtro de produto), com as mesmas regras de js/atalhos.js:
 *    nunca digitando em campo, nunca com Ctrl/Alt/Meta, respeitando a chave "Atalhos de teclado ativados".
 * A validação e o bloqueio de envio duplo são do js/formularios.js (form[data-sh-validar]); Ctrl+Enter, do atalhos.js.
 * O envio sem recarregar e o livro-razão são do js/v3-estoque.js, que usa SH.mov.formulario daqui.
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
  var previaSelo = document.getElementById('previa-selo');
  var previaAntes = document.getElementById('previa-antes');
  var previaDepois = document.getElementById('previa-depois');
  var previaDelta = document.getElementById('previa-delta');
  var previaMedidor = document.getElementById('previa-medidor');
  var previaFill = document.getElementById('previa-fill');
  var previaTexto = document.getElementById('previa-texto');
  var previaAviso = document.getElementById('previa-aviso');
  var previaLeitor = document.getElementById('previa-leitor');
  var motivos = document.getElementById('motivos');
  var radios = Array.prototype.slice.call(form.querySelectorAll('input[name="tipo"]'));
  var botoesQuantidade = Array.prototype.slice.call(form.querySelectorAll('[data-sh-qtd]'));
  var chips = Array.prototype.slice.call(form.querySelectorAll('.v3e-chip[data-motivo]'));
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

  var CLASSE_NIVEL = { NORMAL: 'normal', BAIXO: 'baixo', SEM_ESTOQUE: 'sem' };

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
  var nivelDoSelo = null;
  var ultimoDepois = null;

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

  /** Troca discreta do número (opacidade e 3 px de subida); nada com menos movimento. */
  function pulsar(el) {
    if (!el.animate || (SH.reduzMovimento && SH.reduzMovimento())) {
      return;
    }
    el.animate(
      [
        { opacity: 0.35, transform: 'translateY(3px)' },
        { opacity: 1, transform: 'none' }
      ],
      { duration: 180, easing: 'cubic-bezier(.2,.8,.2,1)' }
    );
  }

  function mostrarAviso(texto) {
    limpar(previaAviso);
    var icone = elemento('i', 'bi bi-exclamation-triangle-fill');
    icone.setAttribute('aria-hidden', 'true');
    previaAviso.appendChild(icone);
    previaAviso.appendChild(elemento('span', null, texto));
    previaAviso.hidden = false;
  }

  function definirSelo(nivel) {
    if (nivel === nivelDoSelo) {
      return;
    }
    nivelDoSelo = nivel;
    limpar(previaSelo);
    if (nivel) {
      previaSelo.appendChild(MOV.criarBadge(nivel));
    }
  }

  /** Medidor como o da tag medidorEstoque: barra até saldo / (2 x mínimo), em degraus de 5 %, marcador no mínimo. */
  function definirMedidor(saldo, minimo, nivel) {
    if (!previaMedidor || !previaFill) {
      return;
    }
    if (nivel === null || minimo <= 0) {
      previaMedidor.hidden = true;
      return;
    }
    var pct = Math.max(0, Math.min(100, (saldo * 50) / minimo));
    var passo = Math.round(pct / 5) * 5;
    if (saldo > 0 && passo === 0) {
      passo = 5;
    }
    previaFill.className = 'v3e-medidor__fill v3e-p-' + passo;
    previaMedidor.className = 'sh-meter v3e-medidor v3e-medidor--' + CLASSE_NIVEL[nivel];
    previaMedidor.hidden = false;
  }

  function escrever(depois) {
    if (depois !== ultimoDepois) {
      ultimoDepois = depois;
      previaDepois.textContent = depois;
      previaDepois.classList.toggle('is-vazio', depois === '—');
      pulsar(previaDepois);
    }
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

    previaAviso.hidden = true;
    previaDelta.textContent = '';
    previaDelta.className = 'v3e-previa__delta sh-mono';

    if (!produto) {
      var semProduto = 'Escolha um produto para ver o saldo depois.';
      previaAntes.textContent = '—';
      escrever('—');
      definirSelo(null);
      definirMedidor(0, 0, null);
      previaTexto.textContent = semProduto;
      falar(semProduto);
      return;
    }

    var nivelAtual = MOV.nivel(produto.saldo, produto.minimo);
    previaAntes.textContent = fmt(produto.saldo);

    if (valor === null || valor < cfg.min || valor > cfg.max) {
      var pedido =
        'Saldo atual: ' + fmt(produto.saldo) + ' un. ' +
        (tipo === 'AJUSTE' ? 'Informe a contagem final' : 'Informe a quantidade') + ' para ver o saldo depois.';
      escrever('—');
      definirSelo(nivelAtual);
      definirMedidor(produto.saldo, produto.minimo, nivelAtual);
      previaTexto.textContent = pedido;
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

    if (depois < 0) {
      // Saída maior que o saldo: avisa, não bloqueia (o servidor decide, RN-02 e RN-07)
      var recusa = 'Saída maior que o saldo (' + fmt(produto.saldo) + ' un.): o sistema vai recusar.';
      escrever('—');
      definirSelo(nivelAtual);
      definirMedidor(produto.saldo, produto.minimo, nivelAtual);
      previaTexto.textContent = '';
      mostrarAviso(recusa);
      falar(recusa);
      return;
    }

    var nivelDepois = MOV.nivel(depois, produto.minimo);
    escrever(fmt(depois));
    definirSelo(nivelDepois);
    definirMedidor(depois, produto.minimo, nivelDepois);
    previaTexto.textContent = '';

    var diferenca = depois - produto.saldo;
    if (diferenca === 0) {
      previaDelta.textContent = '±0';
    } else {
      previaDelta.textContent = (diferenca > 0 ? '+' : '−') + fmt(Math.abs(diferenca));
      previaDelta.className += diferenca > 0 ? ' v3e-previa__delta--mais' : ' v3e-previa__delta--menos';
    }

    var fala = 'Saldo de ' + fmt(produto.saldo) + ' para ' + fmt(depois) + ' unidades.';
    if (tipo === 'AJUSTE') {
      fala +=
        ' ' + (diferenca === 0 ? 'Sem diferença em relação ao saldo atual.' : (diferenca > 0 ? '+' : '−') + fmt(Math.abs(diferenca)) + ' em relação ao saldo atual.');
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

  function focarSeletor() {
    if (combo) {
      combo.focar();
    } else {
      select.focus();
    }
  }

  function escolherTipo(tipo) {
    for (var i = 0; i < radios.length; i++) {
      if (radios[i].value === tipo && !radios[i].checked) {
        radios[i].checked = true;
        radios[i].dispatchEvent(new window.Event('change', { bubbles: true }));
      }
    }
  }

  /* ---------- Ligações ---------- */
  form.classList.add('v3e--js');
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

  /* ---------- Atalhos de uma tecla: E, S, A (tipo) e F (filtro) ---------- */
  var ultimoPrefixoIr = 0;

  function emCampo(alvo) {
    if (!alvo || !alvo.tagName) {
      return false;
    }
    var t = alvo.tagName.toLowerCase();
    if (t === 'textarea' || t === 'select') {
      return true;
    }
    if (t === 'input') {
      var tipo = (alvo.getAttribute('type') || 'text').toLowerCase();
      return ['checkbox', 'radio', 'button', 'submit', 'reset', 'range', 'color', 'file', 'image'].indexOf(tipo) === -1;
    }
    return !!alvo.isContentEditable;
  }

  document.addEventListener('keydown', function (evento) {
    if (evento.defaultPrevented || evento.repeat || evento.ctrlKey || evento.altKey || evento.metaKey || evento.shiftKey) {
      return;
    }
    if (SH.armazenar && SH.armazenar.ler('sh-atalhos') === 'desligados') {
      return;
    }
    if (document.body.classList.contains('modal-open') || document.querySelector('dialog[open]') || emCampo(evento.target)) {
      return;
    }
    var tecla = evento.key ? evento.key.toLowerCase() : '';
    if (tecla === 'g') {
      ultimoPrefixoIr = Date.now(); // "g" + letra é a navegação do sistema (atalhos.js): não é nosso
      return;
    }
    if (Date.now() - ultimoPrefixoIr < 1500) {
      return;
    }
    var tipoPedido = tecla === 'e' ? 'ENTRADA' : tecla === 's' ? 'SAIDA' : tecla === 'a' ? 'AJUSTE' : '';
    if (tipoPedido) {
      evento.preventDefault();
      escolherTipo(tipoPedido);
      if (produtoAtual()) {
        quantidade.focus();
        if (quantidade.select) {
          quantidade.select();
        }
      } else {
        focarSeletor();
      }
    } else if (tecla === 'm') {
      // Aqui o "m" do atalhos.js não pode digitar a letra no seletor: o preventDefault vem antes (fase de captura)
      evento.preventDefault();
      focarSeletor();
    } else if (tecla === 'f') {
      var filtro = document.getElementById('filtro-produto');
      if (filtro) {
        evento.preventDefault();
        filtro.focus();
      }
    }
  }, true);

  /* ---------- API para o envio sem recarregar (js/v3-estoque.js) ---------- */
  function limparMarcasDeErro() {
    [select, quantidade, observacao].forEach(function (campo) {
      campo.classList.remove('is-invalid');
      campo.removeAttribute('aria-invalid');
    });
    var entradaCombo = document.getElementById('produto-busca');
    if (entradaCombo) {
      entradaCombo.classList.remove('is-invalid');
      entradaCombo.removeAttribute('aria-invalid');
    }
    Array.prototype.forEach.call(form.querySelectorAll('.sh-erro-campo'), function (erro) {
      erro.hidden = true;
    });
    var resumo = form.querySelector('[data-sh-resumo-erros]');
    if (resumo) {
      resumo.hidden = true;
    }
    var alerta = form.querySelector('[data-v3e-alerta]');
    if (alerta) {
      alerta.hidden = true;
    }
  }

  SH.mov.formulario = {
    form: form,
    combo: combo,
    focarSeletor: focarSeletor,
    tipoAtual: tipoAtual,
    atualizarProduto: function (id, saldo) {
      return combo ? combo.atualizarProduto(id, saldo) : false;
    },
    /** Formulário pronto para o próximo lançamento: mantém só o tipo. Não mexe no foco. */
    reiniciar: function () {
      limparMarcasDeErro();
      if (combo) {
        combo.limpar();
      } else {
        select.value = '';
        select.dispatchEvent(new window.Event('change', { bubbles: true }));
      }
      quantidade.value = '';
      observacao.value = '';
      marcarChip(null);
      conferirReservado();
      avisarMudanca(observacao); // atualiza o contador de caracteres
      atualizarTipo(false);
    },
    /** Tira as marcas de erro e o resumo (antes de mostrar os do servidor). */
    limparErros: limparMarcasDeErro
  };

  /* ---------- Pré-seleção por link (?movProduto=ID&movTipo=ENTRADA) e foco ---------- */
  // Depois de um erro do servidor o foco já foi para o resumo de erros ou para o alerta: não o roubamos
  var erroDoServidor = document.querySelector('[data-sh-resumo-erros]:not([hidden]), .sh-alert--danger[role="alert"]:not([hidden]):not([data-v3e-alerta])');
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
