# Shared Project Context - ProgramManagementSystem

Ultima atualizacao: `2026-03-20`

## Leia primeiro

Objetivo do produto:
- construir um SaaS de gerenciamento de projetos para a cadeia automotiva
- atender montadoras, fornecedores Tier 1 e Tier 2
- garantir rastreabilidade, colaboracao entre empresas, controle de entregas, riscos e segregacao de acesso
- este Saas servira como base para outros Saas futuros

Boas praticas de programacao utilizadas
- Encapsulamento 
- Clean code
- Priorize a simplicidade
- Use nomes de variáveis, funções e classes que descrevam claramente sua função
- Comentários uteis
- Modularizacao
- Testes Automatizados
- Valide sempre os dados de entrada para prevenir ataques como SQL Injection e XSS

Objetivo tecnico atual:
- consolidar o backend em AWS
- manter autenticacao via AWS Cognito
- sustentar a evolucao para dominio de negocio, persistencia e multi-tenancy
- operar o backend em ECS/Fargate conectado ao RDS privado
- manter uma exposicao externa simples e funcional via ALB enquanto o desenho final amadurece

Fonte de verdade desta sessao:
- usar este arquivo como ponto de retomada antes de qualquer nova implementacao

Modo de uso compartilhado:
- este arquivo passa a ser o contexto oficial compartilhado entre backend e frontend
- contexto de produto, decisoes, backlog e proximos passos ficam centralizados aqui
- backend atualiza secoes de API, dominio, persistencia, infraestrutura e operacao
- frontend atualiza secoes de UI, rotas, autenticacao cliente, experiencia e gaps de contrato
- quando uma mudanca impactar backend e frontend ao mesmo tempo, registrar primeiro aqui e depois detalhar na implementacao de cada lado
- este arquivo deve permanecer completo e cumulativo; nao deve registrar apenas o ultimo passo isolado
- a cada etapa relevante, registrar contexto atualizado, historico da entrega, especificacoes impactadas, mudancas de contrato, decisoes, gaps e proxima retomada

## Contexto compartilhado

Momento atual do produto:
- o projeto saiu da fase de fundacao tecnica e entrou na fase de validacao de produto
- o backend ja possui contrato suficiente para sustentar a primeira UI do portfolio
- o frontend esta sendo desenvolvido em outra IDE e passa a compartilhar este mesmo arquivo como fonte de contexto
- o fluxo estrutural prioritario compartilhado continua sendo `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento`
- a trilha de governanca paralela continua sendo `Programa -> OpenIssue`

Objetivo compartilhado imediato:
- validar o fluxo ponta a ponta do portfolio com uma UI basica consumindo `/api/portfolio`
- usar o backend atual como contrato real e registrar aqui qualquer ajuste necessario de API, UX, nomenclatura ou navegacao
- manter autenticacao, documentos e backlog de permissoes alinhados entre backend e frontend

## Retomada rapida

Estado atual confirmado:
- o backend valida JWT do Cognito
- `users`, `operations`, `reports` e `audit_log` persistente estao implementados
- o backend em perfil `rds` le credenciais do AWS Secrets Manager
- o RDS esta privado e aceita acesso apenas a partir do SG das tasks ECS
- a imagem do backend esta publicada no ECR
- o service esta rodando no ECS/Fargate com `runningCount=1`
- o backend esta exposto via ALB publico
- `GET /public/ping` respondeu com sucesso via ALB
- `GET /actuator/health` respondeu `UP` via ALB
- `GET /public/auth/config` respondeu com sucesso via ALB
- `GET /actuator/health/liveness` respondeu `UP` via ALB
- `GET /actuator/health/readiness` respondeu `UP` via ALB
- endpoints protegidos sem token responderam `401` via ALB para `api/ping`, `api/auth/me`, `api/authz/check`, `api/users`, `api/operations` e `api/reports`
- CORS via ALB foi validado para `http://localhost:3000` e `https://oryzem.com`
- o service ECS segue em steady state com `desiredCount=1`, `runningCount=1` e `pendingCount=0`
- o cluster ECS esta com `containerInsights=disabled`
- a task ECS atual esta com `healthStatus=HEALTHY`
- sem assumir a role administrativa, o operador atual nao possui permissao para `logs:DescribeLogStreams` nem `elasticloadbalancing:DescribeTargetHealth`
- o usuario operador `oryzem_admin` agora pode assumir a role `program-management-system-platform-admin-role`
- a role `program-management-system-platform-admin-role` concentra elevacao temporaria para IAM operacional e troubleshooting
- com a role assumida, a leitura de CloudWatch Logs e de `DescribeTargetHealth` foi validada com sucesso
- `logs:FilterLogEvents` ainda nao esta disponivel na role assumida, apesar de `DescribeLogStreams` e `GetLogEvents` estarem funcionais
- os logs do runtime confirmam conexao JDBC real com o RDS `program-management-system-db.cns8u4awye4v.sa-east-1.rds.amazonaws.com`
- ainda nao ha evidencia explicita em log provando o uso de Secrets Manager no bootstrap; isso segue pendente de telemetria mais direta
- o dominio do Cognito Hosted UI foi confirmado: `https://sa-east-1aa4i3temf.auth.sa-east-1.amazoncognito.com`
- o app client atual aceita fluxo `authorization_code` com PKCE e escopo `openid`
- o fluxo implicito com `response_type=token` retornou `unauthorized_client`
- a combinacao de escopos `openid email profile` retornou `invalid_scope` no app client atual
- Docker Desktop local esta operacional nesta maquina
- o modulo inicial do dominio principal esta implementado em `/api/portfolio`
- as migrations `V3__create_portfolio_domain.sql` e `V4__create_deliverable_document.sql` ja fazem parte da base Flyway
- o portfolio V1 ja persiste `Organizacao`, `Programa`, `ParticipacaoNoPrograma`, `Projeto`, `Produto`, `Item`, `Entregavel`, `MilestoneTemplate`, `MilestoneTemplateItem`, `ProjetoMilestone`, `OpenIssue` e `DeliverableDocument`
- o fluxo inicial de documentos de entregavel ja suporta preparar upload, confirmar upload, listar, gerar download por URL assinada e exclusao logica
- o provider de documentos padrao em local/testes e `stub`; no perfil `rds` a prioridade e `s3`
- no runtime atual do ECS/Fargate, o provider de documentos foi sobrescrito temporariamente para `stub` ate existir bucket S3 definido para producao
- o upload de documentos esta restrito, por enquanto, a `Entregavel` do tipo `DOCUMENT`
- o comportamento do modulo de portfolio e do fluxo de documentos foi validado por teste automatizado
- o modulo de `users` agora expoe contrato orientado a `organizationId`, valida a existencia da `Organizacao` no backend e devolve `organizationName` na resposta
- o modulo de `users` agora suporta criacao, atualizacao e inativacao logica, mantendo `organizationId` como referencia canonica e `tenant_id`/`tenant_type` apenas como compatibilidade interna
- `DELETE /api/users/{userId}` nao remove mais fisicamente o registro; a operacao agora muda o status do usuario para `INACTIVE`
- usuarios `INACTIVE` nao podem receber `reset-access` nem `resend-invite`
- o modulo de `users` agora persiste vinculo de identidade em `identity_username` e `identity_subject`, preparando operacao real com Cognito sem perder a projecao administrativa local
- o backend de `users` agora sincroniza `create`, `update`, `resend-invite`, `reset-access` e `inactivate` com um gateway de identidade configuravel por ambiente, usando `stub` em local/testes e deixando Cognito pronto para ativacao por configuracao
- usuarios `INVITED` passam a ser promovidos para `ACTIVE` no primeiro request autenticado valido quando o backend consegue reconciliar a identidade por `sub`, `cognito:username` ou email
- o backend agora tambem reconhece a claim `username` do `access_token` do Cognito como fallback operacional para reconciliacao de identidade e exibicao em `/api/auth/me`
- `reset-access` agora aceita apenas usuarios `ACTIVE`, `resend-invite` aceita apenas usuarios `INVITED` e usuarios `ACTIVE` nao podem trocar de `Organizacao` sem recriacao controlada
- a criacao de `Organizacao` no portfolio agora e protegida no backend e aceita apenas usuarios `ADMIN` com `TenantType=INTERNAL`
- as respostas de `Organizacao` no portfolio agora expoem `setupStatus` calculado em runtime com valores `COMPLETED` e `INCOMPLETED`
- uma `Organizacao` `INCOMPLETED` nao pode ser usada como `ownerOrganizationId` na criacao de `Programa`
- uma `Organizacao` `INCOMPLETED` nao pode receber usuarios nao-`ADMIN`; o primeiro usuario precisa ser `ADMIN`
- o modulo de `Organizacao` no backend agora tambem suporta consulta por id, edicao e inativacao logica
- uma `Organizacao` `INACTIVE` nao pode ser editada nem reutilizada como dona ou participante em novos `Programas`
- a inativacao de `Organizacao` agora exige que nao existam usuarios vinculados em status `INVITED` ou `ACTIVE`
- a migration `V6__add_organization_hierarchy.sql` agora adiciona `tenant_type`, `parent_organization_id`, `customer_organization_id` e `hierarchy_level` na tabela `organization`
- `Organizacao` externa agora suporta hierarquia real de `Customer -> filhos -> netos`, com profundidade aberta e raiz externa por customer
- as respostas de `Organizacao` agora tambem expoem `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`, `childrenCount` e `hasChildren`
- a visibilidade do portfolio agora respeita subarvore por organizacao: cada organizacao ve o proprio portfolio e o das descendentes visiveis dentro do mesmo customer
- `ADMIN` externo agora pode criar e gerenciar organizacoes filhas apenas dentro da propria subarvore; criacao de `Customer` raiz continua restrita a `ADMIN` interno
- a inativacao de `Organizacao` agora tambem bloqueia filhos ativos e projetos ativos, alem de usuarios `INVITED`/`ACTIVE`
- a criacao e a leitura de `Programa` agora validam alinhamento por customer, impedindo participantes fora da mesma arvore externa
- `GET /api/portfolio/organizations` agora tambem aceita filtros por `customerOrganizationId`, `parentOrganizationId` e `hierarchyLevel`
- `GET /api/portfolio/programs` agora tambem aceita filtro por `ownerOrganizationId`
- o backend do portfolio agora trata `Organizacao` `INTERNAL` como fora do diretorio funcional do portfolio; `internal-core` nao aparece mais na listagem administrativa nem pode ser usado como owner de programa
- o modulo de `users` agora respeita subarvore para `ADMIN` externo, mantendo `SUPPORT` externo restrito a propria organizacao
- o modulo administrativo de `users` agora fica restrito a `ADMIN` e `SUPPORT`; `MANAGER` e `MEMBER` nao acessam mais essa superficie
- `SUPPORT` interno agora pode consultar `users` cross-customer por `organizationId` sem `supportOverride`; as operacoes sensiveis cross-customer continuam exigindo `supportOverride` e `justification`
- o modulo administrativo de `Organizacao` agora usa a `AuthorizationMatrix` como fonte de verdade de papel: `VIEW` para `ADMIN`/`SUPPORT` e `CREATE`/`EDIT`/`DELETE` apenas para `ADMIN`
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso apos o fechamento do contrato hierarquico do backend, totalizando `81` testes passando
- os atributos customizados `custom:tenant_id`, `custom:tenant_type` e `custom:user_status` ja existem no User Pool do Cognito e foram validados
- o primeiro usuario real do pool recebeu backfill com `custom:tenant_id=internal-core`, `custom:tenant_type=INTERNAL` e `custom:user_status=ACTIVE`
- foi publicada uma Lambda de `Pre Token Generation` para injetar claims de tenant e status no `access_token` e no `id_token`
- o User Pool do Cognito agora esta configurado com `PreTokenGenerationConfig` em `V2_0`
- a validacao real confirmou que o `access_token` agora carrega `tenant_id`, `tenant_type`, `user_status` e os equivalentes `custom:*`
- a Lambda de `Pre Token Generation` agora tambem injeta `username` e `email` no `access_token`, preparando a reconciliacao do primeiro login do usuario convidado no backend
- o comportamento real do primeiro login de usuario convidado foi revalidado com novo token: a listagem de `users` passou a promover corretamente `INVITED` para `ACTIVE` apos o primeiro request autenticado valido
- o deploy AWS mais recente do backend esta ativo em `program-management-system:10`, com runtime configurado para `APP_SECURITY_IDENTITY_PROVIDER=cognito`
- o ambiente de dev agora possui scripts operacionais dedicados para reduzir custo sem risco de apagar o ALB: `scripts/stop-dev-aws-environment.ps1` e `scripts/start-dev-aws-environment.ps1`
- o ambiente de dev agora tambem possui `scripts/status-dev-aws-environment.ps1` para consultar rapidamente ECS, task ativa, RDS e health do endpoint publico
- atalhos na raiz do projeto agora simplificam o uso no Windows: `dev-up.cmd`, `dev-down.cmd` e `dev-status.cmd`
- a operacao de economia do ambiente de dev foi validada em uso real: `dev-down.cmd` conseguiu parar o ECS e, com a credencial operacional correta, o fluxo de parada do RDS tambem funcionou
- a role `program-management-system-ecs-task-role` recebeu permissao para operacoes administrativas de usuario no Cognito (`AdminCreateUser`, `AdminUpdateUserAttributes`, `AdminAddUserToGroup`, `AdminRemoveUserFromGroup`, `AdminResetUserPassword`, `AdminDisableUser`)
- a role `program-management-system-ecs-task-role` agora tambem inclui `AdminGetUser`, habilitando verificacao de existencia da identidade no Cognito antes de saneamentos excepcionais
- o fluxo real do modulo de `users` foi validado pela UI com usuario administrador interno: criar usuario, receber convite, editar, reenviar convite, resetar acesso, inativar, bloquear edicao de inativo e rejeitar login de usuario inativo
- o backend de `users` agora expoe um endpoint excepcional `POST /api/users/{userId}/purge` para saneamento administrativo controlado por `SUPPORT`
- a validacao real do `purge` no runtime AWS encontrou inicialmente divergencia de IAM: a role da task ECS ainda nao tinha `cognito-idp:AdminGetUser` efetivo, apesar da direcao ja registrada no contexto
- a policy inline `ProgramManagementSystemManageCognitoUsers` da role `program-management-system-ecs-task-role` foi corrigida no IAM para incluir `AdminGetUser` no User Pool `sa-east-1_aA4I3tEmF`
- apos `force-new-deployment` do service ECS, o `purge` foi validado com sucesso em uso real pela UI
- o fluxo real de usuario `INACTIVE` tambem foi revalidado pela UI apos os ajustes de Cognito e permaneceu consistente com as regras do backend
- os artefatos versionados de ECS tambem foram alinhados com o runtime atual: `infra/ecs/service-definition.template.json` agora inclui o target group do ALB e `scripts/deploy-to-ecs-fargate.ps1` passou a aguardar `steady state` e aceitar `-ForceNewDeployment`
- ainda nao houve validacao operacional do fluxo de documentos contra um bucket S3 real no runtime AWS
- a primeira UI basica do portfolio ja foi implementada no frontend local consumindo o contrato real de `/api/portfolio`
- o frontend agora consegue listar e criar `Organizacao`, criar `MilestoneTemplate`, criar `Programa` com `Projeto` inicial, navegar no detalhe do programa e seguir o fluxo estrutural ate documento em modo `stub`
- o frontend passou a aplicar fallback controlado para `id_token` quando chamadas protegidas retornam `403` com o `access_token`, reduzindo bloqueios de integracao enquanto o comportamento real do Cognito/backend e refinado
- o backend agora deixa de mascarar excecoes nao tratadas como `403` em `/error` e passa a responder `500` JSON com `correlationId`, facilitando diagnostico de falhas reais no portfolio
- o backend agora tambem responde `404` JSON com `correlationId` para rotas inexistentes, evitando falso diagnostico de `500` em casos de endpoint ausente

Ponto de retomada oficial:
- validar no frontend o novo contrato hierarquico de `Organizacao`, incluindo `Customer`, filhos, visibilidade por subarvore e apresentacao separada do portfolio por organizacao visivel
- adaptar a UX administrativa de organizacoes para criacao de raiz externa apenas por `ADMIN` interno e criacao de filhos pela propria subarvore externa
- revisar no frontend e no backend os fluxos de `users` que ainda precisem refletir a hierarquia completa, especialmente operacoes sensiveis de `SUPPORT`
- validar em uso real a trilha `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento` sob o novo isolamento por customer
- validar em paralelo a trilha de governanca `Programa -> OpenIssue`
- evoluir o modulo `/api/portfolio` com edicao, update de status, ownership e permissoes de negocio acima da base hierarquica ja implantada
- definir a primeira estrutura persistivel para os entregaveis do tipo `FORM`
- preparar e validar o fluxo de documentos com bucket S3 real no ambiente AWS
- consolidar agora a implementacao do frontend sobre o fluxo autenticado real do Cognito ja alinhado com `access_token`
- revisar, apos o smoke test expandido do frontend, se o fallback temporario para `id_token` ainda precisa permanecer
- manter em paralelo a definicao da borda externa final com HTTPS/TLS
- manter a observabilidade fina no backlog controlado, especialmente telemetria explicita de Secrets Manager e `FilterLogEvents`

## Recursos AWS atuais

Regiao:
- `sa-east-1`

VPC e subnets:
- VPC: `vpc-08aca9416504a5a9f`
- subnets do service/ALB:
  - `subnet-0200b652bed2d069a`
  - `subnet-0be1f1a93618c659c`
  - `subnet-01601f5f0d452d7da`

Cognito:
- user pool id: `sa-east-1_aA4I3tEmF`
- app client id: `rv7hk9nkugspb3i4p269sv828`
- hosted UI domain: `https://sa-east-1aa4i3temf.auth.sa-east-1.amazoncognito.com`
- issuer: `https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_aA4I3tEmF`
- JWKS: `https://cognito-idp.sa-east-1.amazonaws.com/sa-east-1_aA4I3tEmF/.well-known/jwks.json`
- custom attributes validados no pool:
  - `custom:tenant_id`
  - `custom:tenant_type`
  - `custom:user_status`
- lambda de `Pre Token Generation`: `arn:aws:lambda:sa-east-1:439533253319:function:program-management-system-cognito-pre-token`
- trigger ativo no pool: `PreTokenGenerationConfig.LambdaVersion=V2_0`
- comportamento validado em token real:
  - `access_token` agora inclui `tenant_id`, `tenant_type`, `user_status`, `username`, `custom:tenant_id`, `custom:tenant_type` e `custom:user_status`
  - `access_token` tambem inclui `email` quando o usuario possuir email preenchido no pool
  - `id_token` continua incluindo os mesmos valores, alem de `email` e `cognito:username`

RDS:
- instancia: `program-management-system-db`
- endpoint: `program-management-system-db.cns8u4awye4v.sa-east-1.rds.amazonaws.com`
- database: `program_management_system`
- engine: PostgreSQL `17.6`
- security group RDS: `sg-044b773a38b2e5b4c`
- acesso atual: somente a partir do SG das tasks ECS
- publicly accessible: `false`

Secrets Manager:
- secret: `program-management-system/rds/master`

ECS/Fargate:
- cluster: `program-management-system-cluster`
- service: `program-management-system-service`
- task definition atual: `program-management-system:10`
- task role: `program-management-system-ecs-task-role`
- execution role: `program-management-system-ecs-execution-role`
- execution role ARN: `arn:aws:iam::439533253319:role/program-management-system-ecs-execution-role`
- task security group: `sg-0af8c0fc744a9ef99` (`program-management-system-ecs-tasks-sg`)
- task atual validada: `276eb4e019d44c939bf1c9db4ad4eec1`

ECR:
- repositorio: `oryzem-backend-dev`
- URI: `439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev`
- imagem validada: `439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev:latest`

ALB:
- security group: `sg-0707623b7ced46517` (`program-management-system-alb-sg`)
- target group: `program-management-system-alb-tg`
- target group ARN: `arn:aws:elasticloadbalancing:sa-east-1:439533253319:targetgroup/program-management-system-alb-tg/1425d73086a3393d`
- listener: HTTP `80`
- DNS: `program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- regra de rede da task: inbound TCP `8080` apenas a partir do SG do ALB
- health check do target group: `GET /public/ping`

CloudWatch Logs:
- log group: `/ecs/program-management-system`

IAM operacional:
- usuario operador: `oryzem_admin`
- role administrativa assumivel: `program-management-system-platform-admin-role`
- ARN da role: `arn:aws:iam::439533253319:role/program-management-system-platform-admin-role`
- elevacao: `oryzem_admin` pode executar `sts:AssumeRole` nesta role

## Scripts e artefatos importantes

Scripts operacionais:
- `dev-down.cmd`
- `dev-status.cmd`
- `dev-up.cmd`
- `scripts/attach-observability-read-policy-to-user.ps1`
- `scripts/attach-observability-read-policy-to-role.ps1`
- `scripts/start-dev-aws-environment.ps1`
- `scripts/new-cognito-hosted-ui-urls.ps1`
- `scripts/run-app-with-rds.ps1`
- `scripts/status-dev-aws-environment.ps1`
- `scripts/stop-dev-aws-environment.ps1`
- `scripts/test-cognito-authenticated-flow.ps1`
- `scripts/test-rds-connection.ps1`
- `scripts/test-observability-read-access.ps1`
- `scripts/revoke-rds-local-access.ps1`
- `scripts/ensure-ecr-repository.ps1`
- `scripts/render-ecs-task-definition.ps1`
- `scripts/render-ecs-service-definition.ps1`
- `scripts/deploy-to-ecs-fargate.ps1`

Artefatos de deploy:
- `Dockerfile`
- `infra/ecs/README.md`
- `infra/cognito/pre-token-generation/README.md`
- `infra/cognito/pre-token-generation/index.mjs`
- `infra/cognito/pre-token-generation/deploy.ps1`
- `infra/ecs/task-definition.template.json`
- `infra/ecs/service-definition.template.json`

Artefatos recentes de dominio e storage:
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioManagementController.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioEntities.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioEnums.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioRequests.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioResponses.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/OrganizationDirectoryService.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioDocumentStorageConfig.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioResetService.java`
- `src/main/resources/db/migration/V3__create_portfolio_domain.sql`
- `src/main/resources/db/migration/V4__create_deliverable_document.sql`
- `src/main/resources/db/migration/V6__add_organization_hierarchy.sql`
- `docs/organization-hierarchy.md`
- `src/main/resources/application.yaml`
- `src/main/resources/application-rds.yaml`
- `src/test/resources/application.yaml`
- `src/test/java/com/oryzem/programmanagementsystem/portfolio/PortfolioManagementControllerSecurityTest.java`

Policies/trust policies versionadas:
- `scripts/grant-observability-read-policy.json`
- `scripts/grant-rds-secret-read-policy.json`
- `scripts/ecs-task-role-trust-policy.json`
- `scripts/ecs-execution-role-trust-policy.json`

## Status executivo

Fase atual:
- fundacao do backend concluida
- autenticacao JWT via Cognito concluida
- autorizacao centralizada em codigo concluida para V1
- persistencia inicial com PostgreSQL/Flyway concluida
- trilha de auditoria persistente concluida
- backend implantado no ECS/Fargate
- exposicao externa inicial via ALB concluida
- planejamento do dominio principal concluido para a V1 estrutural
- primeira implementacao do dominio principal concluida no backend
- gestao inicial de documentos de entregavel concluida no backend com suporte a storage externo e URL assinada
- primeira UI basica de validacao do dominio implementada no frontend local
- hierarquia de organizacoes multiempresa implantada no backend como base para o proximo ciclo de UX e permissoes
- frontend agora adaptado ao contrato hierarquico de `Organizacao` e a matriz de papeis do portfolio
- projeto pronto para validar em uso real o portfolio com isolamento por customer, visibilidade por subarvore e UX restrita por papel

Status do backend:
- operacional como OAuth2 Resource Server
- sem login por senha local
- sem sessao local
- autorizacao baseada em grupos do Cognito
- modulos `users`, `operations` e `reports` ativos
- perfil `rds` operacional
- `audit_log` persistente operacional
- modulo inicial de portfolio ativo em `/api/portfolio`
- fluxo inicial de documentos por entregavel ativo em `/api/portfolio/deliverables/{deliverableId}/documents`
- `GET /public/ping` validado via ALB
- `GET /actuator/health` validado via ALB
- `GET /public/auth/config` validado via ALB
- `GET /actuator/health/liveness` validado via ALB
- `GET /actuator/health/readiness` validado via ALB
- `GET /actuator/info` bloqueado com `401` via ALB
- comportamento `401` sem token validado via ALB para endpoints protegidos principais
- CORS validado via ALB
- service ECS em steady state
- health da task validada como `HEALTHY`
- sem elevacao para a role administrativa, a observabilidade operacional segue parcial por falta de permissoes de leitura em logs e target health
- task definition `:7` implantada com health check de container em producao
- modelo IAM em 2 camadas estabelecido com elevacao temporaria via role assumivel
- com a role assumida, a observabilidade operacional basica foi validada para Logs e Target Health
- provider de documentos configuravel por ambiente: `stub` local/testes e `s3` no perfil `rds`
- rollout de `program-management-system:6` falhou no ECS por falta de `APP_PORTFOLIO_DOCUMENTS_BUCKET_NAME` ao subir com provider `s3`
- task definition `:7` corrigiu o deploy com override temporario `APP_PORTFOLIO_DOCUMENTS_PROVIDER=stub` no runtime AWS
- excecoes nao tratadas agora retornam `500` JSON com `correlationId` em vez de cair em `403` mascarado por `/error`
- o backend de `users` agora aceita `organizationId` como referencia canonica no contrato HTTP e resolve `tenant_id`/`tenant_type` apenas internamente por compatibilidade
- o backend do portfolio agora aplica visibilidade por subarvore de `Organizacao`, separando customers externos e descendentes ligados a eles
- o backend de `Organizacao` agora expoe metadados hierarquicos e bloqueia inativacao tambem por filho ativo e projeto ativo
- o modulo de `users` agora respeita a subarvore organizacional para `ADMIN` externo nas operacoes de listagem e gestao
- o backend do portfolio agora filtra `Organizacao` `INTERNAL` para fora do contrato funcional e aceita filtros hierarquicos explicitos para organizacoes e programas

Status do frontend:
- a stack base do frontend esta definida com `React + JavaScript + Vite`
- o frontend local foi alinhado para rodar em `http://localhost:3000`
- as rotas ativas agora incluem `/`, `/callback`, `/logout`, `/workspace`, `/workspace/users`, `/workspace/programs/:programId` e `/workspace/session`
- a autenticacao base usa `react-oidc-context` com Cognito Hosted UI via `authorization_code + PKCE`
- a base de dados no cliente usa `@tanstack/react-query` e wrapper HTTP com `fetch`
- o login real via localhost foi validado com sucesso
- `GET /api/auth/me` respondeu `200` com token Cognito real via frontend local
- a autorizacao real para usuario no grupo `ADMIN` foi validada em `GET /api/admin/ping` e `GET /api/authz/check`
- a emissao real de claims de tenant no `access_token` foi validada com sucesso apos ativacao da Lambda de `Pre Token Generation`
- o frontend agora possui um workspace de portfolio consumindo `organizations`, `milestone-templates`, `programs`, detalhe de programa, `products`, `items`, `deliverables`, `documents` e `open-issues`
- o frontend agora tambem possui um workspace de `users` consumindo `GET/POST/PUT/DELETE /api/users`, filtro por `organizationId` e as acoes `resend-invite` e `reset-access`
- o workspace de `users` agora tambem cobre a acao excepcional `POST /api/users/{userId}/purge` com justificativa obrigatoria, `supportOverride=true` e UX restrita a `SUPPORT`
- o diagnostico de sessao, claims e validador de endpoints protegidos foi preservado em `/workspace/session`
- a inicializacao do `PortfolioWorkspace` foi estabilizada apos correcao de ordem de inicializacao de estado no calculo de `ownerOrganizationId`
- o workspace e o diagnostico autenticado agora reaproveitam `id_token` como fallback controlado quando o backend responde `403` ao `access_token`, com mensagem de erro orientando validacao em `/workspace/session`
- o workspace de `users` ja cobre listagem, filtro por organizacao, criacao, edicao, inativacao, reenvio de convite, reset de acesso, orientacao para primeiro `ADMIN` e feedback visual minimo de loading/empty/error/toast
- a acao excepcional de `purge` foi incorporada na UI de `users` como fluxo secundario, visivel apenas para `SUPPORT` e apenas em usuarios `INACTIVE`
- o frontend de `users` agora precisa prever tambem a acao excepcional `POST /api/users/{userId}/purge`, visivel apenas para `SUPPORT`, apenas para usuarios `INACTIVE`, com `supportOverride=true` e `justification` obrigatoria
- o contrato do backend de `Organizacao` passou a carregar metadados hierarquicos; a UI ainda precisa evoluir do modo flat atual para arvore/subarvore
- os gaps atuais mais relevantes para integracao sao logout real, enriquecimento da identidade retornada, investigacao de `principal=null` nos endpoints de ping, refinamento de UX/mensagens do novo fluxo de portfolio e smoke test integrado do novo workspace de `users`

Leitura estrategica do momento:
- o projeto saiu da fase de fundacao tecnica e entrou na fase de produto utilizavel
- a espinha dorsal do dominio principal ja existe em codigo, com contrato suficiente para uma UI basica
- agora existem duas trilhas relevantes em paralelo:
  - maturacao do produto com portfolio, documentos, formularios e permissoes
  - fechamento operacional do ambiente AWS com autenticacao real via Cognito, HTTPS/TLS e observabilidade mais madura
- a melhor forma de amadurecer o dominio daqui para frente e validar o fluxo com UI simples e feedback rapido

Macro proximos passos:
1. Validar em uso real a UI basica ja implementada sobre `/api/portfolio` e registrar ajustes de UX e contrato.
2. Evoluir o backend do portfolio com edicao, atualizacao de status, ownership e permissoes de negocio.
3. Modelar a estrutura dos entregaveis do tipo `FORM`.
4. Operacionalizar documentos em bucket S3 real no ambiente AWS.
5. Fechar a validacao autenticada real com Cognito via ALB e a estrategia final de HTTPS/TLS.

O que esta pronto:
- backend Spring Boot 4 protegido por Spring Security
- JWT validado por issuer, JWK set e audience/client id
- grupos Cognito mapeados para `ROLE_*`
- `AuthorizationService` centralizado com restricoes nomeadas
- persistencia JPA/Flyway para `users`, `operations` e `audit_log`
- reports com agregacoes e exportacao inicial
- RDS privado conectado via secret do Secrets Manager
- imagem buildada localmente e publicada no ECR
- service ECS/Fargate em execucao
- ALB internet-facing encaminhando para o service
- endpoints publicos e comportamento sem token validados externamente via ALB
- CORS funcional validado externamente via ALB
- probes `liveness` e `readiness` acessiveis via ALB
- logs do container configurados para CloudWatch Logs no task definition
- imagem Docker publicada com `curl` para suportar health check de container no ECS
- task definition atual `program-management-system:7` implantada com health check em `/actuator/health/liveness`
- task ECS atual rodando com health check de container e status `HEALTHY`
- politica versionada de leitura operacional preparada localmente para IAM
- deploy mais recente executado com sucesso no ECS/Fargate
- service atualizado para `program-management-system:7` e revalidado com task saudavel
- mapa conceitual V1 do dominio principal definido com raiz em `Programa`
- regra de negocio consolidada de que `tenant` representa uma empresa (`Organizacao`)
- colaboracao multiempresa definida via participacao de organizacoes em programas
- hierarquia conceitual definida para V1: `Programa -> Projeto -> Produto -> Item -> Entregavel`
- conceito inicial de issue macro definido como `OpenIssue` vinculado ao `Programa`
- entregavel V1 definido com tipos `DOCUMENTO` e `FORMULARIO`, implementados no codigo como `DOCUMENT` e `FORM`
- ciclo de vida inicial definido e materializado em enums para `Programa`, `Projeto`, `Entregavel`, `ProjetoMilestone`, `OpenIssue` e documentos
- estrategia de `Milestone` refinada para uso de templates aplicados a `Projeto`
- decisao de datas consolidada: backend trabalha com data completa e a UI pode operar em visao semanal `WW/YY`
- primeira entrega tecnica do dominio principal implementada com persistencia, servicos, endpoints e testes
- storage de documentos desenhado com separacao entre metadado no banco e arquivo binario em storage externo
- URL assinada de upload e download implementada via gateway configuravel
- teste automatizado cobre o fluxo principal de portfolio, milestones, documentos e `OpenIssue`
- frontend bootstrapado com `React + JavaScript + Vite`
- frontend autenticando com Cognito Hosted UI em localhost
- chamada autenticada real para `GET /api/auth/me` validada a partir do frontend
- validacao real de autorizacao administrativa executada no frontend para `GET /api/admin/ping`
- workspace de portfolio implementado no frontend para `Organizacao`, `MilestoneTemplate`, `Programa` e detalhe agregado de `Programa`
- gerenciamento administrativo de `Organizacao` implementado no frontend com listagem, criacao, edicao, inativacao logica e refetch apos mutacoes
- contrato do backend de `Organizacao` agora devolve metadados de hierarquia e isolamento por customer/subarvore
- fluxo de criacao ponta a ponta implementado na UI para `Produto`, `Item`, `Entregavel`, `OpenIssue` e documento em modo `stub`
- rota tecnica `/workspace/session` preserva o diagnostico de sessao, claims e endpoints protegidos
- cliente HTTP do frontend com fallback controlado para `id_token` em respostas `403` nas chamadas protegidas, com cobertura automatizada
- backend de `users` adaptado para contrato orientado a `organizationId`, com enriquecimento de `organizationName` e validacao de organizacao existente
- layout compartilhado do frontend ajustado para ocupar melhor a largura da viewport e reduzir margens laterais excessivas nas paginas principais
- backend de `Organizacao` expandido para suportar a jornada administrativa `listar -> consultar -> criar -> editar -> inativar`
- `lint`, `test` e `build` do frontend validados com sucesso

O que ainda falta:
- validar endpoints autenticados com Bearer token Cognito real via ALB
- fechar a estrategia final de HTTPS/TLS e DNS definitivo
- ampliar observabilidade do runtime AWS
- habilitar visibilidade operacional para logs e target health ao operador sem friccao manual
- definir e provisionar o bucket S3 definitivo para documentos do portfolio
- validar o fluxo de documentos com provider `s3` em ambiente AWS real
- validar em uso real a UI basica para navegar, criar e ler o portfolio sem ajustes manuais fora da interface
- confirmar por que algumas chamadas protegidas podem receber `403` com `access_token` e se o fallback para `id_token` deve permanecer ou ser removido apos alinhamento de claims/autorizacao
- refinar mensagens de erro, estados vazios, feedback de sucesso e ergonomia da navegacao no workspace do portfolio
- validar em uso real o novo recorte administrativo de `Organizacao`, incluindo edicao, confirmacao de inativacao e tratamento das novas regras de negocio
- adaptar o frontend de organizacoes para o novo modelo hierarquico `Customer -> filhos`, exibindo pai, nivel e escopo visivel por subarvore
- alinhar no modulo de `users` a UX e o comportamento final esperado para `SUPPORT` interno diante da nova hierarquia externa
- evoluir o restante do modulo de portfolio com operacoes de edicao, update de status e regras de transicao
- estruturar os entregaveis do tipo `FORM`
- definir ownership e permissoes de negocio por papel dentro de `ParticipacaoNoPrograma`
- endurecer o isolamento logico por customer/subarvore no novo modulo de portfolio e no modulo de `users`
- aprofundar auditoria de negocio, versionamento e retencao documental
- validar logout real do frontend com Hosted UI
- decidir como a UI representara identidade exibivel, roles, grupos e contexto de tenant
- entender por que `principal` esta vindo `null` em `GET /api/ping` e `GET /api/admin/ping`
- decidir se `/api/auth/me` deve enriquecer `username` e outros dados uteis para a interface
- validar visualmente o layout ampliado em desktop grande, notebook e mobile para calibrar densidade, respiro e hierarquia

## Frontend

Status atual:
- o frontend esta sendo desenvolvido em `C:\Users\vande\Oryzem\PMS Frontend`
- a fundacao tecnica da aplicacao ja foi validada com autenticacao real em localhost
- a primeira UI real do portfolio foi implementada consumindo o backend em AWS com token Cognito real
- o workspace agora foi dividido entre fluxo de produto e diagnostico tecnico para preservar troubleshooting sem bloquear a evolucao da UI
- o cliente HTTP autenticado do frontend agora aceita fallback para `id_token` quando o backend negar a mesma operacao com `access_token`
- o fallback para `id_token` segue temporariamente disponivel, mas a trilha principal agora ja esta alinhada para uso de `access_token` com claims de tenant
- o `AppShell` compartilhado foi ajustado para usar melhor a largura util da viewport e reduzir as bordas laterais vazias nas rotas principais
- o workspace de portfolio agora cobre a jornada administrativa de `Organizacao` com listagem, criacao, edicao e inativacao logica
- o frontend agora esconde acoes de governanca, gestao e execucao de acordo com o papel do usuario no modulo `PORTFOLIO`
- o workspace de `Organizacao` agora explora o contrato hierarquico com filtros por `customer`, pai, nivel, setup e busca, alem de suportar create root por `ADMIN` interno e create child no escopo visivel
- o workspace de portfolio agora consulta `Organizacao` apenas para `ADMIN`/`SUPPORT`, usa `ownerOrganizationId` para filtrar programas e evita quebrar a UX de `MANAGER`/`MEMBER` por `403` no diretorio administrativo
- o detalhe de programa agora suporta `POST /api/portfolio/programs/{programId}/projects` e exclusao de documento, mantendo formularios visiveis apenas para os papeis autorizados

Stack atual:
- framework base: `React`
- linguagem: `JavaScript`
- bundler/dev server: `Vite`
- roteamento: `React Router`
- autenticacao OIDC: `react-oidc-context`
- cliente HTTP base: `fetch` com wrapper interno
- cache e sincronizacao: `@tanstack/react-query`
- validacao de dados: `zod`
- testes: `Vitest`, `Testing Library` e `MSW`
- estilo inicial: `CSS Modules` com tokens proprios

Rotas e telas base:
- `/`
- `/callback`
- `/logout`
- `/workspace`
- `/workspace/organizations`
- `/workspace/users`
- `/workspace/programs/:programId`
- `/workspace/session`
- pagina `not found`

Estado atual da integracao:
- backend consumido via ALB em `http://program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- login real com Hosted UI validado
- `GET /api/auth/me` validado com token real
- `GET /api/ping` validado com `200`
- `GET /api/admin/ping` validado com `200` para usuario no grupo `ADMIN`
- `GET /api/authz/check?module=USERS&action=VIEW` validado com `allowed=true` para `ADMIN`
- `GET /api/authz/check?module=REPORTS&action=EXPORT` validado com `allowed=true` para `ADMIN`
- `GET/POST /api/portfolio/organizations` consumido no frontend
- `GET /api/portfolio/organizations/{organizationId}`, `PUT /api/portfolio/organizations/{organizationId}` e `DELETE /api/portfolio/organizations/{organizationId}` agora tambem sao consumidos no frontend
- `GET/POST /api/portfolio/milestone-templates` consumido no frontend
- `GET/POST /api/portfolio/programs` consumido no frontend
- `GET /api/portfolio/programs/{programId}` consumido no frontend
- `POST /api/portfolio/programs/{programId}/projects`, `POST /api/portfolio/projects/{projectId}/products`, `POST /api/portfolio/products/{productId}/items`, `POST /api/portfolio/items/{itemId}/deliverables` e `POST /api/portfolio/programs/{programId}/open-issues` consumidos no frontend
- fluxo de documento via `upload-url`, `complete`, `download-url` e `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}` exercitado na UI em modo `stub`
- `GET /api/portfolio/organizations` agora tambem e consumido com filtros `status`, `setupStatus`, `customerOrganizationId`, `parentOrganizationId`, `hierarchyLevel` e `search`
- `GET /api/portfolio/programs` agora tambem e consumido com filtro `ownerOrganizationId`
- chamadas protegidas agora tentam primeiro `access_token` e, em caso de `403`, repetem uma unica vez com `id_token`
- o `access_token` real passou a incluir `tenant_id`, `tenant_type`, `user_status` e os equivalentes `custom:*`, removendo o bloqueio original de claims ausentes
- o fluxo principal real de `users` foi validado na UI com sucesso para `ADMIN` interno:
  - criar usuario
  - receber email com senha provisoria
  - editar usuario
  - reenviar convite
  - resetar acesso
  - inativar usuario
  - bloquear edicao de usuario inativo
  - rejeitar login de usuario inativo

Gaps atuais e pontos de atencao:
- validar logout real com Hosted UI
- entender por que `principal` vem `null` em respostas atuais de ping
- decidir se `/api/auth/me` deve enriquecer `username`, email, roles, groups e contexto de tenant para a UI
- executar smoke test do frontend de `users` com `MANAGER` externo e decidir se o fallback temporario para `id_token` ja pode ser removido
- definir tratamento refinado de `401` e `403`
- refinar a UX do workspace do portfolio com feedback mais claro de erros e sucesso
- decidir a melhor representacao visual para owner organization, participantes e breadcrumbs do portfolio quando a subarvore crescer
- validar em uso real os cenarios de `ADMIN` externo, `SUPPORT` interno, `SUPPORT` externo, `MANAGER` e `MEMBER` apos o novo gating de acoes no frontend

## Snapshot tecnico

Stack:
- linguagem: Java
- build: Maven
- empacotamento: Jar
- framework: Spring Boot 4
- seguranca: Spring Security 7
- banco principal: PostgreSQL
- banco de teste: H2 em modo compatibilidade PostgreSQL
- auth frontend V1: Amazon Cognito Hosted UI
- storage de documentos: AWS S3 via AWS SDK Java 2.x

Java:
- `java.version`: `21`
- execucao local validada em: `JDK 23`

Package real:
- `com.oryzem.programmanagementsystem`

Modulos principais no codigo:
- `authorization`
- `audit`
- `bootstrap`
- `config`
- `operations`
- `reports`
- `security`
- `users`
- `portfolio`
- `web`

Configuracao do backend:
- profile AWS atual: `rds`
- envs principais:
  - `SPRING_PROFILES_ACTIVE=rds`
  - `DB_SECRET_ID=program-management-system/rds/master`
  - `AWS_REGION=sa-east-1`
  - `DB_SSL_MODE=require`
- envs principais do storage de documentos:
  - `APP_PORTFOLIO_DOCUMENTS_PROVIDER`
  - `APP_PORTFOLIO_DOCUMENTS_BUCKET_NAME`
  - `APP_PORTFOLIO_DOCUMENTS_KEY_PREFIX`
  - `APP_PORTFOLIO_DOCUMENTS_PRESIGN_DURATION_MINUTES`
  - `APP_PORTFOLIO_DOCUMENTS_REGION`
- override atual no ECS:
  - `APP_PORTFOLIO_DOCUMENTS_PROVIDER=stub`

Perfis e comportamento:
- default/local: `app.portfolio.documents.provider=stub`
- teste: `app.portfolio.documents.provider=stub` com bucket ficticio `stub-bucket`
- `rds`: `app.portfolio.documents.provider=s3`
- `rds`: `app.bootstrap.seed-data=false`

CORS e callbacks:
- callback local: `http://localhost:3000/callback`
- callback producao: `https://oryzem.com/callback`
- logout local: `http://localhost:3000/logout`
- logout producao: `https://oryzem.com/logout`

Migrations relevantes:
- `V1__create_users_and_operations.sql`
- `V2__create_audit_log.sql`
- `V3__create_portfolio_domain.sql`
- `V4__create_deliverable_document.sql`
- `V5__add_user_identity_columns.sql`
- `V6__add_organization_hierarchy.sql`

Storage de documentos:
- gateway: `PortfolioDocumentStorageGateway`
- provider local/teste: `StubPortfolioDocumentStorageGateway`
- provider AWS: `S3PortfolioDocumentStorageGateway`
- estrategia: upload/download por URL assinada e metadado persistido em banco
- chave de storage montada com prefixo + organizacao dona + programa + projeto + entregavel + documento

## Implementado

Seguranca:
- backend como OAuth2 Resource Server do Cognito
- validacao de JWT por issuer, JWK set e audience
- mapeamento de `cognito:groups` para roles Spring
- respostas padrao para `401` e `403`
- correlacao por `X-Correlation-Id`
- excecoes nao tratadas respondem `500` JSON com `correlationId` e sem mascaramento por `/error`

Autorizacao centralizada:
- enums `Role`, `AppModule`, `Action` e `TenantType`
- `AuthorizationContext`
- `AuthorizationMatrix`
- `AuthorizationService`
- restricoes nomeadas:
  - `SAME_TENANT_ONLY`
  - `MANAGER_TARGET_ROLE_LIMIT`
  - `MEMBER_EDIT_FLOW_RESTRICTION`
  - `AUDIT_TRAIL_REQUIRED`
  - `JUSTIFICATION_REQUIRED`
  - `SENSITIVE_DATA_RESTRICTED`
  - `SUPPORT_SCOPE_RESTRICTION`

Modulos V1:
- gestao de usuarios
- dados operacionais principais
- relatorios
- auditoria persistente em `audit_log`
- planejamento do dominio principal V1 definido com foco em `Programa`, `Projeto`, `Produto`, `Item`, `Entregavel` e `OpenIssue`
- modulo inicial de portfolio implementado com:
  - `Organizacao`
  - `Programa`
  - `ParticipacaoNoPrograma`
  - `Projeto`
  - `Produto`
  - `Item`
  - `Entregavel`
  - `MilestoneTemplate`
  - `MilestoneTemplateItem`
  - `ProjetoMilestone`
  - `OpenIssue`
  - `DeliverableDocument`
- gestao inicial de documentos implementada para `Entregavel` do tipo `DOCUMENT` com:
  - metadados persistidos em banco
  - URLs assinadas para upload e download
  - confirmacao de upload
  - listagem e exclusao logica

API inicial do portfolio:
- `GET /api/portfolio/organizations`
- `GET /api/portfolio/organizations/{organizationId}`
- `POST /api/portfolio/organizations`
- `PUT /api/portfolio/organizations/{organizationId}`
- `DELETE /api/portfolio/organizations/{organizationId}`
- `GET/POST /api/portfolio/milestone-templates`
- `GET/POST /api/portfolio/programs`
- `GET /api/portfolio/programs/{programId}`
- `POST /api/portfolio/programs/{programId}/projects`
- `POST /api/portfolio/projects/{projectId}/products`
- `POST /api/portfolio/products/{productId}/items`
- `POST /api/portfolio/items/{itemId}/deliverables`
- `GET /api/portfolio/deliverables/{deliverableId}/documents`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/upload-url`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/complete`
- `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/download-url`
- `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}`
- `POST /api/portfolio/programs/{programId}/open-issues`

API atual de users:
- `GET /api/users`
- `GET /api/users?organizationId={organizationId}`
- `POST /api/users` com `displayName`, `email`, `role` e `organizationId`
- `PUT /api/users/{userId}` com `displayName`, `email`, `role` e `organizationId`
- `DELETE /api/users/{userId}` com inativacao logica
- `POST /api/users/{userId}/resend-invite`
- `POST /api/users/{userId}/reset-access`
- `POST /api/users/{userId}/purge` com `supportOverride=true` e `justification`

Modulo users:
- objetivo atual:
  - centralizar o cadastro administrativo de usuarios vinculados obrigatoriamente a uma `Organizacao`
  - manter o contrato HTTP orientado a `organizationId` e `organizationName`, ocultando `tenant_id` e `tenant_type` da UX principal
  - suportar a jornada base `listar -> criar -> atualizar -> inativar -> executar acoes sensiveis`
  - manter projecao administrativa local alinhada com identidade real no Cognito
- modelo atual:
  - `Usuario` sempre pertence a uma `Organizacao`
  - o contrato principal responde `id`, `displayName`, `email`, `role`, `organizationId`, `organizationName`, `status`, `createdAt`, `inviteResentAt` e `accessResetAt`
  - o backend ainda persiste `tenant_id` e `tenant_type` para compatibilidade de autorizacao e filtros internos
  - o backend agora persiste tambem `identity_username` e `identity_subject` para reconciliar a identidade real do Cognito com a projecao local
  - os statuses atuais do modulo sao `INVITED`, `ACTIVE` e `INACTIVE`
- endpoints e funcao:
  - `GET /api/users` -> lista usuarios visiveis no escopo do ator autenticado
  - `GET /api/users?organizationId={organizationId}` -> lista usuarios filtrados por organizacao quando o ator possui permissao para esse escopo
  - `POST /api/users` -> cria usuario com `displayName`, `email`, `role` e `organizationId`; o usuario nasce com status `INVITED` e e provisionado no Cognito
  - `PUT /api/users/{userId}` -> atualiza `displayName`, `email`, `role` e `organizationId` de um usuario e sincroniza os atributos/grupos no Cognito
  - `DELETE /api/users/{userId}` -> inativa logicamente o usuario definindo status `INACTIVE` e desabilita o acesso no Cognito
  - `POST /api/users/{userId}/resend-invite` -> reenfileira o convite no Cognito para usuario `INVITED` e autorizado
- `POST /api/users/{userId}/reset-access` -> dispara reset administrativo no Cognito para usuario `ACTIVE` e autorizado
- `POST /api/users/{userId}/purge` -> remove fisicamente o registro administrativo local apenas como excecao operacional, exigindo `SUPPORT`, `supportOverride=true`, `justification`, usuario `INACTIVE` e identidade ja ausente no Cognito
- regras implementadas:
  - `email` e unico globalmente
  - `organizationId` precisa existir e estar ativo para criacao ou atualizacao
  - uma `Organizacao` sem `ADMIN` em status `INVITED` ou `ACTIVE` e considerada `INCOMPLETED`
  - uma `Organizacao` `INCOMPLETED` so pode receber como primeiro usuario um `ADMIN`
  - `ADMIN` externo agora pode listar e gerenciar usuarios da propria organizacao e das descendentes na mesma subarvore
  - `ADMIN` interno continua com visao e gestao globais do modulo
  - `MANAGER` e `MEMBER` nao possuem mais acesso administrativo ao modulo de `users`
  - `SUPPORT` interno pode consultar uma organizacao externa especifica sem `supportOverride`, preservando override apenas nas operacoes sensiveis cross-customer
  - `SUPPORT` externo permanece restrito a propria organizacao
  - `SUPPORT` continua dependente de `supportOverride` e `justification` para operacoes cross-tenant sensiveis no modulo de `users`
  - usuarios `INACTIVE` nao podem ser atualizados nem receber `resend-invite` ou `reset-access`
  - usuarios `INVITED` sao promovidos para `ACTIVE` no primeiro request autenticado valido quando o backend reconcilia `sub`, `cognito:username`, `username` ou email
  - o backend aceita tanto claims legadas `tenant_id` e `tenant_type` quanto claims reais do Cognito `custom:tenant_id` e `custom:tenant_type`
- a emissao real do `access_token` agora depende da Lambda `infra/cognito/pre-token-generation/index.mjs`, ja validada no User Pool de desenvolvimento
- a exclusao atual do modulo de usuarios e logica; o registro permanece persistido para rastreabilidade operacional
- quando houver divergencia excepcional entre banco e Cognito, existe agora um `purge` administrativo controlado para remover apenas a projecao local depois de validar que a identidade ja nao existe mais no Cognito

Regras implementadas no portfolio:
- criacao de `Programa` exige `initialProject`
- criacao de `Programa` exige `ownerOrganizationId`
- a organizacao dona passa a participar automaticamente do programa como `INTERNAL` quando nao vier explicitamente na lista de participantes
- o `initialProject` pode receber `milestoneTemplateId`
- a aplicacao do template cria `ProjetoMilestone` por snapshot com base em `plannedStartDate + offsetWeeks`
- `OpenIssue` e criado em nivel de `Programa`
- o detalhe de `Programa` ja retorna estrutura agregada com participantes, projetos, milestones, produtos, itens, entregaveis, documentos e issues
- documentos so podem ser anexados em `Entregavel` do tipo `DOCUMENT`
- o fluxo de `complete` verifica a existencia do objeto no storage gateway antes de marcar o documento como `AVAILABLE`
- a exclusao atual de documentos e logica, via status `DELETED`
- todo `Usuario` criado via API precisa referenciar uma `Organizacao` existente
- a resposta de `Organizacao` agora inclui `setupStatus`, calculado a partir da existencia de pelo menos um `ADMIN` com status `INVITED` ou `ACTIVE`
- `Organizacao` externa agora possui hierarquia persistida por `parentOrganizationId`, `customerOrganizationId` e `hierarchyLevel`, com `Customer` externo na raiz
- a resposta de `Organizacao` agora tambem inclui `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`, `childrenCount` e `hasChildren`
- `GET /api/portfolio/organizations` agora respeita a subarvore visivel do ator autenticado; `SUPPORT` externo fica restrito a propria organizacao
- `GET /api/portfolio/organizations` agora tambem aceita filtros por `customerOrganizationId`, `parentOrganizationId` e `hierarchyLevel`
- `GET /api/portfolio/programs` e `GET /api/portfolio/programs/{programId}` agora respeitam o escopo visivel de organizacoes pela arvore externa
- `GET /api/portfolio/programs` agora tambem aceita filtro por `ownerOrganizationId`
- `POST /api/portfolio/programs` agora exige que owner e participantes externos pertençam ao mesmo customer
- `GET /api/portfolio/milestone-templates`, `GET /api/portfolio/programs`, `GET /api/portfolio/programs/{programId}`, `GET /api/portfolio/deliverables/{deliverableId}/documents` e `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/download-url` agora usam permissao explicita de `VIEW` no modulo `PORTFOLIO`
- `POST /api/portfolio/milestone-templates` agora usa permissao de configuracao de portfolio e segue restrito a `ADMIN`
- `POST /api/portfolio/programs` agora representa a camada de governanca do portfolio e fica restrito a `ADMIN`
- `POST /api/portfolio/programs/{programId}/projects`, `POST /api/portfolio/projects/{projectId}/products` e `POST /api/portfolio/programs/{programId}/open-issues` agora ficam liberados para `ADMIN` e `MANAGER`
- `POST /api/portfolio/products/{productId}/items`, `POST /api/portfolio/items/{itemId}/deliverables`, `POST /api/portfolio/deliverables/{deliverableId}/documents/upload-url`, `POST /api/portfolio/deliverables/{deliverableId}/documents/{documentId}/complete` e `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}` agora ficam liberados para `ADMIN`, `MANAGER` e `MEMBER`
- `SUPPORT` interno e externo permanecem somente leitura no portfolio; o interno pode atravessar arvores e o externo fica restrito ao proprio escopo visivel
- `AUDITOR` permanece somente leitura no portfolio
- `Organizacao` `INTERNAL` fica fora do contrato funcional do portfolio e nao pode mais ser usada como owner ou alvo administrativo desse modulo
- `Organizacao` com `setupStatus=INCOMPLETED` nao pode ser usada como dona de um novo `Programa`
- `GET /api/portfolio/organizations/{organizationId}` agora permite consultar uma `Organizacao` individual no backend
- `GET /api/portfolio/organizations*` agora exige papel administrativo de `ADMIN` ou `SUPPORT`; `MANAGER` e `MEMBER` nao acessam mais o diretorio administrativo
- `SUPPORT` interno pode consultar todas as arvores externas no modulo administrativo de `Organizacao`; `SUPPORT` externo continua restrito a propria organizacao
- `PUT /api/portfolio/organizations/{organizationId}` agora permite editar `name` e `code` para `ADMIN` no escopo permitido pela hierarquia; `ADMIN` externo pode editar descendentes da propria subarvore
- `DELETE /api/portfolio/organizations/{organizationId}` agora realiza inativacao logica da `Organizacao`
- `Organizacao` com status `INACTIVE` nao pode ser editada
- `Organizacao` com status `INACTIVE` nao pode ser usada como `ownerOrganizationId` nem como participante em novos `Programas`
- a inativacao de `Organizacao` exige ausencia total de usuarios `INVITED` ou `ACTIVE` vinculados
- a inativacao de `Organizacao` agora tambem bloqueia filhos `ACTIVE` e `Projeto` com status `ACTIVE`
- criacao de `Customer` raiz externa segue restrita a `ADMIN + INTERNAL`; `ADMIN` externo pode criar apenas filhos dentro da propria subarvore
- a resposta de `users` agora expoe `organizationId` e `organizationName` em vez de expor `tenantId` e `tenantType` no contrato principal
- o backend preserva `tenant_id` e `tenant_type` como compatibilidade interna de autorizacao ate a evolucao completa do modulo de identidade
- `users` agora suporta update explicito via `PUT /api/users/{userId}`
- `users` agora trata delete como inativacao logica via status `INACTIVE`
- acoes sensiveis de `users` (`resend-invite` e `reset-access`) passaram a bloquear alvos `INACTIVE`
- a criacao de `Organizacao` em `/api/portfolio/organizations` agora e operacao de plataforma protegida para `ADMIN + INTERNAL`

Persistencia:
- Flyway com PostgreSQL `17.6`
- migrations versionadas
- JPA para `users`, `operations` e `audit_log`
- seed controlado por configuracao
- dominio principal inicial agora possui migration `V3__create_portfolio_domain.sql`
- persistencia inicial do portfolio implementada para `organization`, `program_record`, `program_participation`, `project_record`, `product_record`, `item_record`, `deliverable`, `milestone_template`, `milestone_template_item`, `project_milestone` e `open_issue`
- migration `V4__create_deliverable_document.sql` adicionada para metadados de documentos de entregavel
- migration `V5__add_user_identity_columns.sql` adicionada para identidade local de usuarios
- migration `V6__add_organization_hierarchy.sql` adicionada para hierarquia de organizacoes por customer

Storage de documentos:
- dependencia `software.amazon.awssdk:s3` adicionada ao backend
- modelo `DeliverableDocument` persiste `fileName`, `contentType`, `fileSize`, `storageBucket`, `storageKey`, `status` e `uploadedAt`
- statuses atuais de documento: `PENDING_UPLOAD`, `AVAILABLE`, `DELETED`
- presigned URL de upload e download suportada por provider configuravel
- provider `stub` retorna URLs falsas controladas para ambiente local/teste
- provider `s3` usa `S3Presigner` e `S3Client`
- em `rds`, o backend espera bucket/prefixo/regiao via configuracao de ambiente

Frontend:
- workspace de portfolio em `React` consumindo o contrato real do backend com `React Query`
- workspace de `users` em `React` consumindo `GET/POST/PUT/DELETE /api/users` e `POST /api/users/{userId}/resend-invite|reset-access`
- fluxo excepcional de saneamento no frontend para `POST /api/users/{userId}/purge` com justificativa obrigatoria e confirmacao explicita
- casca compartilhada do frontend agora usa largura responsiva mais ampla, com padding lateral reduzido e melhor aproveitamento horizontal em desktop
- o frontend agora possui um modulo dedicado em `/workspace/organizations` para gestao administrativa de organizacoes, separado do workspace operacional de portfolio
- o modulo de organizacoes consome `GET/POST/PUT/DELETE /api/portfolio/organizations` e `GET /api/portfolio/organizations/{organizationId}`, mostra `name`, `code`, `status` e `setupStatus`, faz refetch apos mutacoes e traduz erros frequentes da API
- o backend ja devolve `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`, `childrenCount` e `hasChildren` para preparar a evolucao da UI hierarquica
- o frontend agora ja pode se adaptar a matriz fechada de portfolio: acoes de governanca ficam visiveis apenas para `ADMIN`, acoes de gestao para `ADMIN` e `MANAGER`, e a camada de execucao para `ADMIN`, `MANAGER` e `MEMBER`
- a navegacao agora exibe `Organizacoes` para `ADMIN` e `SUPPORT`, mantendo `MANAGER` e `MEMBER` fora do diretorio administrativo
- o workspace de `Organizacao` agora suporta filtros por `status`, `setupStatus`, `customerOrganizationId`, `parentOrganizationId`, `hierarchyLevel` e `search`
- o workspace de `Organizacao` agora exibe `tenantType`, pai, customer, nivel, quantidade de filhos, `canInactivate` e `inactivationBlockedReason`
- o formulario de `Organizacao` agora aceita `parentOrganizationId`, permitindo create root por `ADMIN` interno e create child dentro da subarvore visivel do `ADMIN` externo
- os selects de novo programa no frontend passaram a considerar apenas organizacoes `ACTIVE`, evitando uso de organizacoes inativas em novos cadastros
- formularios ativos para criacao de `Organizacao`, `MilestoneTemplate` e `Programa` com `Projeto` inicial
- formularios ativos para `CreateUserForm` e `EditUserForm` com `displayName`, `email`, `role` e `organizationId`
- detalhe de programa renderiza participantes, milestones, produtos, itens, entregaveis, documentos e `OpenIssue`
- a UI agora permite criar `Projeto`, `Produto`, `Item`, `Entregavel`, `OpenIssue`, registrar documento via provider `stub` e excluir documento
- o workspace de portfolio agora usa `ownerOrganizationId` para filtrar programas visualmente por organizacao dona
- o workspace de portfolio deixou de consultar o diretorio administrativo de `Organizacao` para papeis sem acesso (`MANAGER` e `MEMBER`), evitando `403` desnecessario no overview
- `SUPPORT` e `AUDITOR` agora permanecem em leitura no portfolio tambem no frontend, com formularios e botoes de mutacao ocultos
- o workspace tecnico anterior foi preservado em `/workspace/session`
- a inicializacao do workspace de portfolio foi corrigida apos um `ReferenceError` causado por acesso antecipado a estado do formulario de programa
- o wrapper HTTP do frontend passou a repetir uma vez chamadas autenticadas com `id_token` quando o backend responder `403` ao `access_token`
- as mensagens de erro de `403` no portfolio agora orientam relogin e validacao da sessao em `/workspace/session`
- a UI de `users` oculta acoes sensiveis para `INACTIVE`, mostra `resend-invite` apenas para `INVITED`, `reset-access` apenas para `ACTIVE` e evita sugerir troca de organizacao para usuario `ACTIVE`
- a UI de `users` mostra `Purge` apenas para `SUPPORT`, apenas em usuarios `INACTIVE` e executa refetch da listagem apos sucesso
- o workspace de `users` tambem trata `400`, `401`, `403` e `500` com mensagens amigaveis, confirmacao antes de inativar e toast de sucesso para create/update/resend/reset/inactivate

AWS runtime:
- RDS privado provisionado e validado
- leitura de credenciais via Secrets Manager no bootstrap
- ECS/Fargate operacional
- ECR operacional
- ALB operacional

Qualidade:
- suite `./mvnw.cmd test` validada repetidamente durante a implantacao
- testes cobrindo autenticacao, autorizacao, CORS, `users`, `operations` e `reports`
- teste do modulo de portfolio cobre autenticacao obrigatoria, criacao da hierarquia principal, aplicacao de milestone template, upload stub, confirmacao de upload, download, listagem de documentos e criacao de `OpenIssue`
- o teste do modulo de `users` agora cobre update, inativacao logica e bloqueio de acoes sensiveis para usuarios `INACTIVE`
- o teste do modulo de `users` agora tambem cobre o fluxo excepcional de `purge` por `SUPPORT`, incluindo bloqueio para usuario ativo, exigencia de `supportOverride=true` e validacao de identidade ausente no Cognito
- o teste do modulo de portfolio agora tambem cobre a restricao `ADMIN + INTERNAL` para criacao de `Organizacao`
- o teste do modulo de portfolio agora tambem cobre consulta por id, edicao e inativacao de `Organizacao`, inclusive bloqueio de inativacao com usuarios ainda ativos/convidados e bloqueio de uso de organizacao inativa em novos programas
- o teste do modulo de portfolio agora tambem cobre hierarquia organizacional, visibilidade por subarvore, criacao de filhos por `ADMIN` externo dentro da propria arvore e bloqueio de inativacao com projeto ativo
- o teste do modulo de portfolio agora tambem cobre filtros hierarquicos, ocultacao de `Organizacao` interna no portfolio e filtro de programas por organizacao dona
- o teste do modulo de portfolio agora tambem cobre a matriz por papel: bloqueio de `MANAGER` em `Programa`, bloqueio de `MEMBER` na camada de gestao e `SUPPORT` somente leitura
- ultima execucao conhecida: `90` testes passando

## Mapa oficial do dominio V1

Camada de colaboracao entre empresas:
- `Organizacao` -> empresa/tenant do sistema
  - campos minimos conceituais: `id`, `nome`, `codigo`, `status`, `tenantType`, `parentOrganizationId`, `customerOrganizationId`, `hierarchyLevel`
  - leitura de visibilidade: cada organizacao enxerga o proprio portfolio e o das descendentes dentro da mesma arvore externa
- `Programa` -> raiz principal do dominio
  - campos minimos conceituais: `id`, `nome`, `codigo`, `descricao`, `status`, `organizacaoDonaId`, `dataInicioPlanejada`, `dataFimPlanejada`
- `ParticipacaoNoPrograma` -> vinculo entre `Organizacao` e `Programa`
  - campos minimos conceituais: `id`, `programaId`, `organizacaoId`, `papel`, `status`

Camada estrutural de execucao:
- `Projeto` -> unidade obrigatoria dentro do `Programa`
  - campos minimos conceituais: `id`, `programaId`, `nome`, `codigo`, `descricao`, `status`, `dataInicioPlanejada`, `dataFimPlanejada`
- `Produto` -> agrupador funcional dentro do `Projeto`
  - campos minimos conceituais: `id`, `projetoId`, `nome`, `codigo`, `descricao`, `status`
- `Item` -> unidade principal de controle operacional
  - campos minimos conceituais: `id`, `produtoId`, `nome`, `codigo`, `descricao`, `status`
- `Entregavel` -> entrega ligada ao `Item`
  - campos minimos conceituais: `id`, `itemId`, `nome`, `descricao`, `tipo`, `status`, `dataPlanejada`, `dataLimite`
- `DeliverableDocument` -> metadado do documento anexado ao `Entregavel`
  - campos minimos conceituais: `id`, `deliverableId`, `fileName`, `contentType`, `fileSize`, `storageBucket`, `storageKey`, `status`

Camada de planejamento:
- `MilestoneTemplate` -> template reutilizavel de milestones
  - campos minimos conceituais: `id`, `nome`, `descricao`, `status`
- `MilestoneTemplateItem` -> linha do template
  - campos minimos conceituais: `id`, `milestoneTemplateId`, `nome`, `ordem`, `obrigatorio`, `offsetWeeks`
- `ProjetoMilestone` -> milestone efetivo de um projeto
  - campos minimos conceituais: `id`, `projetoId`, `nome`, `ordem`, `status`, `dataPlanejada`, `dataReal`

Camada de governanca:
- `OpenIssue` -> issue macro do programa
  - campos minimos conceituais: `id`, `programaId`, `titulo`, `descricao`, `status`, `severidade`, `dataAbertura`

## Estados e enums atuais

Portfolio e documentos:
- `OrganizationStatus`: `ACTIVE`, `INACTIVE`
- `UserStatus`: `INVITED`, `ACTIVE`, `INACTIVE`
- `ProgramStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `CANCELED`
- `ParticipationRole`: `CLIENT`, `SUPPLIER`, `INTERNAL`, `PARTNER`
- `ParticipationStatus`: `ACTIVE`, `INACTIVE`
- `ProjectStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELED`
- `ProductStatus`: `ACTIVE`, `INACTIVE`
- `ItemStatus`: `ACTIVE`, `INACTIVE`
- `DeliverableType`: `DOCUMENT`, `FORM`
- `DeliverableStatus`: `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELED`
- `DeliverableDocumentStatus`: `PENDING_UPLOAD`, `AVAILABLE`, `DELETED`
- `MilestoneTemplateStatus`: `ACTIVE`, `INACTIVE`
- `ProjectMilestoneStatus`: `PLANNED`, `COMPLETED`, `CANCELED`
- `OpenIssueStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELED`
- `OpenIssueSeverity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `DocumentStorageProvider`: `STUB`, `S3`

## Decisoes fechadas

1. O backend nao implementa login por senha.
2. O backend apenas valida JWT emitido pelo Cognito.
3. A arquitetura de autenticacao e stateless.
4. A autorizacao inicial e baseada em grupos do Cognito convertidos para roles Spring.
5. O arquivo `PROJECT_CONTEXT.md` e a memoria operacional entre sessoes.
6. Backend e frontend passam a compartilhar o mesmo `PROJECT_CONTEXT.md` como fonte oficial de contexto.
7. Os grupos funcionais V1 sao `ADMIN`, `MANAGER`, `MEMBER`, `SUPPORT` e `AUDITOR`.
8. O contexto organizacional usa `tenant_type` e `tenant_id`.
9. `tenant_type` e `tenant_id` podem existir como claims auxiliares, mas nao sao a fonte primaria de autorizacao.
10. A validacao final de tenant e permissoes acontece na camada de aplicacao.
11. A V1 de autorizacao fica centralizada em codigo.
12. Amazon Verified Permissions nao entra na V1.
13. A V1 usa banco unico com tabelas compartilhadas e isolamento logico por `tenant_id`.
14. O banco inicial e PostgreSQL na AWS, preferencialmente RDS.
15. Os modulos prioritarios da V1 sao `users`, `operations` e `reports`.
16. A persistencia inicial usa JPA + Flyway.
17. A auditoria V1 e persistida em `audit_log`.
18. A hospedagem preferencial do backend e AWS ECS/Fargate.
19. O RDS fica privado e o acesso e feito a partir da camada de aplicacao.
20. O secret primario do banco fica no AWS Secrets Manager.
21. A primeira exposicao externa funcional do backend pode usar ALB simples antes do desenho final de rede e HTTPS.
22. O dominio principal do produto sera centrado em `Programa`.
23. `tenant` representa uma empresa e sera modelado por `Organizacao`.
24. Um `Programa` pode ser compartilhado entre multiplas organizacoes.
25. `Cliente`, `Fornecedor`, `Interna` e `Parceira` serao papeis da organizacao dentro de `ParticipacaoNoPrograma`, nao entidades separadas nesta fase.
26. Todo `Programa` deve possuir ao menos `1` `Projeto`.
27. A hierarquia conceitual V1 do dominio principal sera `Programa -> Projeto -> Produto -> Item -> Entregavel`.
28. O controle inicial de problemas macro sera feito por `OpenIssue` vinculado ao `Programa`, em trilha paralela a hierarquia operacional.
29. `Risco` e `Issue` nao serao separados na V1; o foco inicial sera `OpenIssue`.
30. `Entregavel` tera inicialmente dois tipos de negocio: `DOCUMENTO` e `FORMULARIO`, implementados no codigo como `DOCUMENT` e `FORM`.
31. `Milestone` deixara de ser apenas cadastro solto e passara a usar template aplicado a `Projeto`.
32. O backend persiste datas completas; a visao semanal `WW/YY` fica na UI.
33. O ciclo de vida inicial das entidades principais sera mantido simples: `Programa`, `Projeto`, `Entregavel` e `OpenIssue` com status definidos em nivel conceitual e materializados em enums no backend.
34. A primeira entrega tecnica do dominio principal sera exposta no backend sob `/api/portfolio`.
35. O binario de documentos de entregavel ficara em storage externo e os metadados ficarao persistidos no banco.
36. A estrategia inicial para documentos usa URLs assinadas para upload e download.
37. O storage provider padrao fora do runtime AWS sera `stub`, enquanto o perfil `rds` prioriza S3.
38. Na implementacao atual, a aplicacao de `MilestoneTemplate` em `Projeto` gera `ProjetoMilestone` por snapshot usando `plannedStartDate + offsetWeeks`.
39. Na implementacao atual, a organizacao dona do `Programa` passa a participar automaticamente como `INTERNAL` quando nao for enviada explicitamente na requisicao de criacao.
40. O fluxo inicial de documentos inclui preparar upload, confirmar upload, listar, gerar download por URL assinada e excluir logicamente.
41. O upload de documentos fica restrito, por enquanto, a `Entregavel` do tipo `DOCUMENT`.
42. A melhor forma de maturar o dominio daqui para frente sera combinar evolucao incremental do backend com uma UI basica de validacao.
43. O frontend passa a usar `/workspace` como entrada principal da UI do portfolio e preserva o diagnostico tecnico em `/workspace/session`.
44. Todo `Usuario` deve pertencer obrigatoriamente a uma `Organizacao`.
45. No modelo de produto, o vinculo canonico de `Usuario` passa a ser `organizationId`; `tenant_id` e `tenant_type` permanecem apenas como compatibilidade temporaria enquanto o backend legado de `users` e adaptado.
46. O cadastro de `Usuario` na UI nao deve expor `tenantId` nem `tenantType`; deve sempre selecionar uma `Organizacao` existente.
47. O fluxo de `Organizacao` e `Usuario` deve ser tratado como jornada integrada, com opcao explicita para criar o primeiro administrador logo apos o cadastro da organizacao.
48. `ParticipacaoNoPrograma` continua representando o papel da organizacao dentro do programa e nao substitui o vinculo obrigatorio entre `Usuario` e `Organizacao`.
49. A criacao de `Organizacao` passa a ser tratada como operacao de plataforma e deve ser permitida apenas para usuarios com `Role=ADMIN` e `TenantType=INTERNAL`.
50. O primeiro usuario de uma `Organizacao` deve ser obrigatoriamente `ADMIN`.
51. A UI deve forcar a jornada `Criar Organizacao -> Criar primeiro ADMIN`.
52. O backend deve considerar a `Organizacao` em estado incompleto quando nao existir nenhum usuario `ADMIN` com status `INVITED` ou `ACTIVE`.
53. O estado de onboarding da `Organizacao` sera calculado em runtime como `setupStatus`, com valores `COMPLETED` e `INCOMPLETED`, sem persistencia dedicada em banco nesta etapa.
54. O frontend de `users` deve trabalhar apenas com `organizationId` e `organizationName`, sem expor `tenant_id` nem `tenant_type` na UX.
55. O `access_token` permanece como trilha principal de autenticacao no frontend, mantendo fallback temporario com `id_token` ate o smoke test final confirmar que ele pode ser removido.
56. A casca compartilhada do frontend deve aproveitar melhor a largura da viewport nas telas de workspace, evitando um container central estreito com margens laterais excessivas em desktop.
57. O `code` de `Organizacao` continua sendo informado manualmente pelo frontend; apenas o `id` segue gerado automaticamente no backend.
58. A inativacao de `Organizacao` passa a ser logica e deve ser bloqueada quando ainda existirem usuarios `INVITED` ou `ACTIVE` vinculados.
59. Os formularios administrativos de `Organizacao` no frontend devem trabalhar apenas com `name` e `code`, deixando `status` derivado do backend na criacao e na inativacao logica.
60. A UI deve impedir no proprio frontend o uso de organizacoes `INACTIVE` em novos programas e concentrar create/edit/inactivate no modulo administrativo dedicado de organizacoes.
61. `Customer` passa a ser uma `Organizacao` externa raiz; `INTERNAL` fica reservado exclusivamente para a estrutura da plataforma/Oryzem.
62. A arvore externa de organizacoes passa a ser modelada por `parentOrganizationId`, `customerOrganizationId` e `hierarchyLevel`, sem limitar a profundidade em `Tier1`, `Tier2` ou `Tier3`.
63. Toda organizacao externa pertence a exatamente um customer raiz e pode ter no maximo um pai direto.
64. A visibilidade do portfolio passa a ser sempre por subarvore: a organizacao ve o proprio portfolio e o das descendentes, sem visibilidade lateral entre irmaos nem entre customers diferentes.
65. `Customer` externo tambem enxerga o proprio portfolio.
66. `ADMIN` interno continua como unico papel autorizado a criar `Customer` raiz.
67. `ADMIN` externo pode criar e gerenciar organizacoes descendentes apenas dentro da propria subarvore.
68. Uma organizacao nao pode mudar de pai depois de criada.
69. `SUPPORT` interno pode atravessar arvores de customer no portfolio; no modulo de `users`, operacoes cross-tenant sensiveis continuam exigindo `supportOverride` e `justification` para rastreabilidade.
70. `SUPPORT` externo fica restrito a propria organizacao no modulo de `users` e ao proprio escopo visivel no portfolio.
71. A inativacao de `Organizacao` deve ser bloqueada quando existirem usuarios `INVITED`/`ACTIVE`, filhos ativos ou projetos ativos.
72. Participantes externos de um `Programa` devem permanecer dentro da mesma arvore de customer do owner.
73. O contrato funcional de `/api/portfolio` passa a operar apenas sobre organizacoes `EXTERNAL`; estruturas `INTERNAL` ficam reservadas para a plataforma e fora do diretorio administrativo do portfolio.
74. O modulo `PORTFOLIO` passa a existir explicitamente na `AuthorizationMatrix` como superficie propria de autorizacao.
75. No portfolio, `ADMIN` opera governanca, gestao e execucao; `MANAGER` opera gestao e execucao; `MEMBER` opera apenas execucao.
76. `SUPPORT` e `AUDITOR` ficam somente leitura no portfolio; o `SUPPORT` interno pode atravessar arvores externas para consulta.
77. Na API atual, `Programa` pertence a governanca; `Projeto`, `Produto` e `OpenIssue` pertencem a gestao; `Item`, `Entregavel` e `Document` pertencem a execucao.
78. No frontend, o diretorio administrativo de `Organizacao` passa a ficar visivel para `ADMIN` e `SUPPORT`, preservando `CREATE`/`EDIT`/`DELETE` apenas para `ADMIN`.
79. No frontend, `MANAGER` e `MEMBER` nao devem acionar `GET /api/portfolio/organizations` dentro do workspace principal, para nao transformar a restricao administrativa em falha do overview.
80. O frontend passa a usar o filtro `ownerOrganizationId` como primeira separacao visual do portfolio por organizacao dona, mantendo uma representacao ainda em lista enriquecida antes de uma arvore mais sofisticada.

## Decisoes em aberto

1. Quais limites operacionais e de aprovacao serao exigidos para `impersonation`.
2. Como sera a estrategia de provisionamento de banco para ambientes futuros.
3. Quando o arquivo local legado `C:\Users\vande\.aws\program-management-system-db.password.txt` sera removido do ambiente do operador.
4. Qual sera o desenho final de exposicao externa: HTTP temporario vs. HTTPS/TLS com DNS definitivo.
5. Quais permissoes de observabilidade serao mantidas para logs, tasks e rede no runtime AWS.
6. Quando o service deixara de usar `assignPublicIp=ENABLED` em favor de uma topologia mais madura.
7. Qual bucket S3 definitivo sera usado para documentos, com quais politicas de IAM, encryption, lifecycle e naming.
8. Se o fluxo de upload exigira validacao de extensao, MIME type, tamanho maximo, antivirus ou limites por tipo de entregavel.
9. Como sera estruturado o modelo dos entregaveis do tipo `FORM`.
10. Como sera o desenho inicial de permissoes de negocio por papel em `ParticipacaoNoPrograma`.
11. Como a matriz atual do portfolio vai evoluir quando entrarem endpoints de update/delete/change-status para `Programa`, `Projeto`, `Produto`, `Item`, `Entregavel` e `OpenIssue`.
12. Quais transicoes de status terao validacao estrita no backend para `Programa`, `Projeto`, `Entregavel`, `ProjetoMilestone` e `OpenIssue`.
13. Se `OpenIssue` permanecera apenas em nivel de `Programa` por toda a V1 ou se depois podera descer para `Projeto`, `Produto`, `Item` ou `Entregavel`.
14. Como os templates de milestone serao classificados, versionados e selecionados por tipo de projeto.
15. Como sera a estrategia de versionamento, exclusao fisica, retencao e auditoria de documentos em S3.
16. Como a UI vai representar `WW/YY` e converter isso de forma consistente sem contaminar a persistencia canonica por data completa.
17. Como evoluir a lista enriquecida atual de `Organizacao` para uma visualizacao em arvore sem perder simplicidade operacional.
18. Se a aprovacao futura de entregavel ocorrera no nivel do `Entregavel`, do `DeliverableDocument` ou em ambos.
19. Como serao paginacao, filtros e busca das listagens de portfolio quando o volume crescer.
20. Como sera a integracao futura entre `Usuario` local e provisioning real no Cognito para convite, reenvio e sincronizacao de status.
21. Como `SUPPORT` e `AUDITOR` serao apresentados operacionalmente na UI mantendo a regra de pertencimento obrigatorio a uma `Organizacao`.
22. Se a V1 do frontend de `users` precisara expor filtros adicionais, paginacao, busca textual ou reativacao de usuario.
23. Se o modulo de `users` deve alinhar totalmente o comportamento de `SUPPORT` interno com a nova regra geral de travessia entre arvores, ou manter `supportOverride` nas operacoes mais sensiveis por auditoria.
24. Como a UI do portfolio vai separar visualmente o portfolio proprio e o portfolio herdado das organizacoes filhas.

## Proxima sessao

Checklist recomendado:
- validar em uso real o fluxo administrativo completo de `Organizacao` em `/workspace/organizations`, cobrindo create root por `ADMIN` interno, create child por `ADMIN` externo da propria arvore, edit, inactivate e bloqueios por usuarios, filhos ativos e projeto ativo
- revisar mensagens de erro, estados vazios e feedbacks de sucesso do modulo de organizacoes e do workspace do portfolio apos a introducao da hierarquia
- executar o smoke test manual do workspace de `users` cobrindo `ADMIN` interno, `ADMIN` externo com descendentes, `MANAGER` externo, onboarding do primeiro `ADMIN`, `resend-invite`, `reset-access` e inativacao
- validar no fluxo da UI a criacao ponta a ponta `Programa -> Projeto -> Produto -> Item -> Entregavel` usando organizacoes da mesma arvore externa
- testar na UI o fluxo de documento com provider `stub` em mais de um cenario de uso
- revisar, apos o smoke test de `users`, se o comportamento final de `SUPPORT` interno no modulo de usuarios fica livre por hierarquia ou continua exigindo `supportOverride` nas operacoes sensiveis
- revisar, apos o smoke test expandido, se o fallback temporario para `id_token` ainda e necessario nas chamadas autenticadas
- decidir o primeiro recorte de edicao e mudanca de status no backend do portfolio acima da base hierarquica ja entregue
- definir o desenho inicial do `FORM` antes de implementar respostas mais ricas
- comecar a matriz de permissao por papel dentro de `ParticipacaoNoPrograma`
- definir bucket, prefixo e politicas minimas para rodar documentos com `s3` no ambiente AWS
- manter no radar a validacao real com Cognito via ALB e a estrategia final de HTTPS/TLS
- manter no backlog operacional a telemetria explicita do Secrets Manager e o refinamento de observabilidade

Se o foco for UI basica:
- validar com usuarios reais a lista enriquecida atual de `Organizacao` antes de migrar para uma arvore interativa
- refinar a separacao visual entre portfolio proprio e portfolio herdado das descendentes
- desenhar a UX de `Organizacao -> Criar primeiro administrador` dentro da nova arvore
- preparar listagem de usuarios por organizacao visivel no detalhe ou workspace administrativo
- validar e refinar `GET/POST /api/portfolio/milestone-templates`
- validar e refinar `GET/POST /api/portfolio/programs`
- validar e refinar `GET /api/portfolio/programs/{programId}` com separacao por organizacao visivel
- amadurecer o fluxo de documentos por URL assinada do entregavel `DOCUMENT`

Se o foco for backend de negocio:
- implementar update e mudanca de status para `Programa`, `Projeto`, `Entregavel` e `OpenIssue`
- definir ownership por organizacao/usuario nas entidades criticas
- desenhar a estrutura de perguntas e respostas do `FORM`
- aplicar `AuthorizationService` nas operacoes novas do portfolio
- adicionar testes para transicoes, regras de negocio e cenario multi-customer

Se o foco for operacao AWS:
- validar token Cognito real via ALB
- preparar bucket S3 real e validar upload/download assinado em runtime `rds`
- definir listener HTTPS, certificado e DNS final
- revisar a necessidade de manter `assignPublicIp=ENABLED`

## Backlog priorizado

1. Adaptacao da UI de organizacoes e portfolio ao novo modelo hierarquico por customer/subarvore.
2. Evolucao do modulo de portfolio: edicao, mudanca de status, ownership e permissoes por papel acima da base hierarquica ja implantada.
3. Estruturacao dos entregaveis do tipo `FORM`.
4. Operacionalizacao de documentos com bucket S3 real, IAM minimo, validacoes de arquivo e politicas basicas.
5. Validacao autenticada fim a fim com Cognito real: Hosted UI, JWT real, ALB e autorizacao por grupos/roles.
6. Exposicao externa definitiva: HTTPS/TLS, certificado, DNS final e maturidade de rede.
7. Endurecimento do isolamento logico no portfolio e no modulo de `users` com base em customer/subarvore, incluindo UX e filtros coerentes.
8. Auditoria e trilha de alteracoes de negocio para o novo dominio.
9. Evolucao de `relatorios` orientados ao novo portfolio.
10. Endurecimento de arquitetura e operacao: IAM, observabilidade, pipeline, ambientes e revisao de rede.
11. Gestao centralizada de segredos e eliminacao de artefatos locais legados.

## Direcao imediata

Proximo passo oficial:
- adaptar e validar a UI administrativa de `Organizacao` sobre o novo contrato hierarquico por customer/subarvore
- validar o fluxo ponta a ponta `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel` dentro da mesma arvore externa
- exercitar o fluxo de documentos via provider `stub`
- escolher o primeiro pacote de evolucao do backend acima da base hierarquica: edicao/status ou `FORM`
- preparar o bucket S3 real e o checklist operacional para depois trocar o provider em ambiente AWS
- manter a validacao autenticada com Cognito via ALB e a definicao de HTTPS/TLS como trilha paralela de infraestrutura

## Plano sugerido da proxima sprint

Premissa de execucao:
- usar o backend do portfolio ja implementado como contrato para iniciar a UI basica
- manter a trilha de infraestrutura viva, mas sem bloquear a maturacao do dominio em ambiente local
- capturar feedback cedo do fluxo de negocio antes de sofisticar permissoes e formularios

Objetivo da sprint:
- sair com um fluxo ponta a ponta utilizavel do portfolio, com documentos em modo local/teste, e com backlog tecnico mais refinado para S3 real, permissoes e `FORM`

Entregas da sprint:
1. UI basica consumindo os endpoints principais do portfolio.
2. Backend do portfolio com primeiro pacote de edicao ou mudanca de status.
3. Modelo inicial dos entregaveis `FORM` definido.
4. Checklist tecnico para operacionalizar documentos em bucket S3 real.

Ordem recomendada:
1. Subir UI simples para `Organizacao`, `MilestoneTemplate`, `Programa` e detalhe do programa.
2. Validar o fluxo de criacao ponta a ponta e registrar ajustes de UX e contrato.
3. Implementar o primeiro pacote de update/status no backend.
4. Definir o shape inicial de perguntas/respostas do `FORM`.
5. Preparar a passagem controlada de `stub` para `s3` em ambiente AWS.
6. Em paralelo, manter a validacao do Cognito real via ALB e a proposta de HTTPS/TLS.

Pacote 1 - UI basica do portfolio:
- Entregaveis:
  - tela/lista de organizacoes
  - cadastro de milestone template
  - cadastro de programa com projeto inicial
  - detalhe de programa com projetos, produtos, itens, entregaveis e trilha paralela de `OpenIssue`
  - fluxo simples de documento com upload por URL assinada em modo `stub`
- Criterios de aceite:
  - usuario de teste consegue navegar pela hierarquia principal do portfolio
  - a UI consegue criar a estrutura ponta a ponta sem ajustes manuais no banco
  - a UI consegue listar e confirmar um documento de entregavel
- Riscos e dependencias:
  - pequenas adequacoes de contrato podem surgir durante o primeiro uso real da API

Pacote 2 - Evolucao imediata do backend do portfolio:
- Entregaveis:
  - update parcial para entidades principais ou transicoes iniciais de status
  - primeiras regras de ownership/permissao no portfolio
  - testes cobrindo transicoes e cenarios invalidos
- Criterios de aceite:
  - existe pelo menos um fluxo de alteracao alem de criacao e leitura
  - regras invalidas retornam erro de dominio consistente
  - testes do modulo seguem verdes
- Riscos e dependencias:
  - depende da definicao minima de ownership e do escopo de permissoes por papel

Pacote 3 - Formularios e documentos:
- Entregaveis:
  - shape inicial do `FORM` documentado
  - checklist de bucket S3, IAM, prefixo, encryption e lifecycle
  - backlog de validacoes de arquivo quebrado em etapas
- Criterios de aceite:
  - existe modelo inicial claro para `FORM`
  - existe definicao objetiva para comecar a validacao com `s3`
  - backlog tecnico de documentos esta priorizado de forma executavel
- Riscos e dependencias:
  - depende de escolhas de negocio sobre perguntas, respostas e aprovacao
  - depende da criacao/configuracao do bucket fora do repositorio

Definicao de pronto da sprint:
- a equipe consegue usar a UI para validar o dominio principal sem depender apenas de testes de backend
- o portfolio ganha o primeiro fluxo de alteracao controlada
- documentos e `FORM` passam a ter uma trilha clara de evolucao
- a ponte para `s3` real fica preparada sem bloquear o uso local imediato

Proxima fila apos a sprint:
1. validar documentos em `s3` real no ambiente AWS
2. aplicar permissoes por papel no portfolio
3. expandir `FORM` para respostas persistidas
4. retomar autenticacao real via Cognito e endurecimento da borda externa

## Como validar localmente

1. Configurar variaveis de ambiente do Cognito se necessario.
2. Subir a aplicacao com Maven.
3. Validar `GET /public/ping`.
4. Validar `GET /public/auth/config`.
5. Validar `GET /api/ping` sem token e confirmar `401`.
6. Validar `GET /api/auth/me` com Bearer token valido.
7. Validar `GET /api/admin/ping` com token de administrador.
8. Criar `Organizacao` em `POST /api/portfolio/organizations`.
9. Criar `MilestoneTemplate` em `POST /api/portfolio/milestone-templates`.
10. Criar `Programa` com `initialProject` em `POST /api/portfolio/programs`.
11. Criar `Produto`, `Item` e `Entregavel` na sequencia.
12. Para `Entregavel` do tipo `DOCUMENT`, validar `upload-url`, `complete`, `list` e `download-url`.
13. Validar `GET /api/portfolio/programs/{programId}` e conferir a estrutura agregada retornada.

## Criterios de aceite ja atendidos

- JWT valido acessa `GET /api/ping`
- sem token retorna `401` em `GET /api/ping`
- token sem `ROLE_ADMIN` retorna `403` em `GET /api/admin/ping`
- token com `ROLE_ADMIN` retorna `200` em `GET /api/admin/ping`
- CORS funcional para `http://localhost:3000` e `https://oryzem.com`
- `GET /public/ping` responde com sucesso via ALB publico
- `GET /actuator/health` responde `UP` via ALB publico
- `GET /public/auth/config` responde com sucesso via ALB publico
- `GET /actuator/health/liveness` responde `UP` via ALB publico
- `GET /actuator/health/readiness` responde `UP` via ALB publico
- sem token retorna `401` via ALB em `GET /api/auth/me`
- sem token retorna `401` via ALB em `GET /api/authz/check`
- sem token retorna `401` via ALB em `GET /api/users`
- sem token retorna `401` via ALB em `GET /api/operations`
- sem token retorna `401` via ALB em `GET /api/reports/summary`
- preflight CORS responde com sucesso via ALB para `https://oryzem.com`
- task ECS atual em `program-management-system:5` responde com `healthStatus=HEALTHY`
- `POST /api/portfolio/organizations` funciona com autenticacao valida
- `POST /api/portfolio/milestone-templates` funciona com autenticacao valida
- `POST /api/portfolio/programs` cria `Programa` com `Projeto` inicial e milestones aplicados por template
- `POST /api/portfolio/projects/{projectId}/products` funciona com autenticacao valida
- `POST /api/portfolio/products/{productId}/items` funciona com autenticacao valida
- `POST /api/portfolio/items/{itemId}/deliverables` funciona com autenticacao valida
- o fluxo de documentos com provider `stub` cobre preparar upload, concluir upload, listar e gerar download
- `POST /api/portfolio/programs/{programId}/open-issues` funciona com autenticacao valida
- `GET /api/portfolio/programs/{programId}` retorna a estrutura agregada completa do programa criada no teste automatizado

## Historico resumido

### 2026-03-07 - Base de autenticacao, autorizacao e persistencia inicial
- backend configurado como Resource Server do Cognito
- endpoint `GET /public/auth/config` criado
- endpoint `GET /api/auth/me` criado
- base de autorizacao centralizada implementada
- modulos `users` e `operations` implementados
- persistencia inicial com JPA + Flyway para `users` e `operations`
- modulo `reports` implementado
- trilha de auditoria persistente implementada em `audit_log`
- provisionamento inicial do RDS e validacao JDBC com SSL
- criacao do secret `program-management-system/rds/master`

### 2026-03-08 - Secrets Manager, Docker e primeira subida no ECS/Fargate
- integracao nativa do perfil `rds` com AWS Secrets Manager
- scripts locais ajustados para operar com secret gerenciado
- compatibilidade do Flyway com PostgreSQL `17.6` adicionada
- roles ECS criadas e preparadas
- Docker Desktop operacionalizado nesta maquina
- imagem buildada localmente e publicada no ECR
- cluster ECS criado
- task definition e service ECS criados
- falha inicial de `logs:CreateLogGroup` identificada e tratada removendo `awslogs-create-group` quando o log group ja existia
- task definition `program-management-system:3` registrada
- service estabilizado com `runningCount=1`
- validacao operacional da task em runtime `rds` concluida por comportamento

### 2026-03-09 - ALB publico e validacao externa
- security group do ALB criado: `program-management-system-alb-sg`
- task SG ajustado para aceitar `8080` apenas a partir do SG do ALB
- target group HTTP criado com health check em `/public/ping`
- ALB internet-facing criado e associado ao service ECS
- DNS do ALB consolidado: `program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com`
- `GET /public/ping` validado com sucesso via ALB
- `GET /actuator/health` validado com `UP` via ALB
- `GET /public/auth/config` validado com sucesso via ALB
- comportamento `401` sem token validado via ALB para `GET /api/ping`, `GET /api/auth/me`, `GET /api/authz/check`, `GET /api/users`, `GET /api/operations` e `GET /api/reports/summary`
- CORS validado via ALB para `http://localhost:3000` e `https://oryzem.com`
- validacao autenticada com token Cognito real permaneceu pendente nesta sessao
- `GET /actuator/health/liveness` e `GET /actuator/health/readiness` validados com `UP` via ALB
- `GET /actuator/info` retornou `401` via ALB
- observabilidade atual confirmada com `containerInsights=disabled`, service em steady state e task com `healthStatus=UNKNOWN`
- leitura de CloudWatch Logs e `DescribeTargetHealth` ficou bloqueada por falta de permissao no operador atual

### 2026-03-10 - Preparacao do health check de container
- `Dockerfile` ajustado para incluir `curl` na imagem final
- `infra/ecs/task-definition.template.json` ajustado com health check do container em `GET /actuator/health/liveness`
- `infra/ecs/task-definition.rendered.json` regenerado com o novo bloco de health check
- imagem `latest` rebuildada e publicada no ECR com digest `sha256:78ee7b15d5ee39c634d7329fc077285bd3cefcbe1e3ea2b78d5febaee564d284`
- task definition `program-management-system:4` registrada e implantada no ECS/Fargate
- service estabilizado novamente com `runningCount=1`
- nova task `84fb1ca3cade42c6b6c3bb1aeca74fb0` validada com `healthStatus=HEALTHY`
- `GET /public/ping`, `GET /actuator/health`, `GET /actuator/health/liveness` e `GET /actuator/health/readiness` revalidados com sucesso via ALB apos o deploy
- `scripts/grant-observability-read-policy.json` e `scripts/attach-observability-read-policy-to-user.ps1` criados para fechar o gap de leitura operacional
- tentativa de aplicar a politica no usuario `oryzem_admin` falhou por falta de permissao `iam:PutUserPolicy`
- priorizacao executiva consolidada para a proxima fase: observabilidade minima, autenticacao real com Cognito, exposicao externa definitiva, dominio principal e endurecimento arquitetural
- `scripts/attach-observability-read-policy-to-role.ps1` criado para suportar concessao via role quando o principal operacional nao for um usuario IAM
- `scripts/test-observability-read-access.ps1` criado para validar leitura de CloudWatch Logs, `DescribeTargetHealth` e evidencias de runtime do Secrets Manager + RDS
- o bloqueio operacional foi revalidado no usuario `oryzem_admin`: `DescribeLogStreams` e `DescribeTargetHealth` continuam negados ate concessao de IAM
- a role `program-management-system-platform-admin-role` foi criada e configurada como camada administrativa assumivel pelo usuario `oryzem_admin`
- o fluxo `sts:AssumeRole` foi validado com sucesso para a role administrativa
- a leitura operacional via role assumida foi validada com sucesso para `logs:DescribeLogStreams`, `logs:GetLogEvents` e `elasticloadbalancing:DescribeTargetHealth`
- `logs:FilterLogEvents` permaneceu negado na role assumida e pode exigir alinhamento fino da policy operacional
- os logs do container confirmaram startup do backend com Hikari e Flyway conectando ao RDS `program-management-system-db.cns8u4awye4v.sa-east-1.rds.amazonaws.com`
- a evidencia explicita do uso de Secrets Manager no bootstrap ainda nao apareceu nos logs atuais e pode exigir telemetria adicional no codigo
- o foco imediato foi realinhado para o passo 2: validacao autenticada fim a fim com Cognito, deixando a observabilidade fina em backlog nao critico
- `scripts/new-cognito-hosted-ui-urls.ps1` criado para montar URLs de login/logout do Hosted UI e o material de PKCE
- `scripts/test-cognito-authenticated-flow.ps1` criado para decodificar o JWT real e validar `/api/ping`, `/api/auth/me`, `/api/admin/ping` e `/api/authz/check` via ALB
- o dominio do Hosted UI foi confirmado como `https://sa-east-1aa4i3temf.auth.sa-east-1.amazoncognito.com`
- o app client atual respondeu com redirecionamento valido para `/login` apenas no fluxo `authorization_code` com PKCE e escopo `openid`
- o fluxo implicito com `response_type=token` retornou `unauthorized_client`
- a tentativa com escopos `openid email profile` retornou `invalid_scope`, indicando que o app client atual aceita um conjunto mais restrito de escopos
- `./mvnw.cmd test` foi executado com sucesso antes do deploy, com `46` testes passando
- o deploy mais recente registrou a task definition `program-management-system:5` e atualizou o service ECS
- a nova task `a2897ccffde54d35a2fbc9f1a5096f8f` subiu com `healthStatus=HEALTHY`
- o target `172.31.9.59:8080` ficou `healthy` no target group e o target anterior entrou em `draining`
- `GET /public/ping` e `GET /actuator/health` foram revalidados com sucesso via ALB apos o deploy
- a imagem implantada manteve o digest `sha256:78ee7b15d5ee39c634d7329fc077285bd3cefcbe1e3ea2b78d5febaee564d284`, indicando que o runtime publicado nao mudou em relacao ao deploy anterior

### 2026-03-11 - Planejamento do dominio principal V1
- foi definida a decisao de modelar o dominio principal com raiz em `Programa`
- foi fechado que `tenant` representa uma empresa, modelada por `Organizacao`
- foi definido que um `Programa` pode ser compartilhado entre multiplas organizacoes
- foi definido que `Cliente`, `Fornecedor`, `Interna` e `Parceira` serao papeis da organizacao dentro de `ParticipacaoNoPrograma`
- foi definida a estrutura conceitual V1 `Programa -> Projeto -> Produto -> Item -> Entregavel`
- foi fechado que todo `Programa` deve possuir ao menos `1` `Projeto`
- foi definido o uso inicial de `OpenIssue` como issue macro vinculada ao `Programa`
- foi fechado que `Entregavel` tera inicialmente os tipos `DOCUMENTO` e `FORMULARIO`
- foi definido que `Milestone` passara a ser baseado em template aplicado a `Projeto`, preservando instancias proprias por projeto
- foi definido que o backend continuara trabalhando com data completa e a UI podera trabalhar com visao semanal `WW/YY`
- foram propostos ciclos de vida iniciais para `Programa`, `Projeto`, `Entregavel` e `OpenIssue`, depois materializados em enums no backend
- foram consolidados os campos minimos conceituais das entidades V1 do portfolio
- a migration `V3__create_portfolio_domain.sql` foi criada para introduzir a persistencia inicial do portfolio
- o pacote `com.oryzem.programmanagementsystem.portfolio` foi implementado com entidades, enums, repositorios, servico e controller
- o backend passou a expor o modulo inicial de portfolio em `/api/portfolio`
- foram implementados endpoints de criacao e consulta para `Organizacao`, `MilestoneTemplate`, `Programa`, `Projeto`, `Produto`, `Item`, `Entregavel` e `OpenIssue`
- a criacao de `Programa` passou a exigir `Projeto` inicial e suportar aplicacao de `MilestoneTemplate`
- a aplicacao de milestone template gera `ProjetoMilestone` por snapshot, com `plannedDate` derivada da data inicial do projeto e de `offsetWeeks`
- `BootstrapDataService.reset()` passou a limpar tambem os dados do novo modulo de portfolio
- `ApiExceptionHandler` passou a tratar `PortfolioNotFoundException` com `404`
- a suite `./mvnw.cmd test` foi executada com sucesso apos a implementacao, totalizando `48` testes passando
- a dependencia `software.amazon.awssdk:s3` foi adicionada ao backend para suportar a integracao de documentos
- foi implementado o modelo `DeliverableDocument` para persistir metadados de documentos vinculados a `Entregavel`
- a migration `V4__create_deliverable_document.sql` foi criada para persistir metadados de documentos
- foi definida a estrategia de storage com separacao entre binario em storage externo e metadados no banco
- foi implementado `PortfolioDocumentStorageGateway` com dois providers:
  - `stub` para ambiente local/testes
  - `s3` para runtime AWS
- o backend passou a expor endpoints para documentos de entregavel em `/api/portfolio/deliverables/{deliverableId}/documents`
- o fluxo inicial implementado para documentos cobre:
  - criacao de URL assinada de upload
  - confirmacao de upload
  - listagem de documentos
  - criacao de URL assinada de download
  - exclusao logica
- o upload de documentos foi inicialmente restrito a `Entregavel` do tipo `DOCUMENT`
- o teste de integracao do portfolio foi ampliado para validar o ciclo principal de documento com provider `stub`
- o `PROJECT_CONTEXT.md` foi ampliado para registrar o mapa oficial do dominio V1, os enums atuais, o estado do portfolio, o fluxo de documentos e a direcao das proximas etapas

### 2026-03-13 - Unificacao do contexto compartilhado entre backend e frontend
- `PROJECT_CONTEXT.md` passou a ser o arquivo oficial compartilhado entre backend e frontend
- foi adicionada uma secao de contexto compartilhado para centralizar produto, backlog e alinhamento entre as duas frentes
- foi adicionada uma secao dedicada ao frontend para registrar stack, rotas, autenticacao na UI e gaps de contrato encontrados durante a implementacao
- a regra de atualizacao deste arquivo passou a valer explicitamente para entregas de backend e frontend
- o snapshot operacional do frontend foi incorporado ao arquivo compartilhado a partir de `C:\Users\vande\Oryzem\PMS Frontend\PROJECT_CONTEXT_FRONTEND.md`
- o contexto compartilhado agora registra stack real do frontend, rotas base, status da autenticacao Cognito, validacoes reais contra `/api/auth/me`, `/api/admin/ping` e `authz/check`, alem dos gaps atuais de logout e identidade exibivel
- o fluxo principal do produto foi esclarecido separando a hierarquia estrutural `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento` da trilha paralela de governanca `Programa -> OpenIssue`
- referencias antigas a `program-management-system:4` foram removidas das secoes de snapshot atual e mantidas apenas no historico
- a redacao de observabilidade passou a distinguir explicitamente o que ocorre sem elevacao e com role assumida
- a numeracao de `Decisoes fechadas` foi corrigida

### 2026-03-14 - Primeira UI basica do portfolio no frontend
- o frontend passou a consumir o contrato real de `/api/portfolio` em um workspace de produto
- a rota `/workspace` virou a entrada principal da UI do portfolio e a rota `/workspace/session` preservou o diagnostico tecnico anterior
- foram implementadas na UI as listagens e criacoes de `Organizacao`, `MilestoneTemplate` e `Programa` com `Projeto` inicial
- o detalhe de `Programa` passou a renderizar participantes, milestones, produtos, itens, entregaveis, documentos e `OpenIssue`
- a UI passou a permitir criar `Produto`, `Item`, `Entregavel`, `OpenIssue` e registrar documento via provider `stub`
- `npm run lint`, `npm run test` e `npm run build` foram executados com sucesso apos a implementacao

### 2026-03-14 - Correcao de inicializacao do PortfolioWorkspace
- o frontend apresentava `ReferenceError` ao abrir o workspace por calcular `effectiveOwnerOrganizationId` antes da inicializacao de `programForm`
- o calculo foi movido para depois do `useState` correspondente, estabilizando a montagem inicial da tela
- `npm run lint`, `npm run test` e `npm run build` foram reexecutados com sucesso apos a correcao

### 2026-03-14 - Recuperacao do deploy ECS apos falha de configuracao de documentos
- o build local com `./mvnw.cmd clean test` voltou a passar com `48` testes
- o primeiro rollout de `program-management-system:6` falhou no ECS porque o profile `rds` subiu com provider `s3` sem `APP_PORTFOLIO_DOCUMENTS_BUCKET_NAME`
- os logs do CloudWatch confirmaram erro de bootstrap em `PortfolioDocumentStorageConfig` exigindo `app.portfolio.documents.bucket-name`
- o template `infra/ecs/task-definition.template.json` foi ajustado para sobrescrever temporariamente `APP_PORTFOLIO_DOCUMENTS_PROVIDER=stub` no runtime AWS
- um novo deploy registrou `program-management-system:7`, promoveu a task `afbc2008a7dd4d7493e24d4c7f4c57d5` e recolocou o service em steady state
- `GET /public/ping` e `GET /actuator/health/liveness` responderam `200` via ALB apos a recuperacao

### 2026-03-14 - Compatibilidade de token no frontend para `403` do portfolio
- o frontend passou a repetir uma unica vez chamadas autenticadas com `id_token` quando o backend responder `403` ao `access_token`
- o ajuste cobre o workspace do portfolio, o detalhe de programa e o diagnostico autenticado em `/workspace/session`
- as mensagens de erro de `403` no portfolio passaram a orientar relogin e validacao da sessao em `/workspace/session`
- `npm run test`, `npm run lint` e `npm run build` foram reexecutados com sucesso apos a correcao

### 2026-03-15 - Tratamento de erro do backend para falso `403` no portfolio
- foi identificado que algumas falhas de runtime estavam sendo encaminhadas para `/error` e apareciam na UI como `403 Forbidden`, apesar da operacao principal ja ter sido concluida
- `SecurityConfig` passou a liberar `/error` para impedir que esse encaminhamento interno esconda a falha real atras da camada de seguranca
- `ApiExceptionHandler` passou a capturar excecoes nao tratadas, responder `500` JSON consistente e incluir `correlationId` no corpo para facilitar troubleshooting
- foi adicionado teste automatizado cobrindo o fluxo de excecao nao tratada sem mascaramento por `403`
- a validacao local executou `./mvnw.cmd test "-Dtest=ApiExceptionHandlerTest,PortfolioManagementControllerSecurityTest"` com sucesso

### 2026-03-15 - Contrato de `users` orientado a `organizationId`
- `CreateUserRequest` deixou de expor `tenantType` e passou a usar `organizationId` como referencia canonica da organizacao do usuario
- `GET /api/users` e `POST /api/users` passaram a responder `organizationId` e `organizationName` no contrato principal
- o backend passou a validar que a `Organizacao` informada existe e esta ativa antes de criar o usuario
- foi criado um diretório publico de organizacoes para permitir lookup e enriquecimento de resposta sem vazar detalhes internos das entidades de portfolio
- o seed local agora cria organizacoes basicas para `internal-core`, `tenant-a` e `tenant-b`, mantendo coerencia entre usuarios seeded e portfolio
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `50` testes passando

### 2026-03-15 - Planejamento integrado de cadastro de `Organizacao` e `Usuario`
- foi fechado que todo `Usuario` pertence obrigatoriamente a uma `Organizacao`, eliminando a exposicao conceitual de tenant abstrato na UX
- a direcao de produto passou a tratar `organizationId` como referencia canonica do cadastro de usuario, mantendo `tenant_id` e `tenant_type` apenas como compatibilidade tecnica temporaria
- foi definido que a jornada ideal de UX e `Criar organizacao -> opcionalmente criar primeiro administrador -> listar e convidar demais usuarios`
- `ParticipacaoNoPrograma` foi reafirmada como vinculo entre `Organizacao` e `Programa`, sem substituir o pertencimento base do usuario a sua organizacao
- o backlog das proximas sessoes passou a incluir a adaptacao do contrato de `users`, a UX do primeiro administrador e a exibicao de usuarios por organizacao

### 2026-03-17 - Evolucao do modulo de `users` com update e inativacao logica
- foi adicionado `PUT /api/users/{userId}` para atualizar `displayName`, `email`, `role` e `organizationId`
- `DELETE /api/users/{userId}` deixou de remover fisicamente o registro e passou a definir o status `INACTIVE`
- o enum `UserStatus` passou a incluir `INACTIVE`
- `resend-invite` e `reset-access` passaram a bloquear usuarios inativos
- o contrato de `users` manteve `organizationId` como referencia canonica e continuou ocultando `tenant_id` e `tenant_type` da UX principal
- os testes do modulo de `users` foram ampliados para cobrir update, inativacao logica e bloqueio de acoes sensiveis em usuarios inativos
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `53` testes passando

### 2026-03-17 - Protecao de criacao de `Organizacao` para `ADMIN + INTERNAL`
- `POST /api/portfolio/organizations` passou a validar no backend que apenas usuarios `ADMIN` com `TenantType=INTERNAL` podem criar organizacoes
- a regra foi aplicada no controller/service do portfolio usando o contexto autenticado real do JWT
- o teste de seguranca do portfolio passou a cobrir o cenario permitido para `ADMIN + INTERNAL` e o bloqueio para `ADMIN + EXTERNAL`
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `54` testes passando

### 2026-03-17 - `setupStatus` calculado para `Organizacao` e bloqueios de onboarding
- `GET/POST /api/portfolio/organizations` passaram a responder `setupStatus` com valores `COMPLETED` e `INCOMPLETED`, calculados em runtime sem persistencia dedicada
- o backend passou a considerar `Organizacao` incompleta quando nao existe nenhum usuario `ADMIN` com status `INVITED` ou `ACTIVE`
- `POST /api/portfolio/programs` agora bloqueia `ownerOrganizationId` incompleto, exigindo ao menos um `ADMIN` convidado ou ativo
- `POST /api/users` e `PUT /api/users/{userId}` agora bloqueiam criacao ou atualizacao de usuarios nao-`ADMIN` para `Organizacao` ainda incompleta
- os dados seed de organizacoes externas passaram a incluir administradores para preservar coerencia com a nova regra
- a cobertura de testes foi ampliada para validar `setupStatus`, bloqueio de `Programa` com organizacao incompleta, bloqueio de primeiro usuario nao-`ADMIN` e o contrato JSON de erro `500`
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `56` testes passando

### 2026-03-19 - Integracao factivel do modulo de `users` com identidade real
- foi adicionada a migration `V5__add_user_identity_columns.sql` para persistir `identity_username` e `identity_subject` no modulo de `users`
- o backend passou a ter um `UserIdentityGateway` configuravel por ambiente, com implementacao `stub` para local/testes e implementacao Cognito pronta para uso por configuracao
- `POST /api/users`, `PUT /api/users/{userId}`, `POST /api/users/{userId}/resend-invite`, `POST /api/users/{userId}/reset-access` e `DELETE /api/users/{userId}` agora sincronizam o estado local com o gateway de identidade
- a auditoria do modulo de `users` foi ampliada para cobrir `USER_CREATE`, `USER_UPDATE` e `USER_INACTIVATE`, alem das acoes sensiveis ja existentes
- foi fechado que `reset-access` aceita apenas usuarios `ACTIVE`, `resend-invite` aceita apenas usuarios `INVITED` e usuarios `ACTIVE` nao podem trocar de `Organizacao`
- foi adicionado um filtro autenticado para reconciliar identidade em runtime e promover usuarios `INVITED` para `ACTIVE` no primeiro request autenticado valido
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `62` testes passando

### 2026-03-19 - Alinhamento real do Cognito para claims de tenant no `access_token`
- os atributos `custom:tenant_id`, `custom:tenant_type` e `custom:user_status` foram criados e validados no User Pool `sa-east-1_aA4I3tEmF`
- o usuario administrador real recebeu backfill com `internal-core`, `INTERNAL` e `ACTIVE`
- foi criada e publicada a Lambda `program-management-system-cognito-pre-token` com codigo versionado em `infra/cognito/pre-token-generation/index.mjs`
- o User Pool passou a usar `PreTokenGenerationConfig` em `V2_0` apontando para a Lambda acima
- a validacao real em `/workspace/session` confirmou que o `access_token` agora inclui `tenant_id`, `tenant_type`, `user_status` e os equivalentes `custom:*`
- o backend de autenticacao e sincronizacao de usuarios foi ajustado para aceitar tambem a claim `username` presente no `access_token`

### 2026-03-19 - Validacao funcional real do modulo de `users` via UI
- o backend foi reimplantado no ECS/Fargate como `program-management-system:8`, agora com `APP_SECURITY_IDENTITY_PROVIDER=cognito` ativo no runtime
- o runtime AWS falhou inicialmente ao criar usuarios por falta de permissao `cognito-idp:AdminCreateUser` na role `program-management-system-ecs-task-role`
- a role da task recebeu permissao explicita para administrar usuarios no User Pool, cobrindo criacao, update de atributos, grupos, reset de senha e disable
- apos novo deployment, o fluxo real do modulo de `users` foi validado com sucesso na UI:
  - criacao de usuario com email de senha provisoria
  - edicao de usuario
  - reenvio de convite
  - reset de acesso
  - inativacao
  - bloqueio de edicao para usuario inativo
  - rejeicao de login para usuario inativo
- com isso, o modulo de `users` passa a ser considerado funcional ponta a ponta para o fluxo principal com `ADMIN` interno

### 2026-03-19 - Primeiro workspace de `users` no frontend
- foi criada a rota `/workspace/users` para a administracao de usuarios ao lado do workspace de portfolio
- o frontend passou a consumir `GET/POST/PUT/DELETE /api/users` e `POST /api/users/{userId}/resend-invite|reset-access` com `React Query`
- o workspace de `users` passou a listar usuarios, filtrar por `organizationId`, criar, editar, inativar, reenviar convite e resetar acesso
- a UI passou a refletir as regras do backend: esconder acoes sensiveis para `INACTIVE`, permitir `resend-invite` apenas para `INVITED`, `reset-access` apenas para `ACTIVE` e bloquear sugestao de troca de `Organizacao` para usuario `ACTIVE`
- o frontend passou a orientar explicitamente a criacao do primeiro `ADMIN` quando a organizacao selecionada estiver com `setupStatus=INCOMPLETED`
- `npm run lint`, `npm run test` e `npm run build` foram executados com sucesso apos a implementacao do workspace de `users`

### 2026-03-19 - Acao excepcional de `purge` integrada ao frontend de `users`
- o frontend passou a consumir `POST /api/users/{userId}/purge?supportOverride=true&justification=...`
- a acao `Purge` ficou visivel apenas para sessao com role `SUPPORT` e apenas para usuarios `INACTIVE`
- a UX de `purge` passou a exigir justificativa obrigatoria, confirmacao explicita e refetch da listagem apos sucesso
- `npm run lint`, `npm run test` e `npm run build` foram reexecutados com sucesso apos a integracao do `purge`

### 2026-03-19 - Validacao real do `purge` no runtime AWS
- o primeiro teste real de `POST /api/users/{userId}/purge` no ALB falhou com `500` e `correlationId=91f7a049-f23d-43b0-afb4-3d2358638889`
- a analise de CloudWatch mostrou que a task antiga ainda nao expunha a rota de `purge`, resultando em `NoResourceFoundException` para `/api/users/{userId}/purge`
- o backend foi reimplantado no ECS/Fargate como `program-management-system:9`
- `ApiExceptionHandler` foi ajustado para responder `404` JSON com `correlationId` quando a falha real for rota inexistente
- testes direcionados de backend foram reexecutados com sucesso para `ApiExceptionHandlerTest` e `UserManagementControllerSecurityTest`
- no segundo teste real de `purge`, a analise de CloudWatch mostrou falha de IAM em `cognito-idp:AdminGetUser` para a role `program-management-system-ecs-task-role`
- a policy inline `ProgramManagementSystemManageCognitoUsers` foi corrigida no IAM para incluir `AdminGetUser`, seguida de `force-new-deployment` no service ECS
- apos a reciclagem da task, o `purge` foi executado com sucesso em ambiente AWS

### 2026-03-19 - Ajuste da Lambda de `Pre Token Generation` para primeiro login
- a analise do `access_token` real de um usuario convidado mostrou ausencia de `email`, `cognito:username` e `username` util para reconciliacao no backend; o campo `username` observado em `/api/auth/me` estava apenas refletindo o fallback do `subject`
- isso impedia a promocao automatica local de `INVITED` para `ACTIVE` na primeira chamada autenticada do usuario, mesmo quando a listagem de `users` era consultada depois
- a Lambda `program-management-system-cognito-pre-token` foi ajustada para adicionar `username=event.userName` e `email=userAttributes.email` no `access_token` e no `id_token`
- a validacao local do payload transformado confirmou a presenca de `username` e `email` junto de `tenant_id`, `tenant_type` e `user_status`
- o codigo da Lambda foi republicado em `2026-03-19`; para validar no frontend, e necessario fazer logout/login novamente para forcar emissao de novos tokens
- apos novo login com token renovado, o primeiro request autenticado do usuario convidado voltou a reconciliar corretamente a identidade local e promoveu o registro de `INVITED` para `ACTIVE`
- em seguida, o fluxo real de usuario `INACTIVE` tambem foi exercitado com sucesso e permaneceu bloqueando acesso e acoes sensiveis conforme esperado

### 2026-03-19 - Alinhamento dos artefatos versionados de ECS com o runtime atual
- `infra/ecs/service-definition.template.json` passou a refletir o service real com `loadBalancers` para o target group `program-management-system-alb-tg` e `healthCheckGracePeriodSeconds=120`
- `scripts/deploy-to-ecs-fargate.ps1` passou a aguardar `aws ecs wait services-stable` antes de retornar, reduzindo falsos positivos de deploy concluido
- o script de deploy tambem ganhou suporte a `-ForceNewDeployment`, facilitando reciclagem de tasks em cenarios como ajuste de IAM sem depender de comando AWS manual separado

### 2026-03-19 - Scripts operacionais para ligar e desligar o ambiente de dev AWS
- foi criado `scripts/stop-dev-aws-environment.ps1` para escalar o ECS service para `desiredCount=0` e parar o RDS
- o script de parada foi deliberadamente ajustado para nunca deletar o ALB, eliminando risco operacional de remover o endpoint por engano
- foi criado `scripts/start-dev-aws-environment.ps1` para iniciar o RDS e restaurar o ECS service para `desiredCount=1`
- foi criado `scripts/status-dev-aws-environment.ps1` para consultar rapidamente conta AWS, status do ECS, task atual, status do RDS e health do endpoint publico
- foram adicionados atalhos `dev-up.cmd`, `dev-down.cmd` e `dev-status.cmd` na raiz do projeto para acionar esses scripts com menos digitacao no Windows
- o fluxo foi validado em uso real no terminal; o ajuste final deixou os scripts exibindo melhor erros de IAM e a parada completa funcionou quando executada com a credencial operacional adequada
- a documentacao de ECS passou a registrar esse fluxo como mecanismo de economia para ambiente de desenvolvimento, com a ressalva de que o ALB continua gerando custo enquanto existir

### 2026-03-19 - Evolucao do modulo de `Organizacao` no backend
- `GET /api/portfolio/organizations/{organizationId}` foi adicionado para consulta individual de organizacao
- `PUT /api/portfolio/organizations/{organizationId}` foi adicionado para editar `name` e `code`
- `DELETE /api/portfolio/organizations/{organizationId}` passou a realizar inativacao logica da organizacao
- a gestao de `Organizacao` agora permanece restrita a `ADMIN + INTERNAL` para criar, editar e inativar
- o backend passou a bloquear edicao de `Organizacao` `INACTIVE`
- o backend passou a bloquear inativacao de `Organizacao` quando ainda existem usuarios vinculados em status `INVITED` ou `ACTIVE`
- `Programas` novos tambem passaram a bloquear `ownerOrganizationId` e participantes com organizacao inativa
- o teste do portfolio foi ampliado para cobrir consulta por id, edicao, inativacao, bloqueio com usuarios vinculados e bloqueio de uso de organizacao inativa em novos programas
- o backend com esse pacote foi publicado no ECS/Fargate como `program-management-system:10` e validado com `GET /public/ping` no ALB

### 2026-03-19 - Ampliacao do layout compartilhado do frontend
- o `AppShell` compartilhado deixou de limitar hero e conteudo a `1180px`, passando a usar largura responsiva mais ampla nas telas principais
- o frontend passou a usar padding lateral proporcional a viewport, reduzindo bordas vazias em desktop sem comprometer o mobile
- a composicao da hero foi recalibrada para aproveitar melhor o espaco horizontal e deixar a copia principal menos comprimida
- `npm run lint` e `npm run build` foram executados com sucesso apos o ajuste visual

### 2026-03-19 - Gestao administrativa completa de organizacoes no frontend
- o workspace de portfolio passou a consumir `GET/POST/PUT/DELETE /api/portfolio/organizations` e `GET /api/portfolio/organizations/{organizationId}` para gerenciamento administrativo completo
- a UI agora lista `name`, `code`, `status` e `setupStatus`, com badges visuais e acoes por linha para editar e inativar organizacoes ativas
- o formulario de organizacao foi simplificado para trabalhar apenas com `name` e `code`, deixando `id` gerado no backend e mantendo a normalizacao de `code` na API
- a UX passou a confirmar explicitamente a inativacao, refazer a listagem apos mutacoes e traduzir erros comuns como codigo duplicado, usuarios ainda ativos/convidados e falta de permissao
- o fluxo de criacao de programas no frontend passou a considerar apenas organizacoes `ACTIVE` nos selects
- `npm run lint`, `npm run test -- --run` e `npm run build` foram executados com sucesso apos a entrega

### 2026-03-19 - Modulo dedicado para administracao de organizacoes
- a gestao administrativa de organizacoes foi separada do workspace de portfolio e ganhou a rota dedicada `/workspace/organizations`
- o portfolio passou a manter apenas o diretorio operacional de organizacoes e um atalho contextual para o modulo administrativo quando o usuario possui permissao
- a navegacao principal agora exibe `Organizacoes` apenas para usuarios `ADMIN` internos e a propria rota redireciona perfis sem permissao para `/workspace`
- o modulo dedicado reaproveita a camada de API ja existente, preserva badges de `status/setupStatus`, create, edit, inactivate e mensagens amigaveis de erro

### 2026-03-20 - Hierarquia de organizacoes por customer e visibilidade por subarvore
- foi adicionada a migration `V6__add_organization_hierarchy.sql` para persistir `tenant_type`, `parent_organization_id`, `customer_organization_id` e `hierarchy_level` em `organization`
- `Organizacao` externa passou a suportar cadastro hierarquico real com `Customer` na raiz, filhos ilimitados e metadados de arvore retornando no contrato HTTP
- `GET /api/portfolio/organizations` e as operacoes de portfolio agora respeitam o escopo visivel por subarvore, isolando customers entre si e impedindo visibilidade lateral entre irmaos
- `ADMIN` externo passou a poder criar e gerenciar filhos apenas dentro da propria subarvore; criacao de `Customer` raiz permaneceu restrita a `ADMIN` interno
- a inativacao de `Organizacao` passou a bloquear tambem filhos ativos e projetos ativos
- o modulo de `users` passou a respeitar a subarvore para `ADMIN` externo, mantendo `SUPPORT` externo restrito a propria organizacao e preservando `supportOverride` nas operacoes sensiveis de suporte
- a documentacao funcional da hierarquia foi registrada em `docs/organization-hierarchy.md`
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `79` testes passando

### 2026-03-20 - Fechamento do contrato backend para consumo do frontend
- `GET /api/portfolio/organizations` passou a aceitar filtros por `customerOrganizationId`, `parentOrganizationId` e `hierarchyLevel`, facilitando arvore, subarvore e navegacao incremental no frontend
- `GET /api/portfolio/programs` passou a aceitar filtro por `ownerOrganizationId`, permitindo separar o portfolio visivel por organizacao dona
- o diretorio funcional de `/api/portfolio/organizations` passou a operar apenas sobre organizacoes `EXTERNAL`; `internal-core` e demais estruturas `INTERNAL` nao aparecem mais na listagem nem podem ser usadas como owner de `Programa`
- os testes do portfolio foram ampliados para cobrir filtros hierarquicos, ocultacao de organizacao interna e filtro de programas por owner
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `81` testes passando

### 2026-03-20 - Alinhamento final de permissoes em `users` e `Organizacao`
- a `AuthorizationMatrix` passou a ser a fonte de verdade de papel para os modulos administrativos de `users` e `Organizacao`
- o modulo de `users` deixou de expor gestao administrativa para `MANAGER`; agora apenas `ADMIN` gerencia usuarios e `SUPPORT` atua em consulta/saneamento segundo as regras de suporte
- `SUPPORT` interno passou a poder consultar `users` cross-customer por `organizationId` sem `supportOverride`, mantendo `supportOverride + justification` para `reset-access`, `resend-invite` e `purge` quando cross-customer
- o modulo administrativo de `Organizacao` passou a exigir `ADMIN` ou `SUPPORT` para consulta e apenas `ADMIN` para create/edit/inactivate, preservando a subarvore como segunda camada de controle
- os testes de `users`, `portfolio` e `AuthorizationService` foram ampliados para cobrir `ADMIN` externo em descendentes, `SUPPORT` interno cross-customer e bloqueio de `MANAGER` na superficie administrativa

### 2026-03-20 - Fechamento das permissoes de portfolio por papel
- a `AuthorizationMatrix` passou a expor explicitamente o modulo `PORTFOLIO`, separando o portfolio dos modulos administrativos de `users` e `Organizacao`
- `ADMIN` ficou com governanca, gestao e execucao do portfolio; `MANAGER` ficou com gestao e execucao; `MEMBER` ficou apenas com execucao; `SUPPORT` e `AUDITOR` permaneceram somente leitura
- `POST /api/portfolio/programs` e `POST /api/portfolio/milestone-templates` ficaram restritos a `ADMIN`
- `POST /api/portfolio/programs/{programId}/projects`, `POST /api/portfolio/projects/{projectId}/products` e `POST /api/portfolio/programs/{programId}/open-issues` ficaram liberados para `ADMIN` e `MANAGER`
- `POST /api/portfolio/products/{productId}/items`, `POST /api/portfolio/items/{itemId}/deliverables`, upload/complete/delete de documento ficaram liberados para `ADMIN`, `MANAGER` e `MEMBER`
- `SUPPORT` interno passou a poder consultar portfolio cross-customer sem `supportOverride`, mantendo o modulo em leitura para suporte
- a documentacao funcional foi atualizada em `docs/organization-hierarchy.md`
- a suite completa `./mvnw.cmd test` foi reexecutada com sucesso, totalizando `90` testes passando

### 2026-03-20 - Frontend alinhado a hierarquia de `Organizacao` e permissoes do portfolio
- o frontend passou a esconder acoes de governanca, gestao e execucao conforme a matriz fechada do modulo `PORTFOLIO`, mantendo `SUPPORT` e `AUDITOR` em leitura
- o workspace de portfolio agora evita consultar `GET /api/portfolio/organizations` para `MANAGER` e `MEMBER`, impedindo `403` desnecessario no overview
- o frontend passou a consumir `GET /api/portfolio/programs` com `ownerOrganizationId`, separando visualmente os programas por organizacao dona
- o detalhe de programa passou a consumir `POST /api/portfolio/programs/{programId}/projects` e `DELETE /api/portfolio/deliverables/{deliverableId}/documents/{documentId}`
- o modulo `/workspace/organizations` agora explora a hierarquia com filtros por `status`, `setupStatus`, `customerOrganizationId`, `parentOrganizationId`, `hierarchyLevel` e `search`
- o formulario de organizacoes passou a aceitar `parentOrganizationId`, permitindo create root por `ADMIN` interno e create child na subarvore visivel de `ADMIN` externo
- a navegacao agora exibe `Organizacoes` para `ADMIN` e `SUPPORT`, preservando create/edit/inactivate apenas para `ADMIN`
- `npm run lint`, `npm run test` e `npm run build` foram executados com sucesso apos a implementacao

## Regra de atualizacao

Objetivo desta regra:
- manter este arquivo como memoria operacional completa do projeto, e nao apenas como log resumido de alteracoes
- permitir retomada de contexto em novas sessoes sem depender de memoria externa, arquivos paralelos ou historico informal
- garantir que frontend e backend leiam o mesmo retrato atualizado do produto, da arquitetura e do backlog
- para cada modulo importante do produto, manter uma secao dedicada com objetivo, modelo, endpoints, funcao de cada endpoint, regras implementadas e gaps relevantes; `users` passa a ser o primeiro modulo organizado nesse formato

Como atualizar em cada etapa:
- ao concluir cada etapa relevante, revisar o que mudou no produto, no contrato, na UX, na arquitetura, no backlog e na prioridade
- refletir a mudanca nas secoes vivas do documento, e nao apenas no `Historico resumido`
- registrar o que ficou pronto, o que ainda falta, o que mudou de direcao e qual passa a ser o novo ponto de retomada
- quando uma implementacao abrir novos gaps, riscos ou decisoes, adicionar isso explicitamente nas secoes apropriadas
- quando houver validacao tecnica, operacional ou funcional, registrar tambem o resultado da validacao e seu contexto

Nivel de detalhe esperado por entrega:
- contexto: o documento deve continuar explicando onde o projeto esta e por que a etapa atual importa
- especificacoes: o documento deve registrar rotas, fluxos, regras de negocio, contratos e comportamento esperado quando forem afetados
- historico: cada entrega relevante deve entrar no `Historico resumido` com data, objetivo e resultado
- estado atual: as secoes de status devem refletir o estado real apos a entrega, evitando texto desatualizado
- backlog: itens concluidos, refinados, despriorizados ou novos devem ser ajustados no backlog e na proxima sessao

Checklist minimo de manutencao por etapa:
- atualizar o retrato atual do frontend e/ou backend
- atualizar fluxos, rotas, APIs ou regras afetadas
- atualizar `O que esta pronto`
- atualizar `O que ainda falta`
- atualizar `Frontend` quando houver impacto na UI, navegacao, autenticacao cliente ou experiencia
- atualizar `Implementado` quando houver nova capacidade entregue
- atualizar `Decisoes fechadas` ou `Decisoes em aberto` quando a etapa resolver ou abrir pontos de definicao
- atualizar `Proxima sessao` com o novo ponto de retomada
- adicionar entrada correspondente em `Historico resumido`

Sempre atualizar este arquivo quando houver:
- nova feature implementada
- nova tela, rota ou fluxo relevante no frontend
- mudanca de arquitetura
- mudanca de infraestrutura
- mudanca de contrato de API ou integracao entre backend e frontend
- nova decisao importante
- nova decisao relevante de UX que impacte o produto
- alteracao de backlog ou prioridade

Campos obrigatorios a revisar em toda entrega:
- `Status executivo`
- `Frontend`
- `Implementado`
- `Decisoes fechadas`
- `Decisoes em aberto`
- `Proxima sessao`
- `Historico resumido`

Regra operacional daqui para frente:
- toda nova etapa conduzida nesta sessao deve considerar este arquivo como artefato obrigatorio de fechamento
- ao final de cada entrega, este arquivo deve sair atualizado, coerente e suficiente para uma retomada completa futura
