package labs.breakout.core;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Matematica de colisao do jogo.
 *
 * Nada aqui toca OpenGL: Rectangle, Vector2 e MathUtils sao Java puro dentro do libGDX,
 * sem natives e sem contexto grafico. E por isso que esta classe roda em teste de unidade
 * normal, sem abrir janela. Toda regra que da para testar deve morar aqui, nao na Screen.
 */
public final class Collisions {

    private Collisions() {
    }

    /** Qual eixo da velocidade deve ser invertido numa colisao retangulo-retangulo. */
    public enum Axis {
        /** Bateu de lado: inverter velocity.x. */
        HORIZONTAL,
        /** Bateu em cima ou embaixo: inverter velocity.y. */
        VERTICAL
    }

    /**
     * Decide o eixo da reflexao pela menor penetracao.
     *
     * A ideia: se os retangulos se sobrepoem 0.2 no eixo X e 1.8 no eixo Y, a bola entrou
     * pela lateral. Reflexao ingenua (inverter sempre Y) faz a bola atravessar tijolos pela
     * quina e, no paddle, pode prender a bola em loop.
     */
    public static Axis resolveAxis(Rectangle moving, Rectangle still) {
        float overlapX = Math.min(moving.x + moving.width, still.x + still.width)
                - Math.max(moving.x, still.x);
        float overlapY = Math.min(moving.y + moving.height, still.y + still.height)
                - Math.max(moving.y, still.y);
        return overlapX < overlapY ? Axis.HORIZONTAL : Axis.VERTICAL;
    }

    /**
     * Rebate a bola no paddle com angulo dependente do ponto de impacto, preservando a
     * velocidade escalar.
     *
     * Nao e uma reflexao fisica: e uma decisao de game design. Inverter apenas velocity.y
     * deixa a bola presa em angulos quase horizontais e o jogador sem controle. Aqui o
     * paddle vira um "mira": centro devolve reto para cima, pontas devolvem na diagonal.
     * O resultado e sempre para cima, o que torna impossivel a bola grudar no paddle.
     *
     * @param velocity      vetor alterado no lugar
     * @param ballCenterX   centro da bola no eixo X
     * @param paddleCenterX centro do paddle no eixo X
     * @param paddleWidth   largura total do paddle
     * @param maxDeflectDeg desvio maximo em relacao a vertical nas pontas (ex. 60)
     */
    public static void bounceOffPaddle(Vector2 velocity, float ballCenterX, float paddleCenterX,
                                       float paddleWidth, float maxDeflectDeg) {
        float offset = MathUtils.clamp((ballCenterX - paddleCenterX) / (paddleWidth / 2f), -1f, 1f);
        float speed = velocity.len();
        float angleDeg = 90f - offset * maxDeflectDeg;
        velocity.set(MathUtils.cosDeg(angleDeg), MathUtils.sinDeg(angleDeg)).scl(speed);
    }

    /**
     * Em quantos passos o movimento deste frame deve ser dividido para a bola nunca pular
     * por cima de um obstaculo.
     *
     * O problema tem nome: tunneling. A 60 FPS e 60 unidades/s a bola anda 1 unidade por
     * frame; num frame de lag de 100ms ela anda 6 e atravessa uma fileira inteira de tijolos.
     * A solucao e integrar o movimento em fatias menores que o menor obstaculo.
     *
     * @param distanceThisFrame velocidade escalar * delta
     * @param smallestObstacle  menor dimensao que precisa ser detectada
     */
    public static int substeps(float distanceThisFrame, float smallestObstacle) {
        if (smallestObstacle <= 0f) {
            throw new IllegalArgumentException("smallestObstacle deve ser > 0");
        }
        return Math.max(1, MathUtils.ceil(distanceThisFrame / smallestObstacle));
    }
}
