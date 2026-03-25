# Open Gaps

Ultima atualizacao: `2026-03-24`

## Adoção do modelo de membership no contrato principal de users
- Descricao: o backend ja suporta multiplos memberships e possui API propria em `/api/access/*`, mas o contrato principal de `users` ainda permanece plano em `organizationId` e `role`.
- Impacto: o modelo novo existe, mas parte importante da administracao ainda depende de compatibilidade legada.
- Prioridade: P0
- Area: backend
- Status: aberto

## Adoção do contexto ativo no frontend
- Descricao: a UI ja permite trocar e visualizar o contexto ativo no `AppShell`, agora com invalidação seletiva de cache e observabilidade, mas ainda precisa de homologacao mais ampla entre perfis reais.
- Impacto: o fluxo principal esta mais robusto, porem ainda ha risco residual sem rodada final de UX e validacao manual multi-tenant.
- Prioridade: P0
- Area: frontend
- Status: aberto

## Gestao funcional de markets na UI
- Descricao: a superficie administrativa de markets ja existe em `/workspace/markets`, com tenant label explicito via `/api/access/tenants`, mas ainda precisa de homologacao com dados reais, filtros adicionais e naming final de negocio.
- Impacto: a dimensao multi-market ja esta operacional na UI, porem ainda requer amadurecimento para escala e uso cotidiano.
- Prioridade: P0
- Area: frontend
- Status: aberto

## Homologacao operacional final do contexto ativo
- Descricao: backend e frontend ja expõem `correlationId`, logs estruturados e checklist de validacao, mas ainda falta executar a homologacao guiada em ambiente alvo com usuarios multi-membership e multi-market.
- Impacto: sem essa rodada final, permanece risco residual em troca de contexto persistente, membership inativado durante sessao e inativacao de market em uso.
- Prioridade: P0
- Area: operacao
- Status: aberto

## Remocao progressiva do dual-write legado em user
- Descricao: `app_user.role`, `app_user.tenant_id` e `app_user.tenant_type` ainda sao mantidos por compatibilidade.
- Impacto: aumenta a complexidade e prolonga a necessidade de sincronizacao entre dois modelos.
- Prioridade: P0
- Area: arquitetura
- Status: aberto

## Claims do Cognito alinhadas ao novo contexto selecionavel
- Descricao: o Cognito continua autenticando corretamente, mas os claims ainda refletem o modelo legado e nao representam plenamente a selecao dinamica de membership.
- Impacto: os tokens continuam uteis para auth, mas nao expressam sozinhos o contexto contextual completo da aplicacao.
- Prioridade: P1
- Area: integracao
- Status: aberto

## Homologacao real do login proprio com Cognito
- Descricao: o backend e o frontend ja suportam login proprio, primeiro acesso, reset por codigo, refresh e logout, mas a homologacao ponta a ponta ainda precisa continuar em runtime real.
- Impacto: sem essa rodada final de integracao, ainda existe risco de divergencia entre ambiente local e ambiente AWS.
- Prioridade: P1
- Area: integracao
- Status: aberto

## Virada operacional do runtime AWS para documentos em S3
- Descricao: o codigo ja suporta `S3` real e o frontend ja envia o binario ao presigned URL, mas a infraestrutura AWS ainda precisa ficar totalmente alinhada para abandonar o provider `stub`.
- Impacto: parte do fluxo real ainda depende de homologacao operacional, nao de codigo.
- Prioridade: P1
- Area: backend
- Status: aberto

## Modelagem de deliverable do tipo FORM
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
- Descricao: ownership de `Program` esta implicito no owner organization, mas ownership operacional de `Project`, `Product`, `Item`, `Deliverable` e `OpenIssue` ainda nao foi fechado como regra explicita.
- Impacto: limita a evolucao de permissoes, auditoria de negocio e UX de responsabilidade.
- Prioridade: P1
- Area: negocio
- Status: aberto

## Paginacao, busca e escala de volume
- Descricao: o contrato atual ainda nao tem paginacao e possui busca textual apenas em organizacoes.
- Impacto: risco de degradacao funcional quando o volume crescer.
- Prioridade: P2
- Area: arquitetura
- Status: aberto

## Protecao arquitetural profunda entre modulos
- Descricao: guardrails basicos com ArchUnit ja existem, mas ainda nao ha cobertura profunda para todos os agrupamentos internos de `access`, `tenant`, `documents` e `projectmanagement`.
- Impacto: a base esta protegida contra regressao estrutural obvia, mas nao contra todo acoplamento fino.
- Prioridade: P2
- Area: arquitetura
- Status: aberto
