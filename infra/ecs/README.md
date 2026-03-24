# ECS/Fargate First Deploy

Objetivo desta primeira entrega:
- subir a aplicacao no ECS/Fargate
- consumir o secret `program-management-system/rds/master`
- conectar no RDS privado

Escopo intencionalmente fora desta leva:
- ALB sofisticado
- autoscaling completo
- subnets privadas + NAT + endpoints
- pipeline CI/CD completo

Recursos esperados:
- task role: `program-management-system-ecs-task-role`
- execution role: `program-management-system-ecs-execution-role`
- task security group: `sg-0af8c0fc744a9ef99`
- target group ALB: `arn:aws:elasticloadbalancing:sa-east-1:439533253319:targetgroup/program-management-system-alb-tg/1425d73086a3393d`
- repositorio ECR: `oryzem-backend-dev`
- URI ECR: `439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev`
- VPC: `vpc-08aca9416504a5a9f`
- subnets:
  - `subnet-0200b652bed2d069a`
  - `subnet-0be1f1a93618c659c`
  - `subnet-01601f5f0d452d7da`

Configuracao minima da aplicacao no container:
- `SPRING_PROFILES_ACTIVE=rds`
- `DB_SECRET_ID=program-management-system/rds/master`
- `AWS_REGION=sa-east-1`
- `DB_SSL_MODE=require`
- `APP_PORTFOLIO_DOCUMENTS_PROVIDER=s3`
- `APP_PORTFOLIO_DOCUMENTS_BUCKET_NAME=program-management-system-documents-dev-439533253319-sa-east-1`
- `APP_PORTFOLIO_DOCUMENTS_KEY_PREFIX=portfolio`

Checklist minimo para documentos em S3 real:
- criar/confirmar o bucket `program-management-system-documents-dev-439533253319-sa-east-1`
- aplicar `Block Public Access`, `BucketOwnerEnforced` e encryption `AES256`
- aplicar CORS para `PUT/GET/HEAD` a partir de `http://localhost:3000`, `http://localhost:5173` e `https://oryzem.com`
- anexar a policy `scripts/grant-document-storage-policy.json` na task role `program-management-system-ecs-task-role`
- reciclar as tasks do ECS apos qualquer ajuste de policy IAM

Bootstrap opcional do primeiro acesso administrativo:
- o backend agora garante `internal-core` mesmo quando `APP_BOOTSTRAP_SEED_DATA=false`
- para criar o primeiro `ADMIN INTERNAL` de forma controlada, o container aceita:
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_ENABLED`
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_EMAIL`
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_DISPLAY_NAME`
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_OTHER_INTERNAL_USERS` (opcional, saneamento temporario de usuarios internos legados)
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_PASSWORD` (opcional, define senha permanente e suprime o convite)
  - `APP_BOOTSTRAP_INTERNAL_ADMIN_TEMPORARY_PASSWORD` (opcional)
- comportamento:
  - so executa quando `APP_BOOTSTRAP_INTERNAL_ADMIN_ENABLED=true`
  - garante/reconcilia o usuario configurado por e-mail, mesmo quando ele estiver ausente apenas no banco local ou apenas no Cognito
  - salva o usuario interno bootstrapado como `ACTIVE`
  - garante os grupos `ADMIN`, `SUPPORT` e `AUDITOR` para o usuario interno de emergencia
  - se `APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_OTHER_INTERNAL_USERS=true`, remove do banco e tenta remover do Cognito outros usuarios internos de `internal-core` diferentes do e-mail preservado
  - se `APP_BOOTSTRAP_INTERNAL_ADMIN_PASSWORD` estiver preenchido, cria/atualiza o usuario com senha permanente no Cognito
  - recomendacao operacional: usar a flag apenas no deploy de bootstrap inicial e depois voltar para `false`

Complemento operacional de autenticacao:
- o backend depende do Cognito para JWT e grupos
- o fluxo real de tenant no `access_token` agora depende tambem da Lambda versionada em `infra/cognito/pre-token-generation`
- o primeiro login de usuarios `INVITED` no backend agora tambem depende dessa Lambda publicar `username` e `email` no `access_token`, para a reconciliacao local do modulo de `users`
- a task role do backend tambem precisa manter permissoes administrativas de usuario no Cognito, incluindo `AdminGetUser` e `AdminDeleteUser`, porque os saneamentos excepcionais de `users` e `portfolio` agora podem validar/remover identidades durante `purge`
- artefatos relevantes:
  - `infra/cognito/pre-token-generation/index.mjs`
  - `infra/cognito/pre-token-generation/deploy.ps1`
  - `infra/cognito/pre-token-generation/README.md`
- antes de validar o frontend contra o ambiente AWS, confirmar que o User Pool continua apontando para a Lambda `program-management-system-cognito-pre-token` com `PreTokenGenerationConfig.LambdaVersion=V2_0`

Checklist operacional adicional para `users`:
- confirmar que a task role `program-management-system-ecs-task-role` inclui `AdminCreateUser`, `AdminUpdateUserAttributes`, `AdminAddUserToGroup`, `AdminRemoveUserFromGroup`, `AdminResetUserPassword`, `AdminSetUserPassword`, `AdminDisableUser`, `AdminGetUser` e `AdminDeleteUser`
- se houver ajuste de policy IAM da task role, executar `force-new-deployment` no service ECS para reciclar as tasks e renovar as credenciais efetivas
- se houver ajuste na Lambda de `Pre Token Generation`, fazer logout/login no frontend antes de retestar o fluxo autenticado
- se `POST /api/users/{userId}/purge` falhar, verificar primeiro CloudWatch para distinguir rota ausente, IAM faltante no Cognito ou regra de negocio

Checklist operacional adicional para `documents`:
- garantir que a task role tenha `s3:GetObject`, `s3:PutObject` e `s3:DeleteObject` no bucket de documentos
- se o upload via browser falhar antes de chegar no backend, revisar o CORS do bucket
- se o `complete` falhar, verificar se o objeto realmente foi enviado ao bucket e se a task role consegue executar `HeadObject`
- se a task role receber policy nova, executar `force-new-deployment` no ECS para renovar as credenciais

Observacoes sobre os artefatos versionados:
- `infra/ecs/service-definition.template.json` agora inclui o `targetGroupArn` do ALB, `containerName`, `containerPort` e `healthCheckGracePeriodSeconds=120`, para que um `create-service` reflita melhor o runtime atual
- `scripts/deploy-to-ecs-fargate.ps1` agora espera o service atingir `steady state` antes de retornar e aceita `-ForceNewDeployment` para reciclar tasks mesmo quando o ajuste relevante nao muda a imagem

Operacao economica de ambiente de dev:
- `scripts/stop-dev-aws-environment.ps1` escala o ECS service para `desiredCount=0` e solicita parada do RDS, preservando o ALB de forma intencional para evitar risco operacional de delecao acidental
- `scripts/start-dev-aws-environment.ps1` religa o RDS e volta o ECS service para `desiredCount=1`, esperando o service estabilizar
- `scripts/status-dev-aws-environment.ps1` consulta rapidamente ECS, task atual, RDS e health do endpoint publico
- atalhos Windows na raiz do projeto:
  - `dev-down.cmd`
  - `dev-up.cmd`
  - `dev-status.cmd`
- uso sugerido para parar:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev-aws-environment.ps1 -WaitForRdsStopped`
- uso sugerido para subir:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-aws-environment.ps1 -WaitForRdsAvailable`
- uso sugerido para status:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\status-dev-aws-environment.ps1`
- exemplos equivalentes com atalhos:
  - `dev-down.cmd -WaitForRdsStopped`
  - `dev-up.cmd -WaitForRdsAvailable`
  - `dev-status.cmd`
- observacao importante:
  - o ALB continua gerando custo mesmo com ECS e RDS parados, porque este fluxo preserva explicitamente o load balancer

Observacao de rede para esta primeira subida:
- como as subnets atuais sao publicas, a primeira task/service pode usar `assignPublicIp=ENABLED`
- quando a arquitetura amadurecer, revisar a topologia para subnets privadas

Sequencia exata sugerida:
1. Criar a execution role:
   `powershell -ExecutionPolicy Bypass -File .\scripts\create-ecs-execution-role.ps1`
2. Garantir o repositorio ECR:
   `powershell -ExecutionPolicy Bypass -File .\scripts\ensure-ecr-repository.ps1`
3. Garantir o bucket de documentos:
   `powershell -ExecutionPolicy Bypass -File .\scripts\ensure-document-storage-bucket.ps1`
4. Anexar a policy de documentos na task role:
   `powershell -ExecutionPolicy Bypass -File .\scripts\attach-document-storage-policy-to-role.ps1`
5. Renderizar a task definition para revisao:
   `powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-task-definition.ps1 -ImageUri 439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev:latest -DocumentBucketName program-management-system-documents-dev-439533253319-sa-east-1`
6. Renderizar o service para revisao:
   `powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-service-definition.ps1 -TaskDefinitionArn arn:aws:ecs:sa-east-1:439533253319:task-definition/program-management-system:1`
7. Executar o deploy inicial completo:
   `powershell -ExecutionPolicy Bypass -File .\scripts\deploy-to-ecs-fargate.ps1`

Sequencia sugerida:
1. criar/confirmar a execution role com `scripts/create-ecs-execution-role.ps1`
2. criar/confirmar o repositorio ECR com `scripts/ensure-ecr-repository.ps1`
3. construir e publicar a imagem com `scripts/deploy-to-ecs-fargate.ps1`
4. registrar a task definition
5. criar ou atualizar o service ECS/Fargate

Operacao minima recomendada apos o deploy:
1. anexar a policy de observabilidade ao principal operacional:
   `powershell -ExecutionPolicy Bypass -File .\scripts\attach-observability-read-policy-to-user.ps1`
2. se a leitura for concedida por role em vez de usuario:
   `powershell -ExecutionPolicy Bypass -File .\scripts\attach-observability-read-policy-to-role.ps1 -RoleName <role>`
3. validar Logs + Target Health:
   `powershell -ExecutionPolicy Bypass -File .\scripts\test-observability-read-access.ps1`
4. se a validacao for feita via role assumivel:
   `powershell -ExecutionPolicy Bypass -File .\scripts\test-observability-read-access.ps1 -RoleArn arn:aws:iam::439533253319:role/program-management-system-platform-admin-role`

Observacao importante sobre rollout:
- `scripts/deploy-to-ecs-fargate.ps1` agora reaplica `healthCheckGracePeriodSeconds` tambem em `update-service`
- isso evita que rollouts de task definition em services ja existentes ignorem a grace period configurada no template/rendered service definition
- no ambiente dev atual, o service opera com `healthCheckGracePeriodSeconds=120`
- apos a recuperacao de acesso do usuario interno, o bootstrap emergencial foi desligado no task definition versionado e os valores sensiveis (`email` e `password`) deixaram de ficar embutidos no template do ECS
- se esse bootstrap precisar ser reativado no futuro, a recomendacao e habilitar temporariamente as variaveis `APP_BOOTSTRAP_INTERNAL_ADMIN_*` apenas para a janela operacional necessaria e removelas novamente apos validacao do acesso
- pendencia futura: remover do codigo a logica temporaria de bootstrap/prune do usuario interno de emergencia quando o procedimento operacional definitivo de recuperacao de acesso estiver decidido

Ultima validacao operacional registrada:
- data: `2026-03-20`
- script executado: `powershell -ExecutionPolicy Bypass -File .\scripts\deploy-to-ecs-fargate.ps1`
- imagem publicada: `439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev:latest`
- digest publicado: `sha256:036c2f3d977a02c444ffe743f9d17a1f2f317b11980ca1010141c0e84e783abe`
- task definition resultante: `program-management-system:20` (hardening anterior) e depois `program-management-system:21` com bootstrap emergencial desligado no runtime
- validacao final: `powershell -ExecutionPolicy Bypass -File .\scripts\status-dev-aws-environment.ps1` com `runningCount=1`, `Primary rollout=COMPLETED` e `GET /public/ping` retornando `OK`
- observacao adicional desta rodada: o bootstrap interno de emergencia ficou configurado temporariamente para `vanderson.verza@gmail.com`, com reconciliacao automatica banco/Cognito e grupos `ADMIN`, `SUPPORT` e `AUDITOR`; a task definition `program-management-system:19` executou o prune temporario de usuarios internos legados, a `program-management-system:20` consolidou o hardening com a grace period aplicada no update do service e a `program-management-system:21` desligou o bootstrap automatico no runtime
- observacao operacional: a policy IAM da task role ainda precisa receber integralmente `AdminDeleteUser` e `AdminSetUserPassword`; a tentativa de atualizar via `put-role-policy` falhou por falta de permissao `iam:PutRolePolicy` do principal atual
