package hansung.arcade.core.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OklchTest {

    @Test
    void sRGB를_OKLCH로_바꿨다가_되돌리면_채널마다_1_이내로_같다() {
        int worst = 0;
        // 0, 3, 6 … 255 의 모든 조합(86³ ≈ 64만 색). 끝값 255 가 들어가도록 3씩 건너뛴다
        for (int r = 0; r <= 255; r += 3) {
            for (int g = 0; g <= 255; g += 3) {
                for (int b = 0; b <= 255; b += 3) {
                    int rgb = (r << 16) | (g << 8) | b;
                    int back = Oklch.fromRgb(rgb).toRgb();
                    worst = Math.max(worst, channelDistance(rgb, back));
                    if (worst > 1) {
                        throw new AssertionError("#%06X → #%06X".formatted(rgb, back));
                    }
                }
            }
        }
        assertTrue(worst <= 1);
    }

    @Test
    void 회색과_기본색은_정확히_되돌아온다() {
        for (int v = 0; v <= 255; v++) {
            int gray = (v << 16) | (v << 8) | v;
            assertEquals(gray, Oklch.fromRgb(gray).toRgb(), "#%06X".formatted(gray));
        }
        for (int rgb : new int[]{0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0x00FFFF, 0xFF00FF, 0x53B6C3, 0xD52F8F}) {
            assertEquals(rgb, Oklch.fromRgb(rgb).toRgb(), "#%06X".formatted(rgb));
        }
    }

    @Test
    void 알려진_OKLCH_값과_같다() {
        // Björn Ottosson 의 OKLab 기준값: sRGB 빨강 = OKLab(0.627955, 0.224863, 0.125846)
        Oklch red = Oklch.fromRgb(0xFF0000);
        assertEquals(0.62796, red.l(), 1e-4);
        assertEquals(Math.hypot(0.224863, 0.125846), red.c(), 1e-4);
        assertEquals(Math.toDegrees(Math.atan2(0.125846, 0.224863)), red.h(), 0.05);

        assertEquals(1.0, Oklch.fromRgb(0xFFFFFF).l(), 1e-6);
        assertEquals(0.0, Oklch.fromRgb(0xFFFFFF).c(), 1e-6);
        assertEquals(0.0, Oklch.fromRgb(0x000000).l(), 1e-9);
    }

    @Test
    void 색상각은_0이상_360미만으로_맞춘다() {
        assertEquals(330, new Oklch(0.5, 0.1, -30).h(), 1e-9);
        assertEquals(10, new Oklch(0.5, 0.1, 370).h(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new Oklch(0.5, -0.1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Oklch(Double.NaN, 0.1, 0));
    }

    @Test
    void sRGB_범위_밖이면_밝기와_색상각은_두고_채도만_줄인다() {
        Oklch tooVivid = new Oklch(0.7, 0.4, 145);
        assertFalse(tooVivid.isInGamut());

        Oklch mapped = tooVivid.inGamut();
        assertTrue(mapped.isInGamut());
        assertEquals(0.7, mapped.l(), 1e-12);
        assertEquals(145, mapped.h(), 1e-12);
        assertTrue(mapped.c() < 0.4 && mapped.c() > 0.15, "채도 " + mapped.c());
        // 줄인 채도가 가장 큰 값이다: 조금만 더 올리면 범위를 벗어난다
        assertFalse(mapped.withChroma(mapped.c() + 1e-4).isInGamut());

        // sRGB로 바꾼 색도 밝기·색상각이 거의 그대로다
        Oklch back = Oklch.fromRgb(tooVivid.toRgb());
        assertEquals(0.7, back.l(), 0.005);
        assertEquals(145, back.h(), 1.0);
    }

    @Test
    void 범위_안의_색은_그대로_두고_밝기가_0이나_1을_넘으면_검정이나_흰색이다() {
        Oklch inside = new Oklch(0.6, 0.1, 30);
        assertSame(inside, inside.inGamut());
        assertEquals(0x000000, new Oklch(-0.2, 0.1, 30).toRgb());
        assertEquals(0xFFFFFF, new Oklch(1.3, 0.1, 30).toRgb());
    }

    private static int channelDistance(int a, int b) {
        int worst = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            worst = Math.max(worst, Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)));
        }
        return worst;
    }
}
