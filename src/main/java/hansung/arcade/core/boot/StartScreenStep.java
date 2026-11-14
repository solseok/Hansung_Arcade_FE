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
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다.
 * 아웃게임과 연결되기 전까지는 인게임 화면을 바로 띄운다. 연결 후에는 로비 화면으로 바꾼다.
 * 그때까지 내 닉네임·캐릭터·팀은 설정값 player.nickname, player.character, player.team 으로,
 * 경기 방식(개인전·팀전)은 game.match-mode 로 정한다.
 * 캐릭터 선택 화면이 없으므로 설정이 없으면 기본 캐릭터({@link Mascot#DEFAULT}), 1팀, 개인전으로 시작한다.
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
        GameProperties properties = context.get(GameProperties.class);
        PlayerProfile me = localProfile(properties);
        MatchMode mode = matchMode(properties);

        SwingThread.runAndWait(() -> {
            InGamePanel inGamePanel = new InGamePanel(new GameWorld(map, me, mode), tileset, skin, characters);
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
        int team = properties.getInt("player.team", PlayerProfile.DEFAULT_TEAM);
        if (team < 1) {
            throw new IllegalStateException("설정값 player.team=%d 를 쓸 수 없습니다. 팀 번호는 1 이상입니다.".formatted(team));
        }
        return new PlayerProfile(nickname, mascot, team);
    }

    static MatchMode matchMode(GameProperties properties) {
        String mode = properties.get("game.match-mode", MatchMode.SOLO.id());
        try {
            return MatchMode.fromId(mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("설정값 game.match-mode=%s 를 쓸 수 없습니다. %s"
                    .formatted(mode, e.getMessage()), e);
        }
    }
}
