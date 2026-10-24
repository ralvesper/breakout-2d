package labs.breakout.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import labs.breakout.core.Rules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O power-up de paddle largo, testado sem abrir janela. */
class PaddleTest {

    private static final float EPS = 0.001f;

    @Test
    @DisplayName("alargar mantem o centro no lugar")
    void wideningKeepsTheCenter() {
        Paddle paddle = new Paddle();
        paddle.move(1f, 0.2f); // tira do centro para o teste nao passar por acaso
        float centerBefore = paddle.centerX();

        paddle.widen();

        assertEquals(centerBefore, paddle.centerX(), EPS,
                "o paddle nao pode pular sob o dedo do jogador ao crescer");
        assertEquals(Rules.PADDLE_WIDE_WIDTH, paddle.bounds.width, EPS);
    }

    @Test
    @DisplayName("o paddle largo volta ao normal quando o tempo acaba")
    void wideExpires() {
        Paddle paddle = new Paddle();
        paddle.widen();
        assertTrue(paddle.isWide());

        // um frame antes de expirar
        paddle.tickPowerUps(Rules.PADDLE_WIDE_SECONDS - 0.05f);
        assertTrue(paddle.isWide(), "ainda deveria estar largo");
        assertEquals(Rules.PADDLE_WIDE_WIDTH, paddle.bounds.width, EPS);

        paddle.tickPowerUps(0.1f);
        assertFalse(paddle.isWide());
        assertEquals(Rules.PADDLE_WIDTH, paddle.bounds.width, EPS);
    }

    @Test
    @DisplayName("alargar encostado na borda nao joga o paddle para fora do mundo")
    void wideningAtTheEdgeStaysInside() {
        Paddle paddle = new Paddle();
        paddle.move(1f, 10f); // vai ate a borda direita e trava la

        paddle.widen();

        assertTrue(paddle.bounds.x >= Rules.PLAY_LEFT - EPS, "entrou na parede esquerda");
        assertTrue(paddle.bounds.x + paddle.bounds.width <= Rules.PLAY_RIGHT + EPS,
                "entrou na parede direita");
    }

    @Test
    @DisplayName("o paddle encosta exatamente na parede, nem antes nem depois")
    void paddleStopsFlushAgainstTheWalls() {
        // Paddle e bola tem que concordar sobre onde fica a parede. Quando o paddle para
        // antes, sobra um vao junto a borda por onde a bola passa por baixo dele.
        Paddle paddle = new Paddle();

        paddle.move(-1f, 10f);
        assertEquals(Rules.PLAY_LEFT, paddle.bounds.x, EPS);

        paddle.move(1f, 10f);
        assertEquals(Rules.PLAY_RIGHT, paddle.bounds.x + paddle.bounds.width, EPS);
    }

    @Test
    @DisplayName("a area de jogo fica dentro do mundo, com parede dos dois lados")
    void playAreaLeavesRoomForBothWalls() {
        assertEquals(Rules.WALL_THICKNESS, Rules.PLAY_LEFT, EPS);
        assertEquals(Rules.WORLD_WIDTH - Rules.WALL_THICKNESS, Rules.PLAY_RIGHT, EPS);
        assertTrue(Rules.PLAY_WIDTH > Rules.PADDLE_WIDE_WIDTH,
                "a area de jogo precisa caber o paddle largo");
    }

    @Test
    @DisplayName("perder uma vida cancela o power-up ativo")
    void resetCancelsTheWidePaddle() {
        Paddle paddle = new Paddle();
        paddle.widen();

        paddle.resetToCenter();

        assertFalse(paddle.isWide());
        assertEquals(Rules.PADDLE_WIDTH, paddle.bounds.width, EPS);
    }

    @Test
    @DisplayName("a capsula cai e some abaixo da tela")
    void powerUpFallsAndLeaves() {
        PowerUp powerUp = new PowerUp(50f, 40f);
        assertFalse(powerUp.isOffScreen());

        float fallTime = 40f / Rules.POWERUP_FALL_SPEED + 0.5f;
        powerUp.update(fallTime);

        assertTrue(powerUp.isOffScreen(), "deveria ter saido por baixo");
    }
}
