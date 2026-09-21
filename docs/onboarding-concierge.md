# Onboarding de um novo cliente (concierge)

Roteiro para colocar um estabelecimento no ar com ajuda da equipe, do cadastro ao primeiro acesso. Vale enquanto o
cadastro é feito pelo `PLATFORM_ADMIN`; o autocadastro fica para depois.

## 0. Antes de começar (uma vez por ambiente)

- Backend com `APP_BASE_URL` (endereço público do front) e o SMTP configurado (`MAIL_HOST`, `MAIL_FROM`, ...). Sem
  isso o backend **não sobe** fora do perfil `dev`. Detalhes em `.env.example`.
- Cobrança combinada com o cliente (Pix/boleto): o sistema não cobra, só bloqueia (passo 6).

## 1. Criar o estabelecimento (admin)

Painel: **Cadastros > Estabelecimentos**. Informe nome, e-mail, telefone, CNPJ válido e o `valuePerPoint`
(quantos reais o cliente gasta para ganhar 1 ponto).

Via API: `POST /establishments` com o token do admin.

O estabelecimento nasce **em teste por 14 dias** (`TRIAL`). Isso pode ser ajustado no passo 6.

## 2. Convidar o dono (admin)

Painel: **Cadastros > Usuários**, papel *Dono do Estabelecimento*, escolha o estabelecimento e deixe marcado
**"Enviar convite por e-mail"**. Não peça senha: a pessoa recebe um e-mail com um link para **escolher a própria
senha** (vale 72 horas, uso único).

Via API: `POST /users` **sem** o campo `password`.

Se o e-mail não chegar (caixa de spam, endereço errado): corrija o e-mail em **Listagens > Usuários** e reenvie
pedindo "Esqueci minha senha" na tela de login (também gera um link novo). Cada novo link cancela o anterior.

### Alternativa: senha provisória

Desmarque o convite e informe uma senha provisória (mínimo 8 caracteres). Passe-a à pessoa por um canal seguro (nunca
por e-mail). No primeiro acesso o sistema **obriga** a trocá-la antes de qualquer outra coisa.

## 3. Primeiro acesso (dono)

1. Abre o link do e-mail, escolhe a senha e entra pelo login.
2. Em **Recompensas > Configurar Recompensas** escolhe como os pontos funcionam: catálogo de brindes, desconto na
   compra ou cashback (e o valor de cada ponto, se for desconto/cashback).
3. Se escolheu catálogo: **Recompensas > Catálogo de Prêmios** para cadastrar os brindes.
4. Em **Cadastros > Usuários** cria os funcionários (mesmo convite por e-mail).

## 4. Primeiro uso (funcionário)

Cadastrar cliente por CPF (**Cadastros > Clientes**), registrar compra (**Cadastros > Compras**) e, no modo catálogo,
trocar pontos (**Recompensas > Resgatar Brinde**). O funcionário não altera configurações nem cancela compras.

## 5. Checklist de conferência

- [ ] O dono entra e vê o próprio estabelecimento (e só ele).
- [ ] Um cliente de teste é cadastrado e uma compra gera pontos.
- [ ] O resgate (ou desconto) funciona no modo escolhido.
- [ ] O funcionário entra pelo celular e registra uma compra.
- [ ] O e-mail de "Esqueci minha senha" chega e o link funciona.

## 6. Situação comercial e cobrança manual

Painel do admin: **Listagens > Estabelecimentos > Plano** (ou `PUT /establishments/{id}/plan`).

| Situação | Efeito |
|---|---|
| **Em teste** (`TRIAL`) | Funciona até a data de fim. Depois dela o estabelecimento passa a valer como **suspenso** sozinho, sem ninguém precisar fazer nada. O dono vê uma faixa nos últimos 7 dias. |
| **Ativo** (`ACTIVE`) | Funciona normalmente. É o que você define ao fechar o contrato. |
| **Suspenso** (`SUSPENDED`) | Ninguém do estabelecimento entra (login dá 402) e as sessões abertas passam a ver "Acesso suspenso". Os dados ficam guardados. O admin da plataforma não é afetado. |

Rotina sugerida: ao receber o pagamento, marque **Ativo** e preencha o rótulo do plano; se atrasar, **Suspender agora**;
ao regularizar, **Reativar agora**. A mudança vale na próxima chamada, sem esperar o token expirar.

## 7. Problemas comuns

| Sintoma | Causa provável |
|---|---|
| "Este link é inválido, expirou ou já foi usado" | Passou o prazo (60 min no esqueci a senha, 72 h no convite), foi usado ou um link mais novo o substituiu. Peça outro. |
| "Muitos pedidos seguidos" ao pedir o link | Limite de 5 pedidos por 15 minutos para o mesmo e-mail e origem. Aguardar. |
| Login responde "acesso suspenso" | Situação Suspenso ou teste vencido: ver passo 6. |
| Backend não sobe e cita `MAIL_HOST`/`APP_BASE_URL` | Falta configurar o e-mail (passo 0). |
| E-mails não chegam em produção | Confira os logs do backend (`Failed to send email`): o envio falho não aparece para o usuário de propósito. |
