# Project Context

Ultima atualizacao: `2026-03-22`

## Objetivo do produto
- Construir um SaaS B2B de gerenciamento de programas e projetos para a cadeia automotiva.
- Atender montadoras, Tier 1 e Tier 2 com colaboracao multiempresa, rastreabilidade e segregacao por tenant.

## Escopo atual
- Backend unico em Spring Boot 4 + Java 21 + Maven.
- Frontend separado em React + Vite, no workspace local `C:\Users\vande\Oryzem\PMS Frontend`.
- Autenticacao via Amazon Cognito Hosted UI e backend como OAuth2 Resource Server.
- Persistencia principal em PostgreSQL no RDS, com Flyway e JPA.
- Runtime de dev em AWS com ECS/Fargate, ALB, ECR e Secrets Manager.

## Modulos principais
- `platform.auth`: Cognito, JWT, filtros e `/api/auth/*`.
- `platform.authorization`: matriz de autorizacao e `/api/authz/check`.
- `platform.tenant`: organizacoes, hierarquia, subtree e purge operacional.
- `platform.users`: administracao de usuarios e reconciliacao com identidade.
- `platform.documents`: storage e presigned URLs.
- `modules.projectmanagement`: portfolio e dominio principal.
- `modules.operations`: operacoes administrativas legadas.
- `modules.reports`: relatrios e exportacao.

## Fluxos principais
- `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento`
- `Programa -> OpenIssue`
- `Organizacao -> primeiro ADMIN -> onboarding de usuarios`
- `SUPPORT INTERNAL -> purge-subtree` para saneamento operacional controlado

## Estado atual resumido
- O backend ja entrega contrato suficiente para organizacoes, portfolio, users, operations e reports.
- A hierarquia externa por customer/subarvore esta implementada e governa visibilidade do portfolio.
- O frontend ja consome organizacoes, milestone templates, programas, detalhe agregado, users e purge administrativo.
- O monolito modular ja passou por refactor estrutural e consolidacao de fronteiras entre modulos.
- `platform.users` virou o primeiro modulo com separacao interna mais explicita em `api`, `application`, `domain` e `infrastructure`.
- O provider de documentos em dev continua em `stub`; bucket S3 real ainda nao foi homologado.
- O `access_token` e a trilha principal; o fallback para `id_token` ainda existe de forma temporaria no frontend.

## Proximos passos resumidos
- Validar o fluxo ponta a ponta do portfolio com organizacoes visiveis por subarvore.
- Fechar ownership, transicoes de status e modelagem de `FORM`.
- Operacionalizar documentos em S3 real.
- Homologar logout completo e revisar se o fallback de `id_token` pode ser removido.
- Continuar o endurecimento do monolito modular e adicionar protecoes arquiteturais depois da estabilizacao interna.

## Navegacao
- [Regras de negocio vigentes](./BUSINESS_RULES.md)
- [Contrato de API vigente](./API_CONTRACT.md)
- [Alinhamento frontend/backend](./FRONTEND_BACKEND_ALIGNMENT.md)
- [Arquitetura atual](./ARCHITECTURE.md)
- [Decisoes ativas e temporarias](./DECISIONS.md)
- [Gaps reais ainda abertos](./OPEN_GAPS.md)
- [Historico resumido](./CHANGELOG.md)
