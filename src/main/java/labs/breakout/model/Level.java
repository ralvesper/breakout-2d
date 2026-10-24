package labs.breakout.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import labs.breakout.core.Rules;

/**
 * Um nivel, carregado de um arquivo de texto em assets/levels/.
 *
 * O ponto da etapa 5: dados de nivel nao sao codigo. Editar 03.txt num editor de texto e
 * jogar de novo nao exige recompilar nada, e um designer que nao programa consegue criar
 * fases. Formato: uma linha por fileira, cada caractere e uma coluna.
 *   '.' ou ' ' -> vazio
 *   '1'..'3'   -> tijolo com essa quantidade de batidas
 */
public class Level {

    public final Array<Brick> bricks = new Array<>();
    public final String name;

    private Level(String name) {
        this.name = name;
    }

    public static Level load(String internalPath) {
        Level level = new Level(internalPath);
        FileHandle file = Gdx.files.internal(internalPath);
        if (!file.exists()) {
            throw new IllegalArgumentException("nivel nao encontrado: " + internalPath);
        }
        String[] rows = file.readString("UTF-8").split("\\r?\\n");

        // gridRow conta apenas as fileiras de verdade. Usar o indice do laco contaria as
        // linhas de comentario como fileiras vazias e empurraria o nivel inteiro para baixo.
        int gridRow = 0;
        for (String line : rows) {
            if (line.startsWith("#")) {
                continue; // comentario
            }
            int row = gridRow++;
            int columns = Math.min(line.length(), Rules.BRICK_COLUMNS);
            for (int column = 0; column < columns; column++) {
                char cell = line.charAt(column);
                if (cell == '.' || cell == ' ') {
                    continue;
                }
                int hits = Character.isDigit(cell) ? Character.getNumericValue(cell) : 1;
                if (hits <= 0) {
                    continue;
                }
                float x = Rules.BRICK_MARGIN_X + column * Rules.BRICK_WIDTH;
                float y = Rules.BRICK_TOP_Y - (row + 1) * Rules.BRICK_HEIGHT;
                level.bricks.add(new Brick(x, y, Rules.BRICK_WIDTH, Rules.BRICK_HEIGHT, hits));
            }
        }
        return level;
    }

    /** Quantos arquivos 01.txt, 02.txt, ... existem de fato. */
    public static int countAvailable() {
        int found = 0;
        while (Gdx.files.internal(pathFor(found + 1)).exists()) {
            found++;
        }
        return found;
    }

    public static String pathFor(int levelNumber) {
        return String.format("levels/%02d.txt", levelNumber);
    }

    public boolean isCleared() {
        return bricks.isEmpty();
    }
}
