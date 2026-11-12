package hansung.arcade.ingame.render;

import hansung.arcade.ingame.map.TileMap;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 타일 그림 한 장과, 칸 왼쪽 위 기준으로 그림을 놓을 위치.
 * 블록 그림은 칸보다 키가 커서 위로 솟아 보이므로 offsetY가 보통 음수다.
 */
public record TileSprite(BufferedImage image, int offsetX, int offsetY) {

    private static final int T = TileMap.TILE_SIZE;

    /** 그림 바닥을 칸 바닥에 맞추고, 가로는 widthInTiles칸 너비의 가운데에 둔다. */
    public static TileSprite standing(BufferedImage image, int widthInTiles) {
        return new TileSprite(image, (widthInTiles * T - image.getWidth()) / 2, T - image.getHeight());
    }

    /** (px, py)는 칸 왼쪽 위 픽셀 좌표. */
    public void draw(Graphics2D g, int px, int py) {
        g.drawImage(image, px + offsetX, py + offsetY, null);
    }
}
