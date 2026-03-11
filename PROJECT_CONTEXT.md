# Project Context - ProgramManagementSystem

Ultima atualizacao: `2026-03-11`

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
- o operador atual nao possui permissao para `logs:DescribeLogStreams` nem `elasticloadbalancing:DescribeTargetHealth`
- o usuario operador `oryzem_admin` agora pode assumir a role `program-management-system-platform-admin-role`
- a role `program-management-system-platform-admin-role` concentra elevacao temporaria para IAM operacional e troubleshooting
- a leitura de CloudWatch Logs e de `DescribeTargetHealth` foi validada com sucesso via role assumida
- `logs:FilterLogEvents` ainda nao esta disponivel na role assumida, apesar de `DescribeLogStreams` e `GetLogEvents` estarem funcionais
- os logs do runtime confirmam conexao JDBC real com o RDS `program-management-system-db.cns8u4awye4v.sa-east-1.rds.amazonaws.com`
- ainda nao ha evidencia explicita em log provando o uso de Secrets Manager no bootstrap; isso segue pendente de telemetria mais direta
- o dominio do Cognito Hosted UI foi confirmado: `https://sa-east-1aa4i3temf.auth.sa-east-1.amazoncognito.com`
- o app client atual aceita fluxo `authorization_code` com PKCE e escopo `openid`
- o fluxo implicito com `response_type=token` retornou `unauthorized_client`
- a combinacao de escopos `openid email profile` retornou `invalid_scope` no app client atual
- Docker Desktop local esta operacional nesta maquina

Ponto de retomada oficial:
- validar endpoints autenticados com token Cognito real via ALB
- validar login real via Cognito Hosted UI com callback e logout coerentes
- validar autorizacao real por grupos/roles em endpoints de negocio
- consolidar um roteiro operacional reutilizavel para captura de JWT e teste autenticado
- decidir a estrategia final de exposicao externa com HTTPS/TLS e DNS definitivo
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
- task definition atual: `program-management-system:5`
- task role: `program-management-system-ecs-task-role`
- execution role: `program-management-system-ecs-execution-role`
- execution role ARN: `arn:aws:iam::439533253319:role/program-management-system-ecs-execution-role`
- task security group: `sg-0af8c0fc744a9ef99` (`program-management-system-ecs-tasks-sg`)
- task atual validada: `a2897ccffde54d35a2fbc9f1a5096f8f`
- ENI da task: `eni-04d37855bfd82fd0e`
- IP privado observado: `172.31.9.59`
- IP publico observado: `56.124.56.159`

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
- planejamento do dominio principal iniciado e estrutura conceitual V1 definida

Status do backend:
- operacional como OAuth2 Resource Server
- sem login por senha local
- sem sessao local
- autorizacao baseada em grupos do Cognito
- modulos `users`, `operations` e `reports` ativos
- perfil `rds` operacional
- `audit_log` persistente operacional
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
- observabilidade operacional parcial por falta de permissoes de leitura em logs e target health
- task definition `:5` implantada com health check de container em producao
- modelo IAM em 2 camadas estabelecido com elevacao temporaria via role assumivel
- observabilidade operacional basica validada via role assumida para Logs e Target Health

Leitura estrategica do momento:
- o projeto saiu de prova de conceito e infraestrutura inicial
- o backend ja opera na AWS e esta pronto para virar produto de verdade
- a prioridade executiva atual e fechar operacao minima, autenticacao real e borda externa definitiva antes de acelerar o core do SaaS
- o dominio principal ja tem uma espinha dorsal conceitual definida e pode evoluir de forma incremental sem quebrar a base tecnica atual

Macro proximos passos:
1. Fechar a operacao minima em desenvolvimento com observabilidade basica.
2. Validar autenticacao e autorizacao reais fim a fim com Cognito.
3. Definir a exposicao externa definitiva com HTTPS/TLS, DNS e topologia de rede mais madura.
4. Avancar o dominio principal do produto.
5. Endurecer a arquitetura para escala, governanca e operacao sustentavel.

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
- task definition `program-management-system:4` implantada com health check em `/actuator/health/liveness`
- task ECS rodando com health check de container e status `HEALTHY`
- politica versionada de leitura operacional preparada localmente para IAM
- deploy mais recente executado com sucesso no ECS/Fargate
- service atualizado para `program-management-system:5` e revalidado com task saudavel
- mapa conceitual V1 do dominio principal definido com raiz em `Programa`
- regra de negocio consolidada de que `tenant` representa uma empresa (`Organizacao`)
- colaboracao multiempresa definida via participacao de organizacoes em programas
- hierarquia conceitual definida para V1: `Programa -> Projeto -> Produto -> Item -> Entregavel`
- conceito inicial de issue macro definido como `OpenIssue` vinculado ao `Programa`
- entregavel V1 definido com tipos `DOCUMENTO` e `FORMULARIO`
- ciclo de vida inicial definido para `Programa`, `Projeto`, `Entregavel` e `OpenIssue`
- estrategia de `Milestone` refinada para uso de templates aplicados a `Projeto`
- decisao de datas consolidada: backend trabalha com data completa e a UI pode operar em visao semanal `WW/YY`

O que ainda falta:
- validar endpoints autenticados com Bearer token Cognito real via ALB
- fechar a estrategia final de HTTPS/TLS e DNS definitivo
- ampliar observabilidade do runtime AWS
- habilitar visibilidade operacional para logs e target health ao operador
- aplicar a politica de leitura operacional com um principal autorizado em IAM
- transformar o mapa conceitual do dominio principal em glossario, ownership, campos e regras formais de negocio
- aprofundar multi-tenancy e auditoria de negocio

## Snapshot tecnico

Stack:
- linguagem: Java
- build: Maven
- empacotamento: Jar
- framework: Spring Boot 4
- seguranca: Spring Security 7
- banco: PostgreSQL
- auth frontend V1: Amazon Cognito Hosted UI

Java:
- `java.version`: `21`
- execucao local validada em: `JDK 23`

Package real:
- `com.oryzem.programmanagementsystem`

Configuracao do backend:
- profile AWS atual: `rds`
- envs principais:
  - `SPRING_PROFILES_ACTIVE=rds`
  - `DB_SECRET_ID=program-management-system/rds/master`
  - `AWS_REGION=sa-east-1`
  - `DB_SSL_MODE=require`

CORS e callbacks:
- callback local: `http://localhost:3000/callback`
- callback producao: `https://oryzem.com/callback`
- logout local: `http://localhost:3000/logout`
- logout producao: `https://oryzem.com/logout`

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

Persistencia:
- Flyway com PostgreSQL `17.6`
- migrations versionadas
- JPA para `users`, `operations` e `audit_log`
- seed controlado por configuracao
- futuro dominio principal ainda em fase de definicao conceitual, sem implementacao persistente nesta sessao

AWS runtime:
- RDS privado provisionado e validado
- leitura de credenciais via Secrets Manager no bootstrap
- ECS/Fargate operacional
- ECR operacional
- ALB operacional

Qualidade:
- suite `./mvnw.cmd test` validada repetidamente durante a implantacao
- testes cobrindo autenticacao, autorizacao, CORS, `users`, `operations` e `reports`

## Decisoes fechadas

1. O backend nao implementa login por senha.
2. O backend apenas valida JWT emitido pelo Cognito.
3. A arquitetura de autenticacao e stateless.
4. A autorizacao inicial e baseada em grupos do Cognito convertidos para roles Spring.
5. O arquivo `PROJECT_CONTEXT.md` e a memoria operacional entre sessoes.
6. Os grupos funcionais V1 sao `ADMIN`, `MANAGER`, `MEMBER`, `SUPPORT` e `AUDITOR`.
7. O contexto organizacional usa `tenant_type` e `tenant_id`.
8. `tenant_type` e `tenant_id` podem existir como claims auxiliares, mas nao sao a fonte primaria de autorizacao.
9. A validacao final de tenant e permissoes acontece na camada de aplicacao.
10. A V1 de autorizacao fica centralizada em codigo.
11. Amazon Verified Permissions nao entra na V1.
12. A V1 usa banco unico com tabelas compartilhadas e isolamento logico por `tenant_id`.
13. O banco inicial e PostgreSQL na AWS, preferencialmente RDS.
14. Os modulos prioritarios da V1 sao `users`, `operations` e `reports`.
15. A persistencia inicial usa JPA + Flyway.
16. A auditoria V1 e persistida em `audit_log`.
17. A hospedagem preferencial do backend e AWS ECS/Fargate.
18. O RDS fica privado e o acesso e feito a partir da camada de aplicacao.
19. O secret primario do banco fica no AWS Secrets Manager.
20. A primeira exposicao externa funcional do backend pode usar ALB simples antes do desenho final de rede e HTTPS.
21. O dominio principal do produto sera centrado em `Programa`.
22. `tenant` representa uma empresa e sera modelado por `Organizacao`.
23. Um `Programa` pode ser compartilhado entre multiplas organizacoes.
24. `Cliente`, `Fornecedor`, `Interna` e `Parceira` serao papeis da organizacao dentro de `ParticipacaoNoPrograma`, nao entidades separadas nesta fase.
25. Todo `Programa` deve possuir ao menos `1` `Projeto`.
26. A hierarquia conceitual V1 do dominio principal sera `Programa -> Projeto -> Produto -> Item -> Entregavel`.
27. O controle inicial de problemas macro sera feito por `OpenIssue` vinculado ao `Programa`.
28. `Risco` e `Issue` nao serao separados na V1; o foco inicial sera `OpenIssue`.
29. `Entregavel` tera inicialmente dois tipos: `DOCUMENTO` e `FORMULARIO`.
30. `Milestone` deixara de ser apenas cadastro solto e passara a usar template aplicado a `Projeto`.
31. O backend persiste datas completas; a visao semanal `WW/YY` fica na UI.
32. O ciclo de vida inicial das entidades principais sera mantido simples: `Programa`, `Projeto`, `Entregavel` e `OpenIssue` com status definidos em nivel conceitual.

## Decisoes em aberto

1. Quais limites operacionais e de aprovacao serao exigidos para `impersonation`.
2. Como sera a estrategia de provisionamento de banco para ambientes futuros.
3. Quando o arquivo local legado `C:\Users\vande\.aws\program-management-system-db.password.txt` sera removido do ambiente do operador.
4. Qual sera o desenho final de exposicao externa: HTTP temporario vs. HTTPS/TLS com DNS definitivo.
5. Quais permissoes de observabilidade serao mantidas para logs, tasks e rede no runtime AWS.
6. Quando o service deixara de usar `assignPublicIp=ENABLED` em favor de uma topologia mais madura.
7. Qual sera a estrategia para health check da task ECS alem do health check atual do target group.
8. Se a role operacional tambem concentrara futuras acoes sensiveis de IAM ou se havera segmentacao adicional por escopo.
9. Quais campos minimos obrigatorios serao fixados para cada entidade do dominio principal na V1.
10. Como sera o desenho inicial de permissoes de negocio por papel em `ParticipacaoNoPrograma`.
11. Se `OpenIssue` permanecera apenas em nivel de `Programa` por toda a V1 ou se depois podera descer para `Projeto`, `Produto`, `Item` ou `Entregavel`.
12. Como sera estruturado o modelo de formulario dos `Entregaveis` do tipo `FORMULARIO`.
13. Como os templates de milestone serao classificados, versionados e selecionados por tipo de projeto.

## Proxima sessao

Checklist recomendado:
- usar a role `program-management-system-platform-admin-role` como caminho padrao de elevacao operacional
- validar login real via Cognito Hosted UI e uso de token JWT real via ALB
- montar URLs com `scripts/new-cognito-hosted-ui-urls.ps1`
- validar o token real com `scripts/test-cognito-authenticated-flow.ps1`
- validar autorizacao por grupos/roles em endpoints de negocio
- definir a estrategia de HTTPS/TLS e DNS final
- revisar o nivel de exposicao publica, `assignPublicIp=ENABLED` e a topologia de rede do service
- manter no backlog nao critico a telemetria explicita do Secrets Manager e o refinamento de observabilidade
- iniciar o proximo recorte do dominio principal apos fechar operacao, autenticacao real e borda externa
- consolidar o glossario funcional V1 do dominio principal
- definir campos minimos das entidades `Organizacao`, `Programa`, `Projeto`, `Produto`, `Item`, `Entregavel`, `OpenIssue` e estruturas de milestone template
- detalhar regras de data, ownership e permissoes de negocio por papel dentro do programa

Se o foco for autenticacao:
- definir claims obrigatorias
- registrar permissoes por perfil
- desenhar a integracao frontend + callback + logout

Se o foco for backend de negocio:
- consolidar o modelo conceitual do dominio antes de criar entidades
- definir glossario, ownership e campos minimos
- definir contratos REST iniciais somente apos fechar a linguagem do dominio
- aplicar `AuthorizationService` nos novos contratos
- reutilizar a base JPA + Flyway existente

## Backlog priorizado

1. Validacao autenticada fim a fim com Cognito real: Hosted UI, JWT real, ALB e autorizacao por grupos/roles.
2. Exposicao externa definitiva: HTTPS/TLS, certificado, DNS final e maturidade de rede.
3. Modelagem do dominio principal: `organizacao`, `projeto`, `cliente`, `fornecedor`, `milestone`, `risco` e `action item`.
3. Modelagem do dominio principal centrado em `Programa`: `organizacao`, `participacao_no_programa`, `projeto`, `produto`, `item`, `entregavel`, `milestone_template` e `open_issue`.
4. Evolucao da persistencia e do modelo de dominio.
5. Estrategia de multi-tenancy com isolamento por `tenant_id`.
6. Auditoria e trilha de alteracoes de negocio.
7. Evolucao de `relatorios`.
8. Endurecimento de arquitetura e operacao: IAM, observabilidade, pipeline, ambientes e revisao de rede.
9. Observabilidade complementar em AWS: telemetria explicita de Secrets Manager, refinamento de logs e `FilterLogEvents`.
10. Gestao centralizada de segredos.

## Direcao imediata

Proximo passo oficial:
- validar endpoints autenticados com token Cognito real via ALB
- validar login real via Hosted UI e autorizacao por grupos do Cognito
- usar `scripts/new-cognito-hosted-ui-urls.ps1` para montar URLs de login, logout e PKCE
- usar `scripts/test-cognito-authenticated-flow.ps1` para validar o JWT real contra o ALB
- manter o RDS privado com origem autorizada no SG das tasks ECS
- evoluir da exposicao HTTP inicial para a estrategia final com HTTPS/TLS
- manter no backlog nao critico a telemetria explicita de Secrets Manager e o refinamento de observabilidade

## Plano sugerido da proxima sprint

Premissa de execucao:
- concentrar a sprint em fechar operacao minima, autenticacao real e direcao de borda externa
- nao abrir frentes grandes de dominio antes de concluir esses bloqueadores estruturais

Objetivo da sprint:
- sair de backend funcional em AWS para backend operavel, autenticado fim a fim e com caminho definido para exposicao definitiva

Entregas da sprint:
1. Observabilidade minima operacional habilitada.
2. Fluxo autenticado fim a fim com Cognito validado em ambiente AWS.
3. Decisao tecnica documentada para HTTPS/TLS, DNS e maturidade de rede.

Ordem recomendada:
1. Liberar leitura operacional de CloudWatch Logs e target health.
2. Confirmar por evidencias de runtime o uso de Secrets Manager e RDS.
3. Executar login real via Hosted UI e capturar um JWT valido.
4. Validar endpoints autenticados e autorizacao por grupos via ALB.
5. Fechar a proposta de exposicao externa definitiva.

Pacote 1 - Observabilidade minima:
- Entregaveis:
  - politica de leitura operacional aplicada ao principal correto
  - leitura validada de CloudWatch Logs
  - leitura validada de `DescribeTargetHealth`
  - evidencias do runtime mostrando bootstrap com secret e conexao ao banco
- Criterios de aceite:
  - operador consegue consultar streams e eventos do log group `/ecs/program-management-system`
  - operador consegue consultar health do target group `program-management-system-alb-tg`
  - existe evidencia objetiva de que a task le o secret gerenciado e conecta no RDS esperado
- Riscos e dependencias:
  - dependencia de um principal com permissao de IAM para `PutUserPolicy` ou politica equivalente

Pacote 2 - Autenticacao real com Cognito:
- Entregaveis:
  - fluxo de login via Hosted UI executado com sucesso
  - token JWT real capturado e validado contra o backend via ALB
  - matriz basica de autorizacao validada para ao menos um perfil autorizado e um nao autorizado
- Criterios de aceite:
  - `GET /api/auth/me` responde `200` com token valido
  - endpoint protegido de negocio responde `200` para grupo permitido
  - endpoint protegido de negocio responde `403` para grupo sem permissao adequada
  - logout/callback do fluxo nao apresentam inconsistencias basicas
- Riscos e dependencias:
  - necessidade de usuario real no Cognito com grupos configurados para teste
  - possivel ajuste de claims, callback ou mapeamento de grupos no frontend/backend

Pacote 3 - Exposicao externa definitiva:
- Entregaveis:
  - decisao tecnica registrada para HTTPS/TLS
  - definicao de certificado e DNS alvo
  - direcao de rede registrada para manter ou remover `assignPublicIp=ENABLED`
- Criterios de aceite:
  - existe uma proposta objetiva aprovada para listener HTTPS, certificado e dominio final
  - existe definicao inicial sobre topologia alvo do service e proximos passos de rede
  - backlog tecnico da migracao de borda esta quebrado em etapas executaveis
- Riscos e dependencias:
  - dependencia de dominio, certificado e possivel ajuste de rota/DNS fora do repositorio

Definicao de pronto da sprint:
- o operador deixa de depender de inferencia para diagnosticar runtime
- o backend prova autenticacao e autorizacao reais com Cognito via ALB
- a equipe encerra a sprint com um desenho claro para sair do HTTP temporario

Proxima fila apos a sprint:
1. abrir o recorte de dominio principal com `organizacao`, `projeto`, `cliente` e `fornecedor`
2. seguir para `milestone`, `risco` e `action item`
3. retomar endurecimento arquitetural e operacional com base no desenho validado

## Como validar localmente

1. Configurar variaveis de ambiente do Cognito se necessario.
2. Subir a aplicacao com Maven.
3. Validar `GET /public/ping`.
4. Validar `GET /public/auth/config`.
5. Validar `GET /api/ping` sem token e confirmar `401`.
6. Validar `GET /api/auth/me` com Bearer token valido.
7. Validar `GET /api/admin/ping` com token de administrador.

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
- task ECS em `program-management-system:4` responde com `healthStatus=HEALTHY`

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
- foram propostos ciclos de vida iniciais para `Programa`, `Projeto`, `Entregavel` e `OpenIssue`, a serem refinados junto com permissoes e campos minimos

## Regra de atualizacao

Sempre atualizar este arquivo quando houver:
- nova feature implementada
- mudanca de arquitetura
- mudanca de infraestrutura
- nova decisao importante
- alteracao de backlog ou prioridade

Campos obrigatorios a revisar em toda entrega:
- `Status executivo`
- `Implementado`
- `Decisoes fechadas`
- `Decisoes em aberto`
- `Proxima sessao`
- `Historico resumido`
