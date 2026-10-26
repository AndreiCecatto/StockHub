# Arquitetura do StockHub

Cada decisão tem o motivo e as alternativas descartadas. Quando uma decisão mudar, atualize este arquivo no mesmo commit.

## AD-01 — Spring Boot 4.1.2 e Java 17
- **Decisão:** parent `spring-boot-starter-parent` 4.1.2, Java 17 como mínimo.
- **Motivo:** 4.1.2 é o patch estável mais recente da linha 4.1 no Maven Central. A documentação oficial (requisitos de sistema) diz que exige Java 17 no mínimo e roda até o Java 27.
- **Descartadas:** 4.2.0-RC1 (candidata, não estável) e 3.5.x (a linha 3 sairá de suporte antes da 4).

## AD-02 — Monólito em camadas, pacote por camada
- **Decisão:** `controller` → `service` → `repository` → banco; `model` (entidades), `dto`, `exception`, `config`. O controller nunca acessa repository.
- **Motivo:** o domínio é pequeno (Produto, Categoria, Usuario, MovimentacaoEstoque). As camadas já existem, a equipe as conhece, e a regra "regra de negócio só no service" fica fácil de revisar.
- **Descartadas:** pacote por funcionalidade e arquitetura hexagonal (cerimônia demais para 4 entidades); API REST com SPA (o requisito do projeto é JSP).

## AD-03 — Onde cada responsabilidade fica
| Responsabilidade | Onde | Observação |
|---|---|---|
| Formato da entrada (obrigatório, tamanho, faixa) | Bean Validation nos DTOs (`@Valid` + `BindingResult` no controller) | o formulário volta com erros por campo |
| Regras de negócio e consistência | `service` (`@Transactional`) | ver `regras-negocio.md` |
| Erros de negócio | `exception` (`RegraNegocioException`, `RecursoNaoEncontradoException`) + `@ControllerAdvice` | nunca deixar virar 500 com stack trace |
| Segurança | `config/SecurityConfig` (Spring Security, BCrypt, CSRF, logout por POST) | escopo por usuário no service |
| Integridade final | restrições e índices do banco (Flyway) | última defesa, não a única |

## AD-04 — DTOs só na entrada; entidades na saída
- **Decisão:** formulários usam DTOs (`ProdutoForm`, `CategoriaForm`, `CadastroForm`, `MovimentacaoForm`). As JSPs de leitura continuam recebendo entidades.
- **Motivo:** o diagnóstico mostrou que usar a entidade como formulário permite mass assignment (o `id` e o dono vindos do POST). Na saída, a entidade é só leitura e criar DTO de resposta para cada tela só duplicaria código.
- **Descartadas:** DTO para tudo e MapStruct (peso desnecessário); entidade como formulário (inseguro).
- **Compatibilidade:** os DTOs mantêm os nomes de campo e o atributo de model usados hoje nas JSPs.

## AD-05 — Banco: Flyway manda, Hibernate só valida
- **Decisão:** `spring.jpa.hibernate.ddl-auto=validate`; todo ajuste de schema é uma migration `V<n>__descricao.sql` em `db/migration`. Migration aplicada nunca é editada, só se cria a próxima.
- **Motivo:** schema reproduzível e versionado, e o `validate` falha na subida se entidade e banco divergirem.
- **Descartadas:** `ddl-auto=update` (muda o schema sem rastro).

## AD-06 — Concorrência: bloqueio pessimista na movimentação
- **Decisão:** `EstoqueService.movimentar` carrega o produto com `PESSIMISTIC_WRITE` dentro da transação.
- **Motivo:** o diagnóstico apontou lost update. Num estoque a contagem tem de estar certa, e o bloqueio por linha é simples e correto para este volume.
- **Descartadas:** `@Version` com retry (mais código no controller para ganho de desempenho que o projeto não precisa).

## AD-07 — Exclusão lógica de produto
- **Decisão:** produto com histórico é inativado (`ativo=false`), nunca apagado. Detalhes em `regras-negocio.md` (RN-08).
- **Motivo:** o histórico de movimentações é a trilha de auditoria; apagar o produto destruiria ou bloquearia isso.

## AD-08 — Testes
- **Decisão:** testes unitários de service com Mockito, `@WebMvcTest` com `spring-security-test` nos controllers, e um teste de integração com o banco local para o que depende de SQL.
- **Motivo:** as regras de estoque estão no service, então é lá que a cobertura vale mais.
