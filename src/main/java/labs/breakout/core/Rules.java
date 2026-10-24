package labs.breakout.core;

/**
 * Constantes do mundo do jogo.
 *
 * Tudo em "unidades de mundo", nunca em pixels. O mundo tem 100x75 unidades e o viewport
 * cuida de mapear isso para a janela, seja ela 800x600 ou 2560x1440. Se a logica usasse
 * pixels, o jogo mudaria de dificuldade conforme o monitor.
 */
public final class Rules {

    private Rules() {
    }

    public static final float WORLD_WIDTH = 100f;
    public static final float WORLD_HEIGHT = 75f;

    // ---- paredes laterais ---------------------------------------------------

    /**
     * Espessura das paredes laterais.
     *
     * Elas OCUPAM espaco: a area de jogo e o que sobra entre as duas, nao o mundo
     * inteiro. Desenhar paredes por cima de uma area de jogo que continua indo de 0 a
     * WORLD_WIDTH e o caminho curto, e produz tijolos escondidos atras da parede e uma
     * bola que quica num lugar diferente de onde a parede aparece.
     */
    public static final float WALL_THICKNESS = 3f;

    /** Limites da area jogavel. Bola, paddle e tijolos respeitam estes, nao o mundo. */
    public static final float PLAY_LEFT = WALL_THICKNESS;
    public static final float PLAY_RIGHT = WORLD_WIDTH - WALL_THICKNESS;
    public static final float PLAY_WIDTH = PLAY_RIGHT - PLAY_LEFT;
    public static final float PLAY_CENTER_X = (PLAY_LEFT + PLAY_RIGHT) / 2f;

    public static final float PADDLE_WIDTH = 16f;
    public static final float PADDLE_HEIGHT = 2f;
    public static final float PADDLE_Y = 4f;
    /** Unidades por segundo, nao por frame. */
    public static final float PADDLE_SPEED = 85f;
    /** Desvio maximo em relacao a vertical quando a bola bate na ponta do paddle. */
    public static final float PADDLE_MAX_DEFLECT_DEG = 60f;

    public static final float BALL_SIZE = 1.4f;
    public static final float BALL_START_SPEED = 46f;
    /** A cada tijolo quebrado a bola acelera um pouco; limitado por BALL_MAX_SPEED. */
    public static final float BALL_SPEED_PER_BRICK = 0.18f;
    public static final float BALL_MAX_SPEED = 80f;

    public static final int BRICK_COLUMNS = 14;
    /**
     * Derivada da area de jogo, nao escolhida a mao: as 14 colunas preenchem exatamente
     * o vao entre as paredes. Mudar WALL_THICKNESS reacomoda as fases sozinho, sem
     * precisar reeditar nenhum arquivo de nivel.
     */
    public static final float BRICK_WIDTH = PLAY_WIDTH / BRICK_COLUMNS;
    public static final float BRICK_HEIGHT = 3f;
    public static final float BRICK_MARGIN_X = PLAY_LEFT;
    /** Topo da primeira fileira, deixando espaco para o HUD. */
    public static final float BRICK_TOP_Y = 69f;
    /** Folga visual entre tijolos: o retangulo de colisao e cheio, o desenho e menor. */
    public static final float BRICK_GAP = 0.4f;

    public static final int STARTING_LIVES = 3;
    public static final int POINTS_PER_HIT = 10;

    // ---- power-up: paddle largo --------------------------------------------

    public static final float PADDLE_WIDE_WIDTH = 26f;
    public static final float PADDLE_WIDE_SECONDS = 12f;
    public static final float POWERUP_SIZE = 3f;
    public static final float POWERUP_FALL_SPEED = 22f;
    /** Chance de um tijolo destruido largar um power-up. */
    public static final float POWERUP_DROP_CHANCE = 0.14f;

    // ---- screen shake -------------------------------------------------------

    /** Deslocamento maximo da camera, em unidades de mundo. Pouco e o suficiente. */
    public static final float SHAKE_MAGNITUDE = 0.9f;
    public static final float SHAKE_SECONDS = 0.18f;
    public static final int PARTICLES_PER_BRICK = 14;
}
