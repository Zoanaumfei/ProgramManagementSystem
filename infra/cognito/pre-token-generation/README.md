# Cognito Pre Token Generation

Esta Lambda injeta as claims de tenant no `access_token` e no `id_token`, usando os atributos customizados do User Pool:

- `custom:tenant_id`
- `custom:tenant_type`
- `custom:user_status`

Ela tambem publica aliases sem o prefixo `custom:` para facilitar o consumo pelo backend:

- `tenant_id`
- `tenant_type`
- `user_status`

Ela tambem passa a publicar no `access_token` identificadores operacionais para reconciliacao do usuario no backend:

- `username` usando `event.userName`
- `email` usando `userAttributes.email`, quando disponivel

Essa extensao foi validada no fluxo real de `users` para destravar o primeiro login de usuarios `INVITED`, permitindo que o backend reconcilie o registro local e promova o status para `ACTIVE`.

## Arquivos

- `infra/cognito/pre-token-generation/index.mjs`
- `infra/cognito/pre-token-generation/deploy.ps1`

## Runtime usado no ambiente atual

- `nodejs20.x`

## Ambiente atual validado

- regiao: `sa-east-1`
- user pool: `sa-east-1_aA4I3tEmF`
- app client: `rv7hk9nkugspb3i4p269sv828`
- lambda publicada: `arn:aws:lambda:sa-east-1:439533253319:function:program-management-system-cognito-pre-token`
- trigger ativo: `PreTokenGenerationConfig.LambdaVersion=V2_0`
- atributos customizados exigidos no pool:
  - `custom:tenant_id`
  - `custom:tenant_type`
  - `custom:user_status`

## Deploy manual rapido

1. Executar `powershell -ExecutionPolicy Bypass -File .\infra\cognito\pre-token-generation\deploy.ps1`
2. Confirmar a permissao do Cognito para invocar a Lambda
3. Configurar ou atualizar o User Pool com `PreTokenGenerationConfig` em `V2_0`
4. Fazer logout/login no frontend para forcar emissao de novos tokens

## Backfill operacional minimo

Antes da validacao dos tokens, usuarios ja existentes no Cognito precisam ter os atributos preenchidos.

Exemplo usado no ambiente atual:

```powershell
aws cognito-idp admin-update-user-attributes `
  --user-pool-id sa-east-1_aA4I3tEmF `
  --username <username-ou-sub> `
  --region sa-east-1 `
  --user-attributes `
    Name=custom:tenant_id,Value=internal-core `
    Name=custom:tenant_type,Value=INTERNAL `
    Name=custom:user_status,Value=ACTIVE
```

## Validacao operacional recomendada

1. Fazer login novo no frontend
2. Abrir `/workspace/session`
3. Confirmar no `accessTokenPayload`:
   - `tenant_id`
   - `tenant_type`
   - `user_status`
   - `username`
   - `email` quando o usuario possuir email preenchido no pool
   - `custom:tenant_id`
   - `custom:tenant_type`
   - `custom:user_status`
4. Confirmar no `idTokenPayload` os mesmos campos e tambem `email`

## Observacao importante

O frontend envia primeiro o `access_token` para a API. Sem essa Lambda, os atributos customizados do Cognito aparecem apenas no `id_token`, e a autorizacao do backend fica incompleta.

Sem `username` e `email` no `access_token`, o primeiro login de usuarios convidados tambem pode falhar na reconciliacao local do backend, mantendo o usuario em `INVITED` na listagem administrativa mesmo apos autenticacao bem-sucedida.
