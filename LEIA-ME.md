# Treino e Nutrição: como publicar e instalar

O app é um site que se instala no celular como aplicativo. Você publica uma vez no GitHub Pages, que é gratuito, e as duas pessoas instalam pelo mesmo endereço. Cada celular guarda os próprios dados e a própria chave de IA. Um não vê o do outro.

## 1. Publicar no GitHub Pages (uma vez, pelo computador)

1. Crie uma conta em https://github.com, se ainda não tiver.
2. Clique em **New repository**. Dê o nome `treino`, marque **Public** e clique em **Create repository**.
3. Na página do repositório, clique em **uploading an existing file**.
4. Descompacte o .zip e arraste **todo o conteúdo da pasta** para a página: `index.html`, `manifest.webmanifest`, `sw.js` e a pasta `icons`. Arraste os arquivos, não a pasta de fora.
5. Clique em **Commit changes**.
6. Vá em **Settings › Pages**. Em *Branch*, escolha `main` e a pasta `/ (root)`. Clique em **Save**.
7. Espere 1 a 2 minutos. O endereço aparece no topo dessa mesma tela, no formato `https://SEU-USUARIO.github.io/treino/`.

O repositório público mostra apenas o código do app. Chaves e dados de saúde nunca vão para o GitHub. Eles ficam só no celular de cada pessoa.

## 2. Instalar no Android (cada pessoa, no seu celular)

1. Abra o endereço no **Chrome**.
2. Toque no menu ⋮ e em **Instalar app** (ou **Adicionar à tela inicial**).
3. Abra pelo ícone novo. Preencha o perfil e escolha a IA.

## 3. Criar a chave de IA

Escolha um provedor. A chave é de quem usa o celular, e o custo vai para a conta dessa pessoa.

| Provedor | Onde criar a chave | Cobrança |
|---|---|---|
| Google Gemini | https://aistudio.google.com/apikey | Tem faixa gratuita com limites |
| Anthropic Claude | https://console.anthropic.com/settings/keys | Crédito pré-pago, por uso |
| OpenAI | https://platform.openai.com/api-keys | Crédito pré-pago, por uso |

No app, toque em **Testar** antes de salvar. Se der "chave recusada", copie a chave de novo, inteira.

**Cuidados com a chave**
- Crie uma chave só para este app.
- No painel do provedor, defina um limite de gasto mensal.
- Se perder o celular, apague a chave no site do provedor.

## 4. Atualizar o app depois

Envie o `index.html` novo para o mesmo repositório (**Add file › Upload files**, substituindo o antigo). Os celulares pegam a versão nova na próxima vez que abrirem o app com internet. Os dados de cada pessoa continuam lá.

## 4b. App Android com dados do relógio (Conexão Saúde)

A versão instalada pelo Chrome não consegue ler o Conexão Saúde. Para ler sono, frequência cardíaca, passos e treinos do relógio, use o **app Android (APK)**, gerado pelo próprio GitHub a partir da pasta `android-app`.

- O APK abre o mesmo site do GitHub Pages; atualizações do `index.html` chegam sozinhas, sem reinstalar.
- O APK só precisa ser gerado de novo se a pasta `android-app` mudar.
- O segredo `KEYSTORE_PASSWORD` (em Settings › Secrets and variables › Actions) guarda a senha da chave que assina o app. Guarde essa senha: sem ela, uma versão nova do APK não instala por cima da antiga.
- Os dados do app Android ficam separados dos do Chrome. Para levar seus dados: no Chrome, Perfil › Salvar cópia; no APK, Perfil › Restaurar cópia. A chave de IA precisa ser colada de novo.

## 5. Senha e código de recuperação

Na primeira abertura, o app pede para criar uma senha. Ela criptografa perfil, exames, plano e chave de IA no celular. O app pede a senha ao abrir e depois de 5 minutos fora dele.

Ao criar a senha, o app gera um **código de recuperação** de 20 caracteres e abre seu e-mail com o código pronto para você mesmo. Envie e guarde esse e-mail. Se esquecer a senha, toque em **Esqueci a senha** e digite o código para criar uma senha nova sem perder nada.

Sem a senha e sem o código, ninguém consegue abrir os dados, nem você. Nesse caso, o único caminho é apagar tudo e restaurar uma cópia de segurança.

Em **Perfil › Senha e recuperação** dá para trocar a senha, bloquear o app na hora e gerar um código novo (o antigo deixa de valer).

## 6. Cópia de segurança

Os dados ficam só no celular. Em **Perfil › Salvar cópia**, o app baixa um arquivo `.json` com perfil, exames e plano (a chave não vai junto). Para trocar de celular, instale o app no novo aparelho e use **Restaurar cópia**.

## Se algo der errado

- **"O modelo informado não existe"**: os provedores aposentam modelos de tempos em tempos. Em **Configurar IA**, apague o campo Modelo para usar o padrão. Se o padrão também tiver saído, coloque o nome do modelo atual que aparece na página do provedor.
- **Leitura do laudo saiu errada**: prefira o PDF original baixado do site do laboratório, não foto nem PDF escaneado. Laudos do Laboratório Unimed Litoral são lidos direto pelo app, sem IA. Na bioimpedância da Pratique, o app lê o texto direto e usa a IA só para os 8 números que o relatório traz como imagem; sem IA, esses campos ficam em branco para preencher. Os de outros laboratórios em PDF digital vão como texto para a IA, e só os escaneados ou fotos vão como imagem. Confira na tela de revisão, corrija o valor ou use **Digitar os valores**.
- **Sem internet**: o app abre e mostra treino e dieta, mas não lê exames nem gera plano.

O plano é uma orientação geral com base nos dados informados. Ele não substitui médico ou nutricionista.
