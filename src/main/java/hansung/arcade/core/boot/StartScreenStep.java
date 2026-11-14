package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.ui.SwingThread;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다.
 * 아웃게임과 연결되기 전까지는 인게임 화면을 바로 띄운다. 연결 후에는 로비 화면으로 바꾼다.
 * 그때까지 내 닉네임과 캐릭터는 설정값 player.nickname, player.character 로 정한다.
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
        HudSkin skin = context.get(HudSkin.class);
        PlayerProfile me = localProfile(context.get(GameProperties.class));

        SwingThread.runAndWait(() -> {
            InGamePanel inGamePanel = new InGamePanel(new GameWorld(map, me), tileset, skin);
            window.show(inGamePanel);
            inGamePanel.start();
        });
    }

    static PlayerProfile localProfile(GameProperties properties) {
        String nickname = properties.get("player.nickname", "Player");
        String character = properties.get("player.character", Mascot.SANGSANGBUGI.id());
        Mascot mascot;
        try {
            mascot = Mascot.fromId(character);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("설정값 player.character=%s 를 쓸 수 없습니다. %s"
                    .formatted(character, e.getMessage()), e);
        }
        return new PlayerProfile(nickname, mascot);
    }
}
