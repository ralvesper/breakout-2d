package labs.breakout.model;

import com.badlogic.gdx.math.Rectangle;
import labs.breakout.core.Rules;

/** Capsula que cai de um tijolo quebrado e alarga o paddle se for pega. */
public class PowerUp {

    public final Rectangle bounds;

    public PowerUp(float centerX, float centerY) {
        bounds = new Rectangle(centerX - Rules.POWERUP_SIZE / 2f,
                centerY - Rules.POWERUP_SIZE / 2f,
                Rules.POWERUP_SIZE, Rules.POWERUP_SIZE);
    }

    public void update(float delta) {
        bounds.y -= Rules.POWERUP_FALL_SPEED * delta;
    }

    public boolean isOffScreen() {
        return bounds.y + bounds.height < 0f;
    }
}
