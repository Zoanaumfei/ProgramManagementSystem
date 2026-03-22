# Business Rules

Ultima atualizacao: `2026-03-22`

## 1. Organizacao e tenant
- `tenant` representa uma empresa e e modelado por `Organizacao`.
- `TenantType.INTERNAL` e reservado para a estrutura da plataforma/Oryzem.
- O contrato funcional de `/api/portfolio` opera apenas sobre organizacoes `EXTERNAL`.
- `Customer` e uma `Organizacao` externa raiz.
- Toda organizacao externa pertence a exatamente um customer raiz.
- Toda organizacao externa pode ter no maximo um pai direto.
- `parentOrganizationId`, `customerOrganizationId` e `hierarchyLevel` modelam a arvore externa.
- Uma organizacao nao pode mudar de pai depois de criada.
- `ADMIN INTERNAL` cria customer raiz.
- `ADMIN` externo cria e gerencia apenas filhas dentro da propria subarvore.

## 2. Visibilidade por subarvore
- Cada organizacao externa enxerga o proprio portfolio e o das descendentes dentro da mesma arvore externa.
- Nao existe visibilidade lateral entre irmas nem entre customers diferentes.
- `SUPPORT INTERNAL` pode atravessar arvores externas para leitura no portfolio.
- `SUPPORT` externo fica restrito ao proprio escopo visivel.
- `GET /api/portfolio/organizations` e o diretorio administrativo de organizacoes; ele nao e um endpoint de overview para `MANAGER` e `MEMBER`.

## 3. Onboarding de organizacao e usuarios
- Todo `Usuario` pertence obrigatoriamente a uma `Organizacao`.
- O vinculo canonico do contrato de usuarios e `organizationId`.
- `tenant_id` e `tenant_type` permanecem apenas como compatibilidade interna de autorizacao.
- O primeiro usuario de uma organizacao deve ser `ADMIN`.
- Uma organizacao sem `ADMIN` em status `INVITED` ou `ACTIVE` fica com `setupStatus=INCOMPLETED`.
- Uma organizacao `INCOMPLETED` nao pode receber usuarios nao-`ADMIN`.
- Uma organizacao `INCOMPLETED` nao pode ser usada como `ownerOrganizationId` de um novo programa.

## 4. Portfolio e colaboracao
- O dominio principal vigente e `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel`.
- `OpenIssue` fica em paralelo no nivel de `Programa`.
- Todo `Programa` deve possuir ao menos um `Projeto` inicial no create.
- A organizacao dona do programa entra automaticamente como participante `INTERNAL` quando nao for enviada explicitamente.
- Participantes externos do programa devem pertencer ao mesmo customer do owner.
- `MilestoneTemplate` pode ser aplicado ao projeto inicial e gera snapshot de `ProjectMilestone` a partir de `plannedStartDate + offsetWeeks`.

## 5. Permissoes por papel no portfolio
- `ADMIN`: governanca, gestao e execucao.
- `MANAGER`: gestao e execucao.
- `MEMBER`: execucao.
- `SUPPORT`: somente leitura no portfolio; `SUPPORT INTERNAL` atravessa arvores para leitura.
- `AUDITOR`: somente leitura no portfolio.
- Na API atual:
- `Programa` fica na camada de governanca.
- `Projeto`, `Produto` e `OpenIssue` ficam na camada de gestao.
- `Item`, `Entregavel` e `Documento` ficam na camada de execucao.

## 6. Users
- `ADMIN INTERNAL` tem visao global do modulo de usuarios.
- `ADMIN` externo pode listar e gerenciar usuarios da propria subarvore.
- `MANAGER` e `MEMBER` nao acessam a superficie administrativa de usuarios.
- `SUPPORT INTERNAL` pode consultar uma organizacao especifica sem `supportOverride`.
- Operacoes sensiveis cross-tenant em `users` continuam exigindo `supportOverride` e `justification`.
- `SUPPORT` externo permanece restrito a propria organizacao.
- `email` e unico globalmente.
- Usuario `ACTIVE` nao troca de organizacao; o fluxo aceito e inativar/recriar ou editar enquanto ainda estiver `INVITED`.
- Usuario `INACTIVE` nao pode receber `resend-invite` nem `reset-access`.

## 7. Inativacao e purge
- `DELETE /api/users/{userId}` faz inativacao logica.
- `POST /api/users/{userId}/purge` existe apenas como excecao operacional.
- `purge` de usuario exige `SUPPORT`, `supportOverride=true`, `justification`, usuario `INACTIVE` e identidade ja ausente no Cognito.
- `DELETE /api/portfolio/organizations/{organizationId}` faz inativacao logica.
- Uma organizacao so pode ser inativada quando nao houver:
- usuarios `INVITED` ou `ACTIVE`
- filhos `ACTIVE`
- projetos ativos no portfolio proprio
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree` e uma acao operacional separada de `inativar`.
- `purge-subtree` e restrito a `SUPPORT INTERNAL` e exige `supportOverride=true` e `justification`.
- `purge-subtree` falha se a subarvore ainda participar de programas cujo owner esteja fora dela.

## 8. Documentos
- Documentos sao permitidos apenas em `Entregavel` do tipo `DOCUMENT`.
- O binario fica em storage externo; o metadado fica no banco.
- O fluxo atual e: preparar upload -> confirmar upload -> listar -> gerar download -> exclusao logica.
- O `complete` so marca o documento como `AVAILABLE` quando o objeto existe no storage gateway.
- O provider atual e `stub` em local/testes e tambem no runtime dev atual.
- A operacao com bucket S3 real ainda nao foi homologada.

## 9. Status vigentes
- `OrganizationStatus`: `ACTIVE`, `INACTIVE`
- `OrganizationSetupStatus`: `COMPLETED`, `INCOMPLETED`
- `UserStatus`: `INVITED`, `ACTIVE`, `INACTIVE`
- `ProgramStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `CANCELED`
- `ProjectStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELED`
- `ProductStatus`: `ACTIVE`, `INACTIVE`
- `ItemStatus`: `ACTIVE`, `INACTIVE`
- `DeliverableStatus`: `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELED`
- `DeliverableDocumentStatus`: `PENDING_UPLOAD`, `AVAILABLE`, `DELETED`
- `ProjectMilestoneStatus`: `PLANNED`, `COMPLETED`, `CANCELED`
- `OpenIssueStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELED`
- As transicoes estritas de status ainda nao estao fechadas no backend; isso permanece em `OPEN_GAPS.md`.
