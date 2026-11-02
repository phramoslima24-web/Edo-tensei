# Narutoto - Edo Tensei Addon (Forge 1.20.1)

Addon para o mod **Narutoto 0.4.1** (Forge 1.20.1). Nao precisa do jar do Narutoto para compilar:
ele so e necessario para **rodar** (dependencia obrigatoria no `mods.toml`).

## Compilar pelo GitHub
1. Crie um repositorio e envie todo o conteudo desta pasta (`git add . && git commit && git push`).
2. Va na aba **Actions** -> workflow **Build mod** -> baixe o artefato `edotensei-addon` (o `.jar`).
3. Coloque o jar em `mods/` junto com o `narutoto` (e as dependencias dele: GeckoLib, etc).

(Se o build reclamar da versao do Gradle, troque `gradle-version: 8.8` por `8.1.1` em `.github/workflows/build.yml`.)

## O que tem
- **Jutsus Proibidos**: botao no canto superior direito das telas `JutsuBoard`/`OtherBoard` do Narutoto
  (ou use o comando `/jutsusproibidos`). Contem **Shiki Fujin** (40 niveis de XP) e **Edo Tensei** (30 niveis).
- **Shiki Fujin**: clique direito mata o alvo na mira (ate 12 blocos). Custo: vida cai para 2 coracoes, fraqueza/lentidao/fome, recarga de 5 min.
- **Edo Tensei**: aprender entrega o **Pergaminho do Edo Tensei**.
- **Edo Tensei em qualquer mob do Narutoto**: ao morrer, todo NPC/ninja/bandido/bijuu do Narutoto (menos dummies, clones e projéteis) deixa o corpo no chão (desenhado deitado, 10 min). A seringa coleta o sangue dele, e o ritual traz esse mob de volta obedecendo você.
- **Hokages 1, 2 e 3** (Hashirama, Tobirama, Hiruzen): nascem vivos pelo mundo (spawn raro, em qualquer bioma, como os NPCs do Narutoto)
  e tambem tem ovos de spawn. Ao morrer deixam o **corpo no chao por 10 minutos**.
- **Seringa** (craftavel: pepita de ferro / vidraca / barra de ferro na vertical): clique direito no corpo de um Hokage = **seringa com sangue**.
- **Papel do Edo Tensei** (craftavel: papel + saco de tinta + redstone -> 2 papeis).
- **Selar um mob**: kunai do Narutoto numa mao + papel na outra, clique direito no mob -> ele fica imobilizado (e brilha).
- **Pergaminho** (bloco no chao): ele aponta para a direcao em que voce olhava ao colocar.
  Clique direito com a seringa de sangue = guarda o sangue.
  **Shift + clique direito com as duas maos vazias** com o mob selado na frente do pergaminho (ate 12 blocos) = o mob morre e o Hokage aparece no lugar,
  **obedece e protege** quem invocou. Precisa ter aprendido o Edo Tensei.

## Habilidades dos Hokages (novo)
- **Hashirama**: Mokuton (raizes prendem e ferem o alvo, onda de madeira em volta), regeneracao com pouca vida e **cura o invocador** que estiver por perto.
- **Tobirama**: Hiraishin (aparece atras do alvo atacando) e Suiton (onda de agua que arremessa e deixa lento).
- **Hiruzen**: Katon (leque de bolas de fogo) e Kawarimi (com pouca vida troca de lugar e evita o golpe).

## Observacoes
- Os Hokages usam o modelo humanoide padrao do Minecraft e skins dos Hokages em `assets/edotensei/textures/entity/` (hashirama, tobirama, hiruzen).
- `data/forge/tags/entity_types/konoha_ninja.json` adiciona os Hokages a tag de ninjas de Konoha do Narutoto; apague o arquivo se nao quiser.
- Hokages so nascem em chunks **novos** (como animais).
