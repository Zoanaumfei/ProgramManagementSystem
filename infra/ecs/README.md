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

Complemento operacional de autenticacao:
- o backend depende do Cognito para JWT e grupos
- o fluxo real de tenant no `access_token` agora depende tambem da Lambda versionada em `infra/cognito/pre-token-generation`
- o primeiro login de usuarios `INVITED` no backend agora tambem depende dessa Lambda publicar `username` e `email` no `access_token`, para a reconciliacao local do modulo de `users`
- a task role do backend tambem precisa manter permissoes administrativas de usuario no Cognito, incluindo `AdminGetUser`, porque o modulo de `users` agora valida a existencia da identidade antes do saneamento excepcional `POST /api/users/{userId}/purge`
- artefatos relevantes:
  - `infra/cognito/pre-token-generation/index.mjs`
  - `infra/cognito/pre-token-generation/deploy.ps1`
  - `infra/cognito/pre-token-generation/README.md`
- antes de validar o frontend contra o ambiente AWS, confirmar que o User Pool continua apontando para a Lambda `program-management-system-cognito-pre-token` com `PreTokenGenerationConfig.LambdaVersion=V2_0`

Checklist operacional adicional para `users`:
- confirmar que a task role `program-management-system-ecs-task-role` inclui `AdminCreateUser`, `AdminUpdateUserAttributes`, `AdminAddUserToGroup`, `AdminRemoveUserFromGroup`, `AdminResetUserPassword`, `AdminDisableUser` e `AdminGetUser`
- se houver ajuste de policy IAM da task role, executar `force-new-deployment` no service ECS para reciclar as tasks e renovar as credenciais efetivas
- se houver ajuste na Lambda de `Pre Token Generation`, fazer logout/login no frontend antes de retestar o fluxo autenticado
- se `POST /api/users/{userId}/purge` falhar, verificar primeiro CloudWatch para distinguir rota ausente, IAM faltante no Cognito ou regra de negocio

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
3. Renderizar a task definition para revisao:
   `powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-task-definition.ps1 -ImageUri 439533253319.dkr.ecr.sa-east-1.amazonaws.com/oryzem-backend-dev:latest`
4. Renderizar o service para revisao:
   `powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-service-definition.ps1 -TaskDefinitionArn arn:aws:ecs:sa-east-1:439533253319:task-definition/program-management-system:1`
5. Executar o deploy inicial completo:
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
