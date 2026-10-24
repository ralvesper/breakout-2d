package labs.breakout;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Ponto de entrada no desktop. O jogo em si nao sabe em que plataforma roda. */
public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Breakout 2D - labs/gaming");
        config.setWindowedMode(960, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);

        // Checklist de delta time: descomente para rodar a 15 FPS. O jogo deve ficar
        // visivelmente mais travado, mas a bola NAO pode mudar de velocidade.
        // config.useVsync(false);
        // config.setForegroundFPS(15);

        new Lwjgl3Application(new BreakoutGame(), config);
    }
}
