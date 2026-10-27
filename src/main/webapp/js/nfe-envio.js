/*
 * StockHub: envio do XML da NF-e (tela /compras/nfe).
 *
 * Marcas da JSP (todas em css/nfe.css e WEB-INF/pages/nfe-lista.jsp):
 *   form[data-nfe-envio]  data-url, data-max-bytes, data-max-mb, data-login
 *   [data-nfe-zona]       área que aceita arquivo arrastado
 *   [data-nfe-estado]     texto de estado (role="status"): arquivo escolhido, "Enviando…"
 *   [data-nfe-erro]       alerta de erro (role="alert", hidden); [data-nfe-erro-texto] recebe a mensagem
 *   [data-nfe-enviar]     botão de envio; [data-nfe-enviar-rotulo] o texto dele
 *
 * Envio: fetch POST com FormData (só o campo "arquivo"), cabeçalhos X-CSRF-TOKEN (lido do campo oculto _csrf),
 * X-Requested-With: XMLHttpRequest e Accept: application/json. Respostas tratadas: 200 {ok, redirect}, 422 {erro},
 * 413, 400, 429 (Retry-After), 403 e 401. A validação de extensão e tamanho aqui é só conveniência: o servidor
 * valida de novo. Sem JavaScript o formulário envia comum. Mensagens entram por textContent.
 */
(function () {
  'use strict';

  var form = document.querySelector('form[data-nfe-envio]');
  if (!form) {
    return;
  }

  var SH = window.SH || {};
  var entrada = form.querySelector('input[type="file"]');
  var zona = form.querySelector('[data-nfe-zona]');
  var estado = form.querySelector('[data-nfe-estado]');
  var caixaErro = form.querySelector('[data-nfe-erro]');
  var textoErro = form.querySelector('[data-nfe-erro-texto]');
  var botao = form.querySelector('[data-nfe-enviar]');
  var rotulo = form.querySelector('[data-nfe-enviar-rotulo]');
  var rotuloOriginal = rotulo ? rotulo.textContent : '';
  var maxBytes = parseInt(form.getAttribute('data-max-bytes'), 10) || 2097152;
  var maxMb = form.getAttribute('data-max-mb') || '2';
  var urlEnvio = form.getAttribute('data-url');
  var urlLogin = form.getAttribute('data-login') || '/login';
  var enviando = false;

  // Com JS a mensagem de "selecione o arquivo" é a nossa (acessível e em português), não o balão do navegador
  form.noValidate = true;

  function token() {
    var campo = form.querySelector('input[type="hidden"][name="_csrf"]');
    return campo ? campo.value : '';
  }

  function tamanhoLegivel(bytes) {
    if (bytes < 1024) {
      return bytes + ' B';
    }
    if (bytes < 1048576) {
      return (bytes / 1024).toFixed(1).replace('.', ',') + ' KB';
    }
    return (bytes / 1048576).toFixed(2).replace('.', ',') + ' MB';
  }

  function mostrarErro(mensagem) {
    textoErro.textContent = mensagem;
    caixaErro.hidden = false;
    caixaErro.focus();
  }

  function limparErro() {
    caixaErro.hidden = true;
    textoErro.textContent = '';
    entrada.removeAttribute('aria-invalid');
  }

  function definirEnviando(valor) {
    enviando = valor;
    botao.disabled = valor;
    entrada.disabled = valor;
    form.setAttribute('aria-busy', valor ? 'true' : 'false');
    if (valor) {
      rotulo.textContent = 'Enviando…';
      botao.classList.add('is-enviando');
      var giro = document.createElement('span');
      giro.className = 'sh-spinner';
      giro.setAttribute('aria-hidden', 'true');
      giro.setAttribute('data-nfe-giro', '');
      botao.insertBefore(giro, botao.firstChild);
      var icone = botao.querySelector('.bi');
      if (icone) {
        icone.hidden = true;
      }
      estado.textContent = 'Enviando e lendo o arquivo…';
    } else {
      rotulo.textContent = rotuloOriginal;
      botao.classList.remove('is-enviando');
      var g = botao.querySelector('[data-nfe-giro]');
      if (g) {
        g.parentNode.removeChild(g);
      }
      var ic = botao.querySelector('.bi');
      if (ic) {
        ic.hidden = false;
      }
    }
  }

  /** Confere extensão e tamanho (conveniência). Devolve o texto do problema, ou '' se estiver bom. */
  function problemaDo(arquivo) {
    if (!arquivo) {
      return 'Selecione o arquivo XML da NF-e.';
    }
    if (!/\.xml$/i.test(arquivo.name)) {
      return 'O arquivo precisa ter a extensão .xml. Envie o XML da nota, não o PDF (DANFE).';
    }
    if (arquivo.size === 0) {
      return 'O arquivo está vazio. Escolha o XML da nota.';
    }
    if (arquivo.size > maxBytes) {
      return 'O arquivo tem ' + tamanhoLegivel(arquivo.size) + ' e passa do limite de ' + maxMb + ' MB.';
    }
    return '';
  }

  function aoEscolher() {
    limparErro();
    var arquivo = entrada.files && entrada.files[0];
    if (!arquivo) {
      estado.textContent = '';
      return;
    }
    var problema = problemaDo(arquivo);
    if (problema) {
      estado.textContent = '';
      entrada.setAttribute('aria-invalid', 'true');
      mostrarErro(problema);
      return;
    }
    estado.textContent = 'Arquivo escolhido: ' + arquivo.name + ' (' + tamanhoLegivel(arquivo.size) + ').';
  }

  function lerResposta(resposta) {
    return resposta.text().then(function (texto) {
      try {
        return JSON.parse(texto);
      } catch (e) {
        return null;
      }
    });
  }

  function tempoDeEspera(resposta) {
    var segundos = parseInt(resposta.headers.get('Retry-After'), 10);
    if (isNaN(segundos) || segundos <= 0) {
      return '';
    }
    if (segundos < 90) {
      return ' Tente de novo em cerca de ' + segundos + ' segundos.';
    }
    return ' Tente de novo em cerca de ' + Math.ceil(segundos / 60) + ' minutos.';
  }

  function caminhoSeguro(destino) {
    // só aceita ir para a revisão de uma importação, dentro do próprio site
    return typeof destino === 'string' && /^\/compras\/nfe\/importacoes\/[0-9a-fA-F-]{36}$/.test(destino);
  }

  function enviar() {
    var arquivo = entrada.files && entrada.files[0];
    var problema = problemaDo(arquivo);
    if (problema) {
      entrada.setAttribute('aria-invalid', 'true');
      mostrarErro(problema);
      entrada.focus();
      return;
    }
    limparErro();
    definirEnviando(true);

    var dados = new window.FormData();
    dados.append('arquivo', arquivo, arquivo.name);

    window.fetch(urlEnvio, {
      method: 'POST',
      body: dados,
      credentials: 'same-origin',
      headers: {
        'X-CSRF-TOKEN': token(),
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
      }
    }).then(function (resposta) {
      return lerResposta(resposta).then(function (corpo) {
        var status = resposta.status;
        if (status === 200 && corpo && corpo.ok && caminhoSeguro(corpo.redirect)) {
          estado.textContent = 'Nota lida. Abrindo a revisão…';
          window.location.assign((SH.contexto || '') + corpo.redirect);
          return;
        }
        definirEnviando(false);
        estado.textContent = '';
        var mensagem;
        if (status !== 429 && corpo && typeof corpo.erro === 'string' && corpo.erro) {
          mensagem = corpo.erro; // o texto do servidor sempre vence (entra por textContent)
        } else if (status === 413) {
          mensagem = 'O arquivo passa do limite de ' + maxMb + ' MB.';
        } else if (status === 429) {
          mensagem = ((corpo && corpo.erro) || 'Muitos envios em pouco tempo.') + tempoDeEspera(resposta);
        } else if (status === 400) {
          mensagem = 'O envio não foi entendido. Escolha o arquivo XML de novo e tente outra vez.';
        } else if (status === 403) {
          mensagem = 'Não foi possível validar o envio (token de segurança expirado). Recarregue a página e tente de novo.';
        } else if (status === 401) {
          mensagem = 'Sua sessão expirou. Entre de novo para enviar a nota.';
        } else {
          mensagem = (corpo && corpo.erro) || 'Não foi possível enviar a nota agora. Tente de novo em instantes.';
        }
        mostrarErro(mensagem);
        if (status === 401) {
          var link = document.createElement('a');
          link.href = urlLogin;
          link.textContent = ' Ir para o login';
          textoErro.appendChild(link);
        }
      });
    }).catch(function () {
      definirEnviando(false);
      estado.textContent = '';
      mostrarErro('Sem conexão com o servidor. Verifique a internet e tente de novo.');
    });
  }

  form.addEventListener('submit', function (evento) {
    evento.preventDefault();
    if (!enviando) {
      enviar();
    }
  });

  entrada.addEventListener('change', aoEscolher);

  /* ---------- Arrastar e soltar ---------- */
  if (zona) {
    ['dragenter', 'dragover'].forEach(function (nome) {
      zona.addEventListener(nome, function (evento) {
        evento.preventDefault();
        zona.classList.add('is-arrastando');
      });
    });
    ['dragleave', 'dragend'].forEach(function (nome) {
      zona.addEventListener(nome, function () {
        zona.classList.remove('is-arrastando');
      });
    });
    zona.addEventListener('drop', function (evento) {
      evento.preventDefault();
      zona.classList.remove('is-arrastando');
      var arquivos = evento.dataTransfer && evento.dataTransfer.files;
      if (!arquivos || !arquivos.length || enviando) {
        return;
      }
      try {
        entrada.files = arquivos;
      } catch (e) {
        mostrarErro('Seu navegador não aceitou o arquivo arrastado. Use o botão para escolher o arquivo.');
        return;
      }
      aoEscolher();
    });
  }
})();
