# Open Gaps

Ultima atualizacao: `2026-03-23`

## Estabilizacao do runtime AWS do login proprio
- Descricao: a correcao que impede o fallback indevido para `StubPublicAuthenticationGateway` ja esta em codigo, testada e publicada em imagem, mas a task `program-management-system:27` ainda nao estabilizou no ECS; ao fim do dia a task `:26` seguia atendendo o ALB em dev.
- Impacto: enquanto a task corrigida nao assumir o trafego com estabilidade, a homologacao real do login proprio fica bloqueada e o ambiente de dev pode continuar respondendo com comportamento de `stub`.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Homologacao real do login proprio com Cognito
- Descricao: o frontend ja migrou para login proprio consumindo os endpoints publicos do backend, o app client ja aceita `ALLOW_USER_PASSWORD_AUTH` e o Cognito respondeu corretamente a `initiate-auth` direta; ainda falta validar ponta a ponta em runtime corrigido os fluxos de login, primeiro acesso, reset por codigo, restore de sessao e logout global.
- Impacto: sem essa homologacao, ainda existe risco de divergencia entre o comportamento local, o runtime AWS e o app client real do Cognito.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Remocao do fallback de id_token
- Descricao: o `access_token` ja e a trilha principal e carrega claims de tenant; ainda falta decidir quando o fallback temporario de `id_token` pode ser removido com seguranca.
- Impacto: complexidade extra no frontend e risco de comportamento diferente entre chamadas.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Habilitacao operacional do app client para login proprio
- Descricao: o app client ja recebeu `ALLOW_USER_PASSWORD_AUTH` e `ALLOW_ADMIN_USER_PASSWORD_AUTH`, mas ainda precisa ser homologado em uso real para refresh token e logout global na UI propria do PMS.
- Impacto: a implementacao do frontend pode falhar em homologacao se ainda houver divergencia entre os flows habilitados e o comportamento real esperado no ambiente.
- Prioridade: P0
- Area: integracao
- Status: aberto

## Virada operacional do runtime AWS para documentos em S3
- Descricao: o codigo ja suporta `S3` real e o frontend ja envia o binario ao presigned URL, mas o principal operacional atual nao possui `s3:CreateBucket` nem `iam:PutRolePolicy` para concluir bucket e permissao da task role no ambiente AWS.
- Impacto: o runtime AWS segue preso ao provider `stub`, apesar de o fluxo real ja existir no codigo.
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

## Visibilidade administrativa do status de verificacao de email
- Descricao: a sessao autenticada ja enxerga `emailVerified`, mas o diretorio administrativo de usuarios ainda nao expoe o estado de verificacao de cada usuario gerenciado.
- Impacto: administradores ainda descobrem a falta de verificacao apenas ao tentar `reset-access` ou via console do Cognito.
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
