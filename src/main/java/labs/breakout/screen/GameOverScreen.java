package labs.breakout.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import labs.breakout.BreakoutGame;

/** Fim de partida, tanto por vitoria quanto por derrota. */
public class GameOverScreen extends ScreenAdapter {

    private final BreakoutGame game;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout();
    private final boolean won;

    public GameOverScreen(BreakoutGame game, boolean won) {
        this.game = game;
        this.won = won;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched()) {
            game.setScreen(new MenuScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(won
                ? new Color(0.06f, 0.12f, 0.09f, 1f)
                : new Color(0.12f, 0.06f, 0.07f, 1f));

        game.hud.apply();
        game.batch.setProjectionMatrix(game.hud.getCamera().combined);
        game.batch.begin();

        float centerX = game.hud.getWorldWidth() / 2f;
        float centerY = game.hud.getWorldHeight() / 2f;

        drawCentered(game.assets.titleFont, Color.WHITE,
                won ? "VOCE VENCEU" : "FIM DE JOGO", centerX, centerY + 70f);
        drawCentered(game.assets.font, Color.WHITE,
                won ? "todos os niveis limpos" : "a bola se perdeu pela ultima vez",
                centerX, centerY + 20f);
        drawCentered(game.assets.font, new Color(0.95f, 0.85f, 0.45f, 1f),
                "Pontos: " + game.score, centerX, centerY - 15f);
        drawCentered(game.assets.font, new Color(0.65f, 0.69f, 0.78f, 1f),
                "ENTER para voltar ao menu", centerX, centerY - 60f);

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
