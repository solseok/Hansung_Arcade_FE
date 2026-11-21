package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.SwingThread;
import hansung.arcade.ingame.GameSound;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.input.KeyBindings;
import hansung.arcade.ingame.item.TestItems;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.render.AirplaneSprites;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.outgame.GamePreviewScene;
import hansung.arcade.outgame.LoginPanel;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.lobby.LobbyPanel;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * 5단계: 첫 화면을 조립해 창에 띄운다. 기본 화면은 목업 로그인이고(app.start-screen=login, lobby로 로비부터),
 * 로그인 → 로비 → 대기실 → 게임 → 결과 모달 → 대기실로 이어진다.
 *
 * <p>app.start-screen=game 으로 실행하면 아웃게임 없이 인게임을 바로 띄운다. 이때 내 닉네임·캐릭터·팀·고정색은
 * 설정값 player.nickname, player.character, player.team, player.color 로, 경기 방식(개인전·팀전)은 game.match-mode 로 정한다.
 * 설정이 없으면 기본 캐릭터({@link Mascot#DEFAULT})를 원래 색 그대로, 1팀, 개인전으로 시작한다.</p>
 *
 * <p>인게임을 바로 띄울 때 설정값 game.local-players=2 를 주면 한 키보드 2인 모드로 시작한다.
 * 2P는 player2.nickname, player2.character, player2.team, player2.color 로 정한다 (없으면 Player2, 한성냥이, 2팀, 원래 색).</p>
 *
 * <p>인게임을 바로 띄울 때 설정값 game.test-items=true 를 주면 1P 스폰 둘레에 아이템 11종을 한 개씩 깔아 두고 시작한다 ({@link TestItems}).</p>
 */
public final class StartScreenStep implements BootStep {

    private static final Logger log = LoggerFactory.getLogger(StartScreenStep.class);
    /** 한 키보드로 함께 할 인원 (1 또는 2) */
    static final String LOCAL_PLAYERS = "game.local-players";
    /** 아이템을 시험해 보도록 시작할 때 11종을 깔아 둘지 (true/false) */
    static final String TEST_ITEMS = "game.test-items";
    /** 첫 화면: login(기본), lobby, game */
    static final String START_SCREEN = "app.start-screen";

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
        AirplaneSprites airplanes = context.get(AirplaneSprites.class);
        GameProperties properties = context.get(GameProperties.class);
        String screen = properties.get(START_SCREEN, "login");
        if (screen.equals("login") || screen.equals("lobby")) {
            SwingThread.runAndWait(() -> {
                LobbyService service = new MockLobbyService();
                SceneManager scenes = new SceneManager(window::show);
                OutgameSession session = new OutgameSession(service);
                scenes.register("login", new LoginPanel(session, scenes));
                scenes.register("lobby", new LobbyPanel(session, scenes));
                scenes.register("waiting", new WaitingRoomPanel(session, scenes));
                GamePreviewScene game = new GamePreviewScene(scenes, skin, characters, balloons, items, airplanes);
                // 결과는 게임 화면 위 모달로 뜨므로, 끝나는 소리는 경기가 끝나는 때 낸다
                game.setOnGameOver(() -> Sound.play("Ntc_GameEnd"));
                // Ready·Start, 물풍선·아이템 같은 인게임 소리와 맵마다 다른 BGM
                game.setSoundSink(sound -> Sound.play(sound.key()));
                game.setOnGameStart(theme -> Sound.playBgm(Sound.bgmForMap(theme)));
                scenes.register("game", game);
                scenes.onShow(name -> {
                    // 게임 화면의 곡은 맵을 알아야 정하므로 위 setOnGameStart가 튼다
                    if (!name.equals("game")) {
                        Sound.playBgm(Sound.bgmFor(name));
                    }
                });
                // 게임에 들어갈 때 처음 나는 효과음이 늦지 않게 미리 풀어 둔다
                Sound.preload(GameSound.keys());
                context.register(LobbyService.class, service);
                context.register(SceneManager.class, scenes);
                scenes.show(screen, null);
            });
            return;
        }
        if (!screen.equals("game")) {
            throw new IllegalArgumentException("지원하지 않는 시작 화면: %s=%s (login, lobby, game)".formatted(START_SCREEN, screen));
        }

        List<PlayerProfile> players = localProfiles(properties);
        MatchMode mode = matchMode(properties);
        if (players.size() > 1) {
            log.info("한 키보드 2인 모드로 시작합니다. 1P({}): W/A/S/D 이동, Space 물풍선, {} 아이템 / "
                    + "2P({}): 방향키 이동, / 물풍선, {} 아이템", players.get(0).nickname(),
                    KeyBindings.PLAYER_1.itemKeyGuide(), players.get(1).nickname(), KeyBindings.PLAYER_2.itemKeyGuide());
        }

        GameWorld world = new GameWorld(map, players, mode);
        if (testItems(properties)) {
            TestItems.scatter(map, world.items());
            log.info("아이템 시험 모드: 1P 스폰 둘레에 아이템 {}종을 깔아 둡니다.", world.items().all().size());
        }
        world.beginIntro();
        Sound.preload(GameSound.keys());

        SwingThread.runAndWait(() -> {
            InGamePanel inGamePanel = new InGamePanel(world, tileset, skin, characters, balloons, items, airplanes);
            inGamePanel.setSoundSink(sound -> Sound.play(sound.key()));
            // 아웃게임 없이 띄운 게임이라 나가기는 창을 닫는다
            inGamePanel.setOnExit(() -> {
                Window frame = SwingUtilities.getWindowAncestor(inGamePanel);
                if (frame != null) {
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
            });
            window.show(inGamePanel);
            Sound.playBgm(Sound.bgmForMap(map.theme()));
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

    /** {@value #TEST_ITEMS} 설정값. 없으면 끈다. true·false 말고 다른 값이면 실패한다 */
    static boolean testItems(GameProperties properties) {
        String value = properties.get(TEST_ITEMS, "false").strip();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalStateException("설정값 %s=%s 는 쓸 수 없습니다. true 또는 false만 됩니다.".formatted(TEST_ITEMS, value));
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
