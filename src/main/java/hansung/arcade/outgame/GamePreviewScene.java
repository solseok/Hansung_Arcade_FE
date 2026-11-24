package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.GameSound;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.render.AirplaneSprites;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.outgame.ui.GlossButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 인게임 공개 API를 이용한 아웃게임 흐름 확인용 연결. 이 컴퓨터의 플레이어(1P, 2P면 2P까지)를 대기실에서 고른
 * 캐릭터·팀 색으로 인게임에 넣는다. 인게임의 실제 승패(MatchResult)는 아직 결과 모달에 잇지 않아서
 * "목업 경기 끝내기"로 마지막 플레이어가 죽은 상황을 흉내 낸다.
 * 경기가 끝나면 게임 화면 위에 결과 모달({@link ResultOverlay})을 띄우고, 화면이 검어지면 원래 대기방으로 돌아간다.
 *
 * <p>게임은 시작 연출("Ready... Start!")부터 하고, 인게임 오른쪽 아래 나가기 버튼을 누르면 경기를 그만두고 대기방으로 돌아간다.
 * 소리는 이 화면이 직접 내지 않고, 밖에서 넘겨준 곳으로 보낸다 ({@link #setSoundSink}, {@link #setOnGameStart}).</p>
 */
public final class GamePreviewScene implements Scene {
    private static final int FRAME_MS = 16;

    private final JPanel view = new JPanel(new BorderLayout());
    private final JLayeredPane layers = new JLayeredPane();
    private final ResultOverlay overlay = new ResultOverlay();
    private final Timer clock = new Timer(FRAME_MS, event -> tickOverlay());
    private final HudSkin skin;
    private final CharacterSprites sprites;
    private final BalloonSprites balloons;
    private final ItemSprites items;
    private final AirplaneSprites airplanes;
    private final SceneManager scenes;
    private InGamePanel game;
    private GameWorld world;
    private GameStartInfo start;
    private long endedAt;
    private Runnable onGameOver = () -> { };
    private final JButton finish;
    /** 인게임 나가기. null이면 대기실로 돌아간다 (오프라인) */
    private Runnable onExit;
    private Consumer<GameSound> soundSink = sound -> { };
    private Consumer<String> onGameStart = theme -> { };

    /** 그림은 부팅 때 한 번 읽어 둔 것을 받는다 (ResourceStep) */
    public GamePreviewScene(SceneManager scenes, HudSkin skin, CharacterSprites sprites, BalloonSprites balloons,
                            ItemSprites items, AirplaneSprites airplanes) {
        this.skin = skin;
        this.sprites = sprites;
        this.balloons = balloons;
        this.items = items;
        this.airplanes = airplanes;
        this.scenes = scenes;
        layers.setPreferredSize(new Dimension(HudLayout.WIDTH, HudLayout.HEIGHT));
        overlay.setBounds(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        layers.add(overlay, JLayeredPane.MODAL_LAYER);
        overlay.onFinished(() -> {
            clock.stop();
            scenes.show("waiting", null);
        });
        view.add(layers, BorderLayout.CENTER);
        finish = new GlossButton("목업 경기 끝내기", GlossButton.BLUE);
        finish.setName("mock-finish");
        finish.setPreferredSize(new Dimension(HudLayout.WIDTH, 28));
        finish.addActionListener(event -> endGame(mockResult()));
        view.add(finish, BorderLayout.SOUTH);
    }

    /**
     * 온라인으로 쓴다: 경기 결과는 서버가 GAME_OVER로 정하므로 목업 경기 끝내기 버튼을 숨기고,
     * 인게임 나가기는 onExit(서버에 GAME_EXIT를 보내고 로비로)를 부른다.
     */
    public void setOnline(Runnable onExit) {
        this.onExit = onExit;
        finish.setVisible(false);
    }

    /** 경기가 끝났을 때 한 번 불린다 (끝나는 소리 등) */
    public void setOnGameOver(Runnable listener) {
        onGameOver = listener;
    }

    /** 게임에서 난 소리(Ready·Start, 물풍선, 아이템, 나가기 등)를 받을 곳 */
    public void setSoundSink(Consumer<GameSound> sink) {
        soundSink = Objects.requireNonNull(sink);
    }

    /** 게임 화면에 들어갈 때 맵 테마(camp, patrit, village)를 받는다. 맵마다 다른 BGM을 틀 때 쓴다 */
    public void setOnGameStart(Consumer<String> listener) {
        onGameStart = Objects.requireNonNull(listener);
    }

    /** 지금 띄운 게임 화면. 없으면 null (테스트에서 나가기 버튼을 누를 때 쓴다) */
    public InGamePanel game() {
        return game;
    }

    public ResultOverlay resultOverlay() {
        return overlay;
    }

    /** 마지막 플레이어가 죽어 경기가 끝났다. 지금부터 결과 연출 시계를 잰다. */
    public void endGame(GameResult result) {
        if (game == null || start == null || overlay.isRunning()) {
            return;
        }
        endedAt = System.nanoTime();
        // 시작 연출 중에 끝났으면(목업 버튼) 연출을 끝내 둔다. 그대로 멈추면 검은 화면이 결과 모달 아래에 남는다
        world.intro().finish();
        // 경기가 끝났으니 더는 움직이지 않게 게임 루프와 입력을 멈춘다 (맵은 마지막 모습으로 남는다)
        game.stop();
        game.repaint();
        // 이 컴퓨터의 플레이어(players 앞쪽 localPlayers명)의 줄을 강조한다
        Set<Integer> local = start.players().stream().limit(start.localPlayers())
                .map(GameStartInfo.PlayerInfo::id).collect(Collectors.toSet());
        overlay.show(result, local);
        onGameOver.run();
        clock.restart();
    }

    private void tickOverlay() {
        overlay.tick((System.nanoTime() - endedAt) / 1_000_000);
    }

    /** 이 컴퓨터의 플레이어가 이기고, 1P가 다른 사람을 모두 터뜨린 것으로 친 목업 결과 */
    private GameResult mockResult() {
        List<GameResult.Standing> standings = new ArrayList<>();
        int others = start.players().size() - start.localPlayers();
        for (int i = 0; i < start.players().size(); i++) {
            GameStartInfo.PlayerInfo player = start.players().get(i);
            boolean local = i < start.localPlayers();
            standings.add(new GameResult.Standing(player.id(), player.nickname(),
                    local ? GameResult.Outcome.WIN : GameResult.Outcome.LOSE,
                    i == 0 ? others : 0, local && i > 0 ? 1 : 0));
        }
        return new GameResult(standings);
    }

    @Override
    public void enter(Object data) {
        start = (GameStartInfo) data;
        overlay.reset();
        var map = MapLoader.loadResource(start.mapId());
        world = new GameWorld(map, localProfiles(start), MatchMode.SOLO);
        world.beginIntro();
        game = new InGamePanel(world, Tileset.forTheme(map.theme()), skin, sprites, balloons, items, airplanes);
        game.setSoundSink(soundSink);
        // 나가기: 경기를 그만두고 대기방으로 돌아간다 (방장이면 방을 다시 대기 중으로 돌린다)
        game.setOnExit(onExit != null ? onExit : () -> scenes.show("waiting", null));
        game.setBounds(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        layers.add(game, JLayeredPane.DEFAULT_LAYER);
        view.revalidate();
        var window = javax.swing.SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.pack();
        }
        onGameStart.accept(map.theme());
        game.start();
        game.requestFocusInWindow();
    }

    /** 이 컴퓨터의 플레이어(players 앞쪽 localPlayers명). 팀 번호는 팀 순서(빨강 1)이고, 캐릭터 고정색은 팀 색으로 칠한다 */
    private static List<PlayerProfile> localProfiles(GameStartInfo start) {
        return start.players().stream().limit(start.localPlayers()).map(player -> {
            Team team = Team.fromLabel(player.team());
            return new PlayerProfile(player.nickname(), player.character(), team.ordinal() + 1, team.characterColor());
        }).toList();
    }

    @Override
    public void exit() {
        clock.stop();
        overlay.reset();
        if (game != null) {
            game.stop();
            layers.remove(game);
            game = null;
        }
        world = null;
    }

    @Override
    public JComponent view() { return view; }
}
