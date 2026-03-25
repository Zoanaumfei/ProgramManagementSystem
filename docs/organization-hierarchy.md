# Hierarquia de Organizacoes

Ultima atualizacao: `2026-03-25`

## Objetivo

A hierarquia de `organization` agora existe dentro de um modelo SaaS membership-first:
- `tenant` = fronteira SaaS
- `organization` = estrutura interna de negocio
- `market` = dimensao regional/comercial/operacional
- `membership` = contexto de acesso do usuario

## Estado atual
- toda `organization` pertence a um `tenant`
- `organization` pode apontar para `market`
- a visibilidade segue por subarvore
- `tenant` nao e tratado como sinonimo de `organization`

## Regras
- apenas `ADMIN INTERNAL` cria organizacoes raiz
- organizacoes filhas herdam o `tenant_id` do pai
- `ADMIN` externo administra apenas a propria subarvore
- `SUPPORT INTERNAL` pode operar entre tenants sob regras explicitas de autorizacao

## Autorizacao contextual
- o backend resolve `membershipId`, `activeTenantId`, `activeOrganizationId` e `activeMarketId` a partir de `user_membership`
- `POST /api/access/context/activate` troca o contexto ativo
- `X-Access-Context` permite selecao request-scoped
- nao ha mais fallback de autorizacao para claims legadas de tenant no JWT

## Migrations relevantes
- `src/main/resources/db/migration/V6__add_organization_hierarchy.sql`
- `src/main/resources/db/migration/V7__introduce_tenant_membership_and_market.sql`
- `src/main/resources/db/migration/V8__allow_multiple_memberships_per_user.sql`
