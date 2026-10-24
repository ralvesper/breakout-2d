package labs.breakout.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import labs.breakout.BreakoutGame;
import labs.breakout.core.Collisions;
import labs.breakout.core.Particles;
import labs.breakout.core.Rules;
import labs.breakout.model.Ball;
import labs.breakout.model.Brick;
import labs.breakout.model.Level;
import labs.breakout.model.Paddle;
import labs.breakout.model.PowerUp;

/** A tela onde o jogo acontece. */
public class GameScreen extends ScreenAdapter {

    private static final Color BACKGROUND = new Color(0.07f, 0.08f, 0.11f, 1f);
    /** Teto do delta: evita que um engasgo de 2s teleporte a bola para fora do mundo. */
    private static final float MAX_DELTA = 1f / 20f;

    private final BreakoutGame game;
    private final Paddle paddle = new Paddle();
    private final Ball ball = new Ball();
    private final Particles particles = new Particles();
    private final Array<PowerUp> powerUps = new Array<>();
    private final Vector3 touchPoint = new Vector3();
    private final Vector3 hudAnchor = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private Level level;
    private boolean paused;
    private float shakeTimer;

    public GameScreen(BreakoutGame game) {
        this.game = game;
        loadCurrentLevel();
    }

    private void loadCurrentLevel() {
        level = Level.load(Level.pathFor(game.levelNumber));
        paddle.resetToCenter();
        ball.resetOn(paddle);
        powerUps.clear();
        particles.clear();
    }

    @Override
    public void render(float rawDelta) {
        float delta = Math.min(rawDelta, MAX_DELTA);

        handleGlobalInput();
        if (handedOver()) {
            return;
        }
        if (!paused) {
            update(delta);
        }
        if (handedOver()) {
            return;
        }
        draw();
    }

    /**
     * Esta tela ja passou o bastao para outra neste mesmo frame? Perder a ultima vida ou
     * limpar o ultimo nivel troca de tela no meio do update; continuar ate o draw()
     * significa desenhar um estado que nao existe mais.
     */
    private boolean handedOver() {
        return game.getScreen() != this;
    }

    // ---------------------------------------------------------------- update

    private void handleGlobalInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            paused = !paused;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            dispose();
        }
    }

    private void update(float delta) {
        movePaddle(delta);
        paddle.tickPowerUps(delta);
        particles.update(delta);
        updatePowerUps(delta);

        if (shakeTimer > 0f) {
            shakeTimer -= delta;
        }

        if (ball.stuckToPaddle) {
            ball.followPaddle(paddle);
            boolean launch = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.justTouched();
            if (launch) {
                // Sai inclinada para o lado em que o paddle esta em relacao ao centro.
                float bias = (paddle.centerX() - Rules.PLAY_CENTER_X) / Rules.PLAY_WIDTH;
                ball.launch(bias);
                game.assets.launch.play(0.6f);
            }
            return;
        }

        // Anti-tunneling: em vez de um salto grande, varios passos pequenos.
        // O menor obstaculo do mundo e o paddle (2 unidades de altura).
        float distanceThisFrame = ball.velocity.len() * delta;
        int steps = Collisions.substeps(distanceThisFrame, Rules.PADDLE_HEIGHT);
        float stepDelta = delta / steps;

        for (int step = 0; step < steps; step++) {
            ball.bounds.x += ball.velocity.x * stepDelta;
            ball.bounds.y += ball.velocity.y * stepDelta;

            collideWithWalls();
            collideWithPaddle();
            collideWithBricks();

            if (ballIsLost()) {
                loseLife();
                return;
            }
        }

        if (level.isCleared()) {
            advanceLevel();
        }
    }

    private void movePaddle(float delta) {
        float direction = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            direction -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            direction += 1f;
        }

        if (direction != 0f) {
            paddle.move(direction, delta);
        } else if (Gdx.input.getDeltaX() != 0) {
            // unproject: pixel da janela -> unidade de mundo. Sem isso o mouse "anda errado"
            // em qualquer resolucao diferente da de referencia.
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            game.world.unproject(touchPoint);
            paddle.moveToward(touchPoint.x, delta);
        }
    }

    private void updatePowerUps(float delta) {
        for (int i = powerUps.size - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            powerUp.update(delta);

            if (powerUp.bounds.overlaps(paddle.bounds)) {
                paddle.widen();
                game.assets.powerUpTaken.play(0.7f);
                powerUps.removeIndex(i);
            } else if (powerUp.isOffScreen()) {
                powerUps.removeIndex(i);
            }
        }
    }

    private void collideWithWalls() {
        boolean bounced = false;
        if (ball.bounds.x < Rules.PLAY_LEFT) {
            ball.bounds.x = Rules.PLAY_LEFT;
            ball.velocity.x = -ball.velocity.x;
            bounced = true;
        } else if (ball.bounds.x + ball.bounds.width > Rules.PLAY_RIGHT) {
            ball.bounds.x = Rules.PLAY_RIGHT - ball.bounds.width;
            ball.velocity.x = -ball.velocity.x;
            bounced = true;
        }
        if (ball.bounds.y + ball.bounds.height > Rules.WORLD_HEIGHT) {
            ball.bounds.y = Rules.WORLD_HEIGHT - ball.bounds.height;
            ball.velocity.y = -ball.velocity.y;
            bounced = true;
        }
        if (bounced) {
            game.assets.wallHit.play(0.35f);
        }
        // Nao existe parede embaixo: e por ali que a bola se perde.
    }

    private void collideWithPaddle() {
        boolean descending = ball.velocity.y < 0f;
        if (!descending || !ball.bounds.overlaps(paddle.bounds)) {
            return;
        }
        ball.bounds.y = paddle.bounds.y + paddle.bounds.height;
        Collisions.bounceOffPaddle(ball.velocity, ball.centerX(), paddle.centerX(),
                paddle.bounds.width, Rules.PADDLE_MAX_DEFLECT_DEG);
        game.assets.paddleHit.play(0.5f);
    }

    private void collideWithBricks() {
        for (int i = 0; i < level.bricks.size; i++) {
            Brick brick = level.bricks.get(i);
            if (!ball.bounds.overlaps(brick.bounds)) {
                continue;
            }

            Collisions.Axis axis = Collisions.resolveAxis(ball.bounds, brick.bounds);
            if (axis == Collisions.Axis.HORIZONTAL) {
                ball.bounds.x = ball.velocity.x > 0f
                        ? brick.bounds.x - ball.bounds.width
                        : brick.bounds.x + brick.bounds.width;
                ball.velocity.x = -ball.velocity.x;
            } else {
                ball.bounds.y = ball.velocity.y > 0f
                        ? brick.bounds.y - ball.bounds.height
                        : brick.bounds.y + brick.bounds.height;
                ball.velocity.y = -ball.velocity.y;
            }

            Color tint = brick.color();
            if (brick.hit()) {
                shatter(brick, tint);
                // Remocao segura: removeIndex invalidaria o indice do laco, por isso
                // tratamos um tijolo por substep e saimos. Os substeps cobrem o resto.
                level.bricks.removeIndex(i);
            }
            game.assets.brick.play(0.5f);
            game.score += Rules.POINTS_PER_HIT;
            ball.speedUp(Rules.BALL_SPEED_PER_BRICK);
            return;
        }
    }

    private void shatter(Brick brick, Color tint) {
        float centerX = brick.bounds.x + brick.bounds.width / 2f;
        float centerY = brick.bounds.y + brick.bounds.height / 2f;

        particles.burst(centerX, centerY, tint, Rules.PARTICLES_PER_BRICK);
        shakeTimer = Rules.SHAKE_SECONDS;

        if (MathUtils.random() < Rules.POWERUP_DROP_CHANCE) {
            powerUps.add(new PowerUp(centerX, centerY));
        }
    }

    private boolean ballIsLost() {
        return ball.bounds.y + ball.bounds.height < 0f;
    }

    private void loseLife() {
        game.lives--;
        game.assets.lose.play(0.6f);
        if (game.lives <= 0) {
            game.setScreen(new GameOverScreen(game, false));
            dispose();
            return;
        }
        paddle.resetToCenter();
        ball.resetOn(paddle);
        powerUps.clear();
    }

    private void advanceLevel() {
        int lastLevel = Level.countAvailable();
        if (game.levelNumber >= lastLevel) {
            game.setScreen(new GameOverScreen(game, true));
            dispose();
            return;
        }
        game.levelNumber++;
        loadCurrentLevel();
    }

    // ---------------------------------------------------------------- render

    private void draw() {
        ScreenUtils.clear(BACKGROUND);

        applyShake();
        game.world.apply();
        game.batch.setProjectionMatrix(game.world.getCamera().combined);
        game.batch.begin();

        for (Brick brick : level.bricks) {
            game.batch.setColor(Color.WHITE);
            game.batch.draw(game.assets.brickFor(brick.hitsLeft()),
                    brick.bounds.x, brick.bounds.y,
                    brick.bounds.width - Rules.BRICK_GAP,
                    brick.bounds.height - Rules.BRICK_GAP);
        }

        for (PowerUp powerUp : powerUps) {
            game.batch.draw(game.assets.powerUp, powerUp.bounds.x, powerUp.bounds.y,
                    powerUp.bounds.width, powerUp.bounds.height);
        }

        particles.draw(game.batch, game.assets.pixel);

        // As paredes vem DEPOIS das particulas: estilhaco que escapa da area de jogo
        // passa por tras da parede, nao por cima dela.
        drawWalls();

        game.batch.draw(game.assets.paddle, paddle.bounds.x, paddle.bounds.y,
                paddle.bounds.width, paddle.bounds.height);
        game.batch.draw(game.assets.ball, ball.bounds.x, ball.bounds.y,
                ball.bounds.width, ball.bounds.height);

        game.batch.end();

        drawHud();
    }

    /**
     * As duas paredes laterais, do chao ao teto do mundo.
     *
     * A regiao do atlas e simetrica, entao a mesma serve dos dois lados sem espelhar.
     * Elas fazem parte do mundo, nao do HUD: tremem junto no screen shake, que e o que
     * da a impressao de que a bola bateu em alguma coisa solida.
     */
    private void drawWalls() {
        game.batch.setColor(Color.WHITE);
        game.batch.draw(game.assets.wall, 0f, 0f,
                Rules.WALL_THICKNESS, Rules.WORLD_HEIGHT);
        game.batch.draw(game.assets.wall, Rules.PLAY_RIGHT, 0f,
                Rules.WALL_THICKNESS, Rules.WORLD_HEIGHT);
    }

    /**
     * Screen shake: desloca a camera do mundo por uma fracao de segundo.
     *
     * Dois cuidados. O deslocamento e minusculo (menos de uma unidade) - exagerar vira
     * enjoo em vez de impacto. E ele mexe so na camera do mundo: o HUD tem camera propria
     * e fica parado, senao o placar treme junto e a tela parece quebrada.
     */
    private void applyShake() {
        OrthographicCamera camera = (OrthographicCamera) game.world.getCamera();
        float offsetX = 0f;
        float offsetY = 0f;

        if (shakeTimer > 0f) {
            float strength = (shakeTimer / Rules.SHAKE_SECONDS) * Rules.SHAKE_MAGNITUDE;
            offsetX = MathUtils.random(-strength, strength);
            offsetY = MathUtils.random(-strength, strength);
        }

        camera.position.set(Rules.WORLD_WIDTH / 2f + offsetX,
                Rules.WORLD_HEIGHT / 2f + offsetY, 0f);
        camera.update();
    }

    private void drawHud() {
        game.hud.apply();
        game.batch.setProjectionMatrix(game.hud.getCamera().combined);
        game.batch.begin();

        float left = screenXOf(Rules.PLAY_LEFT) + 6f;
        float right = screenXOf(Rules.PLAY_RIGHT) - 6f;
        float top = game.hud.getWorldHeight() - 12f;

        game.assets.font.setColor(Color.WHITE);
        game.assets.font.draw(game.batch,
                "PONTOS " + game.score + "    VIDAS " + game.lives
                        + "    NIVEL " + game.levelNumber,
                left, top);

        if (paddle.isWide()) {
            // Medir com GlyphLayout em vez de chutar um offset: com fonte de largura
            // variavel, qualquer palpite corta o texto em alguma resolucao - foi o que
            // aconteceu, o rotulo saia como "PADDLE LARG".
            game.assets.font.setColor(0.55f, 0.95f, 0.6f, 1f);
            layout.setText(game.assets.font, "PADDLE LARGO");
            game.assets.font.draw(game.batch, layout, right - layout.width, top);
        }

        game.assets.font.setColor(0.75f, 0.79f, 0.88f, 1f);
        if (ball.stuckToPaddle) {
            game.assets.font.draw(game.batch, "ESPACO ou clique para sacar", left, 32f);
        }
        if (paused) {
            layout.setText(game.assets.font, "PAUSADO - P para continuar");
            game.assets.font.draw(game.batch, layout,
                    (game.hud.getWorldWidth() - layout.width) / 2f,
                    game.hud.getWorldHeight() / 2f);
        }

        game.batch.end();
    }

    /**
     * Converte um X do mundo para o pixel correspondente na janela.
     *
     * O HUD vive em pixels e as paredes em unidades de mundo, entao uma margem fixa de
     * "14 pixels" escrevia o placar por cima da parede - e em outra resolucao escreveria
     * longe dela. project() e a contraparte do unproject() usado no mouse: le a mesma
     * conversao do viewport, inclusive as tarjas pretas do FitViewport.
     */
    private float screenXOf(float worldX) {
        hudAnchor.set(worldX, 0f, 0f);
        game.world.project(hudAnchor);
        return hudAnchor.x;
    }

    @Override
    public void resize(int width, int height) {
        game.resizeViewports(width, height);
    }
}
