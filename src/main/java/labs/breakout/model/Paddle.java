package labs.breakout.model;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import labs.breakout.core.Rules;

/** A raquete do jogador. */
public class Paddle {

    public final Rectangle bounds = new Rectangle(
            Rules.PLAY_CENTER_X - Rules.PADDLE_WIDTH / 2f,
            Rules.PADDLE_Y,
            Rules.PADDLE_WIDTH,
            Rules.PADDLE_HEIGHT);

    /** Segundos restantes de paddle largo; zero significa largura normal. */
    private float wideTimer;

    /**
     * @param direction -1 esquerda, 0 parado, +1 direita
     * @param delta     segundos desde o frame anterior
     */
    public void move(float direction, float delta) {
        bounds.x += direction * Rules.PADDLE_SPEED * delta;
        clampToPlayArea();
    }

    /** Controle por mouse: o paddle persegue um X alvo em vez de receber direcao. */
    public void moveToward(float targetCenterX, float delta) {
        float desiredX = targetCenterX - bounds.width / 2f;
        float maxStep = Rules.PADDLE_SPEED * delta;
        bounds.x = MathUtils.clamp(desiredX, bounds.x - maxStep, bounds.x + maxStep);
        clampToPlayArea();
    }

    public float centerX() {
        return bounds.x + bounds.width / 2f;
    }

    public void widen() {
        wideTimer = Rules.PADDLE_WIDE_SECONDS;
        resizeKeepingCenter(Rules.PADDLE_WIDE_WIDTH);
    }

    public boolean isWide() {
        return wideTimer > 0f;
    }

    public void tickPowerUps(float delta) {
        if (wideTimer <= 0f) {
            return;
        }
        wideTimer -= delta;
        if (wideTimer <= 0f) {
            resizeKeepingCenter(Rules.PADDLE_WIDTH);
        }
    }

    /**
     * Muda a largura mantendo o centro no lugar.
     *
     * Mexer so em `width` faria o paddle crescer para a direita e saltar sob o dedo do
     * jogador no instante em que o power-up e pego - justo o momento em que ele precisa
     * de controle.
     */
    private void resizeKeepingCenter(float newWidth) {
        float center = centerX();
        bounds.setWidth(newWidth);
        bounds.x = center - newWidth / 2f;
        clampToPlayArea();
    }

    public void resetToCenter() {
        wideTimer = 0f;
        bounds.setWidth(Rules.PADDLE_WIDTH);
        bounds.x = Rules.PLAY_CENTER_X - bounds.width / 2f;
    }

    /**
     * Prende o paddle a area de jogo - as mesmas bordas em que a bola quica.
     *
     * Paddle e bola precisam concordar sobre onde fica a parede. Quando discordam, o
     * paddle para antes e a bola passa por baixo dele junto a borda.
     */
    private void clampToPlayArea() {
        bounds.x = MathUtils.clamp(bounds.x, Rules.PLAY_LEFT,
                Rules.PLAY_RIGHT - bounds.width);
    }
}
