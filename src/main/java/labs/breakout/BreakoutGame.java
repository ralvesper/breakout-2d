package labs.breakout;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import labs.breakout.core.Assets;
import labs.breakout.core.Rules;
import labs.breakout.screen.MenuScreen;

/**
 * Raiz do jogo.
 *
 * Duas responsabilidades: ser dono dos recursos caros (um SpriteBatch e um Assets para
 * todo o jogo, nao um por tela) e guardar o estado da partida que sobrevive a troca de
 * telas (pontos, vidas, nivel atual).
 *
 * Os dois viewports existem porque mundo e interface vivem em escalas diferentes:
 *   - world: 100x75 unidades de jogo. A fisica nunca enxerga pixels.
 *   - hud:   pixels da janela. Texto desenhado em unidades de mundo fica gigante e borrado.
 */
public class BreakoutGame extends Game {

    public SpriteBatch batch;
    public Assets assets;

    public FitViewport world;
    public ScreenViewport hud;

    public int score;
    public int lives;
    public int levelNumber;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
        assets.loadBlocking();

        world = new FitViewport(Rules.WORLD_WIDTH, Rules.WORLD_HEIGHT);
        hud = new ScreenViewport();

        newRun();
        setScreen(new MenuScreen(this));
    }

    /** Zera o estado para uma partida nova. */
    public void newRun() {
        score = 0;
        lives = Rules.STARTING_LIVES;
        levelNumber = 1;
    }

    public void resizeViewports(int width, int height) {
        world.update(width, height, true);
        hud.update(width, height, true);
    }

    @Override
    public void dispose() {
        // A Screen atual tem dispose() proprio; Game nao chama sozinho.
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        assets.dispose();
    }
}
