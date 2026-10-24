package labs.breakout.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.badlogic.gdx.math.Rectangle;
import labs.breakout.core.Rules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Valida os arquivos de nivel de verdade, os mesmos que o jogo carrega.
 *
 * Nao abre janela: basta um Gdx.files, que o backend headless fornece. E a rede de
 * seguranca para editar fases a mao - um nivel com linha torta falha aqui, nao no jogo.
 */
class LevelTest {

    @BeforeAll
    static void installFileSystem() {
        Gdx.files = new HeadlessFiles();
    }

    @Test
    @DisplayName("os tres niveis do disco carregam e tem tijolos")
    void allShippedLevelsLoad() {
        assertEquals(3, Level.countAvailable(), "esperado 01.txt, 02.txt e 03.txt");

        for (int number = 1; number <= 3; number++) {
            Level level = Level.load(Level.pathFor(number));
            assertFalse(level.bricks.isEmpty(), "nivel " + number + " sem tijolos");
            assertFalse(level.isCleared(), "nivel " + number + " ja comeca limpo");
        }
    }

    @Test
    @DisplayName("nenhum tijolo nasce fora do mundo ou em cima do paddle")
    void everyBrickFitsInsideTheWorld() {
        for (int number = 1; number <= Level.countAvailable(); number++) {
            Level level = Level.load(Level.pathFor(number));
            for (Brick brick : level.bricks) {
                Rectangle b = brick.bounds;
                assertTrue(b.x >= Rules.PLAY_LEFT - 0.001f,
                        "nivel " + number + ": tijolo entrou na parede esquerda");
                assertTrue(b.x + b.width <= Rules.PLAY_RIGHT + 0.001f,
                        "nivel " + number + ": tijolo entrou na parede direita");
                assertTrue(b.y + b.height <= Rules.BRICK_TOP_Y + 0.001f,
                        "nivel " + number + ": tijolo invadiu o HUD");
                assertTrue(b.y > Rules.PADDLE_Y + Rules.PADDLE_HEIGHT,
                        "nivel " + number + ": tijolo nasceu na altura do paddle");
            }
        }
    }

    @Test
    @DisplayName("as 14 colunas preenchem exatamente o vao entre as paredes")
    void brickGridFillsThePlayArea() {
        // Se a conta nao fechar, ou sobra uma faixa morta ao lado da parede, ou a ultima
        // coluna fica escondida atras dela.
        assertEquals(Rules.PLAY_WIDTH, Rules.BRICK_WIDTH * Rules.BRICK_COLUMNS, 0.001f);
        assertEquals(Rules.PLAY_LEFT, Rules.BRICK_MARGIN_X, 0.001f);
    }

    @Test
    @DisplayName("linhas de comentario sao ignoradas e nao viram fileira vazia")
    void commentsDoNotShiftRowsDown() {
        // 01.txt comeca com duas linhas de '#'; se fossem contadas como fileiras,
        // os tijolos nasceriam 6 unidades mais abaixo do que o esperado.
        Level level = Level.load(Level.pathFor(1));
        float highestBrickTop = 0f;
        for (Brick brick : level.bricks) {
            highestBrickTop = Math.max(highestBrickTop, brick.bounds.y + brick.bounds.height);
        }
        assertEquals(Rules.BRICK_TOP_Y - Rules.BRICK_HEIGHT, highestBrickTop, 0.001f);
    }

    @Test
    @DisplayName("tijolo de 3 batidas precisa de 3 batidas")
    void brickHitCountsComeFromTheFile() {
        Level fortress = Level.load(Level.pathFor(3));
        Brick corner = fortress.bricks.first();

        assertFalse(corner.hit(), "primeira batida nao destroi");
        assertFalse(corner.hit(), "segunda batida nao destroi");
        assertTrue(corner.hit(), "terceira batida destroi");
        assertTrue(corner.isDestroyed());
    }
}
