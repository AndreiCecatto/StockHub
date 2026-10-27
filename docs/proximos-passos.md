# Próximos passos do StockHub (documento de retomada)

Escrito em 2026-10-27 para retomar o trabalho em outra sessão, sem depender da conversa anterior.

## Estado atual
- Branch `main`, cerca de 40 commits locais **não enviados** ao GitHub (repositório público). Enviar só com aprovação do dono: `git push origin main`, sem `--force`.
- Última suíte completa: 955 testes, 0 falhas, BUILD SUCCESS.
- Banco local `stockhub` zerado e semeado com 21 lojas de teste (uma por tipo e subtipo). Logins em `C:\Users\andre\Documents\StockHub-logins-teste.txt` (fora do repositório; senha `Teste@1234`). Os testes de concorrência apagam só lojas `@teste.invalid`.
- O dono roda o app pelo IntelliJ na porta 8080. Agentes devem usar outra porta (8083 a 8090) e nunca encerrar a 8080.
- Para ver várias lojas ao mesmo tempo sem uma derrubar a sessão da outra: um endereço diferente por loja, como `http://comercio-mercado.localhost:8080` (o cookie de sessão é separado por endereço). Janelas anônimas compartilham cookie entre si.
- Feito nesta rodada: formulários próprios (`css/formularios.css`), página inicial (hash limpo, logo clicável, animação por clique indo até o fim da cena), tela Estoque/Movimentações (painel de lançamento com rolagem própria e botão para esconder; histórico com rolagem interna), dashboard "capa do dia".

## Pedidos do dono ainda abertos (em ordem sugerida)
1. **Identidade visual do sistema logado.** O dono acha que falta algo, que destoa da página inicial/login/cadastro (estes ele aprova), que o dashboard novo não agrada e que o **tema escuro está feio**. Um agente produz 3 direções em protótipos HTML em `C:\Users\andre\Documents\StockHub-prototipos\`. O dono escolhe (ou mistura); só então aplicar em tokens, shell (menu/topo) e todas as telas. Não redesenhar sem ele ver antes.
2. **Mais informação e filtros fáceis** em Clientes, NF-e e OS (e revisar produtos): campos faltando, filtros simples e visíveis.
3. **Gráficos da tela Análises:** o dono acha os tipos de gráfico ruins; escolher formas melhores para cada dado (skill `dataviz`; SVG próprio, tabela alternativa acessível, claro e escuro).
4. **Loja por ramo (subtipo de verdade).** Hoje o subtipo muda quase nada. Cada ramo precisa de campos e blocos próprios (ex.: roupas com tamanho e cor, farmácia com validade e lote, alimentos com validade, oficina com placa e modelo, informática com número de série). Modelar com migration e DTOs, campos opcionais por ramo, sem quebrar lojas existentes.
5. **Personalização pelo dono ("modular").** O dono da loja escolhe quais blocos, atalhos e informações aparecem no início e nas listas. Plano antes de codar: modelo de configuração por loja (tabela de preferências ou JSON validado), lista fechada de blocos disponíveis por tipo/subtipo, só DONO/GERENTE alteram.

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
