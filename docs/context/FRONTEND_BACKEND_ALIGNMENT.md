# Frontend Backend Alignment

Ultima atualizacao: `2026-03-23`

## Backend ja entrega
- `/api/auth/me`, `/api/authz/check`, `/api/users`, `/api/operations`, `/api/reports` e `/api/portfolio` protegidos por JWT do Cognito.
- Contrato de usuarios orientado a `organizationId` e `organizationName`.
- Hierarquia de organizacoes por customer/subarvore.
- Portfolio agregado com milestones, projetos, produtos, itens, entregaveis, documentos e open issues.
- Purge administrativo de usuario e purge de subtree para saneamento operacional controlado.
- Erros `404` e `500` com `correlationId`.

## Frontend ja consome
- Workspace tecnico em `/workspace/session`.
- Workspace administrativo de organizacoes em `/workspace/organizations`.
- Workspace de usuarios em `/workspace/users`.
- Workspace de portfolio em `/workspace` e detalhe em `/workspace/programs/:programId`.
- Login proprio na home do PMS via `POST /public/auth/login`.
- Primeiro acesso em `/auth/first-access` para `NEW_PASSWORD_REQUIRED`.
- Redefinicao por codigo em `/auth/reset-password` para `PASSWORD_RESET_REQUIRED` e esquecimento de senha.
- Restore de sessao em `sessionStorage` com renovacao via `POST /public/auth/refresh`.
- Logout autenticado via `POST /api/auth/logout`.
- Banner autenticado de verificacao de email no `AppShell`, usando `send-code` + `confirm` antes de depender de recovery/reset.
- Tratamento amigavel de `reset-access` no frontend para `409` (email ainda nao verificado) e `429` (muitas tentativas em pouco tempo).
- Comunicacao de criacao de usuario alinhada ao fluxo `senha temporaria -> primeiro login -> verificacao explicita do email`.
- Acoes de documentos no detalhe do programa respeitando a camada de execucao (`ADMIN`, `MANAGER`, `MEMBER`) e mantendo leitura para `SUPPORT` e `AUDITOR`.
- Endpoints consumidos no frontend:
- `GET /public/auth/config`
- `POST /public/auth/login`
- `POST /public/auth/login/new-password`
- `POST /public/auth/password-reset/code`
- `POST /public/auth/password-reset/confirm`
- `POST /public/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `POST /api/auth/email-verification/code`
- `POST /api/auth/email-verification/confirm`
- `GET /api/authz/check`
- `GET/POST/PUT/DELETE /api/users`
- `POST /api/users/{userId}/resend-invite`
- `POST /api/users/{userId}/reset-access`
- `POST /api/users/{userId}/purge`
- `GET/POST/PUT/DELETE /api/portfolio/organizations`
- `GET /api/portfolio/organizations/{organizationId}`
- `POST /api/portfolio/organizations/{organizationId}/purge-subtree`
- `GET/POST /api/portfolio/milestone-templates`
- `GET/POST /api/portfolio/programs`
- `GET /api/portfolio/programs/{programId}`
- `POST /api/portfolio/programs/{programId}/projects`
- `POST /api/portfolio/projects/{projectId}/products`
- `POST /api/portfolio/products/{productId}/items`
- `POST /api/portfolio/items/{itemId}/deliverables`
- `POST /api/portfolio/programs/{programId}/open-issues`
- fluxo de documentos via `upload-url`, PUT real no presigned URL, `complete`, `download-url` e `delete`

## O que ainda esta stubado ou provisiorio
- O runtime AWS atual ainda esta em `stub` por bloqueio operacional de bucket/policy IAM, embora o codigo ja suporte `S3` real ponta a ponta.
- No backend de auth, a correcao para impedir fallback indevido ao `StubPublicAuthenticationGateway` ja esta pronta e testada, mas a task corrigida `program-management-system:27` ainda nao estabilizou no ECS; em dev, o ALB ainda pode responder pela task `:26`.
- Frontend ainda tem fallback controlado para `id_token` quando o backend responde `403` ao `access_token`.
- A visualizacao de organizacoes ainda e majoritariamente em lista enriquecida; a arvore visual completa ainda nao foi implementada.

## Divergencias ou pontos sensiveis atuais
- A trilha de auth do frontend agora depende diretamente dos endpoints publicos do backend; qualquer divergencia de payload nesses endpoints impacta login, refresh e logout imediatamente.
- O app client do Cognito ja aceita `ALLOW_USER_PASSWORD_AUTH`, e uma chamada direta `initiate-auth` com email+senha funcionou; se o login continuar falhando no PMS, a divergencia restante esta no runtime/backend, nao na credencial do usuario.
- O `access_token` ja carrega claims de tenant; ainda falta decidir quando o fallback de `id_token` pode ser removido.
- O frontend ja recebe metadados hierarquicos completos de organizacao, mas a UX ainda nao representa toda a subarvore de forma nativa.
- `/api/auth/me` agora tambem carrega `emailVerified` e `emailVerificationRequired`; a UI ainda nao mostra esse estado no diretorio administrativo de usuarios.
- Persistencia e autorizacao do backend ja respeitam subarvore; a UX de `SUPPORT INTERNAL` ainda precisa de validacao mais ampla em cenarios reais.
- A UI de documentos ja saiu do modo stub, mas a validacao manual ainda precisa confirmar se o runtime atualmente apontado pelo frontend esta de fato com o comportamento esperado para `prepare`, PUT binario, `complete`, listagem e `download-url`.

## Pendencias de integracao
- Estabilizar a task `program-management-system:27` no ECS para que o ambiente de dev deixe de atender a trilha publica de auth pelo `stub`.
- Homologar com Cognito real o fluxo `login -> NEW_PASSWORD_REQUIRED -> /api/auth/me -> verificacao explicita de email`.
- Homologar com Cognito real o fluxo `reset-access -> PASSWORD_RESET_REQUIRED -> confirmacao de reset` reaproveitando o primeiro codigo recebido.
- Homologar com Cognito real o fluxo `refresh -> reload da aplicacao -> restore de sessao`.
- Homologar com Cognito real o fluxo `logout -> invalidacao no Cognito -> limpeza local`.
- Validar visualmente o portfolio proprio vs. herdado da subarvore.
- Confirmar em uso real o fluxo de `SUPPORT INTERNAL` para `resend-invite`, `reset-access` e `purge`.
- Validar o primeiro login do `ADMIN INTERNAL` bootstrapado e o redirect final do frontend.
- Validar em uso real o fluxo `primeiro login -> codigo de verificacao -> confirmacao -> reset-access`.
- Decidir quando remover o fallback de `id_token`.
- Aplicar bucket + policy IAM no runtime AWS para virar documentos de `stub` para `S3` real.

## Roteiro manual sugerido agora
- Criar customer raiz em `/workspace/organizations`.
- Criar organizacao filha dentro da mesma arvore.
- Criar o primeiro `ADMIN` da filha em `/workspace/users`.
- Entrar com o usuario convidado na home do PMS e validar `NEW_PASSWORD_REQUIRED` em `/auth/first-access`.
- Concluir a verificacao explicita de email no banner autenticado.
- Executar `reset-access` no diretorio de usuarios e validar `PASSWORD_RESET_REQUIRED` com confirmacao em `/auth/reset-password` usando o primeiro codigo entregue pelo Cognito.
- Recarregar a aplicacao para validar restore de sessao via refresh token.
- Fazer logout e validar limpeza local + invalidacao remota.
- Criar programa e aprofundar a arvore ate `Entregavel` em `/workspace` e `/workspace/programs/:programId`.
- Voltar ao diretorio de usuarios e validar `reset-access` somente depois da verificacao do email.
- Em entregavel `DOCUMENT`, validar no frontend o fluxo `upload-url -> complete -> list -> download-url`.
- Conferir no backend e na UI que o customer raiz enxerga o programa da filha e que um customer fora da arvore nao enxerga.
- Lembrar que, no runtime AWS atual, o fluxo de documentos ainda passa no provider `stub`; a UX e o contrato ja refletem o fluxo real, mas o storage definitivo ainda nao foi aplicado na task.
