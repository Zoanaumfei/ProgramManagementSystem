# Project Context

Ultima atualizacao: `2026-03-22`

## Uso
- Este arquivo e o ponto de entrada rapido compartilhado entre backend e frontend.
- O contexto detalhado foi reorganizado em `docs/context/` para reduzir duplicacao, conflito e historico acumulado no topo.
- Leia primeiro `docs/context/PROJECT_CONTEXT.md` e depois siga para o documento tematico necessario.

## Produto
- SaaS B2B de gerenciamento de programas e projetos para a cadeia automotiva.
- Foco em colaboracao entre empresas, rastreabilidade, entregas, documentos, issues e segregacao de acesso por tenant.

## Escopo atual
- Backend Spring Boot 4 em Java 21, protegido por Cognito, com RDS PostgreSQL e deploy unico em ECS/Fargate.
- Frontend React separado em outro workspace local, consumindo o backend real.
- Dominio principal atual: `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento`, com `OpenIssue` em paralelo no nivel de `Programa`.

## Estado resumido
- Monolito modular consolidado em `app`, `platform` e `modules`.
- Modulos ativos: `users`, `tenant`, `projectmanagement`, `documents`, `operations`, `reports`, `auth`, `authorization`, `audit`.
- `platform.users` ja opera com separacao interna mais explicita em `api`, `application`, `domain` e `infrastructure`.
- Hierarquia externa por customer/subarvore esta implementada no backend.
- Frontend ja consome organizacoes, portfolio, users e relatrios basicos.
- Documentos ainda operam em `stub` no runtime atual; S3 real segue como proximo passo.

## Proximos passos resumidos
- Validar o fluxo real de portfolio por subarvore no frontend.
- Fechar regras de negocio ainda abertas: transicoes de status, ownership e `FORM`.
- Operacionalizar documentos com S3 real.
- Revisar o fallback temporario de `id_token` e homologar logout completo.
- Continuar o endurecimento arquitetural do monolito modular.

## Documentacao detalhada
- [Resumo compartilhado](docs/context/PROJECT_CONTEXT.md)
- [Regras de negocio](docs/context/BUSINESS_RULES.md)
- [Contrato de API](docs/context/API_CONTRACT.md)
- [Alinhamento frontend/backend](docs/context/FRONTEND_BACKEND_ALIGNMENT.md)
- [Arquitetura](docs/context/ARCHITECTURE.md)
- [Decisoes](docs/context/DECISIONS.md)
- [Gaps abertos](docs/context/OPEN_GAPS.md)
- [Changelog resumido](docs/context/CHANGELOG.md)
- [Refactor do monolito modular](MODULAR_MONOLITH_REFACTOR.md)
