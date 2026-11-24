package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.AirplaneSprites;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.network.FakeServer;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 서버가 연 경기를 게임 화면에 띄우면 경기 인원 모두를 서버 자리에 세우고, 목업 버튼 없이 서버를 따른다 */
@ExtendWith(RunOnEdt.class)
class GamePreviewOnlineTest {

    private static final String RED = Team.RED.label();
    private static final String BLUE = Team.BLUE.label();

    private FakeServer server;
    private ServerClient client;
    private GamePreviewScene game;
    private ServerMatchLink link;
    /** 나가기를 눌렀을 때 (setOnline) */
    private final AtomicInteger exits = new AtomicInteger();
    /** 서버가 뺐을 때 (connect) */
    private final AtomicInteger drops = new AtomicInteger();
    private final AtomicInteger waiting = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(1_000, 1_000, 5_000, 15_000)));
        client.connect().get(3, TimeUnit.SECONDS);
        SceneManager scenes = new SceneManager(view -> { });
        scenes.register("waiting", new hansung.arcade.core.game.Scene() {
            @Override
            public void enter(Object data) {
                waiting.incrementAndGet();
            }

            @Override
            public javax.swing.JComponent view() {
                return new javax.swing.JPanel();
            }
        });
        game = new GamePreviewScene(scenes, HudSkin.load(HudFonts.load()), CharacterSprites.load(),
                BalloonSprites.load(), ItemSprites.load(), AirplaneSprites.load());
        link = new ServerMatchLink(client, Runnable::run);
        game.setOnline(exits::incrementAndGet);
        game.connect(link, drops::incrementAndGet);
    }

    @AfterEach
    void tearDown() throws IOException {
        game.exit();
        client.close();
        server.close();
    }

    private static GameStartInfo online() {
        return new GameStartInfo("camp/camp08", List.of(
                new GameStartInfo.PlayerInfo(12, "부기", Mascot.SANGSANGBUGI, RED, 3),
                new GameStartInfo.PlayerInfo(20, "냥이", Mascot.HANSUNGNYANGI, BLUE, 1)), 1, true);
    }

    @Test
    void 경기_인원_모두를_서버가_정한_번호와_스폰에_세우고_목업_버튼을_숨긴다() {
        game.enter(online());

        GameWorld world = game.world();
        assertTrue(world.server().isPresent());
        assertEquals(List.of(12, 20), world.players().stream().map(Player::id).toList());
        assertEquals(List.of(12), world.localPlayers().stream().map(Player::id).toList());
        TileMap map = MapLoader.loadResource("camp/camp08");
        Point third = map.spawnPoints().get(2);
        Player me = world.localPlayers().getFirst();
        assertEquals(third, new Point(me.col(), me.row()));
        assertFalse(SwingTree.button(game.view(), "목업 경기 끝내기").isVisible());
    }

    @Test
    void 나가기를_누르면_대기방이_아니라_온라인_나가기를_한다() throws Exception {
        game.enter(online());

        clickExit();

        assertEquals(1, exits.get());
        assertEquals(0, drops.get());
        assertEquals(0, waiting.get());
    }

    @Test
    void 서버가_경기를_끝내면_결과를_띄우고_경기를_멈춘다() {
        game.enter(online());

        game.endGame(new GameResult(List.of(new GameResult.Standing(12, "부기", GameResult.Outcome.WIN, 1, 0),
                new GameResult.Standing(20, "냥이", GameResult.Outcome.LOSE, 0, 0))));

        assertTrue(game.resultOverlay().isRunning());
        assertFalse(game.world().server().orElseThrow().isPlaying());
        assertFalse(game.world().intro().isRunning(), "기다리던 검은 화면을 걷는다");
    }

    @Test
    void 화면을_띄우기_전에_서버가_끝낸_경기는_띄우자마자_결과를_보인다() throws Exception {
        server.push("{\"type\":\"GAME_STARTED\",\"body\":{\"mapId\":\"camp/camp08\",\"players\":[]}}");
        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[20],\"reason\":\"DISCONNECT\"}}");
        server.push("{\"type\":\"GAME_OVER\",\"body\":{\"reason\":\"LAST_TEAM\",\"standings\":["
                + "{\"playerId\":12,\"nickname\":\"부기\",\"outcome\":\"WIN\",\"kills\":0,\"saves\":0},"
                + "{\"playerId\":20,\"nickname\":\"냥이\",\"outcome\":\"LOSE\",\"kills\":0,\"saves\":0}]}}");
        Thread.sleep(300);

        game.enter(online());

        assertTrue(game.resultOverlay().isRunning(), "검은 화면에 멈추지 않고 결과를 띄운다");
        assertFalse(game.world().intro().isRunning());
    }

    @Test
    void 로딩_시간_초과로_빠지면_나가기를_보내지_않고_밖에서_정한_곳으로_간다() throws Exception {
        game.enter(online());

        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[12],\"reason\":\"DISCONNECT\"}}");
        for (int i = 0; i < 40 && drops.get() == 0; i++) {
            Thread.sleep(50);
        }

        assertEquals(1, drops.get());
        assertEquals(0, exits.get(), "나가기(GAME_EXIT)를 하지 않는다");
        // 서버가 이미 방에서 뺐으므로 GAME_EXIT를 보내지 않는다 (3초 기다려도 오지 않는다)
        assertThrows(AssertionError.class, () -> server.next("GAME_EXIT"));
    }

    @Test
    void 오프라인_경기는_목업_버튼을_그대로_두고_이_컴퓨터의_플레이어만_넣는다() {
        // 오프라인 모드는 온라인 설정(setOnline·connect)을 하지 않은 게임 화면을 쓴다
        GamePreviewScene offline = new GamePreviewScene(new SceneManager(view -> { }), HudSkin.load(HudFonts.load()),
                CharacterSprites.load(), BalloonSprites.load(), ItemSprites.load(), AirplaneSprites.load());
        offline.enter(new GameStartInfo("camp/camp08", List.of(
                new GameStartInfo.PlayerInfo(1, "부기", Mascot.SANGSANGBUGI, RED),
                new GameStartInfo.PlayerInfo(2, "목업 2", Mascot.KKOKKO, BLUE)), 1));
        try {
            assertTrue(offline.world().server().isEmpty());
            assertEquals(1, offline.world().players().size());
            assertTrue(SwingTree.button(offline.view(), "목업 경기 끝내기").isVisible());
        } finally {
            offline.exit();
        }
    }

    private void clickExit() {
        var panel = game.game();
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        int x = HudLayout.EXIT_BUTTON.x() + HudLayout.EXIT_BUTTON.width() / 2;
        int y = HudLayout.EXIT_BUTTON.y() + HudLayout.EXIT_BUTTON.height() / 2;
        long now = System.currentTimeMillis();
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, now, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, now, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }
}
