package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.InGamePanel;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** 인게임 공개 API를 이용한 아웃게임 흐름 확인용 연결. 승패는 목업 결과로 확인한다. */
public final class GamePreviewScene implements Scene {
    private final JPanel view = new JPanel(new BorderLayout());
    private final HudSkin skin;
    private final CharacterSprites sprites;
    private InGamePanel game;
    private GameStartInfo start;
    private final ResultOverlay overlay = new ResultOverlay();
    private final javax.swing.JLayeredPane layers = new javax.swing.JLayeredPane();
    private javax.swing.Timer resultTimer;
    private long resultStarted;

    public ResultOverlay resultOverlay() {
        return overlay;
    }

    public GamePreviewScene(SceneManager scenes, HudSkin skin, CharacterSprites sprites) {
        this.skin = skin;
        this.sprites = sprites;
        layers.setPreferredSize(
                new java.awt.Dimension(
                        hansung.arcade.ingame.hud.HudLayout.WIDTH,
                        hansung.arcade.ingame.hud.HudLayout.HEIGHT));
        layers.setSize(layers.getPreferredSize());
        overlay.setBounds(0, 0, layers.getWidth(), layers.getHeight());
        overlay.setVisible(false);
        layers.add(overlay, Integer.valueOf(1));
        view.add(layers, BorderLayout.CENTER);
        overlay.onFinished(
                () -> {
                    if (resultTimer != null) resultTimer.stop();
                    scenes.show("waiting", null);
                });
        JButton finish = new JButton("목업 경기 끝내기");
        finish.addActionListener(
                event -> {
                    if (resultTimer != null && resultTimer.isRunning()) return;
                    game.stop();
                    var standings = new ArrayList<GameResult.Standing>();
                    for (var player : start.players())
                        standings.add(
                                new GameResult.Standing(
                                        player.id(),
                                        player.nickname(),
                                        player.id() == 1
                                                ? GameResult.Outcome.WIN
                                                : GameResult.Outcome.LOSE,
                                        player.id() == 1 ? 1 : 0,
                                        0));
                    overlay.show(
                            new GameResult(standings),
                            java.util.stream.IntStream.rangeClosed(1, start.localPlayers())
                                    .boxed()
                                    .collect(java.util.stream.Collectors.toSet()));
                    resultStarted = System.nanoTime();
                    resultTimer =
                            new javax.swing.Timer(
                                    16,
                                    e ->
                                            overlay.tick(
                                                    (System.nanoTime() - resultStarted)
                                                            / 1_000_000));
                    resultTimer.start();
                });
        view.add(finish, BorderLayout.SOUTH);
    }

    @Override
    public void enter(Object data) {
        start = (GameStartInfo) data;
        var map = MapLoader.loadResource(start.mapId());
        var local = start.players().getFirst();
        game =
                new InGamePanel(
                        new GameWorld(map, new PlayerProfile(local.nickname(), local.character())),
                        Tileset.forTheme(map.theme()),
                        skin,
                        sprites);
        game.setBounds(0, 0, layers.getWidth(), layers.getHeight());
        layers.add(game, Integer.valueOf(0));
        overlay.setVisible(false);
        view.revalidate();
        var window = javax.swing.SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.pack();
        }

        game.start();
    }

    @Override
    public void exit() {
        if (resultTimer != null) resultTimer.stop();
        if (game != null) {
            game.stop();
            layers.remove(game);
            game = null;
        }
    }

    @Override
    public JComponent view() {
        return view;
    }
}
