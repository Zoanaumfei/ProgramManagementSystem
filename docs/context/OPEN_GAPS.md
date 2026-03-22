# Open Gaps

Ultima atualizacao: `2026-03-22`

## Logout real com Cognito Hosted UI
- Descricao: o fluxo ja foi implementado no frontend, mas ainda falta homologacao ponta a ponta com o app client atual e `logout_uri` em uso real.
- Impacto: risco de experiencia inconsistente de sessao entre frontend e Hosted UI.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Remocao do fallback de id_token
- Descricao: o `access_token` ja e a trilha principal e carrega claims de tenant; ainda falta decidir quando o fallback temporario de `id_token` pode ser removido com seguranca.
- Impacto: complexidade extra no frontend e risco de comportamento diferente entre chamadas.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Fluxo de documentos em S3 real
- Descricao: o contrato de documentos esta pronto, mas o bucket definitivo, IAM, encryption, lifecycle e homologacao em runtime AWS ainda nao foram fechados.
- Impacto: o modulo de documentos segue preso ao provider `stub` no ambiente atual.
- Prioridade: P0
- Area: backend
- Status: aberto

## Modelagem de entregavel do tipo FORM
- Descricao: `FORM` existe no enum e no contrato de dominio, mas ainda nao tem shape persistente de perguntas, respostas e aprovacao.
- Impacto: parte central do produto existe apenas como placeholder funcional.
- Prioridade: P1
- Area: negocio
- Status: aberto

## Transicoes estritas de status
- Descricao: os enums de `Program`, `Project`, `Deliverable`, `ProjectMilestone` e `OpenIssue` existem, mas a matriz de transicao ainda nao foi fechada no backend.
- Impacto: o sistema ja tem estados, mas nao possui ciclo de vida de negocio totalmente protegido.
- Prioridade: P1
- Area: backend
- Status: aberto

## Ownership detalhado das entidades do portfolio
- Descricao: ownership de `Programa` esta implicito no owner organization, mas ownership operacional de `Projeto`, `Produto`, `Item`, `Entregavel` e `OpenIssue` ainda nao foi fechado como regra de negocio explicita.
- Impacto: limita a evolucao de permissoes, auditoria de negocio e UX de responsabilidade.
- Prioridade: P1
- Area: negocio
- Status: aberto

## Enriquecimento de /api/auth/me
- Descricao: `/api/auth/me` ja atende a sessao atual, mas permanece aberto decidir se ele deve expor mais dados normalizados para a interface.
- Impacto: risco de duplicacao de normalizacao no frontend e leitura incompleta do contexto de sessao.
- Prioridade: P1
- Area: integracao
- Status: aberto

## principal=null em endpoints de ping
- Descricao: ainda existe investigacao em aberto para casos em que `principal` aparece `null` em respostas de ping.
- Impacto: ruido diagnostico e duvida sobre consistencia da autenticacao em alguns cenarios.
- Prioridade: P1
- Area: integracao
- Status: aberto

## UX hierarquica de organizacoes e portfolio herdado
- Descricao: o backend ja entrega metadados hierarquicos completos, mas a UX ainda nao fechou como representar arvore, portfolio proprio e portfolio herdado da subarvore.
- Impacto: risco de leitura confusa conforme a arvore crescer.
- Prioridade: P1
- Area: frontend
- Status: aberto

## Paginacao, busca e escala de volume
- Descricao: o contrato atual ainda nao tem paginacao e possui busca textual apenas em organizacoes.
- Impacto: risco de degradacao funcional quando o volume crescer.
- Prioridade: P2
- Area: arquitetura
- Status: aberto

## Protecao arquitetural profunda entre modulos
- Descricao: guardrails basicos com ArchUnit ja existem, mas ainda nao ha cobertura profunda para todos os agrupamentos internos de `tenant`, `documents` e `projectmanagement`.
- Impacto: a base esta protegida contra regressao estrutural obvia, mas nao contra toda forma de acoplamento fino.
- Prioridade: P2
- Area: arquitetura
- Status: aberto
