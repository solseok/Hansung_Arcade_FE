package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.WorldRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 창 없이 인게임 화면을 그림에 그려, 맵 끝에 붙은 캐릭터 그림이 어디까지 보이는지 확인한다.
 * 꼬꼬의 옆모습은 캐릭터 위치에서 24px까지 뻗고 충돌 박스는 18px이라, 벽에 붙으면 그림이 맵 밖으로 6px 나간다.
 */
class InGamePanelCharacterTest {

    private static final CharacterSprites SPRITES = CharacterSprites.load();
    private static final double TICK = 1.0 / 60;

    @Test
    void 맵_왼쪽_끝에_붙은_캐릭터_그림은_잘리지_않고_프레임_위에_겹쳐_보인다() {
        assertEquals(6, pixelsOutsideField("1" + ".".repeat(14), Direction.LEFT));
    }

    @Test
    void 맵_오른쪽_끝에_붙은_캐릭터_그림도_잘리지_않는다() {
        assertEquals(6, pixelsOutsideField(".".repeat(14) + "1", Direction.RIGHT));
    }

    /**
     * 꼬꼬를 가운데 줄의 스폰에 세우고 toward 쪽 맵 끝까지 밀어붙인 뒤 화면을 그린다.
     * 그림의 불투명한 픽셀이 모두 화면에 그대로 나왔는지 확인하고, 맵 영역 밖으로 가장 멀리 나간 거리(px)를 돌려준다.
     */
    private static int pixelsOutsideField(String spawnRow, Direction toward) {
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < 13; row++) {
            rows.add(row == 6 ? spawnRow : ".".repeat(15));
        }
        GameWorld world = new GameWorld(MapLoader.parse("fixture/edge", rows),
                new PlayerProfile("tester", Mascot.KKOKKO));
        for (int tick = 0; tick < 30; tick++) {
            world.update(TICK, toward);
        }
        Player player = world.localPlayer();
        // 맵 끝에 막혀 멈췄으므로 그쪽을 보고 서 있는 그림이 나온다
        assertFalse(player.isMoving());

        InGamePanel panel = new InGamePanel(world, Tileset.forTheme("fixture"),
                HudSkin.load(HudFonts.load()), SPRITES);
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        panel.paint(g);
        g.dispose();

        BufferedImage sprite = SPRITES.frame(Mascot.KKOKKO, toward, false, 0);
        int left = FIELD.x() + (int) Math.round(player.x()) - CharacterSprites.FRAME_SIZE / 2;
        int top = FIELD.y() + (int) Math.round(player.y()) + WorldRenderer.FEET_BELOW_CENTER - CharacterSprites.FEET_Y;
        int farthest = 0;
        for (int y = 0; y < CharacterSprites.FRAME_SIZE; y++) {
            for (int x = 0; x < CharacterSprites.FRAME_SIZE; x++) {
                int rgb = sprite.getRGB(x, y);
                if ((rgb >>> 24) == 0) {
                    continue;
                }
                int screenX = left + x;
                assertEquals(rgb & 0xFFFFFF, screen.getRGB(screenX, top + y) & 0xFFFFFF, "(%d, %d)".formatted(x, y));
                if (screenX < FIELD.x()) {
                    farthest = Math.max(farthest, FIELD.x() - screenX);
                } else if (screenX >= FIELD.right()) {
                    farthest = Math.max(farthest, screenX - FIELD.right() + 1);
                }
            }
        }
        return farthest;
    }
}
