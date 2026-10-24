package labs.breakout.model;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import labs.breakout.core.Rules;

/** A bola: um retangulo pequeno com um vetor velocidade. */
public class Ball {

    public final Rectangle bounds =
            new Rectangle(0f, 0f, Rules.BALL_SIZE, Rules.BALL_SIZE);
    public final Vector2 velocity = new Vector2();

    /** Antes do primeiro saque a bola fica colada no paddle. */
    public boolean stuckToPaddle = true;

    public void resetOn(Paddle paddle) {
        stuckToPaddle = true;
        velocity.setZero();
        followPaddle(paddle);
    }

    public void followPaddle(Paddle paddle) {
        bounds.x = paddle.centerX() - bounds.width / 2f;
        bounds.y = paddle.bounds.y + paddle.bounds.height + 0.2f;
    }

    /** Saque: sai sempre para cima, com leve inclinacao para o lado em que o paddle aponta. */
    public void launch(float towardX) {
        stuckToPaddle = false;
        velocity.set(towardX, 1f).nor().scl(Rules.BALL_START_SPEED);
    }

    public float centerX() {
        return bounds.x + bounds.width / 2f;
    }

    public void speedUp(float amount) {
        float newSpeed = Math.min(velocity.len() + amount, Rules.BALL_MAX_SPEED);
        velocity.setLength(newSpeed);
    }
}
