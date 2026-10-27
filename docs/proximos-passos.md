# Próximos passos do StockHub (documento de retomada)

Escrito em 2026-10-27 para retomar o trabalho em outra sessão, sem depender da conversa anterior.

## Estado atual
- Branch `main`, cerca de 40 commits locais **não enviados** ao GitHub (repositório público). Enviar só com aprovação do dono: `git push origin main`, sem `--force`.
- Última suíte completa: 1025 testes, 0 falhas, BUILD SUCCESS (antes, 995; os 30 novos são da fatia 1 do item 4) (duas execuções seguidas; uma execução anterior, logo depois do módulo de clientes, deu 9 erros que não se repetiram, causa não identificada: vale reexecutar e, se voltar, olhar `target/surefire-reports`).
- Clientes concluído (V12, RN-91 a RN-98, AD-16): cadastro completo, busca, filtros, resumo e inativação. Falta ver as telas no navegador. Não existe vínculo venda-cliente (sem tabela de venda), então não há "total gasto em compras".
- Protótipos das 3 direções visuais prontos em `C:\Users\andre\Documents\StockHub-prototipos\` (A Edição, B Instrumento, C Grafite). O agente recomenda a A (serifa só em títulos). **Aguardando a escolha do dono** para aplicar em tokens, shell, dashboard e listas.
- Banco local `stockhub` zerado e semeado com 21 lojas de teste (uma por tipo e subtipo). Logins em `C:\Users\andre\Documents\StockHub-logins-teste.txt` (fora do repositório; senha `Teste@1234`). Os testes de concorrência apagam só lojas `@teste.invalid`.
- O dono roda o app pelo IntelliJ na porta 8080. Agentes devem usar outra porta (8083 a 8090) e nunca encerrar a 8080.
- Para ver várias lojas ao mesmo tempo sem uma derrubar a sessão da outra: um endereço diferente por loja, como `http://comercio-mercado.localhost:8080` (o cookie de sessão é separado por endereço). Janelas anônimas compartilham cookie entre si.
- Feito nesta rodada: formulários próprios (`css/formularios.css`), página inicial (hash limpo, logo clicável, animação por clique indo até o fim da cena), tela Estoque/Movimentações (painel de lançamento com rolagem própria e botão para esconder; histórico com rolagem interna), dashboard "capa do dia".

## Pedidos do dono ainda abertos (em ordem sugerida)
1. **Identidade visual do sistema logado.** O dono acha que falta algo, que destoa da página inicial/login/cadastro (estes ele aprova), que o dashboard novo não agrada e que o **tema escuro está feio**. Um agente produz 3 direções em protótipos HTML em `C:\Users\andre\Documents\StockHub-prototipos\`. O dono escolhe (ou mistura); só então aplicar em tokens, shell (menu/topo) e todas as telas. Não redesenhar sem ele ver antes.
2. **Mais informação e filtros fáceis** em Clientes, NF-e e OS (e revisar produtos): campos faltando, filtros simples e visíveis.
3. **Gráficos da tela Análises:** o dono acha os tipos de gráfico ruins; escolher formas melhores para cada dado (skill `dataviz`; SVG próprio, tabela alternativa acessível, claro e escuro).
4. **Loja por ramo (subtipo de verdade).** Cada ramo precisa de campos e blocos próprios, opcionais, sem quebrar lojas existentes.
   - **Fatia 1 concluída** (V13, AD-17, RN-99 a RN-103, 9+ commits locais): produto com tamanho e cor (vestuário), lote e validade (farmácia), validade (mercado, que é o ramo de "alimentos"; não existe subtipo ALIMENTOS) e número de série (informática), pelo mapa `CamposRamo`.
   - **Fatia 2 em andamento** (V14, AD-18, RN-104 a RN-108): oficina com placa e modelo no `equipamento_cliente`, só em MECANICA e MOTO. Plano de execução: banco-migrations (V14), dev-backend, designer-ui, testador-qa.
   - **Pendências da fatia 1:** (a) as JSPs de produto ainda não têm teste de renderização real (só testes de controller/modelo); (b) a lista de produtos não recebe `camposRamo` em todos os caminhos de renderização; (c) o fluxo de funcionário (membro da loja) segue sem teste dos campos por ramo.
5. **Personalização pelo dono ("modular").** Modelo decidido: AD-19 e RN-109 a RN-114 (tabela `preferencia_bloco` por loja e tela, ausência de linha = comportamento atual, lista fechada de blocos no enum `BlocoTela`, só DONO/GERENTE gravam, upsert sem DELETE). Execução fatiada:
   - **Fatia 1 (próxima):** ocultar/mostrar blocos do Início (V15, `PersonalizacaoService`, tela em `/conta/personalizacao`, `c:if` no `dashboard.jsp`). Usa só classes CSS existentes; a identidade visual segue em aberto.
   - **Fatia 2:** colunas visíveis na lista de produtos (mesma tabela, `tela = PRODUTOS`).
   - **Fatia 3:** ordem dos blocos (coluna `ordem`) e atalhos por lista fechada de destinos.
   - Só reaplicar o visual da personalização depois que o dono escolher a direção visual (item 1).
   - Decisões já tomadas na revisão do item 4: a OS antiga continua mostrando placa e modelo após trocar o subtipo (RN-108); placa e modelo ficam só em MECANICA e MOTO.

## Decisões que dependem do dono
- Enviar os commits ao GitHub.
- Apagar o banco `stockhub_analises_tmp` (antigo, não foi tocado).
- Teto de 5 a 10 s nos `@Lock` pessimistas.
- Pendências antigas: senha `1234` do postgres, descrição e tópicos do repositório, e-mail hotmail nos commits, pasta `C:\Users\andre\Documents\StockHub-backup-contexto`, git stash antigo, testes em Firefox, Safari e leitor de tela.

## Regras de trabalho (do `CLAUDE.md` e do dono)
- Responder e comentar em português; commits pequenos, mensagem em português no imperativo, **somente no nome do dono (sem `Co-Authored-By` nem menção a Claude)**.
- Nunca commitar segredos. Antes de dizer que está pronto: `./mvnw test` com resultado real (PowerShell: `$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"; .\mvnw.cmd test`; a saída é enorme, ler só os totais).
- Pedir aprovação antes de `git push`, DROP/TRUNCATE/DELETE em massa, mexer em senhas ou no `application-local.properties`, apagar branches ou reescrever histórico.
- O dono quer economia de tokens: agentes com tarefa fechada e relatório curto; suíte completa só no fim de cada etapa.
