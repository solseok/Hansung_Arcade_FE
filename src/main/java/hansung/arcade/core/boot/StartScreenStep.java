package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.ui.SwingThread;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다.
 * 아웃게임과 연결되기 전까지는 인게임 화면을 바로 띄운다. 연결 후에는 로비 화면으로 바꾼다.
 */
public final class StartScreenStep implements BootStep {

    @Override
    public String name() {
        return "시작 화면 표시";
    }

    @Override
    public void run(GameContext context) {
        GameWindow window = context.get(GameWindow.class);
        TileMap map = context.get(TileMap.class);
        Tileset tileset = context.get(Tileset.class);

        SwingThread.runAndWait(() -> {
            InGamePanel inGamePanel = new InGamePanel(new GameWorld(map), tileset);
            window.show(inGamePanel);
            inGamePanel.start();
        });
    }
}
