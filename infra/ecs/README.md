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
