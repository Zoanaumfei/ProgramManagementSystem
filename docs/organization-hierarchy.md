# Hierarquia de Organizacoes

Ultima atualizacao: `2026-03-20`

## Objetivo

Este documento registra o modelo hierarquico de `Organizacao` implementado no backend para isolar customers externos e controlar a visibilidade do portfolio por subarvore.

## Modelo

Cada `Organizacao` externa pertence a uma arvore com um `Customer` na raiz.

Campos persistidos em `organization`:
- `tenant_type`
- `parent_organization_id`
- `customer_organization_id`
- `hierarchy_level`

Interpretacao:
- `tenant_type=INTERNAL` fica reservado para a estrutura da plataforma/Oryzem
- `tenant_type=EXTERNAL` representa organizacoes de negocio
- `parent_organization_id=null` identifica a raiz externa (`Customer`)
- `customer_organization_id` aponta para o `Customer` raiz da arvore
- `hierarchy_level=0` para o `Customer`, `1` para filhos diretos, `2` para netos e assim por diante

## Regras de cadastro

- so `ADMIN + INTERNAL` pode criar um `Customer` raiz
- `ADMIN` externo pode criar organizacoes filhas apenas dentro da propria subarvore
- toda organizacao externa tem no maximo um pai direto
- uma organizacao nao pode mudar de pai depois de criada
- toda organizacao externa pertence a exatamente um `Customer`

## Regras de visibilidade

A visibilidade e sempre descendente.

- `Customer` ve o proprio portfolio e o das descendentes
- um tier ve o proprio portfolio e o das descendentes
- nao existe visibilidade lateral entre irmaos
- nao existe visibilidade entre arvores de customers diferentes
- `SUPPORT` interno pode atravessar arvores no portfolio
- `SUPPORT` externo nao atravessa arvores; fica restrito a propria organizacao

## Regras de portfolio

- os endpoints administrativos de `Organizacao` em `/api/portfolio/organizations` operam apenas sobre organizacoes `EXTERNAL`
- organizacoes `INTERNAL`, como `internal-core`, ficam fora do diretório funcional do portfolio
- `GET /api/portfolio/organizations` e `GET /api/portfolio/organizations/{organizationId}` agora exigem papel administrativo de `ADMIN` ou `SUPPORT`
- `POST`, `PUT` e `DELETE` em `/api/portfolio/organizations` agora exigem `ADMIN`
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree` agora existe como saneamento operacional explicito para `SUPPORT INTERNAL`, exigindo `supportOverride=true` e `justification`
- `MANAGER` e `MEMBER` nao acessam mais o diretorio administrativo de organizacoes
- `GET /api/portfolio/organizations` respeita a subarvore visivel do ator autenticado
- `GET /api/portfolio/organizations` tambem aceita filtros por `customerOrganizationId`, `parentOrganizationId` e `hierarchyLevel`
- `GET /api/portfolio/programs` e `GET /api/portfolio/programs/{programId}` tambem respeitam a visibilidade por subarvore
- `GET /api/portfolio/programs` tambem aceita filtro por `ownerOrganizationId`
- `Program.ownerOrganizationId` permanece como ancora principal de visibilidade do portfolio
- organizacoes participantes externas de um programa precisam pertencer ao mesmo `customerOrganizationId` do owner

### Permissoes por papel no portfolio

Camadas funcionais:
- governanca: `Organizacao`, `Programa`, participantes e ownership
- gestao: `Projeto`, `Produto` e `OpenIssue`
- execucao: `Item`, `Deliverable` e `Document`

Permissoes fechadas:
- `ADMIN` pode visualizar e operar todas as camadas do portfolio dentro do escopo visivel
- `MANAGER` pode visualizar o portfolio e operar as camadas de gestao e execucao
- `MEMBER` pode visualizar o portfolio e operar apenas a camada de execucao
- `SUPPORT` interno e externo ficam somente leitura no portfolio; o interno pode atravessar arvores e o externo fica restrito ao proprio escopo visivel
- `AUDITOR` fica somente leitura
- excecao operacional: `SUPPORT INTERNAL` pode executar `purge-subtree` de organizacoes externas para limpeza de ambiente, sempre com `supportOverride=true`, `justification` e auditoria

Mapeamento atual dos endpoints:
- `GET /api/portfolio/milestone-templates`, `GET /api/portfolio/programs`, `GET /api/portfolio/programs/{programId}`, `GET /api/portfolio/deliverables/{deliverableId}/documents` e `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/download-url` usam permissao de `VIEW`
- `POST /api/portfolio/milestone-templates` usa permissao de configuracao e fica restrito a `ADMIN`
- `POST /api/portfolio/programs` usa governanca de portfolio e fica restrito a `ADMIN`
- `POST /api/portfolio/programs/{programId}/projects`, `POST /api/portfolio/projects/{projectId}/products` e `POST /api/portfolio/programs/{programId}/open-issues` ficam com `ADMIN` e `MANAGER`
- `POST /api/portfolio/products/{productId}/items`, `POST /api/portfolio/items/{itemId}/deliverables`, `POST /api/portfolio/deliverables/{deliverableId}/documents/upload-url`, `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/complete` e `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}` ficam com `ADMIN`, `MANAGER` e `MEMBER`
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree` fica com `SUPPORT INTERNAL` e nao substitui o `DELETE` normal, que continua sendo apenas inativacao

Observacao:
- na API atual do backend ainda nao existem rotas de update/delete para `Program`, `Project`, `Product`, `Item`, `Deliverable` e `OpenIssue`; as permissoes acima cobrem a superficie hoje exposta
- o `purge-subtree` apaga usuarios, programas e a subarvore de organizacoes; por seguranca, ele falha se a subarvore ainda participar de programas cujo owner esteja fora dela

Impacto no frontend:
- o workspace de organizacoes deve expor `Purge Subtree` apenas para `SUPPORT INTERNAL`
- a chamada deve exigir `supportOverride=true` e `justification`
- `DELETE /api/portfolio/organizations/{organizationId}` continua sendo apenas inativacao; `purge-subtree` deve aparecer como acao separada e claramente destrutiva

Validacao:
- suite completa `./mvnw.cmd test` executada com sucesso em `2026-03-20`, com `94` testes passando

## Regras de inativacao

Uma `Organizacao` nao pode ser inativada quando existir:
- usuario `INVITED`
- usuario `ACTIVE`
- organizacao filha `ACTIVE`
- projeto `ACTIVE`

## Contrato HTTP relevante

`OrganizationResponse` agora inclui:
- `tenantType`
- `parentOrganizationId`
- `customerOrganizationId`
- `hierarchyLevel`
- `childrenCount`
- `hasChildren`
- `setupStatus`
- `userSummary`
- `programSummary`
- `canInactivate`
- `inactivationBlockedReason`

`CreateOrganizationRequest` agora aceita:
- `name`
- `code`
- `parentOrganizationId`

Sem `parentOrganizationId`:
- cria raiz externa e exige `ADMIN + INTERNAL`

Com `parentOrganizationId`:
- cria filha herdando `customerOrganizationId` e `hierarchyLevel` do pai

## Users

No modulo de `users`:
- `ADMIN` externo pode listar e gerenciar usuarios da propria organizacao e das descendentes
- `ADMIN` interno continua com visao e gestao globais
- `MANAGER` e `MEMBER` nao acessam mais o modulo administrativo de usuarios
- `SUPPORT` interno pode consultar uma organizacao externa especifica sem `supportOverride`
- `SUPPORT` externo permanece restrito a propria organizacao
- operacoes cross-tenant sensiveis de `SUPPORT` (`reset-access`, `resend-invite` e `purge`) continuam exigindo `supportOverride` e `justification`

## Migration

A base desta entrega e a migration:
- `src/main/resources/db/migration/V6__add_organization_hierarchy.sql`

## Observacao atual

A UI administrativa de organizacoes ainda precisa ser adaptada para explorar plenamente a hierarquia. O backend ja entrega os metadados necessarios para arvore, filtros e visibilidade por subarvore.
