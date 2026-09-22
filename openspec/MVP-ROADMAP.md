# Roadmap do MVP (primeiro cliente pagante)

> **Estado atual e como retomar: [MVP-STATUS.md](MVP-STATUS.md).** Changes 01 a 05 feitos; faltam 06 e 07.

Origem: avaliação em `~/.claude/plans/mvp-primeiro-cliente.md`. Cada item abaixo é um change do OpenSpec em `openspec/changes/`, com `proposal.md` e `tasks.md`. Ainda não têm `design.md` nem `specs/`; isso é gerado ao rodar `/opsx:propose` (ou o fluxo equivalente) em cada change, antes de `/opsx:apply`.

Tags nas tarefas: `[BACK]` api-points-back, `[FRONT]` points-back-front, `[INFRA]` deploy/operação, `[LEGAL]` documentos jurídicos.

## Ordem de execução

| # | Change | Depende de | Pode rodar em paralelo com |
|---|--------|------------|----------------------------|
| 1 | `mvp-01-secure-foundation` | — | 4 |
| 2 | `mvp-02-client-identity` | 1 | 4, 5 |
| 3 | `mvp-03-points-ledger-rewards` | 1, 2 | 4, 5 |
| 4 | `mvp-04-responsive-frontend` | — (go-live exige) | 1, 2, 3 |
| 5 | `mvp-05-account-lifecycle` | 1 | 2, 3 |
| 6 | `mvp-06-production-deploy` | 1 (go-live exige 4) | 3, 5 |
| 7 | `mvp-07-lgpd-legal` | 2 | 3, 5, 6 |

O texto jurídico (`mvp-07` 1.1 e 1.2) não depende de código: comece cedo, porque a revisão por advogado leva tempo.

## Marcos

1. **Seguro:** `mvp-01` concluído. O backend pode ser exposto sem backdoor nem 500 por entrada inválida.
2. **Produto completo:** `mvp-02` e `mvp-03` concluídos. Fluxo por CPF, compra, resgate e extrato funcionando.
3. **Usável no balcão:** `mvp-04` e `mvp-05`. Celular, tablet e PC, com recuperação de senha e status do estabelecimento.
4. **Go-live:** `mvp-06` e `mvp-07`. Produção com HTTPS, backup e termos aceitos.

## Fora do MVP

Cobrança automática, landing page, autocadastro, expiração de pontos, campanhas, relatórios e exportação, visibilidade completa de clientes compartilhados no grupo (`mvp-02` 2.5 é opcional), refresh token, OpenAPI, i18n, analytics.

## Decisões já tomadas (guiam as tarefas)

- Recompensa por estabelecimento: cashback, desconto ou brindes/cupons, um modo ativo por vez.
- CPF identifica a pessoa; uma pessoa pode ter conta em vários estabelecimentos, cada uma com saldo próprio. Grupos podem compartilhar clientes, com opção de não compartilhar.
- Hospedagem: gratuita na validação (conferir as regras atuais do free tier da AWS), paga e barata na venda, tudo em Docker para poder migrar.
- Uso em PC, tablet e celular.

## Riscos a acompanhar

- `mvp-03` é o maior item, por causa dos três modos de recompensa.
- A migração do `mvp-02` mexe em dados existentes: fazer backup e validar saldos antes e depois.
- Free tier da AWS pode ter prazo ou virar créditos: anotar a data de expiração (`mvp-06` 3.1).
