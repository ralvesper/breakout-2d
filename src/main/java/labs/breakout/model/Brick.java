package labs.breakout.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

/** Um tijolo. Pode exigir mais de uma batida. */
public class Brick {

    private static final Color[] COLORS_BY_REMAINING_HITS = {
            Color.WHITE,                      // nunca usado (0 = destruido)
            new Color(0.36f, 0.78f, 0.98f, 1f),
            new Color(0.98f, 0.73f, 0.26f, 1f),
            new Color(0.93f, 0.33f, 0.40f, 1f)
    };

    public final Rectangle bounds;
    private int hitsLeft;

    public Brick(float x, float y, float width, float height, int hitsLeft) {
        this.bounds = new Rectangle(x, y, width, height);
        this.hitsLeft = hitsLeft;
    }

    /** @return true se o tijolo foi destruido nesta batida */
    public boolean hit() {
        hitsLeft--;
        return hitsLeft <= 0;
    }

    public boolean isDestroyed() {
        return hitsLeft <= 0;
    }

    /** Quantas batidas ainda faltam. O render usa isso para escolher o sprite. */
    public int hitsLeft() {
        return hitsLeft;
    }

    public Color color() {
        int index = Math.min(hitsLeft, COLORS_BY_REMAINING_HITS.length - 1);
        return COLORS_BY_REMAINING_HITS[Math.max(index, 1)];
    }
}
