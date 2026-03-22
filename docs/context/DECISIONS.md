# Decisions

Ultima atualizacao: `2026-03-22`

## Cognito como autenticacao oficial
- Decisao: o backend nao implementa login por senha local e valida apenas JWT emitido pelo Cognito.
- Motivo: manter autenticacao stateless e centralizar identidade fora da aplicacao.
- Impacto: frontend depende do Hosted UI e o backend opera como Resource Server.
- Status: ATIVA

## Monolito modular em vez de microservicos agora
- Decisao: manter uma unica aplicacao Spring Boot e um unico deploy, organizados em modulos.
- Motivo: reduzir complexidade operacional e preparar futuras extracoes sem quebrar o produto agora.
- Impacto: fronteiras sao protegidas por pacotes e portas internas, nao por rede.
- Status: ATIVA

## Tenant e modelado por Organizacao
- Decisao: `tenant` representa uma empresa e e modelado por `Organizacao`.
- Motivo: o dominio real do produto e multiempresa, nao multi-tenant abstrato.
- Impacto: usuarios, portfolio e visibilidade passam a depender da hierarquia organizacional.
- Status: ATIVA

## Customer raiz so por ADMIN INTERNAL
- Decisao: `ADMIN INTERNAL` cria customer raiz; `ADMIN` externo cria apenas descendentes na propria subarvore.
- Motivo: separar governanca de plataforma da administracao operacional de clientes e fornecedores.
- Impacto: a API de organizacoes trata create root e create child com regras diferentes.
- Status: ATIVA

## Contrato de users orientado a organizationId
- Decisao: o contrato principal de usuarios usa `organizationId` e `organizationName`.
- Motivo: refletir o modelo de negocio real e esconder detalhes tecnicos de tenant da UX principal.
- Impacto: `tenant_id` e `tenant_type` ficam apenas como compatibilidade interna.
- Status: ATIVA

## Fallback temporario de id_token no frontend
- Decisao: o frontend tenta `access_token` primeiro e pode repetir uma vez com `id_token` em caso de `403`.
- Motivo: reduzir bloqueio de integracao enquanto a trilha real de claims/autorizacao e homologada ponta a ponta.
- Impacto: existe codigo temporario no frontend que precisara ser removido quando a trilha principal estiver estavel.
- Status: TEMPORARIA

## Storage de documentos configuravel por provider
- Decisao: usar provider configuravel para documentos, com `stub` em local/testes e S3 como alvo definitivo.
- Motivo: permitir evolucao do contrato sem bloquear o produto enquanto o bucket final nao esta pronto.
- Impacto: o fluxo de documentos ja existe no contrato, mas a homologacao em S3 real permanece aberta.
- Status: TEMPORARIA

## Portfolio opera apenas sobre organizacoes EXTERNAL
- Decisao: estruturas `INTERNAL` ficam fora do diretorio funcional do portfolio.
- Motivo: separar governanca da plataforma do portfolio operacional dos clientes.
- Impacto: `internal-core` nao aparece como owner de programa nem como organizacao funcional do portfolio.
- Status: ATIVA

## Purge como excecao operacional explicita
- Decisao: purge de usuario e purge-subtree de organizacao existem como operacoes separadas de inativacao logica.
- Motivo: limpeza de massa de teste e saneamento controlado sem perder a regra geral de soft delete.
- Impacto: exigem `SUPPORT`, override explicito e justificativa.
- Status: ATIVA
