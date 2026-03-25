# Legacy Users Deprecation Runbook

Ultima atualizacao: `2026-03-25`

## Objetivo
- Reduzir e desligar progressivamente a trilha legada de `/api/users` sem interromper a operacao administrativa.
- Usar feature flags, telemetria e rollback rapido por ambiente.

## Feature flags
- `app.features.users-legacy.ui-enabled`
- `app.features.users-legacy.read-enabled`
- `app.features.users-legacy.write-enabled`

## Estagios operacionais
### Etapa A - `ONLY_WARNING`
- `ui-enabled=true`
- `read-enabled=true`
- `write-enabled=true`
- Efeito:
- legado continua operacional
- responses do legado devolvem headers de deprecacao
- painel de adocao passa a medir uso real

### Etapa B - `READ_ONLY`
- `ui-enabled=false`
- `read-enabled=true`
- `write-enabled=false`
- Efeito:
- legado deixa de ser trilha primaria
- leitura continua disponivel para contingencia
- escrita legada passa a retornar `409`

### Etapa C - `OFF_BY_DEFAULT`
- `ui-enabled=false`
- `read-enabled=false`
- `write-enabled=false`
- Efeito:
- legado fica indisponivel por padrao
- leituras legadas retornam `404`
- fluxo principal segue exclusivamente em `membership-first`

### Etapa D - Remocao final
- Somente apos criterios objetivos:
- `legacySharePercent` sustentado abaixo do limite acordado
- nenhum tenant critico em `tenantsStillDependentOnLegacy`
- homologacao e rollback recentes validados

## Defaults por ambiente
- `dev`: `ui=true`, `read=true`, `write=true`
- `homolog`: `ui=true`, `read=true`, `write=true`
- `prod`: `ui=false`, `read=true`, `write=true`

## Pre-checks obrigatorios
1. Confirmar que o backend atual responde `GET /api/access/legacy-users/deprecation-status`.
2. Confirmar que `GET /api/access/legacy-users/adoption-report` retorna dados coerentes para pelo menos `7` dias.
3. Verificar `legacySharePercent`, `membershipSharePercent` e `tenantsStillDependentOnLegacy`.
4. Verificar se nao houve aumento anormal de `LEGACY_USERS_API_BLOCKED`.
5. Confirmar que `membership-first` segue operacional para listar, criar, editar e inativar memberships.

## Sequencia recomendada de rollout
### 1. Entrar em `ONLY_WARNING`
- Aplicar ou manter:
- `ui-enabled=true`
- `read-enabled=true`
- `write-enabled=true`
- Monitorar:
- uso por tenant
- perfis que ainda usam legado
- tendencia semanal

### 2. Avancar para `READ_ONLY`
- Aplicar:
- `ui-enabled=false`
- `read-enabled=true`
- `write-enabled=false`
- Validar:
- criacao/update/delete/reset/resend/purge em `/api/users` retornam `409`
- leitura legada ainda funciona para contingencia
- painel mostra queda consistente de operacoes legadas

### 3. Avancar para `OFF_BY_DEFAULT`
- Aplicar:
- `ui-enabled=false`
- `read-enabled=false`
- `write-enabled=false`
- Validar:
- `GET /api/users` retorna `404`
- memberships continuam operacionais
- tenants criticos nao dependem mais do legado

## Sinais de alerta
- aumento repentino de `legacyOperations` em tenants que deveriam estar migrados
- aumento de `LEGACY_USERS_API_BLOCKED`
- suporte precisando destravar operacao administrativa via legado
- erro recorrente de autorizacao em fluxos `membership-first`
- tenant critico aparecendo em `tenantsStillDependentOnLegacy`

## Rollback
### Rollback imediato para contingencia
- Restaurar:
- `ui-enabled=true`
- `read-enabled=true`
- `write-enabled=true`
- Resultado:
- legado volta a operar integralmente
- nao ha necessidade de rollback de banco para esta fase

### Rollback parcial
- Se o problema for apenas escrita:
- `ui-enabled=false`
- `read-enabled=true`
- `write-enabled=true`
- Mantem UX principal nova, mas reabre escrita legada enquanto o incidente e tratado.

## Consultas operacionais recomendadas
### Status efetivo
```http
GET /api/access/legacy-users/deprecation-status
```

### Relatorio de adocao
```http
GET /api/access/legacy-users/adoption-report?trailingDays=28
```

### O que observar no relatorio
- `legacySharePercent`
- `operationBreakdown`
- `roleBreakdown`
- `weeklyTrend`
- `tenantsStillDependentOnLegacy`

## Evidencias para promover de etapa
1. `legacySharePercent` abaixo do limite acordado por pelo menos duas semanas.
2. Nenhum tenant critico listado como dependente principal do legado.
3. Suporte e operacao validaram contingencia em homolog.
4. Suite automatizada de flags e guardrails executada com sucesso.
