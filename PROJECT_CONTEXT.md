# Shared Project Context - ProgramManagementSystem

Ultima atualizacao: `2026-03-14`

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
- a ultima execucao conhecida de `./mvnw.cmd test` terminou com `48` testes passando
- ainda nao houve validacao operacional do fluxo de documentos contra um bucket S3 real no runtime AWS
- a primeira UI basica do portfolio ja foi implementada no frontend local consumindo o contrato real de `/api/portfolio`
- o frontend agora consegue listar e criar `Organizacao`, criar `MilestoneTemplate`, criar `Programa` com `Projeto` inicial, navegar no detalhe do programa e seguir o fluxo estrutural ate documento em modo `stub`

Ponto de retomada oficial:
- validar em uso real a UI basica sobre `/api/portfolio` para usar o backend ja implementado como contrato real de produto
- refinar UX, mensagens de erro, nomenclatura e pequenos gaps de contrato descobertos no fluxo estrutural ponta a ponta `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento`
- validar em paralelo a trilha de governanca `Programa -> OpenIssue`
- evoluir o modulo `/api/portfolio` com edicao, update de status, ownership e permissoes de negocio
- definir a primeira estrutura persistivel para os entregaveis do tipo `FORM`
- preparar e validar o fluxo de documentos com bucket S3 real no ambiente AWS
- manter em paralelo a validacao de autenticacao real via Cognito Hosted UI + ALB e a definicao da borda externa final com HTTPS/TLS
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
- task definition atual: `program-management-system:7`
- task role: `program-management-system-ecs-task-role`
- execution role: `program-management-system-ecs-execution-role`
- execution role ARN: `arn:aws:iam::439533253319:role/program-management-system-ecs-execution-role`
- task security group: `sg-0af8c0fc744a9ef99` (`program-management-system-ecs-tasks-sg`)
- task atual validada: `afbc2008a7dd4d7493e24d4c7f4c57d5`
- ENI da task: `eni-05101ed5fcbdd779d`
- IP privado observado: `172.31.13.90`
- IP publico observado: `18.229.126.207`

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
- `scripts/attach-observability-read-policy-to-user.ps1`
- `scripts/attach-observability-read-policy-to-role.ps1`
- `scripts/new-cognito-hosted-ui-urls.ps1`
- `scripts/run-app-with-rds.ps1`
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
- `infra/ecs/task-definition.template.json`
- `infra/ecs/service-definition.template.json`

Artefatos recentes de dominio e storage:
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioManagementController.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioManagementService.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioEntities.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioEnums.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioRequests.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioResponses.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioDocumentStorageConfig.java`
- `src/main/java/com/oryzem/programmanagementsystem/portfolio/PortfolioResetService.java`
- `src/main/resources/db/migration/V3__create_portfolio_domain.sql`
- `src/main/resources/db/migration/V4__create_deliverable_document.sql`
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
- projeto pronto para validacao integrada do portfolio com uso real da UI

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

Status do frontend:
- a stack base do frontend esta definida com `React + JavaScript + Vite`
- o frontend local foi alinhado para rodar em `http://localhost:3000`
- as rotas ativas agora incluem `/`, `/callback`, `/logout`, `/workspace`, `/workspace/programs/:programId` e `/workspace/session`
- a autenticacao base usa `react-oidc-context` com Cognito Hosted UI via `authorization_code + PKCE`
- a base de dados no cliente usa `@tanstack/react-query` e wrapper HTTP com `fetch`
- o login real via localhost foi validado com sucesso
- `GET /api/auth/me` respondeu `200` com token Cognito real via frontend local
- a autorizacao real para usuario no grupo `ADMIN` foi validada em `GET /api/admin/ping` e `GET /api/authz/check`
- o frontend agora possui um workspace de portfolio consumindo `organizations`, `milestone-templates`, `programs`, detalhe de programa, `products`, `items`, `deliverables`, `documents` e `open-issues`
- o diagnostico de sessao, claims e validador de endpoints protegidos foi preservado em `/workspace/session`
- a inicializacao do `PortfolioWorkspace` foi estabilizada apos correcao de ordem de inicializacao de estado no calculo de `ownerOrganizationId`
- os gaps atuais mais relevantes para integracao sao logout real, enriquecimento da identidade retornada, investigacao de `principal=null` nos endpoints de ping e refinamento de UX/mensagens do novo fluxo de portfolio

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
- fluxo de criacao ponta a ponta implementado na UI para `Produto`, `Item`, `Entregavel`, `OpenIssue` e documento em modo `stub`
- rota tecnica `/workspace/session` preserva o diagnostico de sessao, claims e endpoints protegidos
- `lint`, `test` e `build` do frontend validados com sucesso

O que ainda falta:
- validar endpoints autenticados com Bearer token Cognito real via ALB
- fechar a estrategia final de HTTPS/TLS e DNS definitivo
- ampliar observabilidade do runtime AWS
- habilitar visibilidade operacional para logs e target health ao operador sem friccao manual
- definir e provisionar o bucket S3 definitivo para documentos do portfolio
- validar o fluxo de documentos com provider `s3` em ambiente AWS real
- validar em uso real a UI basica para navegar, criar e ler o portfolio sem ajustes manuais fora da interface
- refinar mensagens de erro, estados vazios, feedback de sucesso e ergonomia da navegacao no workspace do portfolio
- evoluir o modulo de portfolio com operacoes de edicao, update de status e regras de transicao
- estruturar os entregaveis do tipo `FORM`
- definir ownership e permissoes de negocio por papel dentro de `ParticipacaoNoPrograma`
- reforcar isolamento logico por tenant no novo modulo de portfolio
- aprofundar auditoria de negocio, versionamento e retencao documental
- validar logout real do frontend com Hosted UI
- decidir como a UI representara identidade exibivel, roles, grupos e contexto de tenant
- entender por que `principal` esta vindo `null` em `GET /api/ping` e `GET /api/admin/ping`
- decidir se `/api/auth/me` deve enriquecer `username` e outros dados uteis para a interface
- evoluir o frontend da fundacao de autenticacao para a UI basica do portfolio

## Frontend

Status atual:
- o frontend esta sendo desenvolvido em `C:\Users\vande\Oryzem\PMS Frontend`
- a fundacao tecnica da aplicacao ja foi validada com autenticacao real em localhost
- a primeira UI real do portfolio foi implementada consumindo o backend em AWS com token Cognito real
- o workspace agora foi dividido entre fluxo de produto e diagnostico tecnico para preservar troubleshooting sem bloquear a evolucao da UI

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
- `GET/POST /api/portfolio/milestone-templates` consumido no frontend
- `GET/POST /api/portfolio/programs` consumido no frontend
- `GET /api/portfolio/programs/{programId}` consumido no frontend
- `POST /api/portfolio/projects/{projectId}/products`, `POST /api/portfolio/products/{productId}/items`, `POST /api/portfolio/items/{itemId}/deliverables` e `POST /api/portfolio/programs/{programId}/open-issues` consumidos no frontend
- fluxo de documento via `upload-url`, `complete` e `download-url` exercitado na UI em modo `stub`

Gaps atuais e pontos de atencao:
- validar logout real com Hosted UI
- entender por que `principal` vem `null` em respostas atuais de ping
- decidir se `/api/auth/me` deve enriquecer `username`, email, roles, groups e contexto de tenant para a UI
- definir tratamento refinado de `401` e `403`
- refinar a UX do workspace do portfolio com feedback mais claro de erros e sucesso
- decidir a melhor representacao visual para owner organization, participantes e breadcrumbs do portfolio

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
- `GET/POST /api/portfolio/organizations`
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

Persistencia:
- Flyway com PostgreSQL `17.6`
- migrations versionadas
- JPA para `users`, `operations` e `audit_log`
- seed controlado por configuracao
- dominio principal inicial agora possui migration `V3__create_portfolio_domain.sql`
- persistencia inicial do portfolio implementada para `organization`, `program_record`, `program_participation`, `project_record`, `product_record`, `item_record`, `deliverable`, `milestone_template`, `milestone_template_item`, `project_milestone` e `open_issue`
- migration `V4__create_deliverable_document.sql` adicionada para metadados de documentos de entregavel

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
- formularios ativos para criacao de `Organizacao`, `MilestoneTemplate` e `Programa` com `Projeto` inicial
- detalhe de programa renderiza participantes, milestones, produtos, itens, entregaveis, documentos e `OpenIssue`
- a UI permite criar `Produto`, `Item`, `Entregavel`, `OpenIssue` e registrar documento via provider `stub`
- o workspace tecnico anterior foi preservado em `/workspace/session`
- a inicializacao do workspace de portfolio foi corrigida apos um `ReferenceError` causado por acesso antecipado a estado do formulario de programa

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
- ultima execucao conhecida: `48` testes passando

## Mapa oficial do dominio V1

Camada de colaboracao entre empresas:
- `Organizacao` -> empresa/tenant do sistema
  - campos minimos conceituais: `id`, `nome`, `codigo`, `status`
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
11. Quais operacoes do modulo de portfolio serao somente leitura, criacao, edicao, mudanca de status ou aprovacao para cada papel de negocio.
12. Quais transicoes de status terao validacao estrita no backend para `Programa`, `Projeto`, `Entregavel`, `ProjetoMilestone` e `OpenIssue`.
13. Se `OpenIssue` permanecera apenas em nivel de `Programa` por toda a V1 ou se depois podera descer para `Projeto`, `Produto`, `Item` ou `Entregavel`.
14. Como os templates de milestone serao classificados, versionados e selecionados por tipo de projeto.
15. Como sera a estrategia de versionamento, exclusao fisica, retencao e auditoria de documentos em S3.
16. Como a UI vai representar `WW/YY` e converter isso de forma consistente sem contaminar a persistencia canonica por data completa.
17. Como o isolamento por tenant sera reforcado no modulo de portfolio em consultas, listagens e operacoes de alteracao.
18. Se a aprovacao futura de entregavel ocorrera no nivel do `Entregavel`, do `DeliverableDocument` ou em ambos.
19. Como serao paginacao, filtros e busca das listagens de portfolio quando o volume crescer.

## Proxima sessao

Checklist recomendado:
- validar com uso real a UI basica de `Organizacao`, `MilestoneTemplate`, `Programa` e detalhe de `Programa`
- revisar mensagens de erro, estados vazios e feedbacks de sucesso do workspace do portfolio
- validar no fluxo da UI a criacao ponta a ponta `Programa -> Projeto -> Produto -> Item -> Entregavel`
- testar na UI o fluxo de documento com provider `stub` em mais de um cenario de uso
- decidir o primeiro recorte de edicao e mudanca de status no backend do portfolio
- definir o desenho inicial do `FORM` antes de implementar respostas mais ricas
- comecar a matriz de permissao por papel dentro de `ParticipacaoNoPrograma`
- definir bucket, prefixo e politicas minimas para rodar documentos com `s3` no ambiente AWS
- manter no radar a validacao real com Cognito via ALB e a estrategia final de HTTPS/TLS
- manter no backlog operacional a telemetria explicita do Secrets Manager e o refinamento de observabilidade

Se o foco for UI basica:
- validar e refinar `GET/POST /api/portfolio/organizations`
- validar e refinar `GET/POST /api/portfolio/milestone-templates`
- validar e refinar `GET/POST /api/portfolio/programs`
- validar e refinar `GET /api/portfolio/programs/{programId}`
- amadurecer o fluxo de documentos por URL assinada do entregavel `DOCUMENT`

Se o foco for backend de negocio:
- implementar update e mudanca de status para `Programa`, `Projeto`, `Entregavel` e `OpenIssue`
- definir ownership por organizacao/usuario nas entidades criticas
- desenhar a estrutura de perguntas e respostas do `FORM`
- aplicar `AuthorizationService` nas operacoes novas do portfolio
- adicionar testes para transicoes, regras de negocio e cenario multi-tenant

Se o foco for operacao AWS:
- validar token Cognito real via ALB
- preparar bucket S3 real e validar upload/download assinado em runtime `rds`
- definir listener HTTPS, certificado e DNS final
- revisar a necessidade de manter `assignPublicIp=ENABLED`

## Backlog priorizado

1. UI basica do portfolio consumindo o backend ja implementado para validar o dominio com feedback rapido.
2. Evolucao do modulo de portfolio: edicao, mudanca de status, ownership e permissoes por papel.
3. Estruturacao dos entregaveis do tipo `FORM`.
4. Operacionalizacao de documentos com bucket S3 real, IAM minimo, validacoes de arquivo e politicas basicas.
5. Validacao autenticada fim a fim com Cognito real: Hosted UI, JWT real, ALB e autorizacao por grupos/roles.
6. Exposicao externa definitiva: HTTPS/TLS, certificado, DNS final e maturidade de rede.
7. Estrategia de multi-tenancy no portfolio com isolamento logico consistente por `tenant_id`/organizacao.
8. Auditoria e trilha de alteracoes de negocio para o novo dominio.
9. Evolucao de `relatorios` orientados ao novo portfolio.
10. Endurecimento de arquitetura e operacao: IAM, observabilidade, pipeline, ambientes e revisao de rede.
11. Gestao centralizada de segredos e eliminacao de artefatos locais legados.

## Direcao imediata

Proximo passo oficial:
- iniciar a UI basica sobre `/api/portfolio`
- validar o fluxo ponta a ponta `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel`
- exercitar o fluxo de documentos via provider `stub`
- escolher o primeiro pacote de evolucao do backend: edicao/status ou `FORM`
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

## Regra de atualizacao

Objetivo desta regra:
- manter este arquivo como memoria operacional completa do projeto, e nao apenas como log resumido de alteracoes
- permitir retomada de contexto em novas sessoes sem depender de memoria externa, arquivos paralelos ou historico informal
- garantir que frontend e backend leiam o mesmo retrato atualizado do produto, da arquitetura e do backlog

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
