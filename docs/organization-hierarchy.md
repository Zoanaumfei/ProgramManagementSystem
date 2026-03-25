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
- Organizacoes raiz externas passam a provisionar `tenant` explicito.
- A relacao raiz `organization <-> tenant` ainda e proxima nesta fase, mas os conceitos continuam separados.

### Organization
- `organization` continua representando a arvore operacional interna.
- Campos persistidos relevantes em `organization`:
- `tenant_id`
- `market_id`
- `tenant_type`
- `parent_organization_id`
- `customer_organization_id`
- `hierarchy_level`
- A visibilidade continua descendente por subarvore.

### Market
- `tenant_market` representa a dimensao regional/comercial do tenant.
- `market_id` em `organization` e `user_membership` continua opcional.
- Market nao substitui tenant nem organization; ele complementa o contexto.

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
- organizacao raiz nao deve ser tratada como sinonimo tecnico de tenant
- `organization` preserva visibilidade por subarvore
- `market` e opcional e nao quebra o modelo atual quando ausente
- `tenant_type` segue existindo para compatibilidade e governanca
- `role` e `permission` sao resolvidos no contexto do membership

## Regras de cadastro

- so `ADMIN INTERNAL` pode criar uma organizacao raiz
- criar raiz externa provisiona tambem um `tenant` explicito
- `ADMIN` externo pode criar organizacoes filhas apenas dentro da propria subarvore
- toda organizacao filha herda `tenant_id` do pai
- `market_id` da organizacao filha pode ser herdado do pai quando aplicavel

## Regras de visibilidade

A visibilidade continua descendente por `organization`.

- o ator ve a propria `organization` ativa e as descendentes
- nao existe visibilidade lateral entre irmas
- nao existe visibilidade entre tenants diferentes sem override operacional
- `SUPPORT INTERNAL` continua podendo atravessar tenants sob regras explicitas

## Autorizacao contextual

- `AuthenticatedUser` agora carrega `membershipId`, `activeTenantId`, `activeOrganizationId` e `activeMarketId`
- a aplicacao tenta resolver esse contexto via membership real
- `POST /api/access/context/activate` permite trocar o membership default
- `X-Access-Context` permite pedir um contexto especifico por request
- claims legadas de tenant no JWT seguem sendo apenas fallback quando nao ha contexto resolvido localmente
- servicos de users, tenant e portfolio passaram a separar melhor tenant boundary de organization scope

## Compatibilidade temporaria

- `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` seguem presentes
- salvar um `ManagedUser` sincroniza um membership default automaticamente
- APIs de users continuam aceitando `organizationId` e `role` planos
- a administracao de memberships e markets agora existe em `/api/access/*`
- o contrato principal de `users` ainda nao foi migrado para usar memberships como recurso principal

## Migrations relevantes

- `src/main/resources/db/migration/V6__add_organization_hierarchy.sql`
- `src/main/resources/db/migration/V7__introduce_tenant_membership_and_market.sql`
- `src/main/resources/db/migration/V8__allow_multiple_memberships_per_user.sql`

## Debitos ainda abertos

- migrar a UX administrativa de users para memberships explicitos
- popular `tenant_market` com mercados reais de negocio
- remover dependencia funcional de `app_user.role` e `app_user.tenant_id`
- revisar claims emitidas pelo Cognito para refletir melhor o contexto ativo selecionavel
