# Breakout 2D — diário de conceitos

O que cada etapa ensinou, na ordem em que foi construída. Leia com o código aberto ao lado.

## Como rodar

```bash
./gradlew run         # joga
./gradlew test        # 17 testes, sem abrir janela
./gradlew packageApp  # executavel autocontido em build/jpackage/
```

Controles: setas ou `A`/`D` ou mouse movem o paddle · `ESPAÇO` ou clique saca ·
`P` pausa · `ESC` volta ao menu.

---

## 0 — O loop de jogo

`DesktopLauncher` cria um `Lwjgl3Application`; o libGDX chama `render()` o tempo todo,
tipicamente 60 vezes por segundo. Não existe "esperar input": todo frame lê o estado dos
botões, atualiza o mundo e redesenha tudo do zero.

A diferença mental em relação a uma app normal: **nada é persistente na tela**. Se você
parar de desenhar o paddle, ele some no frame seguinte.

## 1 — Unidades de mundo e viewport

O mundo tem 100×75 unidades (`Rules`). O `FitViewport` mapeia isso para a janela e cria as
barras pretas quando a proporção não bate — por isso a screenshot tem tarjas em cima e
embaixo numa janela 1280×960 para um mundo 4:3.

Por que não usar pixels direto: o jogo mudaria de dificuldade conforme o monitor. Em
unidades de mundo, a bola anda 46 unidades por segundo em qualquer resolução.

`resize()` existe e precisa ser repassado (`game.resizeViewports`), senão o mundo fica
esticado ao redimensionar.

## 2 — Delta time

`paddle.move(direction, delta)` multiplica a velocidade pelo tempo do frame. Se você
somar um valor fixo por frame, o jogo fica mais rápido em máquinas rápidas — o bug
clássico dos jogos de DOS.

Para conferir: descomente o bloco de 15 FPS em `DesktopLauncher`. O jogo fica travado,
mas a bola leva o mesmo tempo para atravessar a tela.

Há um teto: `MAX_DELTA = 1/20s` em `GameScreen`. Sem ele, um engasgo de 2 segundos (ou
arrastar a janela) teleportaria a bola para fora do mundo.

## 3 — Vetores e reflexão

`Ball.velocity` é um `Vector2`: direção e intensidade juntas. Quicar numa parede é
inverter uma componente — `velocity.x = -velocity.x` na lateral, `.y` no teto.

`nor()` (normalizar) e `scl()` (escalar) separam "para onde" de "quão rápido". É por isso
que `launch()` consegue sortear uma direção e aplicar a velocidade inicial depois.

Não há parede embaixo: é por ali que a bola se perde. Isso é regra de jogo, não física.

## 4 — Colisão AABB e o rebote que faz o jogo existir

`Rectangle.overlaps` resolve a detecção. A **resposta** é o que importa:

- **Tijolo** (`Collisions.resolveAxis`): invertemos o eixo de menor penetração. Inverter
  sempre Y faz a bola atravessar tijolos pela quina.
- **Paddle** (`Collisions.bounceOffPaddle`): aqui a física é abandonada de propósito. O
  ponto de impacto vira ângulo de saída — centro devolve reto, pontas devolvem a 60°.
  Reflexão "correta" deixa a bola presa em ângulos rasos e o jogador sem controle.
  O resultado é sempre para cima, então a bola nunca gruda no paddle.

Ambos estão em `core/Collisions.java`, sem OpenGL no caminho, e por isso são testados
sem abrir janela. **Essa é a regra geral: lógica testável não mora na Screen.**

### Tunneling

A 46 unidades/s e 60 FPS a bola anda 0,77 unidade por frame. Num frame de 100 ms ela anda
4,6 e pula por cima do paddle (2 de altura) sem encostar. `Collisions.substeps` divide o
movimento do frame em passos menores que o menor obstáculo. Em frame normal retorna 1, ou
seja, custo zero quando não precisa.

## 5 — Dados fora do código

`assets/levels/*.txt` são grades de 14 colunas; `Level.load` as converte em tijolos.
Editar uma fase não exige recompilar nada, e quem desenha a fase não precisa programar.

**Bug real que o teste pegou aqui:** linhas de comentário `#` eram puladas, mas o índice
da fileira continuava avançando — o nível inteiro nascia duas fileiras mais abaixo. A
correção foi separar `gridRow` (fileiras reais) do índice do laço. Sem `LevelTest` isso
teria passado por "ah, o nível é assim mesmo".

Remoção durante iteração: `collideWithBricks` trata **um** tijolo por substep e sai do
laço, porque `removeIndex` invalidaria o índice corrente.

## 6 — Telas e ciclo de vida

`Game` + `Screen` é a máquina de estados: `MenuScreen` → `GameScreen` → `GameOverScreen`.
O estado que sobrevive à troca (pontos, vidas, nível) mora em `BreakoutGame`, não na tela.

`dispose()` **não** é chamado automaticamente ao trocar de tela — por isso cada
`setScreen` aqui vem seguido de `dispose()` explícito. Recursos caros (`SpriteBatch`,
`ShapeRenderer`, `BitmapFont`) são um só para o jogo inteiro, criados no `create()`.

### Dois viewports

Apareceu antes do previsto, por causa do texto. Mundo e interface vivem em escalas
diferentes: o mundo em unidades (100×75), o HUD em pixels (`ScreenViewport`). Texto
desenhado em unidades de mundo sai gigante e borrado. Daí os dois `apply()` no `draw()`.

---

## 7 — Assets de verdade

Os retângulos coloridos viraram sprites, e a fonte embutida virou FreeType.

### Os assets são gerados, não desenhados

`tools/generate_assets.py` produz o atlas (PNG + `.atlas`) e os seis WAV, sem nenhuma
dependência externa: PNG escrito na mão com `zlib`, WAV com o módulo `wave`. Rodar de novo
é sempre seguro.

Isso resolve um problema real de laboratório — não ter artista à mão — e de quebra deixa a
arte **reproduzível**: a fonte da verdade é o script, não um `.png` que ninguém sabe de
onde veio. Dois detalhes que importaram:

- O atlas é desenhado em 4× e reduzido no fim (supersampling). Sem isso, bordas
  arredondadas ficam serrilhadas.
- Na redução, as cores são **pré-multiplicadas pelo alpha**. Sem isso a média puxa a cor
  dos pixels transparentes para dentro da borda e aparece uma franja escura.
- O brilho do sprite **clareia o que já está desenhado**, não pinta branco por cima.
  Pintar por cima é o que produz aquela tarja chapada no topo — foi o primeiro resultado,
  e dá para ver a diferença olhando as duas versões do atlas.

### AssetManager

Por que ele e não `new Texture(...)` espalhado: deduplica (o mesmo arquivo pedido duas
vezes é o mesmo objeto, não duas texturas na GPU), centraliza o `dispose()` e abre o
caminho para carregar em background com tela de loading. Aqui os assets são minúsculos,
então `finishLoading()` bloqueia e pronto — mas o caminho está aberto.

### Por que um atlas e não cinco PNGs

Cada troca de textura força o `SpriteBatch` a fazer *flush* — mandar o que estava
acumulado para a GPU e começar de novo. Com tudo numa imagem só, o jogo inteiro sai em
praticamente uma chamada de desenho.

### FreeType

`gdx-freetype` gera a `BitmapFont` a partir de um `.ttf` **no tamanho pedido**. A
alternativa (pegar a fonte embutida e escalar) borra. Com fonte de verdade também ficou
possível centralizar texto direito: `GlyphLayout` mede a string antes de desenhar.

Foi exatamente o que faltava no HUD: o rótulo "PADDLE LARGO" estava posicionado por um
offset chutado (`largura - 150`) e saía cortado como "PADDLE LARG". Fonte de largura
variável não perdoa palpite.

## 8 — Juice

Quatro efeitos pequenos que, juntos, mudam a sensação do jogo. Nenhum muda uma regra.

### Som

Seis WAV sintetizados: onda quadrada com envelope exponencial — ataque instantâneo, queda
rápida. É literalmente como soavam os arcades, e cabe em 5 KB.

### Partículas

`core/Particles.java` é um sistema próprio, não o `ParticleEffect` do libGDX. O
`ParticleEffect` lê um `.p` feito no editor gráfico; para um estilhaço de tijolo isso é
mais ferramenta do que o problema pede, e atrás do editor fica invisível o que uma
partícula realmente é: **posição, velocidade, tempo de vida e uma cor que some**. São
quatro arrays.

O detalhe que importa: arrays de tamanho fixo e *swap-remove* (a partícula morta é trocada
com a última viva). Nada é alocado durante a partida, então o GC não tem motivo para rodar
no meio de uma jogada.

### Screen shake

Desloca a câmera **do mundo** por 0,18 s, com amplitude menor que uma unidade. Dois
cuidados: exagerar vira enjoo em vez de impacto, e o HUD tem câmera própria — se o placar
tremer junto, a tela parece quebrada em vez de impactada.

### Power-up

Cápsula que cai de 14% dos tijolos e alarga o paddle por 12 s. O cuidado está em
`Paddle.resizeKeepingCenter`: mexer só na largura faria o paddle crescer para a direita e
saltar sob o dedo do jogador justamente no instante em que ele precisa de controle.

`PaddleTest` cobre isso sem abrir janela — inclusive o caso de pegar o power-up colado na
borda, que não pode empurrar o paddle para fora do mundo.

## 9 — Distribuição

```bash
./gradlew installDist   # pasta com scripts, precisa de Java instalado
./gradlew packageApp    # app autocontido com JRE embutido (~180 MB)
```

Duas coisas precisaram ser resolvidas:

**Assets no classpath.** `Gdx.files.internal` procura primeiro no diretório atual e depois
no classpath. Rodando por `./gradlew run` o diretório atual é `assets/` e tudo funciona —
mas o app instalado é executado de qualquer pasta. Colocando `assets/` também como
`resources` do jar, os níveis passam a viajar dentro dele.

**Class-Path no manifesto.** Sem ele, `jpackage` monta um app que abre e morre em
`NoClassDefFoundError`: ele constrói o classpath a partir do manifesto do jar principal,
não do script do Gradle. De brinde, `java -jar lib/breakout-2d.jar` passou a funcionar.

Detalhe de Gradle 9: a task não pode tocar em `project` durante a execução (`delete(...)`,
`project.name`) — isso quebra o configuration cache e será erro no Gradle 10. Tudo que vem
do `Project` é resolvido na configuração, fora do bloco da task.

---

## Depois das 9 etapas — paredes laterais

As bordas já existiam na colisão, mas eram invisíveis: a bola quicava no nada. Dar corpo
a elas trouxe três decisões que valem mais que o efeito visual.

### Parede que ocupa espaço, não que é pintada por cima

O caminho curto seria desenhar duas barras sobre uma área de jogo que continua indo de 0 a
`WORLD_WIDTH`. O resultado seriam tijolos escondidos atrás da parede e uma bola quicando
num lugar diferente de onde a parede aparece.

Em vez disso, a área jogável passou a ser **o que sobra** entre as duas paredes:

```java
PLAY_LEFT  = WALL_THICKNESS;
PLAY_RIGHT = WORLD_WIDTH - WALL_THICKNESS;
BRICK_WIDTH = PLAY_WIDTH / BRICK_COLUMNS;   // derivada, não escolhida
```

`BRICK_WIDTH` deixou de ser um número escolhido a mão (`7f`) e passou a sair da conta.
Mudar `WALL_THICKNESS` reacomoda as três fases sozinho, sem reeditar nenhum `.txt`. Um
teste trava isso: as 14 colunas têm que preencher exatamente o vão.

### Paddle e bola precisam concordar sobre onde é a parede

Havia uma inconsistência antiga no código: o paddle parava em `WALL_WIDTH = 1` e a bola
quicava em `0`. Sobrava uma faixa de uma unidade junto a cada borda por onde a bola passava
**por baixo** do paddle. Agora os dois usam `PLAY_LEFT`/`PLAY_RIGHT`, e um teste verifica
que o paddle encosta exatamente no limite — nem antes, nem depois.

É o tipo de bug que some sozinho quando o mesmo valor tem um nome só.

### Ordem de desenho e os dois sistemas de coordenadas

As paredes são desenhadas **depois** das partículas: estilhaço que escapa da área de jogo
passa por trás delas, não por cima. E fazem parte do mundo, não do HUD — tremem junto no
screen shake, que é justamente o que dá a impressão de que a bola bateu em algo sólido.

O HUD, porém, vive em pixels. Uma margem fixa de 14 pixels escrevia o placar em cima da
parede, e em outra resolução escreveria longe dela. A correção usa `project()`, a
contraparte exata do `unproject()` já usado para o mouse: converte a borda da área de jogo
em pixel de janela, levando em conta até as tarjas pretas do `FitViewport`.

O sprite da parede é simétrico de propósito — a mesma região do atlas serve dos dois lados,
sem espelhar — e não tem nenhum detalhe que dependa da altura: ele é esticado de 64 px para
75 unidades de mundo, e qualquer marca horizontal viraria uma faixa gorda.

---

## Etapas concluídas

Todas as 9, mais as paredes laterais. O jogo está completo dentro do escopo do laboratório: loop, física, níveis em
arquivo, telas, assets, áudio, partículas, power-up e distribuição.

Se for continuar, os caminhos naturais são mais níveis (é só criar `04.txt`), mais
power-ups (bola lenta, multi-bola, paddle com tiro) e um placar persistente com
`Preferences`.
