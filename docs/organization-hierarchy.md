# Organizacoes e Relacionamentos

Ultima atualizacao: `2026-04-01`

## Objetivo

Documentar o modelo atual de organizacoes dentro do core SaaS membership-first:
- `tenant` = fronteira SaaS
- `organization` = entidade de negocio canonica dentro do tenant
- `market` = dimensao regional/comercial opcional
- `membership` = contexto de acesso do usuario
- `organization_relationship` = ligacao explicita entre organizacoes

## Backend

### Estado atual
- toda `organization` pertence a um `tenant`
- `organization` pode apontar para `market`
- a identidade canonica da organizacao dentro do tenant e `cnpj`
- a visibilidade nao depende mais de uma arvore persistida de pai/filho
- a navegacao organizacional vem de relacionamentos dirigidos

### Tipos de relacionamento
- `CUSTOMER_SUPPLIER` representa a cadeia operacional de cliente/fornecedor
- `PARTNER` representa relacionamento lateral, fora da cadeia cliente/fornecedor
- `localOrganizationCode` pertence ao relacionamento `source -> target`, nao a organizacao

### Regras principais
- apenas organizacoes do mesmo tenant podem participar do mesmo grafo
- `CUSTOMER_SUPPLIER` nao pode apontar para a propria organizacao
- `CUSTOMER_SUPPLIER` nao pode introduzir ciclos
- uma mesma organizacao pode ser cliente de varios fornecedores e fornecedora de varios clientes
- relacionamentos podem permanecer `INACTIVE` por historico
- o purge explicito da subarvore remove tambem relacionamentos persistidos que referenciam a subarvore, inclusive os inativos

### Criacao e reutilizacao
- organizacao externa segue fluxo de cadastrar-ou-vincular por `tenant + cnpj`
- se ja existir uma organizacao com o mesmo `cnpj` no tenant, o backend reutiliza o registro em vez de duplicar
- quando um `EXTERNAL ADMIN` cria ou vincula uma organizacao, o backend cria ou reativa automaticamente o relacionamento `CUSTOMER_SUPPLIER` a partir da organizacao do ator
- se `localOrganizationCode` for enviado nesse fluxo, ele e salvo no relacionamento criado ou reativado

### Autorizacao contextual
- o backend resolve `membershipId`, `activeTenantId`, `activeOrganizationId` e `activeMarketId` a partir de `user_membership`
- `POST /api/access/context/activate` troca o contexto ativo
- `X-Access-Context` permite selecao request-scoped
- nao ha mais fallback de autorizacao para claims legadas de tenant no JWT

## Frontend

### Implicacoes de integracao
- formularios nao devem mais usar `parentOrganizationId`, `customerOrganizationId` ou `hierarchyLevel`
- a UI deve tratar `localOrganizationCode` como metadado do relacionamento
- a UX de onboarding deve comunicar `Cadastrar ou vincular organizacao`
- breadcrumbs ou arvores nao devem sugerir um caminho unico canonico quando o grafo tiver multiplos caminhos validos
- relacionamentos `PARTNER` devem ser exibidos como arestas explicitas, separados da cadeia principal cliente/fornecedor

### Pontos de atencao
- `childrenCount` e `hasChildren` podem ser usados como resumo derivado, nao como prova de arvore rigida
- a definicao visual e de permissao para relacionamentos `PARTNER` ainda precisa de confirmacao de produto

## Migrations relevantes
- `src/main/resources/db/migration/V13__introduce_organization_relationships_and_reset_access_core.sql`
- `src/main/resources/db/migration/V14__replace_organization_hierarchy_with_cnpj_identity.sql`
- `src/main/resources/db/migration/V15__move_organization_code_to_relationship_local_metadata.sql`
