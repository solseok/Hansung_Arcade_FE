package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemPanelTest {

    private static final ItemSprites ITEMS = ItemSprites.load();
    private static final HudSkin SKIN = HudSkin.load(HudFonts.load());

    @Test
    void 개수는_두_자리로_적는다() {
        assertEquals("×01", ItemPanel.countText(1));
        assertEquals("×02", ItemPanel.countText(2));
        assertEquals("×99", ItemPanel.countText(99));
    }

    @Test
    void 두_명이면_A_안에_같은_크기의_칸_두_개가_가로_가운데에_나란히_놓인다() {
        Box first = HudLayout.duoItemView(0);
        Box second = HudLayout.duoItemView(1);

        assertEquals(first.width(), second.width());
        assertEquals(first.y(), second.y());
        assertEquals(HudLayout.DUO_ITEM_VIEW_GAP, second.x() - first.right());
        assertEquals(first.x() - HudLayout.ITEM_INNER.x(), HudLayout.ITEM_INNER.right() - second.right(), 1);
        for (Box view : List.of(first, second, HudLayout.ITEM_VIEW)) {
            // 1P·2P 글씨와 개수 글씨까지 A 안에 들어간다
            assertTrue(HudLayout.itemSlotLabel(view).y() > HudLayout.ITEM_INNER.y());
            assertTrue(HudLayout.itemCount(view).bottom() < HudLayout.ITEM_INNER.bottom());
            // 아이템 그림이 칸 안에 들어간다
            assertTrue(HudLayout.ITEM_IMAGE_TOP + ItemType.IMAGE_SIZE <= view.height());
            assertTrue(ItemType.IMAGE_SIZE <= view.width());
        }
    }

    @Test
    void 액티브_아이템을_들면_칸에_그림이_나오고_칸이_비면_회색만_남는다() {
        TileMap map = MapLoader.parse("fixture/hud", List.of("1...2"));
        GameWorld world = new GameWorld(map, List.of(new PlayerProfile("a", Mascot.KKUKKU),
                new PlayerProfile("b", Mascot.KKOKKO)));
        InGameHud hud = new InGameHud(SKIN, map, Tileset.forTheme("fixture"), ITEMS, List.of("L-Shift", "Ctrl-R"));
        Box second = HudLayout.duoItemView(1);
        int centerX = second.x() + second.width() / 2;
        int centerY = second.y() + HudLayout.ITEM_IMAGE_TOP + ItemType.IMAGE_SIZE / 2;

        int empty = paint(hud, world).getRGB(centerX, centerY);
        world.items().place(4, 0, ItemType.SHIELD);
        world.update(1.0 / 60, Map.of());
        int filled = paint(hud, world).getRGB(centerX, centerY);

        assertEquals(HudPalette.ITEM_VIEW.getRGB(), empty);
        assertNotEquals(empty, filled);
    }

    @Test
    void 아이템_칸은_한_칸이나_두_칸만_그린다() {
        assertThrows(IllegalArgumentException.class, () -> new ItemPanel(SKIN, ITEMS, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ItemPanel(SKIN, ITEMS, List.of("a", "b", "c")));
    }

    private static BufferedImage paint(InGameHud hud, GameWorld world) {
        BufferedImage image = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        hud.paintBelowWorld(g);
        hud.paintAboveWorld(g, world);
        g.dispose();
        return image;
    }
}
