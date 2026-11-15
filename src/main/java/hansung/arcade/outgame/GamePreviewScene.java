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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;

/** 인게임 공개 API를 이용한 아웃게임 흐름 확인용 연결. 승패는 목업 결과로 확인한다. */
public final class GamePreviewScene implements Scene {
    private final JPanel view = new JPanel(new BorderLayout());
    private final HudSkin skin;
    private final CharacterSprites sprites;
    private InGamePanel game;
    private GameStartInfo start;
    private long startedAt;

    public GamePreviewScene(SceneManager scenes, HudSkin skin, CharacterSprites sprites) {
        this.skin = skin;
        this.sprites = sprites;
        JButton finish = new JButton("목업 결과 확인");
        finish.addActionListener(event -> {
            var standings = new ArrayList<GameResult.Standing>();
            int rank = 1;
            for (var player : start.players()) {
                standings.add(new GameResult.Standing(player.id(), player.nickname(), rank++));
            }
            scenes.show("result", new GameResult(standings, start.players().getFirst().id(),
                    (System.nanoTime() - startedAt) / 1_000_000_000));
        });
        view.add(finish, BorderLayout.SOUTH);
    }

    @Override
    public void enter(Object data) {
        start = (GameStartInfo) data;
        var map = MapLoader.loadResource(start.mapId());
        var local = start.players().getFirst();
        game = new InGamePanel(new GameWorld(map, new PlayerProfile(local.nickname(), local.character())),
                Tileset.forTheme(map.theme()), skin, sprites);
        view.add(game, BorderLayout.CENTER);
        view.revalidate();
        var window = javax.swing.SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.pack();
        }
        startedAt = System.nanoTime();
        game.start();
    }

    @Override
    public void exit() {
        if (game != null) {
            game.stop();
            view.remove(game);
            game = null;
        }
    }

    @Override
    public JComponent view() { return view; }
}
