package hansung.arcade.ingame.render;

import hansung.arcade.ingame.airplane.Flight;
import hansung.arcade.ingame.entity.Direction;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirplaneSpritesTest {

    private static final AirplaneSprites SPRITES = AirplaneSprites.load();
    private static final double TICK = 1.0 / 60;

    @Test
    void 두_방향_모두_146x76_네_장을_0_1초씩_되풀이한다() {
        for (Direction heading : new Direction[]{Direction.RIGHT, Direction.LEFT}) {
            Set<BufferedImage> frames = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                BufferedImage frame = SPRITES.frame(heading, i * 0.1);
                assertEquals(AirplaneSprites.FRAME_WIDTH, frame.getWidth());
                assertEquals(AirplaneSprites.FRAME_HEIGHT, frame.getHeight());
                frames.add(frame);
            }
            assertEquals(4, frames.size(), heading.name());
            assertSame(SPRITES.frame(heading, 0), SPRITES.frame(heading, 0.4));

            // 게임 루프처럼 1/60초씩 더한 시간으로도 6틱째에 정확히 넘어간다
            double age = 0;
            for (int i = 0; i < 5; i++) {
                age += TICK;
            }
            assertSame(SPRITES.frame(heading, 0), SPRITES.frame(heading, age));
            assertSame(SPRITES.frame(heading, 0.1), SPRITES.frame(heading, age + TICK));
        }
    }

    @Test
    void 왼쪽으로_나는_그림은_오른쪽_그림을_뒤집은_모양으로_같은_차례에_돈다() {
        // 원본 그림의 두 번째 장만 연기 모양이 조금 달라서 나머지 세 장을 본다
        for (double seconds : new double[]{0, 0.2, 0.3}) {
            BufferedImage east = SPRITES.frame(Direction.RIGHT, seconds);
            BufferedImage west = SPRITES.frame(Direction.LEFT, seconds);
            for (int y = 0; y < AirplaneSprites.FRAME_HEIGHT; y++) {
                for (int x = 0; x < AirplaneSprites.FRAME_WIDTH; x++) {
                    assertEquals(east.getRGB(x, y), west.getRGB(AirplaneSprites.FRAME_WIDTH - 1 - x, y),
                            "%.1f초 (%d, %d)".formatted(seconds, x, y));
                }
            }
        }
    }

    @Test
    void 마젠타가_섞인_분홍_테두리가_남지_않는다() {
        for (Direction heading : new Direction[]{Direction.RIGHT, Direction.LEFT}) {
            for (int i = 0; i < 4; i++) {
                BufferedImage frame = SPRITES.frame(heading, i * 0.1);
                int opaque = 0;
                for (int y = 0; y < frame.getHeight(); y++) {
                    for (int x = 0; x < frame.getWidth(); x++) {
                        int argb = frame.getRGB(x, y);
                        if (argb >>> 24 != 0) {
                            opaque++;
                            assertFalse(AirplaneSprites.isMagentaFringe(argb),
                                    "%s %d번 (%d, %d) %06x".formatted(heading, i, x, y, argb & 0xFFFFFF));
                        }
                    }
                }
                assertTrue(opaque > 5000, "비행기 몸체는 남는다: " + opaque);
            }
        }
    }

    @Test
    void 분홍_픽셀은_투명한_곳에_이어진_것만_지우고_몸체에_둘러싸인_것은_남긴다() {
        int pink = 0xFFF546F5;
        int red = 0xFFA50000;
        // [투명][분홍][분홍][빨강][분홍][빨강]
        BufferedImage strip = new BufferedImage(6, 1, BufferedImage.TYPE_INT_ARGB);
        int[] row = {0, pink, pink, red, pink, red};
        for (int x = 0; x < row.length; x++) {
            strip.setRGB(x, 0, row[x]);
        }

        BufferedImage cleared = AirplaneSprites.clearFringe(strip);

        int[] expected = {0, 0, 0, red, pink, red};
        for (int x = 0; x < expected.length; x++) {
            assertEquals(expected[x], cleared.getRGB(x, 0), "x=" + x);
        }
    }

    @Test
    void 분홍빛만_마젠타가_섞인_색으로_본다() {
        assertTrue(AirplaneSprites.isMagentaFringe(0xF546F5));
        assertTrue(AirplaneSprites.isMagentaFringe(0xE86BE8));
        assertFalse(AirplaneSprites.isMagentaFringe(0xFFFFFF), "흰 연기");
        assertFalse(AirplaneSprites.isMagentaFringe(0xA50000), "빨간 날개");
        assertFalse(AirplaneSprites.isMagentaFringe(0xF8A800), "주황 몸체");
        assertFalse(AirplaneSprites.isMagentaFringe(0x3060F0), "파란 프로펠러");
    }

    @Test
    void 아이템을_떨어뜨리는_자리는_몸체_안이다() {
        for (int i = 0; i < 4; i++) {
            assertEquals(0xFF, SPRITES.frame(Direction.RIGHT, i * 0.1)
                    .getRGB(AirplaneSprites.cargoX(Direction.RIGHT), AirplaneSprites.CARGO_Y) >>> 24);
            assertEquals(0xFF, SPRITES.frame(Direction.LEFT, i * 0.1)
                    .getRGB(AirplaneSprites.cargoX(Direction.LEFT), AirplaneSprites.CARGO_Y) >>> 24);
        }
        assertEquals(AirplaneSprites.FRAME_WIDTH - AirplaneSprites.CARGO_X, AirplaneSprites.cargoX(Direction.LEFT));
    }

    @Test
    void 들어오기_전과_나간_뒤에는_그림이_다_맵_밖에_있다() {
        // 떨어뜨리는 자리에서 그림 양 끝까지가 비행기가 맵 밖에서 출발하는 거리보다 짧다
        assertTrue(AirplaneSprites.CARGO_X <= Flight.OFFSTAGE);
        assertTrue(AirplaneSprites.FRAME_WIDTH - AirplaneSprites.CARGO_X <= Flight.OFFSTAGE);
    }

    @Test
    void 위아래로는_날지_않는다() {
        assertThrows(IllegalArgumentException.class, () -> SPRITES.frame(Direction.UP, 0));
    }
}
