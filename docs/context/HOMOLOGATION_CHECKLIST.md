# Homologation Checklist

Ultima atualizacao: `2026-03-24`

## Objetivo
- Validar a robustez operacional da trilha `membership-first` apos a virada inicial.
- Confirmar rastreabilidade por `correlationId`, consistencia de cache e convivio seguro com o legado de `/api/users`.

## Preparacao
- Garantir pelo menos um usuario com:
- dois ou mais memberships ativos
- memberships em tenants ou organizations diferentes
- pelo menos um market ativo vinculado a um membership
- Abrir frontend, devtools do browser e logs do backend ao mesmo tempo.
- Confirmar que `GET /api/auth/me` responde `userId`, `membershipId`, `activeTenantId`, `activeTenantName`, `activeOrganizationId`, `activeOrganizationName`, `activeMarketId`, `activeMarketName`, `roles`, `permissions`.

## Checklist funcional
### 1. Switch de contexto na sessao
- Abrir o `AppShell` autenticado.
- Selecionar um membership diferente e acionar `Aplicar nesta sessao`.
- Validar que a UI atualiza tenant, organization, market e roles sem refresh manual.
- Validar no browser que as requests subsequentes enviam `X-Access-Context`.
- Validar no backend:
- log de `Access context switch requested`
- log de `Access context switch resolved`
- `correlationId` consistente com a request do browser

### 2. Switch persistente (`makeDefault=true`)
- Selecionar outro membership e acionar `Salvar como padrao`.
- Confirmar sucesso na UI.
- Recarregar a aplicacao.
- Validar que `GET /api/auth/me` volta com o novo `membershipId` default sem depender de `X-Access-Context`.
- Validar que o browser ainda recebe `X-Correlation-Id` na resposta.

### 3. Volta ao default
- Com contexto request-scoped ativo, acionar `Voltar ao default`.
- Validar que a selecao local e limpa.
- Validar que requests futuras deixam de enviar `X-Access-Context`.
- Confirmar que a UI volta ao contexto default resolvido pelo backend.

### 4. Membership inativado durante a sessao
- Manter um usuario logado com um membership request-scoped ativo.
- Em sessao administrativa separada, inativar esse membership em `/workspace/users`.
- Voltar para a sessao original e executar uma acao protegida.
- Validar:
- resposta controlada do backend (`401` ou `403`, conforme o caso)
- mensagem com `correlationId`
- log `access.context.authorization.failed` no frontend
- log de falha/autorizacao no backend com `accessContextPresent=true`

### 5. Market inativo
- Criar ou usar um market ativo.
- Tentar inativar um market ainda em uso por membership ou organization.
- Confirmar bloqueio de negocio com mensagem clara.
- Remover referencias ativas e repetir a inativacao.
- Validar que memberships e telas administrativas nao oferecem o market inativo para novo uso.

### 6. Cache e consistencia visual
- Alternar rapidamente entre dois memberships diferentes.
- Navegar por `Users`, `Markets`, `Organizations` e `Portfolio`.
- Confirmar que nao aparecem:
- tenant antigo em cards novos
- organizations fora do contexto atual
- markets de tenant anterior apos a troca
- Validar no React Query Devtools, se disponivel, que as chaves revalidadas foram:
- `current-user`
- `access/users`
- `access/tenants`
- `users/list`
- `users/organizations`
- `portfolio/overview`
- `portfolio/program`
- `organizations`

### 7. Coexistencia com legado
- Abrir `/workspace/users`.
- Confirmar que o painel de memberships aparece como trilha principal.
- Confirmar que `Cadastro legado de usuario` e `Edicao legada de usuario` aparecem marcados como `compatibilidade`.
- Executar ao menos:
- um create/edit de membership
- um create/edit legado de `/api/users`
- Validar que ambos funcionam sem duplicidade critica ou perda de contexto.

## Checklist de observabilidade
- Cada request relevante devolve `X-Correlation-Id`.
- O frontend registra:
- `api.request.started`
- `api.request.succeeded` ou `api.request.failed`
- `access.context.switch.requested`
- `access.context.switch.completed` ou `access.context.switch.failed`
- `access.context.authorization.failed` quando houver `401/403` com contexto ativo
- O backend registra:
- request autenticada/anonima com presenca ou ausencia de `X-Access-Context`
- falha de autenticacao com `correlationId`
- falha de autorizacao com `correlationId`
- troca de contexto com membership anterior/novo

## Rollback funcional
### Quando acionar
- Context switch causando inconsistencias visuais persistentes.
- Falhas `401/403` recorrentes apos troca de contexto sem causa de permissao legitima.
- Tenant label ou listagem de markets divergindo do backend.

### Passos
1. Orientar operadores a usar apenas o contexto default persistido e evitar `Aplicar nesta sessao`.
2. Limpar selecao local de contexto no frontend (`Voltar ao default` ou limpar `sessionStorage` da chave `pms.access.context`).
3. Manter `/workspace/users` operando pelo bloco legado de compatibilidade para create/edit basico enquanto o incidente e analisado.
4. Suspender temporariamente a superficie de `/workspace/markets` se o problema estiver isolado na trilha multi-market.
5. Usar `GET /api/auth/me` sem `X-Access-Context` como baseline de validacao operacional.
6. Correlacionar incidente usando `X-Correlation-Id` entre browser e backend.

### O que nao precisa voltar atras nesta fase
- Estruturas de banco `tenant`, `tenant_market`, `user_membership`, `membership_role`, `app_permission`, `role_permission`
- Endpoints novos de `/api/access/*`
- Resolucao contextual de autorizacao no backend

## Evidencias minimas para aprovar a fase
- Captura de uma troca de contexto bem-sucedida com mesmo `correlationId` no browser e backend.
- Captura de um bloqueio de autorizacao apos troca de contexto com `correlationId`.
- Validacao manual dos cenarios 1 a 7 acima.
- Testes automatizados verdes no backend e frontend.
