# Decisions

Ultima atualizacao: `2026-03-24`

## Cognito como autenticacao oficial
- Decisao: o backend nao implementa login por senha local e valida apenas JWT emitido pelo Cognito.
- Motivo: manter autenticacao stateless e centralizar identidade fora da aplicacao.
- Impacto: o backend opera como Resource Server e tambem pode brokerar login proprio contra o Cognito sem criar senha local.
- Status: ATIVA

## Hosted UI em retirada gradual
- Decisao: manter o Cognito como IdP, mas preparar o frontend para sair do Hosted UI e usar login proprio consumindo endpoints publicos do backend.
- Motivo: controlar melhor os fluxos `NEW_PASSWORD_REQUIRED`, `PASSWORD_RESET_REQUIRED`, verificacao de email e logout, evitando UX confusa como o envio de dois codigos no reset.
- Impacto: backend agora expone endpoints publicos de auth e o Hosted UI passa a ser trilha temporaria ate o cutover completo do frontend.
- Status: TEMPORARIA

## Monolito modular em vez de microservicos agora
- Decisao: manter uma unica aplicacao Spring Boot e um unico deploy, organizados em modulos.
- Motivo: reduzir complexidade operacional e preparar futuras extracoes sem quebrar o produto agora.
- Impacto: fronteiras sao protegidas por pacotes e portas internas, nao por rede.
- Status: ATIVA

## Tenant explicito e separado de Organization
- Decisao: `tenant` passa a existir explicitamente como fronteira SaaS; `organization` deixa de ser a representacao tecnica do tenant.
- Motivo: o modelo anterior misturava isolamento SaaS com hierarquia operacional, o que dificultava multi-contexto, multi-market e autorizacao contextual.
- Impacto: `organization` agora pertence a `tenant`, `market` e opcional, e a hierarquia continua focada em estrutura de negocio.
- Status: ATIVA

## Membership-first para contexto de acesso
- Decisao: `user_membership` e o novo centro de verdade para contexto de acesso.
- Motivo: um mesmo usuario precisa poder atuar em mais de um tenant/organizacao/market com papeis diferentes.
- Impacto: `role`, `tenant` e escopo ativo deixam de ser responsabilidade principal de `user`; a aplicacao resolve o contexto a partir do membership.
- Status: ATIVA

## Contexto ativo selecionavel explicitamente
- Decisao: o contexto ativo pode ser trocado pela aplicacao sem depender de novas claims emitidas pelo Cognito.
- Motivo: o backend e stateless, mas o usuario precisa conseguir alternar entre memberships reais.
- Impacto: o sistema suporta `POST /api/access/context/activate` para trocar o membership default e tambem aceita `X-Access-Context` como override por request.
- Status: ATIVA

## Compatibilidade temporaria com user legado
- Decisao: manter `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` durante a transicao.
- Motivo: evitar quebra abrupta de fluxos existentes, claims legadas, bootstrap e APIs ainda planas.
- Impacto: existe dual-write para membership default e fallback de leitura de claims legadas enquanto o restante da aplicacao migra.
- Status: TEMPORARIA

## Customer raiz so por ADMIN INTERNAL
- Decisao: `ADMIN INTERNAL` cria tenant/organizacao raiz; `ADMIN` externo cria apenas descendentes na propria subarvore.
- Motivo: separar governanca de plataforma da administracao operacional de clientes e fornecedores.
- Impacto: a API de organizacoes trata create root e create child com regras diferentes.
- Status: ATIVA

## Contrato de users orientado a organizationId
- Decisao: o contrato principal de usuarios continua usando `organizationId` e `organizationName`.
- Motivo: refletir a operacao diaria da UX sem expor toda a complexidade contextual do backend na primeira fase.
- Impacto: `tenantId` legado segue aceito em alguns filtros e auditoria, mas o escopo funcional passa a considerar `organizationId` e membership ativo.
- Status: ATIVA

## Storage de documentos configuravel por provider
- Decisao: usar provider configuravel para documentos, com `stub` em local/testes e S3 como alvo definitivo.
- Motivo: permitir evolucao do contrato sem bloquear o produto enquanto o bucket final nao esta pronto.
- Impacto: o fluxo de documentos ja existe no contrato, mas a homologacao em S3 real permanece aberta.
- Status: TEMPORARIA

## Purge como excecao operacional explicita
- Decisao: purge de usuario e purge-subtree de organizacao existem como operacoes separadas de inativacao logica.
- Motivo: limpeza de massa de teste e saneamento controlado sem perder a regra geral de soft delete.
- Impacto: exigem `SUPPORT`, override explicito e justificativa.
- Status: ATIVA
