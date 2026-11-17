package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 캐릭터 그림의 고정색 부분({@link FixedColorRegion})을 고른 색({@link CharacterColor})으로 다시 칠한다.
 * 서 있는·걷는 그림뿐 아니라 물풍선에 갇힌(bubble)·죽는(die)·바늘로 탈출하는(needle) 그림에도 그대로 쓸 수 있다.
 *
 * <p>OKLCH로 바꿔서 고정색 부분 픽셀의 색상각은 고른 색의 색상각으로 바꾸고, 채도와 밝기는 대표색과의 차이를 지킨다.</p>
 * <ul>
 *   <li>채도: 고른 색의 채도 × (픽셀 채도 / 대표색 채도)</li>
 *   <li>밝기: 대표색보다 어두우면 0~대표색 사이의 비율을 0~고른 색 사이에, 밝으면 대표색~1 사이의 비율을
 *       고른 색~1 사이에 옮긴다. 그래서 그림자는 그림자대로, 하이라이트는 하이라이트대로 남고 0~1을 넘지 않는다.</li>
 * </ul>
 * <p>대표색은 고른 색과 똑같은 sRGB가 된다. 바꾼 색이 sRGB로 나타낼 수 없으면 밝기·색상각은 두고 채도를 줄인다.</p>
 *
 * <p>옅어진 물풍선 속(bubble_2) 고정색({@link FixedColorRegion#fadedRanges})은 이렇게 바꾼 색과 원래 물빛을 OKLab에서
 * {@link #FADED_COLOR_SHARE} : 나머지로 섞는다. 그래서 원래 그림처럼 갇힌 그림이 bubble_1 → 2 → 3으로 갈수록
 * 고른 색이 물빛에 녹아 흐려진다.</p>
 */
public final class CharacterRecolor {

    /** 옅어진 물풍선 속 고정색에 남기는 고른 색의 몫. 나머지는 원래 물빛이다 */
    static final double FADED_COLOR_SHARE = 0.5;

    private CharacterRecolor() {
    }

    /**
     * mascot 그림 image의 고정색 부분을 color로 칠한 새 그림. 원본은 그대로 둔다.
     * 여러 장이 이어 붙은 시트도, 시트에서 자른 한 장도 된다. 투명한 픽셀과 고정색이 아닌 픽셀은 그대로 옮긴다.
     *
     * @param color 칠할 색. null이면 원래 색 그대로라 image를 그대로 돌려준다
     */
    public static BufferedImage recolor(BufferedImage image, Mascot mascot, CharacterColor color) {
        if (color == null) {
            return image;
        }
        FixedColorRegion region = FixedColorRegion.of(mascot);
        // 팔레트 그림이라 같은 색이 수없이 나오므로, 한 번 바꾼 색은 기억해 둔다
        Map<Integer, Integer> converted = new HashMap<>();
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb & 0xFF000000;
                if (alpha == 0) {
                    out.setRGB(x, y, argb);
                    continue;
                }
                int rgb = converted.computeIfAbsent(argb & 0xFFFFFF, original -> recolorRgb(region, color, original));
                out.setRGB(x, y, alpha | rgb);
            }
        }
        return out;
    }

    /** 색 하나(0xRRGGBB)를 다시 칠한 색. 고정색 부분의 색이 아니면 그대로 돌려준다. */
    public static int recolorRgb(Mascot mascot, CharacterColor color, int rgb) {
        return color == null ? rgb & 0xFFFFFF : recolorRgb(FixedColorRegion.of(mascot), color, rgb & 0xFFFFFF);
    }

    private static int recolorRgb(FixedColorRegion region, CharacterColor color, int rgb) {
        if (!region.contains(rgb)) {
            return rgb;
        }
        Oklch pixel = Oklch.fromRgb(rgb);
        Oklch shifted = shift(pixel, region.anchor(), color.oklch());
        if (region.isFaded(rgb)) {
            return mix(pixel, shifted.inGamut(), FADED_COLOR_SHARE).toRgb();
        }
        return shifted.toRgb();
    }

    /** OKLab에서 from과 to를 잇는 직선 위, from에서 to 쪽으로 share(0~1)만큼 간 색 */
    static Oklch mix(Oklch from, Oklch to, double share) {
        double fromA = from.c() * Math.cos(Math.toRadians(from.h()));
        double fromB = from.c() * Math.sin(Math.toRadians(from.h()));
        double toA = to.c() * Math.cos(Math.toRadians(to.h()));
        double toB = to.c() * Math.sin(Math.toRadians(to.h()));
        double a = fromA + (toA - fromA) * share;
        double b = fromB + (toB - fromB) * share;
        return new Oklch(from.l() + (to.l() - from.l()) * share, Math.hypot(a, b), Math.toDegrees(Math.atan2(b, a)));
    }

    /** anchor가 target이 되도록 pixel을 옮긴 색. sRGB 범위는 맞추지 않은 값이다. */
    static Oklch shift(Oklch pixel, Oklch anchor, Oklch target) {
        // 비율을 먼저 구해서 대표색이면 비율이 정확히 1이 되게 한다 (대표색 → 고른 색이 계산 오차 없이 맞는다)
        double l = pixel.l() <= anchor.l()
                ? target.l() * (pixel.l() / anchor.l())
                : target.l() + (1 - target.l()) * ((pixel.l() - anchor.l()) / (1 - anchor.l()));
        double c = target.c() * (pixel.c() / anchor.c());
        return new Oklch(l, c, target.h());
    }
}
