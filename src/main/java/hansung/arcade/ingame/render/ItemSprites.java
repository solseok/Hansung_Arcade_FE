package hansung.arcade.ingame.render;

import hansung.arcade.ingame.item.ItemType;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * 맵에 떨어진 아이템 그림. 아이템마다 {@link ItemType#imagePath()}의 38 x 38 BMP 한 장이고 마젠타 배경은 투명하다.
 */
public final class ItemSprites {

    private final Map<ItemType, BufferedImage> images;

    private ItemSprites(Map<ItemType, BufferedImage> images) {
        this.images = images;
    }

    /** 모든 아이템 그림을 읽는다. 빠진 파일이 있으면 실패한다. */
    public static ItemSprites load() {
        Map<ItemType, BufferedImage> images = new EnumMap<>(ItemType.class);
        for (ItemType item : ItemType.values()) {
            String path = item.imagePath();
            images.put(item, SpriteSheet.find(path)
                    .orElseThrow(() -> new IllegalStateException("아이템 그림 파일이 없습니다: " + path))
                    .frames(ItemType.IMAGE_SIZE).get(0));
        }
        return new ItemSprites(images);
    }

    public BufferedImage image(ItemType item) {
        return images.get(item);
    }
}
