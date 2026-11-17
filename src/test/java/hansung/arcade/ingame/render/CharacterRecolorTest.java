package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 캐릭터 그림(resources/sprites/characters)의 모든 포즈를 다시 칠해서, 고정색 부분만 바뀌고
 * 외곽선·피부·눈·물풍선·이펙트는 그대로인지 확인한다.
 */
class CharacterRecolorTest {

    /** 캐릭터마다 있는 그림 파일 이름(id_ 뒤). 다음 연출에서 쓸 물풍선·죽음·바늘 그림까지 모두 */
    private static final List<String> POSES = List.of("front", "back", "left", "right",
            "walk_front", "walk_back", "walk_left", "walk_right",
            "bubble_1", "bubble_2", "bubble_3", "die_1", "die_2", "needle");
    /** 갇힘·죽음·바늘 그림. 물풍선 물빛과 이펙트가 들어 있다 */
    private static final List<String> EFFECT_POSES = List.of("bubble_1", "bubble_2", "bubble_3", "die_1", "die_2", "needle");

    /**
     * 캐릭터마다 바뀌면 안 되는 색(외곽선, 피부, 눈, 볼터치, 볏·부리·발, 목도리, 배). 그림에 실제로 있는 색인지도 확인한다.
     * 꼭꼬 볏은 한성냥이 가방과 같은 빨강 팔레트라, 캐릭터마다 기준이 따로 있어야 함을 보여 준다.
     */
    private static final Map<Mascot, int[]> KEPT = new EnumMap<>(Map.of(
            Mascot.SANGSANGBUGI, new int[]{0x101838, 0x0C162C, 0x46507A, 0xF4F6FD, 0xDCE2F2, 0xBCC4DE, 0x1A1C34,
                    0x9CBCEE, 0x8EC0F2, 0x7EC3F5},
            Mascot.HANSUNGNYANGI, new int[]{0x802C32, 0xF07E72, 0xFCA496, 0xFFB6B6, 0xD6605A, 0xB64646, 0x46507A,
                    0xF4F6FD, 0xDCE2F2, 0x1A1C34, 0xA06D7C, 0xB67F8A},
            Mascot.KKOKKO, new int[]{0xE43A3E, 0xC0242E, 0x941824, 0x640E1A, 0xF86C66, 0xF48C3C, 0xDC6828, 0x76300C,
                    0xA0A6BA, 0x6E748A, 0x323648, 0x46507A, 0xF4F6FD, 0x1A1C34},
            Mascot.KKUKKU, new int[]{0x401E10, 0xF48C3C, 0xDC6828, 0x76300C, 0xFAC832, 0xFFE678, 0xE2A21E, 0xC27E14,
                    0xE43A3E, 0xB02838, 0x1A1C34},
            Mascot.SANGZZI, new int[]{0x164022, 0xFFFCE2, 0xF4F0BE, 0xDEDA98, 0x1A1C34, 0x306963}));

    /** 정면 그림 한 장에서 다시 칠해지는 픽셀 수의 아래 한계. 고정색 부분이 통째로 빠지지 않았는지 본다 */
    private static final Map<Mascot, Integer> FRONT_MIN_CHANGED = new EnumMap<>(Map.of(
            Mascot.SANGSANGBUGI, 700, Mascot.HANSUNGNYANGI, 100, Mascot.KKOKKO, 80,
            Mascot.KKUKKU, 1000, Mascot.SANGZZI, 1300));

    @Test
    void 대표색은_고른_색과_같은_sRGB가_된다() {
        for (Mascot mascot : Mascot.values()) {
            int anchor = FixedColorRegion.of(mascot).anchorRgb();
            assertTrue(colorsOf(mascot, List.of("front")).contains(anchor), mascot + " 정면 그림에 대표색이 있다");
            for (CharacterColor color : CharacterColor.values()) {
                assertEquals(hex(color.rgb()), hex(CharacterRecolor.recolorRgb(mascot, color, anchor)), mascot + " " + color);
            }
        }
    }

    @Test
    void 외곽선_피부_눈_같은_다른_부분은_바뀌지_않는다() {
        for (Mascot mascot : Mascot.values()) {
            Set<Integer> used = colorsOf(mascot, POSES);
            for (int kept : KEPT.get(mascot)) {
                assertTrue(used.contains(kept), mascot + " 그림에 " + hex(kept) + " 이 있다");
                for (CharacterColor color : CharacterColor.values()) {
                    assertEquals(hex(kept), hex(CharacterRecolor.recolorRgb(mascot, color, kept)), mascot + " " + color);
                }
            }
        }
    }

    @Test
    void 모든_캐릭터가_같이_쓰는_물풍선_물빛과_이펙트_색은_바뀌지_않는다() {
        Set<Integer> shared = null;
        for (Mascot mascot : Mascot.values()) {
            Set<Integer> colors = colorsOf(mascot, EFFECT_POSES);
            if (shared == null) {
                shared = colors;
            } else {
                shared.retainAll(colors);
            }
        }
        // 물풍선 테두리·물빛·반짝임, 죽을 때 터지는 빛·번개·별
        for (int known : new int[]{0x0E4A9C, 0x1C7CD4, 0x38A6EE, 0x6CCCFC, 0xAEE8FF, 0xF6FDFF, 0x2880BE, 0x1CCCD0,
                0xFAC832, 0x7C4808}) {
            assertTrue(shared.contains(known), hex(known));
        }
        for (Mascot mascot : Mascot.values()) {
            for (CharacterColor color : CharacterColor.values()) {
                for (int rgb : shared) {
                    assertEquals(hex(rgb), hex(CharacterRecolor.recolorRgb(mascot, color, rgb)), mascot + " " + color);
                }
            }
        }
    }

    @Test
    void 정면_그림과_물풍선에_갇힌_그림_모두_고정색_부분이_바뀐다() {
        for (Mascot mascot : Mascot.values()) {
            BufferedImage front = firstFrame(mascot, "front");
            BufferedImage bubble = firstFrame(mascot, "bubble_1");
            for (CharacterColor color : CharacterColor.values()) {
                String what = mascot + " " + color;
                int frontChanged = changedPixels(front, CharacterRecolor.recolor(front, mascot, color));
                assertTrue(frontChanged >= FRONT_MIN_CHANGED.get(mascot), what + " 정면 " + frontChanged);
                int bubbleChanged = changedPixels(bubble, CharacterRecolor.recolor(bubble, mascot, color));
                assertTrue(bubbleChanged >= 25, what + " 물풍선 " + bubbleChanged);
            }
        }
    }

    /** 물풍선 물빛. 갇힌 그림은 bubble_1 → 2 → 3으로 갈수록 고정색이 이 색에 녹아 흐려진다 */
    private static final Oklch WATER = Oklch.fromRgb(0x38A6EE);

    @Test
    void 물풍선에_갇힌_그림을_다시_칠해도_bubble_2가_bubble_1보다_흐리다() {
        for (Mascot mascot : Mascot.values()) {
            for (CharacterColor color : CharacterColor.values()) {
                // 하늘·파랑은 물빛과 색상각이 가까워(45도 안) 두 단계 모두 물빛과 구별이 잘 안 된다.
                // 물빛 쪽으로 섞으면 채도가 물빛 채도만큼 오를 수 있어 이 비교에서 뺀다
                if (hueDistance(color.oklch().h(), WATER.h()) <= 45) {
                    continue;
                }
                double[] first = fading(mascot, "bubble_1", color);
                double[] second = fading(mascot, "bubble_2", color);
                // 꾸꾸의 bubble_2는 고정색이 다 물빛이 되어 다시 칠하지 않는다. 원래 그림처럼 흐리다
                if (second[2] == 0) {
                    continue;
                }
                String what = "%s %s bubble_1 %.3f/%.3f, bubble_2 %.3f/%.3f".formatted(mascot, color,
                        first[0], first[1], second[0], second[1]);
                assertTrue(second[0] < 0.8 * first[0], "채도 " + what);
                assertTrue(second[1] < 0.8 * first[1], "물빛과의 대비 " + what);
            }
        }
    }

    @Test
    void 옅어진_물풍선_속_고정색은_다시_칠한_색과_원래_색의_가운데다() {
        // 상상부기 옅어진 물풍선 속 후드
        FixedColorRegion region = FixedColorRegion.of(Mascot.SANGSANGBUGI);
        int faded = 0x569AD3;
        assertTrue(region.isFaded(faded));
        Oklch original = Oklch.fromRgb(faded);
        Oklch red = CharacterColor.RED.oklch();
        Oklch full = CharacterRecolor.shift(original, region.anchor(), red).inGamut();
        Oklch half = Oklch.fromRgb(CharacterRecolor.recolorRgb(Mascot.SANGSANGBUGI, CharacterColor.RED, faded));

        assertEquals((original.l() + full.l()) / 2, half.l(), 0.005);
        // 원래 물빛보다 붉고, 다 칠한 색보다 채도가 낮다
        assertTrue(hueDistance(half.h(), red.h()) < hueDistance(original.h(), red.h()));
        assertTrue(half.c() < full.c());
        // bubble_1의 물풍선 속 후드는 섞지 않고 다 칠한다
        assertFalse(region.isFaded(0x4373B3));
        assertTrue(region.contains(0x4373B3));
    }

    @Test
    void 고정색_부분의_밝기_순서는_다시_칠해도_그대로다() {
        for (Mascot mascot : Mascot.values()) {
            FixedColorRegion region = FixedColorRegion.of(mascot);
            List<Integer> regionColors = new ArrayList<>(colorsOf(mascot, List.of("front", "back", "left")).stream()
                    .filter(region::contains).toList());
            regionColors.sort(Comparator.comparingDouble(rgb -> Oklch.fromRgb(rgb).l()));
            for (CharacterColor color : CharacterColor.values()) {
                double previous = -1;
                for (int rgb : regionColors) {
                    double l = Oklch.fromRgb(CharacterRecolor.recolorRgb(mascot, color, rgb)).l();
                    // sRGB 정수로 반올림하면서 밝기가 0.005 안쪽으로 흔들릴 수 있다
                    assertTrue(l >= previous - 0.005, "%s %s %s".formatted(mascot, color, hex(rgb)));
                    previous = l;
                }
            }
        }
    }

    @Test
    void 대표색보다_어두운_색은_0까지의_비율을_밝은_색은_1까지의_비율을_지킨다() {
        Oklch anchor = new Oklch(0.4, 0.1, 265);
        Oklch target = new Oklch(0.8, 0.15, 100);

        // 대표색 밝기의 절반 → 고른 색 밝기의 절반, 채도도 대표색 대비 비율 그대로
        Oklch shadow = CharacterRecolor.shift(new Oklch(0.2, 0.05, 270), anchor, target);
        assertEquals(0.4, shadow.l(), 1e-12);
        assertEquals(0.075, shadow.c(), 1e-12);
        assertEquals(100, shadow.h(), 1e-12);

        // 대표색~흰색의 가운데 → 고른 색~흰색의 가운데
        Oklch highlight = CharacterRecolor.shift(new Oklch(0.7, 0.05, 260), anchor, target);
        assertEquals(0.9, highlight.l(), 1e-12);

        Oklch same = CharacterRecolor.shift(anchor, anchor, target);
        assertEquals(target, same);
    }

    @Test
    void 색을_고르지_않으면_원본을_그대로_돌려준다() {
        BufferedImage front = firstFrame(Mascot.SANGZZI, "front");
        assertSame(front, CharacterRecolor.recolor(front, Mascot.SANGZZI, null));
        assertEquals(0x70BE40, CharacterRecolor.recolorRgb(Mascot.SANGZZI, null, 0x70BE40));
    }

    @Test
    void 원본은_그대로_두고_크기와_투명한_곳이_같은_새_그림을_만든다() {
        // 죽는 그림 넷째 장: 캐릭터 둘레에 번개 이펙트가 있다
        BufferedImage frame = SpriteSheet.find(CharacterSprites.DIRECTORY + "kkukku_die_1.bmp").orElseThrow()
                .frames(CharacterSprites.FRAME_SIZE).get(3);
        int w = frame.getWidth();
        int h = frame.getHeight();
        int[] before = frame.getRGB(0, 0, w, h, null, 0, w);

        BufferedImage recolored = CharacterRecolor.recolor(frame, Mascot.KKUKKU, CharacterColor.PURPLE);

        assertNotSame(frame, recolored);
        assertEquals(w, recolored.getWidth());
        assertEquals(h, recolored.getHeight());
        assertArrayEquals(before, frame.getRGB(0, 0, w, h, null, 0, w), "원본 픽셀");
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(frame.getRGB(x, y) >>> 24, recolored.getRGB(x, y) >>> 24, "(%d, %d)".formatted(x, y));
            }
        }
    }

    private static BufferedImage firstFrame(Mascot mascot, String pose) {
        return SpriteSheet.find(CharacterSprites.DIRECTORY + mascot.id() + "_" + pose + ".bmp").orElseThrow()
                .frames(CharacterSprites.FRAME_SIZE).get(0);
    }

    /** 캐릭터 그림들에 쓰인 불투명한 색(0xRRGGBB) */
    private static Set<Integer> colorsOf(Mascot mascot, List<String> poses) {
        Set<Integer> colors = new HashSet<>();
        for (String pose : poses) {
            String path = CharacterSprites.DIRECTORY + mascot.id() + "_" + pose + ".bmp";
            for (BufferedImage frame : SpriteSheet.find(path).orElseThrow().frames(CharacterSprites.FRAME_SIZE)) {
                for (int y = 0; y < frame.getHeight(); y++) {
                    for (int x = 0; x < frame.getWidth(); x++) {
                        int argb = frame.getRGB(x, y);
                        if ((argb >>> 24) != 0) {
                            colors.add(argb & 0xFFFFFF);
                        }
                    }
                }
            }
        }
        return colors;
    }

    /**
     * mascot의 pose 그림에서 원래 고정색인 픽셀을 color로 다시 칠했을 때의 평균 채도, 물빛과의 평균 OKLab 거리, 픽셀 수
     */
    private static double[] fading(Mascot mascot, String pose, CharacterColor color) {
        FixedColorRegion region = FixedColorRegion.of(mascot);
        double chroma = 0;
        double contrast = 0;
        int count = 0;
        String path = CharacterSprites.DIRECTORY + mascot.id() + "_" + pose + ".bmp";
        for (BufferedImage frame : SpriteSheet.find(path).orElseThrow().frames(CharacterSprites.FRAME_SIZE)) {
            for (int y = 0; y < frame.getHeight(); y++) {
                for (int x = 0; x < frame.getWidth(); x++) {
                    int argb = frame.getRGB(x, y);
                    if ((argb >>> 24) == 0 || !region.contains(argb & 0xFFFFFF)) {
                        continue;
                    }
                    Oklch recolored = Oklch.fromRgb(CharacterRecolor.recolorRgb(mascot, color, argb));
                    chroma += recolored.c();
                    contrast += labDistance(recolored, WATER);
                    count++;
                }
            }
        }
        return count == 0 ? new double[]{0, 0, 0} : new double[]{chroma / count, contrast / count, count};
    }

    private static double labDistance(Oklch p, Oklch q) {
        double da = p.c() * Math.cos(Math.toRadians(p.h())) - q.c() * Math.cos(Math.toRadians(q.h()));
        double db = p.c() * Math.sin(Math.toRadians(p.h())) - q.c() * Math.sin(Math.toRadians(q.h()));
        return Math.sqrt((p.l() - q.l()) * (p.l() - q.l()) + da * da + db * db);
    }

    /** 두 색상각 사이의 각도(0~180) */
    private static double hueDistance(double h1, double h2) {
        double d = Math.abs(h1 - h2) % 360;
        return Math.min(d, 360 - d);
    }

    private static int changedPixels(BufferedImage before, BufferedImage after) {
        int changed = 0;
        for (int y = 0; y < before.getHeight(); y++) {
            for (int x = 0; x < before.getWidth(); x++) {
                if (before.getRGB(x, y) != after.getRGB(x, y)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private static String hex(int rgb) {
        return "#%06X".formatted(rgb & 0xFFFFFF);
    }
}
