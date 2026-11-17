package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.CharacterSprites;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인게임 공개 API를 이용한 아웃게임 흐름 확인용 연결. 아직 승패 판정이 이 브랜치에 없어서
 * "목업 경기 끝내기"로 마지막 플레이어가 죽은 상황을 흉내 낸다.
 * 경기가 끝나면 게임 화면 위에 결과 모달({@link ResultOverlay})을 띄우고, 화면이 검어지면 원래 대기방으로 돌아간다.
 */
public final class GamePreviewScene implements Scene {
    private static final int FRAME_MS = 16;

    private final JPanel view = new JPanel(new BorderLayout());
    private final JLayeredPane layers = new JLayeredPane();
    private final ResultOverlay overlay = new ResultOverlay();
    private final Timer clock = new Timer(FRAME_MS, event -> tickOverlay());
    private final HudSkin skin;
    private final CharacterSprites sprites;
    private InGamePanel game;
    private GameStartInfo start;
    private long endedAt;
    private Runnable onGameOver = () -> { };

    public GamePreviewScene(SceneManager scenes, HudSkin skin, CharacterSprites sprites) {
        this.skin = skin;
        this.sprites = sprites;
        layers.setPreferredSize(new Dimension(HudLayout.WIDTH, HudLayout.HEIGHT));
        overlay.setBounds(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        layers.add(overlay, JLayeredPane.MODAL_LAYER);
        overlay.onFinished(() -> {
            clock.stop();
            scenes.show("waiting", null);
        });
        view.add(layers, BorderLayout.CENTER);
        JButton finish = new GlossButton("목업 경기 끝내기", GlossButton.BLUE);
        finish.setPreferredSize(new Dimension(HudLayout.WIDTH, 28));
        finish.addActionListener(event -> endGame(mockResult()));
        view.add(finish, BorderLayout.SOUTH);
    }

    /** 경기가 끝났을 때 한 번 불린다 (끝나는 소리 등) */
    public void setOnGameOver(Runnable listener) {
        onGameOver = listener;
    }

    public ResultOverlay resultOverlay() {
        return overlay;
    }

    /** 마지막 플레이어가 죽어 경기가 끝났다. 지금부터 결과 연출 시계를 잰다. */
    public void endGame(GameResult result) {
        if (overlay.isRunning()) {
            return;
        }
        endedAt = System.nanoTime();
        // 경기가 끝났으니 더는 움직이지 않게 게임 루프와 입력을 멈춘다 (맵은 마지막 모습으로 남는다)
        game.stop();
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
        var local = start.players().getFirst();
        game = new InGamePanel(new GameWorld(map, new PlayerProfile(local.nickname(), local.character())),
                Tileset.forTheme(map.theme()), skin, sprites);
        game.setBounds(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        layers.add(game, JLayeredPane.DEFAULT_LAYER);
        view.revalidate();
        var window = javax.swing.SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.pack();
        }
        game.start();
        game.requestFocusInWindow();
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
    }

    @Override
    public JComponent view() { return view; }
}
