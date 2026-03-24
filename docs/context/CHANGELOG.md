# Changelog

Ultima atualizacao: `2026-03-23`

## 2026-03-07
- Base inicial de autenticacao, autorizacao, persistencia e modulos `users`, `operations` e `reports`.

## 2026-03-08 a 2026-03-10
- Consolidacao da operacao AWS com RDS privado, ECS/Fargate, ALB, ECR, Secrets Manager e health checks.

## 2026-03-11
- Fechamento do mapa inicial do dominio principal com raiz em `Programa`.

## 2026-03-13
- `docs/context/PROJECT_CONTEXT.md` passou a ser a memoria operacional compartilhada entre backend e frontend.

## 2026-03-14
- Primeira UI basica do portfolio no frontend consumindo o backend real.
- Ajustes de tratamento de erro e compatibilidade de token no frontend.

## 2026-03-15
- Contrato de `users` migrou para `organizationId` e `organizationName`.
- Jornada integrada `Organizacao -> primeiro ADMIN` entrou no desenho funcional.

## 2026-03-17
- Modulo de `users` ganhou update e inativacao logica.
- `setupStatus` de organizacao e bloqueios de onboarding foram implantados.

## 2026-03-19
- Integracao do modulo de `users` com identidade real no Cognito.
- Lambda de `Pre Token Generation` passou a emitir claims de tenant no `access_token`.
- Workspace de `users` foi implementado no frontend.
- Fluxo de `purge` foi validado no runtime AWS apos ajuste de IAM.

## 2026-03-20
- Hierarquia de organizacoes por customer/subarvore entrou no backend.
- Regras de visibilidade e permissoes de portfolio por papel foram fechadas.
- Frontend foi alinhado ao contrato hierarquico de organizacoes e ao novo recorte de papeis.
- `purge-subtree` entrou no frontend para saneamento operacional restrito.

## 2026-03-21
- Segunda passada do refactor para monolito modular: movimentos fisicos, servicos menores em `projectmanagement` e alinhamento de packages.
- Consolidacao de fronteiras: `OrganizationLookup` movido para `tenant`, tenant/users/reports passaram a conversar por portas explicitas e servicos grandes foram quebrados.
- Testes e pacotes legados foram reorganizados sem mudar o comportamento funcional.

## 2026-03-22
- O contexto compartilhado foi consolidado em `docs/context/PROJECT_CONTEXT.md` como fonte unica.
- `platform.users` foi consolidado como primeiro modulo com camadas internas mais claras: `api`, `application`, `domain` e `infrastructure`.
- DTOs, servicos, repositorios e integracao de identidade de `users` foram movidos fisicamente para pastas coerentes com seus packages.
- O contrato de tenant permaneceu em `platform.tenant.OrganizationLookup`, mantendo `users` desacoplado da implementacao concreta de diretorio organizacional.
- O fluxo de documentos foi preparado para `S3` real sem mudar o contrato HTTP: upload assinado, `complete`, listagem e download assinado.
- O frontend deixou de assumir `stub` e passou a enviar o binario real para o presigned URL antes de confirmar o documento.
- Scripts e task definition do ECS passaram a prever bucket, prefixo e policy minima de documentos para a virada operacional em AWS.
- A homologacao final em AWS ficou bloqueada no principal operacional atual por falta de `s3:CreateBucket` e `iam:PutRolePolicy`.
- O fluxo correto de verificacao explicita de email no Cognito foi introduzido para usuarios autenticados, sem marcar `email_verified=true` no provisionamento administrativo.
- `/api/auth/me` passou a refletir `emailVerified` e `emailVerificationRequired`.
- O frontend passou a orientar o usuario autenticado a enviar/confirmar o codigo de verificacao dentro do workspace antes de depender de recovery/reset.
- `reset-access` deixou de mascarar falhas do Cognito como `500` generico e agora retorna conflito de negocio claro quando o usuario ainda nao verificou o email, alem de `429` para throttling tratado.
- O backend ficou pronto para migracao do frontend para login proprio com Cognito, com endpoints publicos de `login`, `new-password`, `password-reset`, `refresh` e `logout`, sem introduzir senha local.
- A policy operacional versionada do Cognito foi ampliada para cobrir os novos fluxos administrativos de auth (`AdminInitiateAuth`, `AdminRespondToAuthChallenge`, `GlobalSignOut`).
- A versao com os endpoints publicos de auth foi publicada no ECS de dev em `program-management-system:24`.

## 2026-03-23
- O app client do Cognito em dev foi alinhado para `ALLOW_USER_PASSWORD_AUTH`, alem dos flows administrativos ja habilitados.
- Uma chamada direta `initiate-auth` contra o Cognito confirmou que login com email+senha funciona no app client real, descartando problema de credencial do usuario no caso homologado.
- Foi identificado que o backend ainda podia selecionar `StubPublicAuthenticationGateway` mesmo com `APP_SECURITY_IDENTITY_PROVIDER=cognito`, o que explicava respostas de autenticacao incompatíveis com o Cognito real.
- `PublicAuthenticationConfig` foi corrigido para amarrar explicitamente `cognito` ao provider `cognito` e `stub` apenas ao provider `stub` ou ausente.
- Entrou teste de regressao em `PublicAuthenticationConfigTest` para garantir a selecao correta do gateway de auth por provider.
- A imagem `oryzem-backend-dev:custom-login-20260323-0053` foi publicada e a task definition `program-management-system:27` foi registrada.
- O rollout da task `:27` ainda nao estabilizou no ECS; ao fim do dia a task `:26` seguia atendendo o ALB, mantendo a homologacao real do login proprio em aberto.
