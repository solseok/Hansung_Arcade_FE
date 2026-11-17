package hansung.arcade.ingame.render;

import hansung.arcade.ingame.item.ItemType;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSpritesTest {

    private static final ItemSprites SPRITES = ItemSprites.load();

    @Test
    void 아이템_11종_그림이_모두_38px_정사각형이고_마젠타_배경은_투명하다() {
        for (ItemType item : ItemType.values()) {
            BufferedImage image = SPRITES.image(item);

            assertEquals(ItemType.IMAGE_SIZE, image.getWidth(), item.name());
            assertEquals(ItemType.IMAGE_SIZE, image.getHeight(), item.name());
            // 네 귀퉁이는 배경이다
            assertEquals(0, image.getRGB(0, 0) >>> 24, item.name());
            assertEquals(0, image.getRGB(ItemType.IMAGE_SIZE - 1, 0) >>> 24, item.name());
            int opaque = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) == 0xFF) {
                        opaque++;
                    }
                }
            }
            assertTrue(opaque > 300, item.name() + " 불투명 픽셀 " + opaque);
        }
    }
}
