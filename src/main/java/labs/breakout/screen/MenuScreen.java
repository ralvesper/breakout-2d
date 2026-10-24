package labs.breakout.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import labs.breakout.BreakoutGame;
import labs.breakout.model.Level;

/** Menu inicial. Existe para mostrar o ciclo Game -> Screen funcionando. */
public class MenuScreen extends ScreenAdapter {

    private static final Color BACKGROUND = new Color(0.05f, 0.06f, 0.09f, 1f);

    private final BreakoutGame game;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    public MenuScreen(BreakoutGame game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched()) {
            game.newRun();
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        ScreenUtils.clear(BACKGROUND);

        game.hud.apply();
        game.batch.setProjectionMatrix(game.hud.getCamera().combined);
        game.batch.begin();

        float centerX = game.hud.getWorldWidth() / 2f;
        float centerY = game.hud.getWorldHeight() / 2f;

        // Com fonte de verdade da para centralizar pelo texto, em vez de chutar offsets:
        // o GlyphLayout devolvido por draw() ja traz a largura do que foi desenhado.
        drawCentered(game.assets.titleFont, Color.WHITE, "BREAKOUT 2D", centerX, centerY + 90f);
        drawCentered(game.assets.font, new Color(0.78f, 0.82f, 0.9f, 1f),
                "ENTER ou clique para comecar", centerX, centerY + 20f);
        drawCentered(game.assets.font, new Color(0.6f, 0.64f, 0.73f, 1f),
                "setas / A D / mouse: mover    ESPACO: sacar    P: pausar",
                centerX, centerY - 20f);
        drawCentered(game.assets.font, new Color(0.6f, 0.64f, 0.73f, 1f),
                Level.countAvailable() + " niveis em assets/levels/", centerX, centerY - 50f);

        game.batch.end();
    }

    private void drawCentered(com.badlogic.gdx.graphics.g2d.BitmapFont font, Color color,
                              String text, float centerX, float y) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(game.batch, layout, centerX - layout.width / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        game.resizeViewports(width, height);
    }
}
