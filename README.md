# StockHub

Sistema web de gestão de estoque feito com Java e Spring Boot. Cada conta representa uma loja, com seus próprios produtos, categorias e movimentações. O saldo de um produto só muda por uma movimentação registrada (entrada, saída ou ajuste), e essas movimentações formam um histórico que não pode ser editado.

Projeto acadêmico do curso de Sistemas para Internet da Universidade Federal de Santa Maria (UFSM).

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)

## O que o sistema faz

- **Contas e acesso:** cadastro e login de lojas, senha com BCrypt, proteção CSRF, e cada loja só enxerga os próprios dados.
- **Produtos:** cadastro, edição, busca, filtros por categoria e nível de estoque, ordenação, ajuste rápido de saldo com desfazer, painel de detalhe e exportação em CSV (do filtro inteiro ou só dos marcados).
- **Categorias:** cadastro e edição; uma categoria em uso não pode ser excluída.
- **Movimentações:** entrada, saída e ajuste com observação, livro-razão filtrável por produto, tipo e período, e exportação em CSV. O saldo nunca fica negativo, e produtos com histórico são inativados em vez de apagados.
- **Visão geral:** indicadores, produtos que precisam de reposição, saúde do estoque e gráfico das movimentações dos últimos 14 dias.
- **Relatórios:** estoque baixo, unidades e valor do estoque.
- **Usabilidade:** paleta de comandos (`Ctrl+K`), atalhos de teclado (`?` mostra a lista), tema claro, escuro ou do sistema, animação ao trocar de página e uma apresentação pública na página inicial. Respeita a preferência de menos movimento.

As regras completas estão em [docs/regras-negocio.md](docs/regras-negocio.md).

## Tecnologias

| Camada | Tecnologias |
| :--- | :--- |
| Backend | Java 17, Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA (Hibernate), Bean Validation |
| Frontend | JSP, JSTL, Bootstrap 5, JavaScript e CSS próprios (sem build e sem CDN) |
| Banco | PostgreSQL 14+ com migrations Flyway |
| Build e testes | Maven (wrapper incluso), JUnit 5, Mockito, Spring Security Test |

As bibliotecas do front ficam em `src/main/webapp/vendor/`, com versão e licença em [VERSOES.md](src/main/webapp/vendor/VERSOES.md).

## Arquitetura

`Controller` → `Service` → `Repository` → PostgreSQL. A regra de negócio fica no `service`, formulários entram como DTOs e o schema é versionado pelo Flyway (o Hibernate só valida). As decisões e os motivos estão em [docs/arquitetura.md](docs/arquitetura.md).

```text
src/main/java/org/example/stockhubspring/
  config/ controller/ dto/ exception/ model/ repository/ service/ util/
src/main/resources/
  application.properties            configuração (sem credenciais)
  application-example.properties    modelo para o seu application-local.properties
  db/migration/                     migrations Flyway
src/main/webapp/
  WEB-INF/pages/ tags/ includes/    telas JSP
  css/ js/ img/ vendor/             estáticos
docs/                               arquitetura, regras de negócio, design e mercado
```

## Como rodar

**Requisitos:** JDK 17 ou superior, PostgreSQL 14+ e Git. O Maven vem no projeto (`mvnw`).

1. **Clone o repositório**

   ```bash
   git clone <url-do-repositorio>
   cd StockHub
   ```

2. **Crie o banco**

   ```sql
   CREATE DATABASE stockhub;
   ```

   As tabelas são criadas sozinhas pelas migrations na primeira execução.

3. **Informe as credenciais do banco.** Elas nunca ficam no código. Use uma das opções:

   - variáveis de ambiente `DB_USERNAME` e `DB_PASSWORD`;
   - ou copie `src/main/resources/application-example.properties` para `src/main/resources/application-local.properties` e preencha usuário e senha. Esse arquivo é ignorado pelo Git.

4. **Execute**

   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```

   Abra http://localhost:8080 e crie uma conta em "Cadastro". A senha precisa de pelo menos 8 caracteres.

Se o `JAVA_HOME` apontar para um Java anterior ao 17, aponte-o para um JDK 17+ antes de rodar o Maven.

### Variáveis opcionais

| Variável | Para que serve |
| :--- | :--- |
| `DB_USERNAME`, `DB_PASSWORD` | usuário e senha do PostgreSQL |
| `SPRING_DATASOURCE_URL` | outro banco ou outro host (padrão `jdbc:postgresql://localhost:5432/stockhub`) |
| `SERVER_PORT` | outra porta (padrão 8080) |
| `COOKIE_SECURE=true` | obrigatório em produção com HTTPS |

## Testes

```bash
./mvnw test
```

Os testes de integração usam o banco configurado acima, e cada teste desfaz o que gravou (rollback), então não deixam dados. Os demais testes usam objetos simulados e não precisam de banco. Para empacotar: `./mvnw clean package`.

## Documentação

- [Arquitetura e decisões](docs/arquitetura.md)
- [Regras de negócio](docs/regras-negocio.md)
- [Design e front-end](docs/design-system.md)
- [Mercado e requisitos](docs/mercado-requisitos.md)

## Autor

**Andrei Cecatto**, estudante de Sistemas para Internet na UFSM.
