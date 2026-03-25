# Business Rules

Ultima atualizacao: `2026-03-24`

## 1. Tenant, organization e market
- `tenant` e a fronteira SaaS e o principal boundary de isolamento.
- `organization` nao e mais sinonimo de tenant; ela representa a estrutura interna de negocio.
- Toda `organization` pertence a exatamente um `tenant`.
- `organization` pode opcionalmente apontar para um `market`.
- `tenant_market` representa a dimensao regional, comercial ou operacional do tenant.
- `TenantType.INTERNAL` continua reservado para a estrutura da plataforma/Oryzem.
- O contrato funcional de `/api/portfolio` continua operando principalmente sobre organizacoes `EXTERNAL`.

## 2. Membership e contexto de acesso
- `User` representa identidade global.
- `Membership` representa o vinculo contextual de acesso do usuario.
- Um mesmo usuario pode possuir varios memberships.
- Cada membership define `tenant`, `organization`, `market`, `status`, `isDefault` e roles do contexto.
- Papel e permissao nao devem ser tratados como fonte principal de verdade diretamente no `user`.
- O membership default pode ser trocado explicitamente.
- O contexto ativo tambem pode ser solicitado por request usando `X-Access-Context`.

## 3. Hierarquia e visibilidade por subarvore
- Cada organizacao externa enxerga o proprio portfolio e o das descendentes dentro da mesma arvore externa.
- Nao existe visibilidade lateral entre irmas nem entre customers diferentes.
- `SUPPORT INTERNAL` pode atravessar arvores externas para leitura no portfolio sob regras explicitas.
- `organization` continua preservando `parentOrganizationId`, `customerOrganizationId` e `hierarchyLevel`.
- Uma organizacao nao pode mudar de pai depois de criada.

## 4. Cadastro de organizacoes
- So `ADMIN INTERNAL` pode criar organizacao raiz.
- Criar uma organizacao raiz externa provisiona tambem um `tenant` explicito.
- `ADMIN` externo pode criar e gerenciar apenas filhas dentro da propria subarvore.
- Toda organizacao filha herda `tenant_id` do pai.
- `market_id` da organizacao filha pode ser herdado do pai quando fizer sentido operacional.
- Uma organizacao sem `ADMIN` em status `INVITED` ou `ACTIVE` fica com `setupStatus=INCOMPLETED`.
- Uma organizacao `INCOMPLETED` nao pode receber usuarios nao-`ADMIN`.
- Uma organizacao `INCOMPLETED` nao pode ser usada como `ownerOrganizationId` de um novo programa.

## 5. Onboarding de usuarios
- O Cognito continua sendo a fonte de autenticacao.
- A aplicacao continua sendo a fonte de autorizacao contextual.
- A plataforma nao possui senha local.
- Usuario criado administrativamente entra em `INVITED`.
- Trocar a senha temporaria no primeiro login nao verifica o email automaticamente no Cognito.
- A verificacao do email continua explicita e orientada ao proprio usuario autenticado.
- Quando um admin executa `reset-access`, o proximo login do usuario pode entrar em `PASSWORD_RESET_REQUIRED`.
- `reset-access` exige usuario `ACTIVE` e ao menos um canal de recovery verificado no Cognito.
- Quando o email ainda nao estiver verificado, `reset-access` deve falhar como regra de negocio clara, nunca como `500`.

## 6. Memberships de usuarios
- O contrato antigo de users ainda aceita `organizationId` e `role` planos por compatibilidade.
- Salvar ou editar um usuario por esse contrato continua sincronizando um membership default.
- A nova administracao de acessos deve usar `/api/access/users/{userId}/memberships`.
- Inativar um membership nao remove a identidade global do usuario.
- Se o membership default for inativado, outro membership ativo do usuario deve ser promovido quando disponivel.

## 7. Markets
- Cada market pertence a exatamente um tenant.
- Codigo de market deve ser unico dentro do tenant.
- Um market inativo nao pode ser atribuido a novos memberships.
- Um market nao pode ser inativado enquanto estiver referenciado por organization ou membership ativo.

## 8. Portfolio e colaboracao
- O dominio principal vigente e `Organization -> Program -> Project -> Product -> Item -> Deliverable`.
- `OpenIssue` fica em paralelo no nivel de `Program`.
- Todo `Program` deve possuir ao menos um `Project` inicial no create.
- A organizacao dona do programa entra automaticamente como participante `INTERNAL` quando nao for enviada explicitamente.
- Participantes externos do programa devem pertencer ao mesmo customer do owner.
- `MilestoneTemplate` pode ser aplicado ao projeto inicial e gera snapshot de `ProjectMilestone` a partir de `plannedStartDate + offsetWeeks`.

## 9. Permissoes por papel
- `ADMIN`: governanca, gestao e execucao.
- `MANAGER`: gestao e execucao.
- `MEMBER`: execucao.
- `SUPPORT`: somente leitura no portfolio; `SUPPORT INTERNAL` atravessa tenants para leitura.
- `AUDITOR`: somente leitura no portfolio.
- Na autorizacao atual, as permissoes continuam sendo avaliadas pela aplicacao a partir do contexto ativo do membership.

## 10. Users
- `ADMIN INTERNAL` tem visao global do modulo de usuarios.
- `ADMIN` externo pode listar e gerenciar usuarios da propria subarvore.
- `MANAGER` e `MEMBER` nao acessam a superficie administrativa de usuarios.
- `SUPPORT INTERNAL` pode consultar uma organizacao especifica sem `supportOverride`.
- Operacoes sensiveis cross-tenant em `users` continuam exigindo `supportOverride` e `justification`.
- `SUPPORT` externo permanece restrito a propria organizacao.
- `email` continua unico globalmente.
- Usuario `ACTIVE` nao troca de organizacao pelo contrato legado; a trilha recomendada agora e criar ou editar memberships conforme o contexto desejado.

## 11. Inativacao e purge
- `DELETE /api/users/{userId}` faz inativacao logica do usuario no contrato atual.
- `DELETE /api/access/users/{userId}/memberships/{membershipId}` faz inativacao logica do membership.
- `POST /api/users/{userId}/purge` existe apenas como excecao operacional.
- `purge` de usuario exige `SUPPORT`, `supportOverride=true`, `justification`, usuario `INACTIVE` e identidade ja ausente no Cognito.
- `DELETE /api/portfolio/organizations/{organizationId}` faz inativacao logica.
- Uma organizacao so pode ser inativada quando nao houver usuarios `INVITED` ou `ACTIVE`, filhos `ACTIVE` e projetos ativos no portfolio proprio.
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree` e uma acao operacional separada de inativar.

## 12. Documentos
- Documentos sao permitidos apenas em `Deliverable` do tipo `DOCUMENT`.
- O binario fica em storage externo; o metadado fica no banco.
- O fluxo atual e `prepare upload -> complete -> list -> download -> delete`.
- O `complete` so marca o documento como `AVAILABLE` quando o objeto existe no storage gateway.
- O provider atual e `stub` em local/testes e a virada operacional para `S3` real ainda depende da infraestrutura AWS.

## 13. Status vigentes
- `OrganizationStatus`: `ACTIVE`, `INACTIVE`
- `OrganizationSetupStatus`: `COMPLETED`, `INCOMPLETED`
- `UserStatus`: `INVITED`, `ACTIVE`, `INACTIVE`
- `MembershipStatus`: `ACTIVE`, `INACTIVE`
- `MarketStatus`: `ACTIVE`, `INACTIVE`
- `ProgramStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `CANCELED`
- `ProjectStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELED`
- `ProductStatus`: `ACTIVE`, `INACTIVE`
- `ItemStatus`: `ACTIVE`, `INACTIVE`
- `DeliverableStatus`: `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELED`
- `DeliverableDocumentStatus`: `PENDING_UPLOAD`, `AVAILABLE`, `DELETED`
- `ProjectMilestoneStatus`: `PLANNED`, `COMPLETED`, `CANCELED`
- `OpenIssueStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELED`
