package hansung.arcade.ingame.render;

import hansung.arcade.ingame.map.TileMap;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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

    /**
     * 그림 둘레의 투명한 여백을 잘라 내고, 내용이 widthInTiles칸 너비를 꽉 채우도록 키워서 칸 바닥에 세운다.
     * 그림마다 좌우·아래에 1~3px 여백이 남아 있으면 이웃 블록 사이로 바닥이 비쳐 틈처럼 보이기 때문이다.
     * 키울 때는 가로세로 비율을 지키고, 가장 가까운 픽셀을 그대로 써서 도트가 뭉개지지 않게 한다.
     * 여백이 없는 그림은 {@link #standing}과 같다.
     */
    public static TileSprite fitted(BufferedImage image, int widthInTiles) {
        Rectangle ink = opaqueBounds(image);
        if (ink.isEmpty()) {
            return standing(image, widthInTiles);
        }
        int footprint = widthInTiles * T;
        BufferedImage content = image.getSubimage(ink.x, ink.y, ink.width, ink.height);
        if (ink.width < footprint) {
            int height = (int) Math.round(ink.height * (footprint / (double) ink.width));
            content = scaleNearest(content, footprint, height);
        }
        return standing(content, widthInTiles);
    }

    /** 투명하지 않은 픽셀을 모두 담는 가장 작은 영역. 전부 투명하면 빈 영역. */
    static Rectangle opaqueBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage scaleNearest(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    /** (px, py)는 칸 왼쪽 위 픽셀 좌표. */
    public void draw(Graphics2D g, int px, int py) {
        g.drawImage(image, px + offsetX, py + offsetY, null);
    }
}
