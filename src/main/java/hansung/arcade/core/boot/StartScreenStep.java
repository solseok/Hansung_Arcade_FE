package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.ui.SwingThread;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.lobby.LobbyPanel;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다.
 * 기본 화면은 목업 로비다. --app.start-screen=game으로 인게임을 직접 실행한다.
 * 인게임 직접 실행의 닉네임과 캐릭터는 player.nickname, player.character 설정을 쓴다.
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
        CharacterSprites characters = context.get(CharacterSprites.class);
        PlayerProfile me = localProfile(context.get(GameProperties.class));

        SwingThread.runAndWait(() -> {
            String screen = context.get(GameProperties.class).get("app.start-screen", "lobby");
            if (screen.equals("lobby")) {
                LobbyService service = new MockLobbyService();
                SceneManager scenes = new SceneManager(window::show);
                scenes.register("lobby", new LobbyPanel(service, scenes));
                scenes.register("waiting", new WaitingRoomPanel(service, scenes));
                scenes.onShow(name -> {
                    Sound.playBgm(name.equals("lobby") ? "Camp" : "Patrit");
                    if (name.equals("waiting")) {
                        Sound.play("Ntc_GameReady");
                    }
                });
                context.register(LobbyService.class, service);
                context.register(SceneManager.class, scenes);
                scenes.show("lobby", null);
                return;
            }
            if (!screen.equals("game")) {
                throw new IllegalArgumentException("지원하지 않는 시작 화면: " + screen);
            }
            InGamePanel inGamePanel = new InGamePanel(new GameWorld(map, me), tileset, skin, characters);
            window.show(inGamePanel);
            inGamePanel.start();
        });
    }

    static PlayerProfile localProfile(GameProperties properties) {
        String nickname = properties.get("player.nickname", "Player");
        String character = properties.get("player.character", Mascot.DEFAULT.id());
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
