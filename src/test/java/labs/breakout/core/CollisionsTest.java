package labs.breakout.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Roda sem abrir janela: a logica de jogo nao depende de contexto grafico. */
class CollisionsTest {

    private static final float EPS = 0.0001f;

    @Test
    @DisplayName("bola entrando pela lateral do tijolo reflete em X, nao em Y")
    void sideHitReflectsHorizontally() {
        // tijolo 7x3 em (0,0); bola 1x1 encostando na lateral direita, bem no meio da altura
        Rectangle brick = new Rectangle(0f, 0f, 7f, 3f);
        Rectangle ball = new Rectangle(6.8f, 1f, 1f, 1f);
        assertEquals(Collisions.Axis.HORIZONTAL, Collisions.resolveAxis(ball, brick));
    }

    @Test
    @DisplayName("bola entrando por cima do tijolo reflete em Y")
    void topHitReflectsVertically() {
        Rectangle brick = new Rectangle(0f, 0f, 7f, 3f);
        Rectangle ball = new Rectangle(3f, 2.8f, 1f, 1f);
        assertEquals(Collisions.Axis.VERTICAL, Collisions.resolveAxis(ball, brick));
    }

    @Test
    @DisplayName("rebote no centro do paddle sai reto para cima")
    void centerBounceGoesStraightUp() {
        Vector2 velocity = new Vector2(10f, -40f);
        Collisions.bounceOffPaddle(velocity, 50f, 50f, 16f, 60f);

        assertEquals(0f, velocity.x, EPS);
        assertTrue(velocity.y > 0f, "deve subir");
    }

    @Test
    @DisplayName("rebote preserva a velocidade escalar em qualquer ponto do paddle")
    void bouncePreservesSpeed() {
        for (float ballX = 42f; ballX <= 58f; ballX += 1f) {
            Vector2 velocity = new Vector2(13f, -37f);
            float speedBefore = velocity.len();
            Collisions.bounceOffPaddle(velocity, ballX, 50f, 16f, 60f);
            assertEquals(speedBefore, velocity.len(), 0.001f, "ballX=" + ballX);
        }
    }

    @Test
    @DisplayName("qualquer rebote no paddle aponta para cima: a bola nunca gruda")
    void bounceAlwaysPointsUp() {
        for (float ballX = 30f; ballX <= 70f; ballX += 0.5f) {
            Vector2 velocity = new Vector2(0f, -50f);
            Collisions.bounceOffPaddle(velocity, ballX, 50f, 16f, 60f);
            assertTrue(velocity.y > 0f, "ballX=" + ballX + " -> vy=" + velocity.y);
        }
    }

    @Test
    @DisplayName("rebote na ponta esquerda vai para a esquerda, na direita para a direita")
    void bounceDirectionFollowsImpactPoint() {
        Vector2 left = new Vector2(0f, -50f);
        Collisions.bounceOffPaddle(left, 42f, 50f, 16f, 60f);
        assertTrue(left.x < 0f, "ponta esquerda deve devolver para a esquerda");

        Vector2 right = new Vector2(0f, -50f);
        Collisions.bounceOffPaddle(right, 58f, 50f, 16f, 60f);
        assertTrue(right.x > 0f, "ponta direita deve devolver para a direita");
    }

    @Test
    @DisplayName("substeps garante passo menor que o menor obstaculo (anti-tunneling)")
    void substepsKeepStepUnderObstacleSize() {
        // 60 unidades/s num frame de 100ms = 6 unidades; tijolo tem 3 de altura
        float distance = 6f;
        float brickHeight = 3f;

        int steps = Collisions.substeps(distance, brickHeight);

        assertEquals(2, steps);
        assertTrue(distance / steps <= brickHeight, "cada passo deve caber no obstaculo");
    }

    @Test
    @DisplayName("em frame normal nao ha subdivisao: o custo so aparece quando precisa")
    void substepsIsOneOnNormalFrames() {
        assertEquals(1, Collisions.substeps(60f / 60f, 3f));
    }
}
