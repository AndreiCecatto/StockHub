# PrÃ³ximos passos do StockHub (documento de retomada)

Escrito em 2026-10-27 para retomar o trabalho em outra sessÃ£o, sem depender da conversa anterior.

## Estado atual
- Branch `main`, cerca de 40 commits locais **nÃ£o enviados** ao GitHub (repositÃ³rio pÃºblico). Enviar sÃ³ com aprovaÃ§Ã£o do dono: `git push origin main`, sem `--force`.
- Ãšltima suÃ­te completa: 1149+ testes (antes, 1025; os novos sÃ£o da personalizaÃ§Ã£o do inÃ­cio, item 5 fatia 1; confirmar o total e o BUILD SUCCESS na prÃ³xima execuÃ§Ã£o) (na execuÃ§Ã£o de 1025, duas execuÃ§Ãµes seguidas; uma execuÃ§Ã£o anterior, logo depois do mÃ³dulo de clientes, deu 9 erros que nÃ£o se repetiram, causa nÃ£o identificada: vale reexecutar e, se voltar, olhar `target/surefire-reports`).
- Clientes concluÃ­do (V12, RN-91 a RN-98, AD-16): cadastro completo, busca, filtros, resumo e inativaÃ§Ã£o. Falta ver as telas no navegador. NÃ£o existe vÃ­nculo venda-cliente (sem tabela de venda), entÃ£o nÃ£o hÃ¡ "total gasto em compras".
- ProtÃ³tipos das 3 direÃ§Ãµes visuais prontos em `C:\Users\andre\Documents\StockHub-prototipos\` (A EdiÃ§Ã£o, B Instrumento, C Grafite). O agente recomenda a A (serifa sÃ³ em tÃ­tulos). **Aguardando a escolha do dono** para aplicar em tokens, shell, dashboard e listas.
- Banco local `stockhub` zerado e semeado com 21 lojas de teste (uma por tipo e subtipo). Logins em `C:\Users\andre\Documents\StockHub-logins-teste.txt` (fora do repositÃ³rio; a senha também fica nesse arquivo). Os testes de concorrÃªncia apagam sÃ³ lojas `@teste.invalid`.
- O dono roda o app pelo IntelliJ na porta 8080. Agentes devem usar outra porta (8083 a 8090) e nunca encerrar a 8080.
- Para ver vÃ¡rias lojas ao mesmo tempo sem uma derrubar a sessÃ£o da outra: um endereÃ§o diferente por loja, como `http://comercio-mercado.localhost:8080` (o cookie de sessÃ£o Ã© separado por endereÃ§o). Janelas anÃ´nimas compartilham cookie entre si.
- Feito nesta rodada: formulÃ¡rios prÃ³prios (`css/formularios.css`), pÃ¡gina inicial (hash limpo, logo clicÃ¡vel, animaÃ§Ã£o por clique indo atÃ© o fim da cena), tela Estoque/MovimentaÃ§Ãµes (painel de lanÃ§amento com rolagem prÃ³pria e botÃ£o para esconder; histÃ³rico com rolagem interna), dashboard "capa do dia".

## Pedidos do dono ainda abertos (em ordem sugerida)
1. **Identidade visual do sistema logado.** O dono acha que falta algo, que destoa da pÃ¡gina inicial/login/cadastro (estes ele aprova), que o dashboard novo nÃ£o agrada e que o **tema escuro estÃ¡ feio**. Um agente produz 3 direÃ§Ãµes em protÃ³tipos HTML em `C:\Users\andre\Documents\StockHub-prototipos\`. O dono escolhe (ou mistura); sÃ³ entÃ£o aplicar em tokens, shell (menu/topo) e todas as telas. NÃ£o redesenhar sem ele ver antes.
2. **Mais informaÃ§Ã£o e filtros fÃ¡ceis** em Clientes, NF-e e OS (e revisar produtos): campos faltando, filtros simples e visÃ­veis.
3. **GrÃ¡ficos da tela AnÃ¡lises:** o dono acha os tipos de grÃ¡fico ruins; escolher formas melhores para cada dado (skill `dataviz`; SVG prÃ³prio, tabela alternativa acessÃ­vel, claro e escuro).
4. **Loja por ramo (subtipo de verdade).** Cada ramo precisa de campos e blocos prÃ³prios, opcionais, sem quebrar lojas existentes.
   - **Fatia 1 concluÃ­da** (V13, AD-17, RN-99 a RN-103, 9+ commits locais): produto com tamanho e cor (vestuÃ¡rio), lote e validade (farmÃ¡cia), validade (mercado, que Ã© o ramo de "alimentos"; nÃ£o existe subtipo ALIMENTOS) e nÃºmero de sÃ©rie (informÃ¡tica), pelo mapa `CamposRamo`.
   - **Fatia 2 em andamento** (V14, AD-18, RN-104 a RN-108): oficina com placa e modelo no `equipamento_cliente`, sÃ³ em MECANICA e MOTO. Plano de execuÃ§Ã£o: banco-migrations (V14), dev-backend, designer-ui, testador-qa.
   - **PendÃªncias da fatia 1:** (a) as JSPs de produto ainda nÃ£o tÃªm teste de renderizaÃ§Ã£o real (sÃ³ testes de controller/modelo); (b) a lista de produtos nÃ£o recebe `camposRamo` em todos os caminhos de renderizaÃ§Ã£o; (c) o fluxo de funcionÃ¡rio (membro da loja) segue sem teste dos campos por ramo e sem teste sem mock; (d) o aviso de veÃ­culo jÃ¡ cadastrado (RN-107) ainda precisa de verificaÃ§Ã£o.
5. **PersonalizaÃ§Ã£o pelo dono ("modular").** Modelo decidido: AD-19 e RN-109 a RN-114 (tabela `preferencia_bloco` por loja e tela, ausÃªncia de linha = comportamento atual, lista fechada de blocos no enum `BlocoTela`, sÃ³ DONO/GERENTE gravam, upsert sem DELETE). ExecuÃ§Ã£o fatiada:
   - **Fatia 1 concluÃ­da:** ocultar/mostrar blocos do InÃ­cio (V15, AD-19, RN-109 a RN-114; `PersonalizacaoService`, tela em `/conta/personalizacao`, `c:if` no `dashboard.jsp`, testes incluÃ­dos). Usa sÃ³ classes CSS existentes; a identidade visual segue em aberto.
   - **Fatia 2 (prÃ³xima):** colunas visÃ­veis na lista de produtos (mesma tabela, `tela = PRODUTOS`).
   - **Fatia 3:** ordem dos blocos (coluna `ordem`) e atalhos por lista fechada de destinos.
   - **PendÃªncia tÃ©cnica (anterior Ã  fatia 1):** o papel fica guardado no `UsuarioPrincipal` desde o login, entÃ£o um gerente rebaixado mantÃ©m o acesso atÃ© sair e entrar de novo (`MembroAtivoFilter` sÃ³ confere se o membro estÃ¡ ativo). Vale reler o papel a cada requisiÃ§Ã£o ou invalidar a sessÃ£o ao rebaixar.
   - **Falta verificar no navegador:** layout, teclado e responsividade da tela de personalizaÃ§Ã£o; perfis GERENTE; capa com `FAZER_AGORA` e `HOJE_NA_LOJA` ocultos.
   - SÃ³ reaplicar o visual da personalizaÃ§Ã£o depois que o dono escolher a direÃ§Ã£o visual (item 1).
   - DecisÃµes jÃ¡ tomadas na revisÃ£o do item 4: a OS antiga continua mostrando placa e modelo apÃ³s trocar o subtipo (RN-108); placa e modelo ficam sÃ³ em MECANICA e MOTO.

## DecisÃµes que dependem do dono
- Enviar os commits ao GitHub.
- Apagar o banco `stockhub_analises_tmp` (antigo, nÃ£o foi tocado).
- Teto de 5 a 10 s nos `@Lock` pessimistas.
- PendÃªncias antigas: senha `1234` do postgres, descriÃ§Ã£o e tÃ³picos do repositÃ³rio, e-mail hotmail nos commits, pasta `C:\Users\andre\Documents\StockHub-backup-contexto`, git stash antigo, testes em Firefox, Safari e leitor de tela.

## Regras de trabalho (do `CLAUDE.md` e do dono)
- Responder e comentar em portuguÃªs; commits pequenos, mensagem em portuguÃªs no imperativo, **somente no nome do dono (sem `Co-Authored-By` nem menÃ§Ã£o a Claude)**.
- Nunca commitar segredos. Antes de dizer que estÃ¡ pronto: `./mvnw test` com resultado real (PowerShell: `$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"; .\mvnw.cmd test`; a saÃ­da Ã© enorme, ler sÃ³ os totais).
- Pedir aprovaÃ§Ã£o antes de `git push`, DROP/TRUNCATE/DELETE em massa, mexer em senhas ou no `application-local.properties`, apagar branches ou reescrever histÃ³rico.
- O dono quer economia de tokens: agentes com tarefa fechada e relatÃ³rio curto; suÃ­te completa sÃ³ no fim de cada etapa.

