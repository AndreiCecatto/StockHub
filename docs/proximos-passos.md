# Próximos passos do StockHub (documento de retomada)

Atualizado em 2026-10-27 para retomar o trabalho em outra sessão, sem depender da conversa anterior.
Detalhes visuais ficam em `docs/design-system.md`; decisões técnicas em `docs/arquitetura.md`; regras em `docs/regras-negocio.md`. Este arquivo não os repete.

## 1. Estado atual
- Branch `main` sincronizada com `origin/main`. Push autorizado pelo dono "aos poucos": nunca `--force` e sempre varrer segredos no diff antes de enviar.
- Suíte: 1206 testes, 0 falhas.
- Banco local `stockhub` semeado com 21 lojas de teste. Logins em `C:\Users\andre\Documents\StockHub-logins-teste.txt` (fora do repositório).
- O dono roda o app pelo IntelliJ na porta 8080. Agentes usam outra porta (8083 a 8090) e nunca encerram a 8080.
- Para ver várias lojas ao mesmo tempo: um endereço por loja, como `http://comercio-mercado.localhost:8080` (cookie de sessão separado por endereço).

## 2. Concluído
- **Clientes** (V12, AD-16, RN-91 a RN-98).
- **Item 4, campos por ramo:** campos de produto por ramo (V13, AD-17, RN-99 a RN-103) e veículo da oficina (V14, AD-18, RN-104 a RN-108).
- **Item 5, personalização:** fatia 1, blocos do início (V15, AD-19, RN-109 a RN-114); fatia 2, colunas da lista de produtos (AD-20).
- **Item 1, identidade visual:** direção A "Edição" escolhida pelo agente, por delegação do dono, e aplicada nas fatias 1 a 6 (tokens, lateral, dashboard, listas, formulários, tema escuro). Capturas feitas com Edge headless + CDP, guardadas no scratchpad (fora do repositório).

## 3. Pendências técnicas
- Papel guardado no `UsuarioPrincipal`: gerente rebaixado mantém acesso até novo login (reler o papel a cada requisição ou invalidar a sessão).
- Aviso de veículo já cadastrado (RN-107) ainda sem verificação.
- Membro de loja (funcionário) sem teste sem mock.
- Contagem por situação nas abas de produtos e clientes: o backend deve enviar o mapa `contagemNiveis`.
- Nome do produto truncado na lista de produtos no desktop (colunas estreitas; ver capturas `prod-*-d.png`).
- Painel de lançamento de `estoque.jsp` ainda em caixa (fora da direção A).
- Telas ainda não capturadas: OS, NF-e, equipe, impressão, erros de formulário, modais, paleta de comandos, perfis GERENTE e OPERADOR.
- `componentes.css` mantém `.form-control` e `.form-label` antigos, sobrescritos por `formularios.css`: limpar.

## 4. Itens restantes do dono
- **Item 2:** mais campos e filtros fáceis em NF-e e OS; revisar produtos (Clientes já feito).
- **Item 3:** gráficos da tela Análises com formas melhores (skill `dataviz`; SVG próprio, tabela alternativa acessível, claro e escuro).
- **Item 5, fatia 3:** coluna `ordem` dos blocos e atalhos por lista fechada de destinos.

## 5. Dados e limpeza que dependem do dono
- Conta `teste.loja-vazia@stockhub.test`, criada por um agente no banco local (apagar só com aprovação).
- OS de teste 474 e 475 da loja Moto.
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
- O dono quer economia de tokens: agentes com tarefa fechada e relatório curto; suíte completa só no fim de cada etapa.
- Agentes nunca criam contas nem migrations por conta própria. A V16 foi criada sem pedido (aceita), mas toda migration deve passar pelo `banco-migrations`.
