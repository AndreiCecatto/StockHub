# Próximos passos do StockHub (documento de retomada)

Atualizado em 2026-10-27 para retomar o trabalho em outra sessão, sem depender da conversa anterior.
Detalhes visuais ficam em `docs/design-system.md`; decisões técnicas em `docs/arquitetura.md`; regras em `docs/regras-negocio.md`. Este arquivo não os repete.

## 1. Estado atual
- Branch `main` sincronizada com `origin/main`. Push autorizado pelo dono "aos poucos": nunca `--force` e sempre varrer segredos no diff antes de enviar.
- Suíte: 1206 testes, 0 falhas na última contagem confirmada (reconferir rodando a suíte).
- Banco local `stockhub` zerado em 2026-10-27 (backup em `C:\Users\andre\Documents\StockHub-backups\`) e recriado com UMA loja, "Mercado Modelo" (comércio/MERCADO), 35 produtos, 320 movimentações, 4 clientes e 1 NF-e. Login no arquivo `StockHub-logins-teste.txt`, fora do repositório. As lojas antigas e a conta `teste.loja-vazia` deixaram de existir; as OS 474 e 475 também.
- O dono roda o app pelo IntelliJ na porta 8080. Agentes usam outra porta (8083 a 8090) e nunca encerram a 8080.
- Para ver várias lojas ao mesmo tempo: um endereço por loja, como `http://comercio-mercado.localhost:8080` (cookie de sessão separado por endereço).

## 2. Concluído
- **Clientes** (V12, AD-16, RN-91 a RN-98).
- **Item 4, campos por ramo:** campos de produto por ramo (V13, AD-17, RN-99 a RN-103) e veículo da oficina (V14, AD-18, RN-104 a RN-108).
- **Item 5, personalização:** fatia 1, blocos do início (V15, AD-19, RN-109 a RN-114); fatia 2, colunas da lista de produtos (AD-20).
- **Item 1, identidade visual:** a direção A "Edição" foi REJEITADA pelo dono ("totalmente ruim"; ele queria limpo, animado, futurista tipo Apple, com tom sério) e substituída pela nova base visual: índigo frio, vidro, sem serifa, movimento (`docs/design-system.md`, seções 17 a 19). O redesign cobre tokens, lateral, dashboard, listas, formulários, personalização com interruptores e cliente/equipe/NF-e/OS. OS e revisão de NF-e foram só ajustadas por regra, sem ver renderizadas.
- **Item 3, gráficos de Análises:** concluído (composição, linha e Pareto).

## 3. Pendências técnicas
- Papel guardado no `UsuarioPrincipal`: gerente rebaixado mantém acesso até novo login (reler o papel a cada requisição ou invalidar a sessão).
- Aviso de veículo já cadastrado (RN-107) ainda sem verificação.
- Membro de loja (funcionário) sem teste sem mock.
- Contagem por situação nas abas de produtos e clientes: o backend deve enviar o mapa `contagemNiveis`.
- Pedidos de backend vindos de Análises: histograma da distribuição do valor por saída (ou dias parados); comparação período a período com `{indice, valorAtual, valorAnterior}`; lista completa de dias nas saídas por dia.
- Ajuste fino do rótulo final de "Compras e saídas" a 390 px e do ranking no escuro.
- Contraste dos ladrilhos `color-mix` ainda não medido.
- Modo de movimento reduzido só programado, não testado.
- Telas de OS e revisão de NF-e precisam ser vistas renderizadas (exige loja de oficina e nota pendente; não criar contas sem o dono pedir).
- Telas ainda não capturadas: impressão, erros de formulário, paleta de comandos, perfis GERENTE e OPERADOR.
- `componentes.css` mantém `.form-control` e `.form-label` antigos, sobrescritos por `formularios.css`: limpar.

## 4. Itens restantes do dono
- **Item 2:** mais campos e filtros fáceis em NF-e e OS; revisar produtos (Clientes já feito).
- **Item 5, fatia 3 da personalização:** coluna `ordem` dos blocos e atalhos por lista fechada de destinos.

## 5. Dados e limpeza que dependem do dono
- Banco `stockhub_analises_tmp`.
- Senha `1234` do postgres (já está no histórico público).
- Descrição e tópicos do repositório; e-mail hotmail nos commits.
- Pasta `C:\Users\andre\Documents\StockHub-backup-contexto` e git stash antigo.
- Testes em Firefox, Safari e leitor de tela.
- Teto de 5 a 10 s nos `@Lock` pessimistas.

## Regras de trabalho (do `CLAUDE.md` e do dono)
- Responder e comentar em português; commits pequenos, mensagem em português no imperativo, **somente no nome do dono (sem `Co-Authored-By` nem menção a Claude)**.
- Nunca commitar segredos. Antes de dizer que está pronto: `./mvnw test` com resultado real (PowerShell: `$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"; .\mvnw.cmd test`; a saída é enorme, ler só os totais).
- Pedir aprovação antes de `git push`, DROP/TRUNCATE/DELETE em massa, mexer em senhas ou no `application-local.properties`, apagar branches ou reescrever histórico.
- O design deve ser conferido com capturas (Edge headless + CDP) antes de aplicar em massa; a preferência visual está registrada na memória do projeto.
- Contagens de testes relatadas por agentes erraram duas vezes: rodar `.\mvnw.cmd test` e ler a linha final.
- O dono quer economia de tokens: agentes com tarefa fechada e relatório curto; suíte completa só no fim de cada etapa.
- Agentes nunca criam contas nem migrations por conta própria. A V16 foi criada sem pedido (aceita), mas toda migration deve passar pelo `banco-migrations`.
