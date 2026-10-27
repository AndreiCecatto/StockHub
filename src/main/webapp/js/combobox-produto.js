/*
 * StockHub: seletor de produto com busca da tela de Movimentações (seção 4.9 de docs/design-system.md).
 *
 * Padrão ARIA "combobox editável com lista" (APG): input role="combobox" + listbox + options, com
 * aria-expanded, aria-controls, aria-autocomplete="list" e aria-activedescendant.
 * Teclas: setas navegam, Home/End vão ao primeiro/último (com a lista aberta), Enter escolhe,
 * Esc fecha (e, fechada, limpa), digitar filtra.
 *
 * Melhoria progressiva: o <select name="produtoId"> da JSP é a fonte dos dados e o campo que vai no POST.
 * Com este script ele fica oculto (classe sh-combo--ativo) e o seletor assume; sem o script, o select funciona.
 * Cada <option> traz data-nome, data-saldo e data-minimo (o servidor não envia a categoria).
 *
 * Regra do projeto: nunca montar HTML com dado do usuário. Tudo entra por createElement e textContent;
 * o trecho encontrado vira <mark> por nós de texto.
 *
 * Expõe SH.mov.criarCombobox(select, opcoes) e alguns utilitários de saldo (SH.mov.nivel, SH.mov.criarBadge).
 * v3: o objeto devolvido também tem limpar() e atualizarProduto(id, saldo), usados pelo envio sem recarregar.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var MOV = (SH.mov = SH.mov || {});
  var MAXIMO_RESULTADOS = 50;
  var MARCAS = /[̀-ͯ]/g;
  var formatador = window.Intl && window.Intl.NumberFormat ? new window.Intl.NumberFormat('pt-BR') : null;

  /* ---------- Utilidades de texto e saldo ---------- */

  MOV.formatar = function (numero) {
    return formatador ? formatador.format(numero) : String(numero);
  };

  /** Texto sem acento e em minúsculas, mais o mapa de cada letra para o texto original (para o destaque). */
  function simplificar(texto) {
    var norma = '';
    var indices = [];
    for (var i = 0; i < texto.length; i++) {
      var parte = texto.charAt(i).normalize('NFD').replace(MARCAS, '').toLowerCase();
      for (var k = 0; k < parte.length; k++) {
        norma += parte.charAt(k);
        indices.push(i);
      }
    }
    return { norma: norma, indices: indices };
  }

  MOV.simplificar = function (texto) {
    return simplificar(String(texto || '')).norma;
  };

  /** Mesma regra do servidor (RN-12): 0 = sem estoque; até o mínimo = baixo; acima = normal. */
  MOV.nivel = function (saldo, minimo) {
    if (saldo <= 0) {
      return 'SEM_ESTOQUE';
    }
    return saldo <= minimo ? 'BAIXO' : 'NORMAL';
  };

  /** Badge igual ao de tags/badgeEstoque.tag: ícone + palavra + cor (nunca só a cor). */
  MOV.criarBadge = function (nivel) {
    var modelos = {
      NORMAL: ['sh-badge--normal', 'bi-check-circle-fill', 'Normal'],
      BAIXO: ['sh-badge--baixo', 'bi-exclamation-triangle-fill', 'Baixo'],
      SEM_ESTOQUE: ['sh-badge--sem', 'bi-x-octagon-fill', 'Sem estoque']
    };
    var modelo = modelos[nivel] || modelos.NORMAL;
    var badge = document.createElement('span');
    badge.className = 'sh-badge ' + modelo[0];
    var icone = document.createElement('i');
    icone.className = 'bi ' + modelo[1];
    icone.setAttribute('aria-hidden', 'true');
    badge.appendChild(icone);
    badge.appendChild(document.createTextNode(modelo[2]));
    return badge;
  };

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

  /** Escreve o nome no elemento, com os trechos que casam com a busca dentro de <mark>. */
  function escreverNome(destino, produto, palavras) {
    limpar(destino);
    var marcados = [];
    palavras.forEach(function (palavra) {
      var inicio = 0;
      var posicao = produto.mapa.norma.indexOf(palavra, inicio);
      while (palavra && posicao !== -1) {
        marcados.push([produto.mapa.indices[posicao], produto.mapa.indices[posicao + palavra.length - 1] + 1]);
        inicio = posicao + palavra.length;
        posicao = produto.mapa.norma.indexOf(palavra, inicio);
      }
    });
    marcados.sort(function (a, b) {
      return a[0] - b[0];
    });
    var unidos = [];
    marcados.forEach(function (faixa) {
      var ultimo = unidos[unidos.length - 1];
      if (ultimo && faixa[0] <= ultimo[1]) {
        ultimo[1] = Math.max(ultimo[1], faixa[1]);
      } else {
        unidos.push([faixa[0], faixa[1]]);
      }
    });
    var cursor = 0;
    unidos.forEach(function (faixa) {
      if (faixa[0] > cursor) {
        destino.appendChild(document.createTextNode(produto.nome.slice(cursor, faixa[0])));
      }
      var marca = document.createElement('mark');
      marca.textContent = produto.nome.slice(faixa[0], faixa[1]);
      destino.appendChild(marca);
      cursor = faixa[1];
    });
    if (cursor < produto.nome.length) {
      destino.appendChild(document.createTextNode(produto.nome.slice(cursor)));
    }
  }

  /**
   * Monta o seletor sobre o <select>.
   * opcoes.aoMudar(produto|null, origem): chamado quando a escolha muda ("ponteiro", "teclado", "limpar", "inicio").
   * Devolve { entrada, selecionado(), selecionarPorId(id), focar(abrir) } ou null se não der para montar.
   */
  MOV.criarCombobox = function (select, opcoes) {
    opcoes = opcoes || {};
    if (!select || !String.prototype.normalize) {
      return null; // sem normalize não dá para buscar sem acento: o select comum continua valendo
    }
    var raiz = select.closest('[data-sh-combo]');
    var rotulo = document.querySelector('label[for="' + select.id + '"]');
    if (!raiz) {
      return null;
    }

    /* ---------- Dados: lidos das opções do select ---------- */
    var produtos = [];
    var porId = {};
    Array.prototype.forEach.call(select.options, function (op) {
      if (!op.value) {
        return;
      }
      var saldo = parseInt(op.getAttribute('data-saldo'), 10);
      var minimo = parseInt(op.getAttribute('data-minimo'), 10);
      var nome = op.getAttribute('data-nome') || op.textContent;
      var produto = {
        id: op.value,
        nome: nome,
        saldo: isFinite(saldo) ? saldo : 0,
        minimo: isFinite(minimo) ? minimo : 0,
        mapa: simplificar(nome)
      };
      produtos.push(produto);
      porId[produto.id] = produto;
    });
    if (!produtos.length) {
      return null;
    }

    /* ---------- Construção da interface ---------- */
    var idBase = 'produto';
    var ui = elemento('div', 'sh-combo__ui');
    var campo = elemento('div', 'sh-combo__campo');
    var lupa = elemento('i', 'bi bi-search');
    lupa.setAttribute('aria-hidden', 'true');

    var entrada = elemento('input', 'form-control sh-combo__input');
    entrada.type = 'text';
    entrada.id = idBase + '-busca';
    entrada.setAttribute('role', 'combobox');
    entrada.setAttribute('aria-autocomplete', 'list');
    entrada.setAttribute('aria-expanded', 'false');
    entrada.setAttribute('aria-controls', idBase + '-lista');
    entrada.setAttribute('aria-describedby', 'dica-produto erro-produtoId');
    entrada.setAttribute('autocomplete', 'off');
    entrada.setAttribute('autocapitalize', 'none');
    entrada.setAttribute('spellcheck', 'false');
    entrada.setAttribute('placeholder', 'Buscar produto pelo nome');
    entrada.setAttribute('data-sh-seletor-produto', '');
    // Sem dono: o campo não entra no formulário nem na validação do formularios.js (o valor vai pelo select)
    entrada.setAttribute('form', 'sh-sem-formulario');

    var popup = elemento('div', 'sh-combo__popup');
    popup.hidden = true;
    var lista = elemento('ul', 'sh-combo__lista');
    lista.id = idBase + '-lista';
    lista.setAttribute('role', 'listbox');
    lista.setAttribute('aria-label', 'Produtos encontrados');
    var aviso = elemento('p', 'sh-combo__aviso');
    aviso.hidden = true;
    popup.appendChild(lista);
    popup.appendChild(aviso);

    campo.appendChild(lupa);
    campo.appendChild(entrada);
    campo.appendChild(popup);

    var cartao = elemento('div', 'sh-combo__cartao');
    cartao.hidden = true;

    var status = elemento('div', 'visually-hidden');
    status.setAttribute('role', 'status');
    status.setAttribute('aria-live', 'polite');

    ui.appendChild(campo);
    ui.appendChild(cartao);
    ui.appendChild(status);
    // Logo depois do rótulo: a mensagem de erro (que vem depois do select) fica abaixo do componente
    if (rotulo && rotulo.nextSibling) {
      rotulo.parentNode.insertBefore(ui, rotulo.nextSibling);
    } else {
      raiz.insertBefore(ui, select);
    }

    raiz.classList.add('sh-combo--ativo');
    select.setAttribute('tabindex', '-1');
    select.setAttribute('aria-hidden', 'true');
    var dica = document.getElementById('dica-produto');
    if (dica) {
      dica.hidden = false;
    }
    // O rótulo passa a apontar para o campo de busca; o select oculto vira só portador do valor
    if (rotulo) {
      rotulo.setAttribute('for', entrada.id);
    }
    // Validação do formularios.js ou link do resumo de erros mandam o foco ao select: leva ao campo visível
    select.addEventListener('focus', function () {
      entrada.focus();
    });
    // O select oculto leva as marcas de erro (is-invalid, aria-invalid): espelha no campo visível
    function espelharErro() {
      var invalido = select.classList.contains('is-invalid') || select.getAttribute('aria-invalid') === 'true';
      entrada.classList.toggle('is-invalid', invalido);
      if (invalido) {
        entrada.setAttribute('aria-invalid', 'true');
      } else {
        entrada.removeAttribute('aria-invalid');
      }
    }
    if (window.MutationObserver) {
      new window.MutationObserver(espelharErro).observe(select, {
        attributes: true,
        attributeFilter: ['class', 'aria-invalid']
      });
    }
    espelharErro();

    /* ---------- Estado ---------- */
    var selecionado = null;
    var resultados = [];
    var ativo = -1;
    var aberto = false;
    var temporizadorStatus = null;

    function anunciar(texto) {
      window.clearTimeout(temporizadorStatus);
      temporizadorStatus = window.setTimeout(function () {
        status.textContent = '';
        window.setTimeout(function () {
          status.textContent = texto;
        }, 40);
      }, 350);
    }

    function textoSaldo(produto) {
      return 'Saldo ' + MOV.formatar(produto.saldo) + ' un.';
    }

    /* ---------- Lista ---------- */
    function palavrasDa(consulta) {
      return simplificar(consulta)
        .norma.split(/\s+/)
        .filter(Boolean);
    }

    function filtrar(consulta) {
      var palavras = palavrasDa(consulta);
      var achados = produtos.filter(function (produto) {
        return palavras.every(function (palavra) {
          return produto.mapa.norma.indexOf(palavra) !== -1;
        });
      });
      if (!achados.length && palavras.length && SH.buscaDifusa) {
        // nada contém o texto: tenta nomes parecidos (erro de digitação), do mais ao menos parecido
        var notas = produtos
          .map(function (produto) {
            return { produto: produto, nota: SH.buscaDifusa.pontuar(consulta, produto.nome) };
          })
          .filter(function (par) {
            return par.nota > 0;
          })
          .sort(function (a, b) {
            return b.nota - a.nota;
          });
        if (notas.length) {
          return {
            palavras: palavras,
            achados: notas.map(function (par) {
              return par.produto;
            }),
            aproximado: true
          };
        }
      }
      return { palavras: palavras, achados: achados, aproximado: false };
    }

    function definirAtivo(indice) {
      var opcoesDom = lista.children;
      if (ativo >= 0 && opcoesDom[ativo]) {
        opcoesDom[ativo].setAttribute('aria-selected', 'false');
      }
      ativo = indice;
      if (indice >= 0 && opcoesDom[indice]) {
        opcoesDom[indice].setAttribute('aria-selected', 'true');
        entrada.setAttribute('aria-activedescendant', opcoesDom[indice].id);
        if (opcoesDom[indice].scrollIntoView) {
          opcoesDom[indice].scrollIntoView({ block: 'nearest' });
        }
      } else {
        entrada.removeAttribute('aria-activedescendant');
      }
    }

    function fechar() {
      aberto = false;
      popup.hidden = true;
      entrada.setAttribute('aria-expanded', 'false');
      definirAtivo(-1);
    }

    function desenhar(consulta) {
      var busca = filtrar(consulta);
      resultados = busca.achados.slice(0, MAXIMO_RESULTADOS);
      limpar(lista);
      ativo = -1;
      entrada.removeAttribute('aria-activedescendant');
      resultados.forEach(function (produto, i) {
        var item = elemento('li', 'sh-combo__opcao');
        item.id = idBase + '-op-' + produto.id;
        item.setAttribute('role', 'option');
        item.setAttribute('aria-selected', 'false');
        item.setAttribute('data-indice', String(i));
        var nome = elemento('span', 'sh-combo__nome');
        escreverNome(nome, produto, busca.aproximado ? [] : busca.palavras);
        if (busca.aproximado) {
          nome.appendChild(document.createTextNode(' '));
          nome.appendChild(elemento('span', 'sh-parecido', 'parecido'));
        }
        var meta = elemento('span', 'sh-combo__meta');
        meta.appendChild(elemento('span', null, MOV.formatar(produto.saldo) + ' un.'));
        meta.appendChild(MOV.criarBadge(MOV.nivel(produto.saldo, produto.minimo)));
        item.appendChild(nome);
        item.appendChild(meta);
        lista.appendChild(item);
      });

      limpar(aviso);
      if (!busca.achados.length) {
        aviso.appendChild(document.createTextNode('Nenhum produto com esse nome. '));
        var link = elemento('a', null, 'Cadastrar produto');
        link.href = (SH.contexto || '') + '/produtos/novo';
        aviso.appendChild(link);
        aviso.hidden = false;
        anunciar('Nenhum produto encontrado.');
      } else if (busca.aproximado) {
        aviso.textContent = 'Nenhum produto com esse nome. Estes são parecidos com o que você digitou.';
        aviso.hidden = false;
        anunciar('Nenhum produto com esse nome. ' + busca.achados.length + ' parecidos.');
      } else if (busca.achados.length > MAXIMO_RESULTADOS) {
        aviso.textContent =
          'Mostrando ' + MAXIMO_RESULTADOS + ' de ' + busca.achados.length + ' produtos. Refine a busca para ver os outros ' +
          (busca.achados.length - MAXIMO_RESULTADOS) + '.';
        aviso.hidden = false;
        anunciar(busca.achados.length + ' produtos encontrados. Refine a busca.');
      } else {
        aviso.hidden = true;
        anunciar(busca.achados.length === 1 ? '1 produto encontrado.' : busca.achados.length + ' produtos encontrados.');
      }
    }

    function abrir(consulta) {
      desenhar(consulta);
      aberto = true;
      popup.hidden = false;
      entrada.setAttribute('aria-expanded', 'true');
    }

    /* ---------- Cartão do produto escolhido ---------- */
    function mostrarCartao(produto) {
      limpar(cartao);
      if (!produto) {
        cartao.hidden = true;
        return;
      }
      var saldo = elemento('p', 'sh-combo__saldo');
      saldo.appendChild(document.createTextNode('Saldo '));
      saldo.appendChild(elemento('strong', null, MOV.formatar(produto.saldo)));
      saldo.appendChild(elemento('span', 'sh-mono', 'mín. ' + MOV.formatar(produto.minimo)));
      saldo.appendChild(MOV.criarBadge(MOV.nivel(produto.saldo, produto.minimo)));

      var acoes = elemento('div', 'sh-combo__acoes');
      var ver = elemento('a', null, 'Ver produto');
      ver.href = (SH.contexto || '') + '/produtos/' + encodeURIComponent(produto.id);
      var trocar = elemento('button', 'btn btn-ghost btn-sm', 'Trocar produto');
      trocar.type = 'button';
      trocar.addEventListener('click', function () {
        definirSelecao(null, 'limpar');
        entrada.value = '';
        entrada.focus();
        abrir('');
      });
      acoes.appendChild(ver);
      acoes.appendChild(trocar);
      cartao.appendChild(saldo);
      cartao.appendChild(acoes);
      cartao.hidden = false;
    }

    /* ---------- Escolha ---------- */
    function definirSelecao(produto, origem) {
      selecionado = produto;
      select.value = produto ? produto.id : '';
      // O evento "change" no select limpa o erro mostrado pelo formularios.js
      select.dispatchEvent(new window.Event('change', { bubbles: true }));
      mostrarCartao(produto);
      if (opcoes.aoMudar) {
        opcoes.aoMudar(produto, origem);
      }
    }

    function escolher(produto, origem) {
      entrada.value = produto.nome;
      definirSelecao(produto, origem);
      fechar();
      anunciar(produto.nome + ' selecionado. ' + textoSaldo(produto) + '.');
    }

    /* ---------- Eventos ---------- */
    entrada.addEventListener('input', function () {
      if (selecionado && entrada.value !== selecionado.nome) {
        definirSelecao(null, 'limpar'); // o texto deixou de ser o produto escolhido
      }
      abrir(entrada.value);
    });

    entrada.addEventListener('click', function () {
      if (!aberto) {
        abrir(selecionado ? '' : entrada.value);
      }
    });

    entrada.addEventListener('keydown', function (evento) {
      var tecla = evento.key;
      if (tecla === 'ArrowDown' || tecla === 'Down') {
        evento.preventDefault();
        if (!aberto) {
          abrir(selecionado ? '' : entrada.value);
          definirAtivo(resultados.length ? 0 : -1);
        } else if (resultados.length) {
          definirAtivo(Math.min(ativo + 1, resultados.length - 1));
        }
      } else if (tecla === 'ArrowUp' || tecla === 'Up') {
        evento.preventDefault();
        if (!aberto) {
          abrir(selecionado ? '' : entrada.value);
          definirAtivo(resultados.length ? resultados.length - 1 : -1);
        } else if (resultados.length) {
          definirAtivo(ativo <= 0 ? 0 : ativo - 1);
        }
      } else if ((tecla === 'Home' || tecla === 'End') && aberto && resultados.length) {
        evento.preventDefault();
        definirAtivo(tecla === 'Home' ? 0 : resultados.length - 1);
      } else if (tecla === 'Enter') {
        if (aberto && resultados.length && (ativo >= 0 || !selecionado)) {
          evento.preventDefault();
          // Sem opção ativa e sem produto escolhido, Enter escolhe o primeiro resultado (o caminho mais curto de quem digitou)
          escolher(resultados[ativo >= 0 ? ativo : 0], 'teclado');
        } else if (aberto && selecionado) {
          evento.preventDefault(); // lista aberta sem opção ativa: mantém o produto e segue
          fechar();
          if (opcoes.aoConfirmar) {
            opcoes.aoConfirmar();
          }
        } else if (!selecionado) {
          evento.preventDefault(); // sem produto, Enter não envia o formulário
          abrir(entrada.value);
        } else {
          evento.preventDefault();
          if (opcoes.aoConfirmar) {
            opcoes.aoConfirmar();
          }
        }
      } else if (tecla === 'Escape' || tecla === 'Esc') {
        if (aberto) {
          evento.preventDefault();
          fechar();
        } else if (entrada.value) {
          evento.preventDefault();
          definirSelecao(null, 'limpar');
          entrada.value = '';
        }
      } else if (tecla === 'Tab') {
        fechar();
      }
    });

    // Clicar numa opção não tira o foco do campo (mousedown) e escolhe no click
    popup.addEventListener('mousedown', function (evento) {
      evento.preventDefault();
    });
    lista.addEventListener('click', function (evento) {
      var item = evento.target.closest ? evento.target.closest('[role="option"]') : null;
      if (item) {
        var produto = resultados[parseInt(item.getAttribute('data-indice'), 10)];
        if (produto) {
          escolher(produto, 'ponteiro');
        }
      }
    });
    lista.addEventListener('mousemove', function (evento) {
      var item = evento.target.closest ? evento.target.closest('[role="option"]') : null;
      if (item) {
        var indice = parseInt(item.getAttribute('data-indice'), 10);
        if (indice !== ativo) {
          definirAtivo(indice);
        }
      }
    });

    entrada.addEventListener('blur', function (evento) {
      if (!ui.contains(evento.relatedTarget)) {
        fechar();
      }
    });
    document.addEventListener('click', function (evento) {
      if (aberto && !ui.contains(evento.target)) {
        fechar();
      }
    });

    /* ---------- Estado inicial (produto devolvido pelo servidor depois de um erro, por exemplo) ---------- */
    if (select.value && porId[select.value]) {
      selecionado = porId[select.value];
      entrada.value = selecionado.nome;
      mostrarCartao(selecionado);
      if (opcoes.aoMudar) {
        opcoes.aoMudar(selecionado, 'inicio');
      }
    }

    return {
      entrada: entrada,
      selecionado: function () {
        return selecionado;
      },
      selecionarPorId: function (id, origem) {
        var produto = porId[String(id)];
        if (!produto) {
          return false;
        }
        escolher(produto, origem || 'inicio');
        return true;
      },
      focar: function () {
        entrada.focus();
      },
      /** Tira a escolha e limpa o texto (formulário pronto para o próximo lançamento). Não mexe no foco. */
      limpar: function () {
        fechar();
        definirSelecao(null, 'limpar');
        entrada.value = '';
      },
      /**
       * Atualiza o saldo de um produto sem recarregar (depois de um lançamento): a lista de busca, o cartão do
       * produto escolhido e a opção do select (que é a fonte dos dados para o resto da tela).
       */
      atualizarProduto: function (id, saldo) {
        var produto = porId[String(id)];
        var numero = parseInt(saldo, 10);
        if (!produto || !isFinite(numero)) {
          return false;
        }
        produto.saldo = numero;
        Array.prototype.forEach.call(select.options, function (op) {
          if (op.value === produto.id) {
            op.setAttribute('data-saldo', String(numero));
            op.textContent = produto.nome + ' (saldo: ' + numero + ' un.)';
          }
        });
        if (selecionado && selecionado.id === produto.id) {
          mostrarCartao(produto);
        }
        if (aberto) {
          desenhar(entrada.value);
        }
        return true;
      }
    };
  };
})();
