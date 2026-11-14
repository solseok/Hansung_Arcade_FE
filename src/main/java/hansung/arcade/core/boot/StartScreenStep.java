package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.ui.SwingThread;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다.
 * 아웃게임과 연결되기 전까지는 인게임 화면을 바로 띄운다. 연결 후에는 로비 화면으로 바꾼다.
 * 그때까지 내 닉네임·캐릭터·팀·고정색은 설정값 player.nickname, player.character, player.team, player.color 로,
 * 경기 방식(개인전·팀전)은 game.match-mode 로 정한다.
 * 캐릭터 선택 화면이 없으므로 설정이 없으면 기본 캐릭터({@link Mascot#DEFAULT})를 원래 색 그대로, 1팀, 개인전으로 시작한다.
 *
 * <p>설정값 game.local-players=2 로 실행하면 대기방 없이 한 키보드 2인 모드로 바로 시작한다.
 * 2P는 player2.nickname, player2.character, player2.team, player2.color 로 정한다 (없으면 Player2, 한성냥이, 2팀, 원래 색).</p>
 */
public final class StartScreenStep implements BootStep {

    private static final Logger log = LoggerFactory.getLogger(StartScreenStep.class);
    /** 한 키보드로 함께 할 인원 (1 또는 2) */
    static final String LOCAL_PLAYERS = "game.local-players";

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
        BalloonSprites balloons = context.get(BalloonSprites.class);
        ItemSprites items = context.get(ItemSprites.class);
        GameProperties properties = context.get(GameProperties.class);
        List<PlayerProfile> players = localProfiles(properties);
        MatchMode mode = matchMode(properties);
        if (players.size() > 1) {
            log.info("한 키보드 2인 모드로 시작합니다. 1P({}): W/A/S/D 이동, Space 물풍선, 왼쪽 Shift 아이템 / "
                    + "2P({}): 방향키 이동, / 물풍선, . 아이템", players.get(0).nickname(), players.get(1).nickname());
        }

        SwingThread.runAndWait(() -> {
            InGamePanel inGamePanel = new InGamePanel(new GameWorld(map, players, mode), tileset, skin, characters,
                    balloons, items);
            window.show(inGamePanel);
            inGamePanel.start();
        });
    }

    /** 이 컴퓨터에서 할 플레이어들의 정보. 1P, 2P 순서이고 인원은 {@value #LOCAL_PLAYERS}로 정한다. */
    static List<PlayerProfile> localProfiles(GameProperties properties) {
        int count = properties.getInt(LOCAL_PLAYERS, 1);
        return switch (count) {
            case 1 -> List.of(localProfile(properties));
            case 2 -> List.of(localProfile(properties), profile(properties, "player2", "Player2", Mascot.HANSUNGNYANGI, 2));
            default -> throw new IllegalStateException("설정값 %s=%d 는 쓸 수 없습니다. 1(혼자) 또는 2(한 키보드 2인)만 됩니다."
                    .formatted(LOCAL_PLAYERS, count));
        };
    }

    static PlayerProfile localProfile(GameProperties properties) {
        return profile(properties, "player", "Player", Mascot.DEFAULT, PlayerProfile.DEFAULT_TEAM);
    }

    /** 설정값 {prefix}.nickname, {prefix}.character, {prefix}.team, {prefix}.color 로 정한 플레이어 정보. 없으면 기본값을 쓴다 */
    private static PlayerProfile profile(GameProperties properties, String prefix, String defaultNickname,
                                         Mascot defaultMascot, int defaultTeam) {
        String nickname = properties.get(prefix + ".nickname", defaultNickname);
        String characterKey = prefix + ".character";
        String character = properties.get(characterKey, defaultMascot.id());
        Mascot mascot;
        try {
            mascot = Mascot.fromId(character);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("설정값 %s=%s 를 쓸 수 없습니다. %s"
                    .formatted(characterKey, character, e.getMessage()), e);
        }
        String teamKey = prefix + ".team";
        int team = properties.getInt(teamKey, defaultTeam);
        if (team < 1) {
            throw new IllegalStateException("설정값 %s=%d 를 쓸 수 없습니다. 팀 번호는 1 이상입니다.".formatted(teamKey, team));
        }
        return new PlayerProfile(nickname, mascot, team, color(properties, prefix + ".color"));
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

    /** 고정색 설정값(player.color 등). 비어 있으면 null(원래 그림 색)이다. */
    private static CharacterColor color(GameProperties properties, String key) {
        String color = properties.get(key, "");
        if (color.isEmpty()) {
            return null;
        }
        try {
            return CharacterColor.fromId(color);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("설정값 %s=%s 를 쓸 수 없습니다. %s"
                    .formatted(key, color, e.getMessage()), e);
        }
    }
}
