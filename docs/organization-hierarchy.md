# Hierarquia de Organizacoes

Ultima atualizacao: `2026-03-24`

## Objetivo

Este documento registra a evolucao da hierarquia de `Organization` para um modelo SaaS em que:
- `tenant` e a fronteira de isolamento
- `organization` e a estrutura de negocio
- `market` e a dimensao regional/comercial
- `membership` e o contexto de acesso do usuario

## Modelo atual

### Tenant
- `tenant` representa o cliente SaaS.
- Toda `organization` pertence a um `tenant`.
- Tenant raiz e provisionado explicitamente para organizacoes raiz.

### Organization
- `organization` continua representando a arvore operacional interna.
- Campos persistidos relevantes em `organization`:
- `tenant_id`
- `market_id`
- `tenant_type`
- `parent_organization_id`
- `customer_organization_id`
- `hierarchy_level`

### Market
- `tenant_market` foi introduzido para representar a dimensao regional/comercial.
- Nesta primeira fase, `market_id` em `organization` e `user_membership` e opcional.

### Membership
- O usuario nao e mais a fonte principal de verdade para tenant e papel.
- O contexto principal de acesso agora mora em `user_membership`.
- Um membership pode apontar para:
- `tenant_id`
- `organization_id`
- `market_id`
- status e papel(is) via `membership_role`
- o membership default pode ser trocado explicitamente pela API de contexto ativo

## Regras de modelagem

- `tenant != organization`
- organizacao raiz nao deve ser tratada como sinonimo tecnico de tenant, embora exista relacao 1:1 para roots nesta fase
- `organization` preserva visibilidade por subarvore
- `market` e opcional e nao quebra o modelo atual quando ausente
- `tenant_type` segue existindo para compatibilidade e governanca, mas a fronteira real passa a ser `tenant`

## Regras de cadastro

- so `ADMIN + INTERNAL` pode criar uma organizacao raiz
- criar raiz externa agora provisiona tambem um `tenant` explicito
- `ADMIN` externo pode criar organizacoes filhas apenas dentro da propria subarvore
- toda organizacao filha herda `tenant_id` do pai
- `market_id` da organizacao filha herda do pai quando existir

## Regras de visibilidade

A visibilidade continua descendente por `organization`.

- o ator ve a propria `organization` ativa e as descendentes
- nao existe visibilidade lateral entre irmas
- nao existe visibilidade entre tenants diferentes sem override operacional
- `SUPPORT INTERNAL` continua podendo atravessar tenants sob regras explicitas

## Autorizacao contextual

- `AuthenticatedUser` agora carrega `membershipId`, `activeTenantId`, `activeOrganizationId` e `activeMarketId`
- a aplicacao tenta resolver esse contexto via membership real
- claims legadas de tenant no JWT seguem sendo apenas fallback/hint
- servicos de users, tenant e portfolio passaram a separar melhor:
- tenant boundary
- organization scope
- papel/permissao do contexto ativo

## Compatibilidade temporaria

- `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` seguem presentes
- salvar um `ManagedUser` sincroniza um membership default automaticamente
- APIs de users continuam aceitando `organizationId` e `role` planos enquanto a administracao de memberships ainda nao foi exposta
- a administracao de memberships e markets agora existe em `/api/access/*`, mas o contrato de users ainda nao foi migrado para retornar memberships como recurso principal

## Migrations relevantes

- `src/main/resources/db/migration/V6__add_organization_hierarchy.sql`
- `src/main/resources/db/migration/V7__introduce_tenant_membership_and_market.sql`

## Debitos ainda abertos

- expor APIs de administracao de multiplos memberships por usuario
- popular `tenant_market` com mercados reais de negocio
- remover dependencia funcional de `app_user.role` e `app_user.tenant_id`
- revisar claims emitidas pelo Cognito para refletir melhor contexto ativo selecionavel
