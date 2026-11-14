package hansung.arcade.ingame.render;

import hansung.arcade.ingame.balloon.WaterCell;
import hansung.arcade.ingame.balloon.WaterCell.Part;
import hansung.arcade.ingame.entity.Direction;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalloonSpritesTest {

    private static final BalloonSprites SPRITES = BalloonSprites.load();

    private static BufferedImage water(Part part, Direction direction, int frame) {
        return SPRITES.water(new WaterCell(0, 0, part, part == Part.CENTER ? null : direction), frame);
    }

    @Test
    void 놓인_물풍선은_47x47_네_장을_0_2초씩_되풀이한다() {
        BufferedImage first = SPRITES.balloon(0);
        assertEquals(BalloonSprites.BALLOON_SIZE, first.getWidth());
        assertEquals(BalloonSprites.BALLOON_SIZE, first.getHeight());

        // 게임 루프처럼 1/60초씩 더한 나이로도 12틱째에 정확히 넘어간다
        double age = 0;
        for (int i = 0; i < 11; i++) {
            age += 1.0 / 60;
        }
        assertSame(first, SPRITES.balloon(age));
        age += 1.0 / 60;
        assertSame(SPRITES.balloon(0.2), SPRITES.balloon(age));
        assertNotSame(first, SPRITES.balloon(age));
        assertNotSame(SPRITES.balloon(0.2), SPRITES.balloon(0.4));
        assertNotSame(SPRITES.balloon(0.4), SPRITES.balloon(0.6));
        assertSame(first, SPRITES.balloon(0.8));
    }

    @Test
    void 놓인_물풍선은_몸체가_불투명하고_그림자는_반투명_알파로_들어_있다() {
        for (int frame = 0; frame < 4; frame++) {
            BufferedImage image = SPRITES.balloon(frame * 0.2);
            int partial = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha > 0 && alpha < 0xFF) {
                        partial++;
                    }
                }
            }
            assertTrue(partial > 100, frame + "번째 장의 반투명 픽셀 " + partial);
            assertEquals(0, image.getRGB(0, 0) >>> 24, "바탕은 투명하다");
        }
        // 눌리지 않은 장은 몸체 가운데가 그림의 (24, 20) 둘레다
        assertEquals(0xFF, SPRITES.balloon(0).getRGB(BalloonSprites.BALLOON_CENTER_X, BalloonSprites.BALLOON_CENTER_Y) >>> 24);
    }

    @Test
    void 물줄기_가운데_칸은_40x40을_꽉_채운다() {
        for (int frame = 0; frame < 3; frame++) {
            assertOpaque(water(Part.CENTER, null, frame));
            for (Direction direction : Direction.values()) {
                assertOpaque(water(Part.MIDDLE, direction, frame));
            }
        }
    }

    @Test
    void 물줄기_끝_칸은_검정_바탕을_지우고_가운데_칸과_맞닿는_줄은_빈틈이_없다() {
        for (Direction direction : Direction.values()) {
            int clearedPixels = 0;
            for (int frame = 0; frame < 3; frame++) {
                BufferedImage end = water(Part.END, direction, frame);
                assertEquals(40, end.getWidth());
                assertEquals(40, end.getHeight());
                int innerOpaque = 0;
                for (int y = 0; y < 40; y++) {
                    for (int x = 0; x < 40; x++) {
                        int argb = end.getRGB(x, y);
                        if ((argb >>> 24) == 0) {
                            clearedPixels++;
                        } else {
                            assertTrue((argb & 0xFFFFFF) != 0, "검정이 남았다: %s %d (%d, %d)".formatted(direction, frame, x, y));
                        }
                    }
                }
                for (int k = 0; k < 40; k++) {
                    int x = switch (direction) {
                        case LEFT -> 39;
                        case RIGHT -> 0;
                        default -> k;
                    };
                    int y = switch (direction) {
                        case UP -> 39;
                        case DOWN -> 0;
                        default -> k;
                    };
                    if ((end.getRGB(x, y) >>> 24) != 0) {
                        innerOpaque++;
                    }
                }
                // 모서리 한 픽셀 정도는 비어도 된다 (가운데 칸 그림도 모서리가 둥글다)
                assertTrue(innerOpaque >= 38, "%s %d번째 장 안쪽 줄 %d/40".formatted(direction, frame, innerOpaque));
            }
            assertTrue(clearedPixels > 500, direction + " 끝 칸의 지운 바탕 " + clearedPixels);
        }
    }

    @Test
    void 안쪽_가장자리가_빈_끝_그림은_바로_안쪽_색으로_메운다() {
        BufferedImage frame = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        frame.setRGB(2, 1, 0xFF112233);

        BalloonSprites.sealInnerEdge(frame, Direction.LEFT);

        assertEquals(0xFF112233, frame.getRGB(3, 1));
        assertEquals(0, frame.getRGB(3, 0), "안쪽도 비어 있으면 그대로 둔다");
    }

    private static void assertOpaque(BufferedImage image) {
        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                assertEquals(0xFF, image.getRGB(x, y) >>> 24, "(%d, %d)".formatted(x, y));
            }
        }
    }
}
