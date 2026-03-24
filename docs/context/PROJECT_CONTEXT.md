# Project Context

Ultima atualizacao: `2026-03-23`

## Objetivo do produto
- Construir um SaaS B2B de gerenciamento de programas e projetos para a cadeia automotiva.
- Atender montadoras, Tier 1 e Tier 2 com colaboracao multiempresa, rastreabilidade e segregacao por tenant.

## Escopo atual
- Backend unico em Spring Boot 4 + Java 21 + Maven.
- Frontend separado em React + Vite, no workspace local `C:\Users\vande\Oryzem\PMS Frontend`.
- Autenticacao via Amazon Cognito e backend como OAuth2 Resource Server validando os JWTs do Cognito; o frontend agora usa login proprio consumindo os endpoints publicos de auth do backend.
- Persistencia principal em PostgreSQL no RDS, com Flyway e JPA.
- Runtime de dev em AWS com ECS/Fargate, ALB, ECR e Secrets Manager.

## Modulos principais
- `platform.auth`: Cognito, JWT, filtros e `/api/auth/*`.
- `platform.authorization`: matriz de autorizacao e `/api/authz/check`.
- `platform.tenant`: organizacoes, hierarquia, subtree e purge operacional.
- `platform.users`: administracao de usuarios e reconciliacao com identidade.
- `platform.documents`: storage e presigned URLs.
- `modules.projectmanagement`: portfolio e dominio principal.
- `modules.operations`: operacoes administrativas legadas.
- `modules.reports`: relatrios e exportacao.

## Fluxos principais
- `Organizacao -> Programa -> Projeto -> Produto -> Item -> Entregavel -> Documento`
- `Programa -> OpenIssue`
- `Organizacao -> primeiro ADMIN -> onboarding de usuarios`
- `SUPPORT INTERNAL -> purge-subtree` para saneamento operacional controlado

## Estado atual resumido
- O backend ja entrega contrato suficiente para organizacoes, portfolio, users, operations e reports.
- A hierarquia externa por customer/subarvore esta implementada e governa visibilidade do portfolio.
- O frontend ja consome organizacoes, milestone templates, programas, detalhe agregado, users e purge administrativo.
- O monolito modular ja passou por refactor estrutural e consolidacao de fronteiras entre modulos.
- `platform.users` virou o primeiro modulo com separacao interna mais explicita em `api`, `application`, `domain` e `infrastructure`.
- O backend ja suporta provider `s3` real com presigned upload/download preservando o contrato atual de documentos.
- O frontend ja faz upload real do binario para o storage assinado e depois confirma o `complete`.
- O runtime AWS de dev continua em `stub` apenas porque o principal operacional atual nao consegue criar o bucket nem anexar a policy IAM da task role.
- O onboarding real de usuarios agora exige verificacao explicita do email no Cognito depois do primeiro login; troca de senha temporaria nao marca `email_verified=true` sozinha.
- `/api/auth/me` passou a refletir `emailVerified` e `emailVerificationRequired`, e o frontend orienta o usuario autenticado a enviar/confirmar o codigo antes de depender de recovery/reset.
- O workspace de usuarios passou a tratar `reset-access` com mensagens claras para `409` e `429`, sem mascarar a nova regra de negocio de verificacao de email.
- O backend agora expoe endpoints publicos para login proprio com Cognito (`login`, `new-password`, `password-reset`, `refresh`) e `logout`.
- O frontend ja consome essa trilha publica do backend para login, `NEW_PASSWORD_REQUIRED`, reset por codigo, refresh e logout, sem Hosted UI.
- O fluxo de `reset-access` agora pode reaproveitar o primeiro codigo enviado pelo Cognito em `/auth/reset-password`, sem acionar um segundo codigo desnecessario.
- O `access_token` e a trilha principal; o fallback para `id_token` ainda existe de forma temporaria no frontend.
- Na homologacao do login proprio em dev, foi identificado que o backend ainda podia selecionar o `StubPublicAuthenticationGateway` mesmo com `APP_SECURITY_IDENTITY_PROVIDER=cognito`; a correcao ja esta em codigo e coberta por teste.
- O app client do Cognito ja foi alinhado para `ALLOW_USER_PASSWORD_AUTH`, e um `initiate-auth` direto no Cognito confirmou que o fluxo real de email+senha funciona fora do backend.
- A imagem corrigida foi publicada e a task definition `program-management-system:27` foi registrada, mas o rollout no ECS nao estabilizou; ao fim do dia o ALB ainda atendia pela task `:26`, entao o login proprio em dev ainda nao estava homologado em runtime.

## Proximos passos resumidos
- Validar o fluxo ponta a ponta do portfolio com organizacoes visiveis por subarvore.
- Estabilizar a task `program-management-system:27` no ECS e confirmar que o runtime de dev deixou de servir o `StubPublicAuthenticationGateway`.
- Homologar em uso real o fluxo `login proprio -> primeiro acesso ou reset por codigo -> enviar codigo -> confirmar email -> reset-access`.
- Fechar ownership, transicoes de status e modelagem de `FORM`.
- Concluir a virada do runtime AWS para `S3` real assim que bucket e policy IAM puderem ser aplicados.
- Homologar logout completo, restore de sessao e revisar se o fallback de `id_token` pode ser removido.
- Continuar o endurecimento do monolito modular e adicionar protecoes arquiteturais depois da estabilizacao interna.

## Roteiro manual atual
- `ADMIN INTERNAL` cria customer raiz.
- `ADMIN` externo do customer cria organizacao filha na propria subarvore.
- `ADMIN` cria o primeiro usuario `ADMIN` da organizacao filha.
- Usuario convidado faz login na home do PMS; se o backend responder `NEW_PASSWORD_REQUIRED`, conclui a troca de senha temporaria em `/auth/first-access`.
- Quando o admin executa `reset-access`, o usuario entra novamente e, se o backend responder `PASSWORD_RESET_REQUIRED`, conclui o reset em `/auth/reset-password` usando o primeiro codigo enviado pelo Cognito.
- Usuario externo da filha cria ou opera o portfolio em `Programa -> Projeto -> Produto -> Item -> Entregavel`.
- Para entregavel `DOCUMENT`, o fluxo atual a validar e `upload-url -> complete -> list -> download-url`.
- `reset-access` so deve ser usado depois que o usuario concluir a verificacao do email; antes disso o backend retorna erro de negocio claro, sem `500`.
- Customer raiz deve enxergar o proprio portfolio e o da filha; um customer fora da arvore nao deve enxergar esse programa.
- Observacao: no runtime AWS atual o contrato de documentos ainda cai em `stub`; a virada para `S3` real depende apenas de bucket + policy IAM.

## Navegacao
- [Regras de negocio vigentes](./BUSINESS_RULES.md)
- [Contrato de API vigente](./API_CONTRACT.md)
- [Alinhamento frontend/backend](./FRONTEND_BACKEND_ALIGNMENT.md)
- [Arquitetura atual](./ARCHITECTURE.md)
- [Decisoes ativas e temporarias](./DECISIONS.md)
- [Gaps reais ainda abertos](./OPEN_GAPS.md)
- [Historico resumido](./CHANGELOG.md)
