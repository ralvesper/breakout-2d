package labs.breakout.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/**
 * Carrega e guarda tudo que vem do disco.
 *
 * Por que um AssetManager e nao `new Texture(...)` espalhado pelo codigo:
 *
 *  - ele deduplica. Pedir o mesmo arquivo duas vezes devolve o mesmo objeto, nao duas
 *    texturas na memoria da GPU;
 *  - ele centraliza o dispose(). Textura vazada so aparece como memoria sumindo aos
 *    poucos, que e o pior tipo de bug para encontrar depois;
 *  - ele sabe carregar em background (update()), que e como se faz tela de loading. Aqui
 *    os assets sao minusculos, entao finishLoading() bloqueia e pronto - mas o caminho
 *    para a tela de loading ja esta aberto.
 *
 * A fonte nao passa pelo AssetManager: FreeType gera a BitmapFont no tamanho pedido a
 * partir do .ttf. Fonte escalada depois fica borrada; fonte gerada no tamanho certo, nao.
 */
public class Assets implements Disposable {

    private static final String ATLAS = "sprites/breakout.atlas";
    private static final String FONT_FILE = "fonts/DejaVuSans-Bold.ttf";

    private static final String[] SOUND_FILES = {
            "sfx/wall.wav", "sfx/paddle.wav", "sfx/brick.wav",
            "sfx/launch.wav", "sfx/powerup.wav", "sfx/lose.wav"
    };

    private final AssetManager manager = new AssetManager();

    public TextureRegion paddle;
    public TextureRegion ball;
    public TextureRegion powerUp;
    public TextureRegion wall;
    public TextureRegion pixel;
    private TextureRegion[] brickByHits;

    public Sound wallHit;
    public Sound paddleHit;
    public Sound brick;
    public Sound launch;
    public Sound powerUpTaken;
    public Sound lose;

    public BitmapFont font;
    public BitmapFont titleFont;

    public void loadBlocking() {
        manager.load(ATLAS, TextureAtlas.class);
        for (String sound : SOUND_FILES) {
            manager.load(sound, Sound.class);
        }
        manager.finishLoading();

        TextureAtlas atlas = manager.get(ATLAS, TextureAtlas.class);
        paddle = atlas.findRegion("paddle");
        ball = atlas.findRegion("ball");
        powerUp = atlas.findRegion("powerup");
        wall = atlas.findRegion("wall");
        pixel = atlas.findRegion("pixel");
        // indice = batidas restantes, entao brickByHits[1] e o tijolo de 1 batida
        brickByHits = new TextureRegion[]{
                null,
                atlas.findRegion("brick1"),
                atlas.findRegion("brick2"),
                atlas.findRegion("brick3")
        };

        wallHit = manager.get("sfx/wall.wav", Sound.class);
        paddleHit = manager.get("sfx/paddle.wav", Sound.class);
        brick = manager.get("sfx/brick.wav", Sound.class);
        launch = manager.get("sfx/launch.wav", Sound.class);
        powerUpTaken = manager.get("sfx/powerup.wav", Sound.class);
        lose = manager.get("sfx/lose.wav", Sound.class);

        buildFonts();
    }

    private void buildFonts() {
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 18;
        parameter.borderWidth = 1.2f;
        parameter.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        font = generator.generateFont(parameter);

        parameter.size = 42;
        parameter.borderWidth = 2f;
        titleFont = generator.generateFont(parameter);

        // O generator so e necessario enquanto gera: as fontes ja estao prontas.
        generator.dispose();
    }

    public TextureRegion brickFor(int hitsLeft) {
        int index = Math.min(Math.max(hitsLeft, 1), brickByHits.length - 1);
        return brickByHits[index];
    }

    @Override
    public void dispose() {
        manager.dispose();
        font.dispose();
        titleFont.dispose();
    }
}
