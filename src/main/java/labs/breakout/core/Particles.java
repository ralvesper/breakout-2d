package labs.breakout.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Sistema de particulas minimo: um estilhaco por tijolo quebrado.
 *
 * O libGDX tem ParticleEffect, que le um arquivo .p feito no editor grafico. Para um
 * estilhaco de tijolo isso e mais ferramenta do que o problema pede - e, mais importante,
 * escondido atras do editor fica invisivel o que uma particula realmente e: posicao,
 * velocidade, tempo de vida e uma cor que some. Sao quatro arrays.
 *
 * Detalhe que faz diferenca: os arrays tem tamanho fixo e as particulas mortas sao
 * trocadas com a ultima viva (swap-remove). Nada e alocado durante o jogo, entao o GC
 * nao tem motivo para rodar no meio de uma partida.
 */
public class Particles {

    private static final int CAPACITY = 400;
    private static final float GRAVITY = -90f;

    private final float[] x = new float[CAPACITY];
    private final float[] y = new float[CAPACITY];
    private final float[] velocityX = new float[CAPACITY];
    private final float[] velocityY = new float[CAPACITY];
    private final float[] life = new float[CAPACITY];
    private final float[] maxLife = new float[CAPACITY];
    private final float[] size = new float[CAPACITY];
    private final Color[] color = new Color[CAPACITY];

    private int alive;

    public Particles() {
        for (int i = 0; i < CAPACITY; i++) {
            color[i] = new Color();
        }
    }

    /** Estilhaco radial a partir do centro de um tijolo. */
    public void burst(float originX, float originY, Color tint, int count) {
        for (int i = 0; i < count && alive < CAPACITY; i++) {
            int p = alive++;
            float angle = MathUtils.random(360f);
            float speed = MathUtils.random(12f, 34f);

            x[p] = originX + MathUtils.random(-1.5f, 1.5f);
            y[p] = originY + MathUtils.random(-0.8f, 0.8f);
            velocityX[p] = MathUtils.cosDeg(angle) * speed;
            velocityY[p] = MathUtils.sinDeg(angle) * speed + 10f;
            maxLife[p] = life[p] = MathUtils.random(0.35f, 0.75f);
            size[p] = MathUtils.random(0.4f, 1.1f);
            color[p].set(tint);
        }
    }

    public void update(float delta) {
        for (int p = 0; p < alive; ) {
            life[p] -= delta;
            if (life[p] <= 0f) {
                swapRemove(p);
                continue; // nao incrementa: a particula trazida para ca ainda nao foi vista
            }
            velocityY[p] += GRAVITY * delta;
            x[p] += velocityX[p] * delta;
            y[p] += velocityY[p] * delta;
            p++;
        }
    }

    public void draw(SpriteBatch batch, TextureRegion pixel) {
        for (int p = 0; p < alive; p++) {
            float fade = life[p] / maxLife[p];
            batch.setColor(color[p].r, color[p].g, color[p].b, fade);
            batch.draw(pixel, x[p], y[p], size[p], size[p]);
        }
        batch.setColor(Color.WHITE); // o batch e compartilhado: devolver limpo
    }

    public void clear() {
        alive = 0;
    }

    private void swapRemove(int index) {
        int last = --alive;
        x[index] = x[last];
        y[index] = y[last];
        velocityX[index] = velocityX[last];
        velocityY[index] = velocityY[last];
        life[index] = life[last];
        maxLife[index] = maxLife[last];
        size[index] = size[last];
        color[index].set(color[last]);
    }

    public int aliveCount() {
        return alive;
    }
}
