# Checklist Manual de Produção para Venda

Use este arquivo para validar o ambiente de produção antes de liberar venda/piloto pago.
Marque cada item como `[OK]`, `[NOK]` ou `[NA]`, e adicione observações quando necessário.

Formato sugerido:

```text
[OK] item validado
[NOK] item com problema - observação: ...
[NA] não aplicável agora - motivo: ...
```

## Pré-Teste

- [ ] Confirmar que frontend produção abre via HTTPS sem erro de certificado.
- [ ] Confirmar que backend produção responde em `/actuator/health/readiness`.
- [ ] Confirmar que backend produção responde em `/actuator/health/liveness`.
- [ ] Confirmar variáveis de produção: API URL, Cognito, S3, CORS, domínio e redirects.
- [ ] Criar usuários de teste com papéis diferentes: `ADMIN`, `SUPPORT`, gestor de projeto, responsável, aprovador e viewer.
- [ ] Garantir que dados criados no teste têm prefixo identificável, exemplo `PROD-SMOKE`.

## Autenticação

- [OK] Login com usuário válido.
- [OK] Logout e novo login.
- [ ] Tentativa de login com senha errada.
- [ ] Sessão expirada ou token inválido redireciona corretamente.
- [ ] Usuário sem e-mail verificado vê banner/fluxo de verificação.
- [ ] Envio de código de verificação funciona.
- [ ] Confirmação de código funciona.
- [ ] Usuário sem permissão não acessa rotas administrativas.

## Contexto Ativo

- [ ] Usuário com uma membership carrega contexto automaticamente.
- [ ] Usuário com múltiplas memberships consegue trocar contexto.
- [ ] Após trocar contexto, menus e dados mudam corretamente.
- [ ] Recarregar a página mantém o contexto ativo esperado.
- [ ] A API recebe `X-Access-Context` nas chamadas protegidas.
- [ ] Usuário não consegue acessar dados de organização/tenant fora do contexto.

## Workspace e Navegação

- [ ] Home carrega sem erro.
- [ ] Workspace carrega dados da sessão.
- [ ] Menu mostra apenas áreas permitidas para o usuário.
- [ ] Recarregar diretamente uma rota profunda funciona, exemplo `/workspace/projects/...`.
- [ ] Botão voltar/avançar do navegador não quebra estado crítico.
- [ ] Chunks lazy carregam sem erro em Projects, Templates, Organizations, Users, Markets, Operations e Session.

## Organizações

- [ ] Criar organização com dados válidos.
- [ ] Validar erro para CNPJ inválido/duplicado, se aplicável.
- [ ] Editar nome/dados principais da organização.
- [ ] Inativar organização permitida.
- [ ] Criar relacionamento entre organizações.
- [ ] Editar relacionamento.
- [ ] Inativar relacionamento.
- [ ] Validar que campos legados não aparecem no fluxo.
- [ ] Usuário sem permissão não consegue criar/editar/inativar.
- [ ] Export workflow: solicitar exportação.
- [ ] Export workflow: concluir exportação.
- [ ] Bloquear transições inválidas de exportação.

## Usuários

- [ ] Criar usuário com membership inicial.
- [ ] Validar convite/e-mail, se integrado.
- [ ] Editar dados básicos do usuário.
- [ ] Adicionar nova membership.
- [ ] Inativar membership.
- [ ] Resetar acesso.
- [ ] Reenviar convite.
- [ ] Listar usuários por organização/contexto.
- [ ] Usuário comum não acessa administração de usuários.
- [ ] Fluxo de usuário órfão aparece apenas para quem pode reparar.
- [ ] Bootstrap membership funciona para usuário órfão controlado.
- [ ] Purge de usuário exige confirmação e justificativa.

## Mercados e Tenants

- [ ] Listar tenants visíveis.
- [ ] Criar mercado em tenant permitido.
- [ ] Editar mercado, se disponível.
- [ ] Trocar service tier com justificativa.
- [ ] Usuário sem permissão não acessa mercado/service tier.
- [ ] Validação de justificativa obrigatória em operação sensível.

## Templates

- [ ] Listar templates autorizados no contexto ativo.
- [ ] Criar template de projeto.
- [ ] Editar template de projeto.
- [ ] Criar fases do template.
- [ ] Criar milestones do template.
- [ ] Criar deliverables do template.
- [ ] Marcar deliverable com `requiredDocument`.
- [ ] Criar structure template.
- [ ] Criar níveis da estrutura.
- [ ] Reordenar níveis.
- [ ] Ativar/deativar structure template.
- [ ] Purge de template exige permissão adequada.
- [ ] Usuário não autorizado não vê templates fora do escopo.

## Criação de Projeto

- [ ] Criar projeto sem template explícito, usando default autorizado.
- [ ] Criar projeto selecionando template específico.
- [ ] Validar erro de código duplicado.
- [ ] Validar erro quando template não pertence ao framework.
- [ ] Validar datas planejadas.
- [ ] Confirmar organização líder vem do contexto ativo.
- [ ] Confirmar organização cliente correta.
- [ ] Abrir detalhe do projeto recém-criado.
- [ ] Recarregar detalhe do projeto e confirmar persistência.

## Projeto — Detalhe

- [ ] Dashboard do projeto carrega contadores.
- [ ] Lista de milestones carrega.
- [ ] Lista de deliverables carrega.
- [ ] Participantes carregam.
- [ ] Documentos gerais do projeto carregam.
- [ ] Editar projeto com sucesso.
- [ ] Customizar milestones/deliverables do projeto criado por template e confirmar que o template original não foi alterado.
- [ ] Incluir milestone e deliverable runtime no projeto e confirmar que o template original não foi alterado.
- [ ] Remover deliverable/milestone runtime sem histórico e validar bloqueio quando houver dependências; usar `WAIVED` quando precisar preservar histórico.
- [ ] Simular concorrência: editar com versão antiga retorna mensagem amigável.
- [ ] Usuário viewer consegue visualizar mas não editar.
- [ ] Usuário fora do projeto não acessa detalhe.

## Estrutura Runtime

- [ ] Estrutura do projeto carrega.
- [ ] Criar nó filho.
- [ ] Editar nó.
- [ ] Mover nó.
- [ ] Selecionar nó filtra dashboard/milestones/deliverables.
- [ ] Recarregar página mantendo `structureNodeId` na URL.
- [ ] Usuário sem permissão não consegue gerenciar estrutura.
- [ ] Erro de estrutura aparece de forma compreensível.

## Milestones

- [ ] Editar status de milestone.
- [ ] Editar datas, se disponível.
- [ ] Validar versionamento otimista.
- [ ] Confirmar que dashboard reflete alteração.
- [ ] Filtrar milestones por nó de estrutura.
- [ ] Usuário sem permissão não consegue editar.

## Deliverables

- [ ] Abrir detalhe de deliverable.
- [ ] Editar responsável.
- [ ] Editar aprovador.
- [ ] Editar status.
- [ ] Editar prazo planejado.
- [ ] Validar versionamento otimista.
- [ ] Confirmar visibilidade por papel: gestor, responsável, aprovador, viewer.
- [ ] Confirmar deliverable escondido não abre para usuário sem permissão.
- [ ] Confirmar pendências aparecem no inbox/pending review.

## Submissões

- [ ] Criar submissão sem documento.
- [ ] Confirmar payload aceito com `documentIds: []`.
- [ ] Abrir detalhe da submissão criada.
- [ ] Confirmar status inicial `SUBMITTED`.
- [ ] Confirmar deliverable muda para status submetido.
- [ ] Tentar criar segunda submissão aberta para o mesmo deliverable e validar bloqueio.
- [ ] Aprovar submissão.
- [ ] Rejeitar submissão com comentário.
- [ ] Validar versionamento em approve/reject.
- [ ] Confirmar usuário responsável não aprova se não tiver permissão.
- [ ] Confirmar aprovador consegue revisar.
- [ ] Confirmar histórico de submissões aparece ordenado.

## Documentos

- [ ] Upload de PDF válido em contexto `PROJECT`.
- [ ] Upload de documento válido em contexto `PROJECT_DELIVERABLE_SUBMISSION`.
- [ ] Finalização de upload ativa documento.
- [ ] Download abre link assinado.
- [ ] Remoção lógica remove documento da lista ativa.
- [ ] Upload de arquivo vazio falha.
- [ ] Upload de arquivo maior que 25 MB falha.
- [ ] Upload de extensão bloqueada falha: `.exe`, `.zip`, `.svg`, `.dwg`.
- [ ] Upload com double extension bloqueada falha, exemplo `file.pdf.exe`.
- [ ] Upload com content type incompatível falha.
- [ ] Documento de outro contexto não pode ser vinculado à submissão.
- [ ] Documento pendente não pode ser usado.
- [ ] Usuário sem permissão não faz upload/download/delete.
- [ ] Recarregar tela após upload mantém documentos visíveis.

## Operações

- [ ] Dashboard operacional carrega métricas.
- [ ] Drill-down abre dados detalhados.
- [ ] Filtros funcionam.
- [ ] Dados respeitam escopo/permissão.
- [ ] Usuário sem permissão não vê área operacional.
- [ ] Erros de API aparecem de forma clara.

## Erros e Resiliência

- [ ] `401` remove sessão/redireciona corretamente.
- [ ] `403` mostra mensagem clara.
- [ ] `409` mostra mensagem de concorrência amigável.
- [ ] `429` mostra mensagem de muitas tentativas.
- [ ] Backend indisponível mostra erro acionável.
- [ ] CORS incorreto não acontece em produção.
- [ ] Correlation ID aparece em erros quando backend retorna.
- [ ] Refresh da página não perde fluxo essencial.
- [ ] Navegação com internet lenta não gera tela branca.

## Segurança Manual

- [ ] Usuário externo não acessa admin interno.
- [ ] Usuário de uma organização não vê dados de outra indevidamente.
- [ ] Troca manual de IDs na URL não expõe dados proibidos.
- [ ] Botões proibidos não aparecem para papéis sem permissão.
- [ ] Mesmo chamando ação pela UI/URL, backend nega operação proibida.
- [ ] Operações destrutivas exigem confirmação/justificativa.
- [ ] Nenhum token aparece em tela, URL ou logs do navegador.
- [ ] Console do navegador não mostra erros sensíveis.

## Performance

- [ ] Primeira carga em aba anônima é aceitável.
- [ ] Navegação para Projects carrega chunk sob demanda.
- [ ] Navegação para Templates/Users/Operations não quebra chunk lazy.
- [ ] Recarregar rota profunda não retorna 404.
- [ ] DevTools Network não mostra chunks faltando.
- [ ] Upload/download não travam a UI.
- [ ] Listas principais continuam responsivas com volume razoável.

## Deploy e Cache

- [ ] Novo deploy invalida assets antigos corretamente.
- [ ] Assets com hash têm cache longo.
- [ ] `index.html` não fica preso em cache antigo.
- [ ] SPA fallback funciona para todas as rotas.
- [ ] Não há erro de chunk antigo após deploy.
- [ ] Rollback do frontend é possível.
- [ ] Rollback do backend é possível.
- [ ] Versão frontend/backend implantadas são rastreáveis.

## Auditoria e Operação

- [ ] Ações sensíveis geram audit log.
- [ ] Justificativas aparecem no audit log.
- [ ] Logs possuem correlation ID.
- [ ] Erros 4xx/5xx aparecem em logs/monitoramento.
- [ ] Health checks estão monitorados.
- [ ] Alertas existem para backend down, erro 5xx elevado, falha de DB e falha de storage.
- [ ] Backup do banco está ativo.
- [ ] Restore foi testado pelo menos uma vez em ambiente não produtivo.
- [ ] Bucket/documentos têm política correta de acesso.

## Comercial e Onboarding

- [ ] Criar tenant/organização de cliente piloto.
- [ ] Criar primeiro admin do cliente.
- [ ] Cliente consegue logar sem ajuda técnica.
- [ ] Cliente entende contexto ativo.
- [ ] Cliente consegue criar projeto usando template.
- [ ] Cliente consegue submeter/aprovar deliverable.
- [ ] Mensagens principais estão compreensíveis para usuário não técnico.
- [ ] Termos de uso/política de privacidade estão disponíveis, se aplicável.
- [ ] Canal de suporte e procedimento de incidente definidos.

## Critério de Go/No-Go

- [ ] Nenhum bloqueador em login, contexto, projeto, submissão, documento ou aprovação.
- [ ] Nenhum vazamento de dados entre tenants/organizações.
- [ ] Nenhum erro 500 em fluxo principal.
- [ ] Upload/download funcionam em produção.
- [ ] SPA fallback/cache funcionam após deploy.
- [ ] Existe plano de rollback.
- [ ] Existe forma de suporte acessar correlation ID/logs.
- [ ] Cliente piloto consegue completar o fluxo sem intervenção técnica.

## Resultado Final

- Status geral: `[ ] GO` `[ ] GO COM RESSALVAS` `[ ] NO-GO`
- Responsável pela validação:
- Data:
- Ambiente/domínio testado:
- Versão frontend:
- Versão backend:
- Observações:
