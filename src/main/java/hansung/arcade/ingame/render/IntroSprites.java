package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;
import hansung.arcade.ingame.entity.CharacterColor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 경기 시작 연출 그림. BMP이고 마젠타 배경은 투명하다.
 *
 * <ul>
 *   <li>resources/intro/game_start.bmp: "GAME"·"START!!" 두 줄 글자. 연출에는 아랫줄 START!!만 쓰고,
 *       글자가 한 자씩 흩어지며 사라지도록 글자({@link Glyph})마다 잘라 둔다.</li>
 *   <li>resources/intro/player_pos.bmp: 내 캐릭터를 가리키는 주황 아래 화살표(45 x 56).
 *       팀 색(캐릭터 고정색)을 고른 플레이어는 그 색으로 다시 칠한 화살표를 쓴다 ({@link #arrow}).</li>
 * </ul>
 */
public final class IntroSprites {

    public static final String START_PATH = "/intro/game_start.bmp";
    public static final String ARROW_PATH = "/intro/player_pos.bmp";
    /** 화살표 그림의 바탕색(주황). 팀 색으로 칠할 때 이 색을 기준으로 밝기·채도를 옮긴다 */
    static final int ARROW_BASE_RGB = 0xFF8700;
    /** 글자 그림에서 이보다 어두운 픽셀을 외곽선으로 본다 (0~255 밝기) */
    private static final int OUTLINE_LUMA = 70;
    /** 글자 맨 위·아래에서 바깥 밝은 테두리만 있는 줄 수 */
    private static final int OUTER_RIM_ROWS = 3;
    /** 이보다 채도가 낮은 픽셀(흰 테두리·반짝임)은 색을 바꾸지 않는다 */
    private static final double NEUTRAL_CHROMA = 0.03;

    private static IntroSprites shared;

    private final List<Glyph> start;
    private final int startWidth;
    private final int startHeight;
    private final BufferedImage arrow;
    private final Map<CharacterColor, BufferedImage> tinted = new EnumMap<>(CharacterColor.class);

    /**
     * START!! 글자 한 자. x는 START!! 줄 왼쪽 끝에서 이 글자 왼쪽 끝까지 거리.
     * exclamation은 느낌표인지 (흩어질 때 이어진 느낌표 둘은 함께 움직인다)
     */
    public record Glyph(int x, BufferedImage image, boolean exclamation) {
    }

    private IntroSprites(List<Glyph> start, int startWidth, int startHeight, BufferedImage arrow) {
        this.start = List.copyOf(start);
        this.startWidth = startWidth;
        this.startHeight = startHeight;
        this.arrow = arrow;
    }

    /** 그림을 처음 쓸 때 한 번 읽어 두고 함께 쓴다. 파일이 없으면 실패한다 */
    public static synchronized IntroSprites shared() {
        if (shared == null) {
            shared = load();
        }
        return shared;
    }

    public static IntroSprites load() {
        BufferedImage sheet = read(START_PATH);
        BufferedImage line = lastLine(sheet);
        BufferedImage arrow = read(ARROW_PATH);
        return new IntroSprites(glyphs(line), line.getWidth(), line.getHeight(), arrow);
    }

    /** START!! 글자들. 왼쪽부터 */
    public List<Glyph> startGlyphs() {
        return start;
    }

    public int startWidth() {
        return startWidth;
    }

    public int startHeight() {
        return startHeight;
    }

    /** 원래 색(주황) 화살표 */
    public BufferedImage arrow() {
        return arrow;
    }

    /** color 팀의 화살표. null이면 원래 색 */
    public synchronized BufferedImage arrow(CharacterColor color) {
        if (color == null) {
            return arrow;
        }
        return tinted.computeIfAbsent(color, c -> tint(arrow, c.oklch()));
    }

    private static BufferedImage read(String path) {
        return SpriteSheet.find(path)
                .orElseThrow(() -> new IllegalStateException("시작 연출 그림 파일이 없습니다: " + path))
                .image();
    }

    /** 투명한 줄로 나뉜 글자 줄 가운데 맨 아랫줄 (game_start.bmp의 START!!) */
    static BufferedImage lastLine(BufferedImage sheet) {
        int bottom = sheet.getHeight() - 1;
        while (bottom > 0 && isBlankRow(sheet, bottom)) {
            bottom--;
        }
        int top = bottom;
        while (top > 0 && !isBlankRow(sheet, top - 1)) {
            top--;
        }
        return sheet.getSubimage(0, top, sheet.getWidth(), bottom - top + 1);
    }

    /**
     * 글자 줄을 투명한 세로줄을 경계로 한 자씩 자른다. 테두리가 맞닿아 두 글자가 한 덩어리가 된 곳(START의 ST)은
     * 다른 글자보다 1.5배 넘게 넓으므로, 가운데 1/3에서 잉크가 가장 적은 세로줄에서 나눈다.
     */
    static List<Glyph> glyphs(BufferedImage line) {
        List<int[]> runs = new ArrayList<>();
        int startX = -1;
        for (int x = 0; x <= line.getWidth(); x++) {
            boolean ink = x < line.getWidth() && inkInColumn(line, x) > 0;
            if (ink && startX < 0) {
                startX = x;
            } else if (!ink && startX >= 0) {
                runs.add(new int[] {startX, x - 1});
                startX = -1;
            }
        }
        List<Integer> widths = runs.stream().map(r -> r[1] - r[0] + 1).sorted().toList();
        int median = widths.isEmpty() ? 0 : widths.get(widths.size() / 2);
        List<Glyph> glyphs = new ArrayList<>();
        for (int[] run : runs) {
            int width = run[1] - run[0] + 1;
            Optional<Integer> split = width > median * 1.5 ? Optional.of(thinnestColumn(line, run)) : Optional.empty();
            if (split.isPresent()) {
                glyphs.add(glyph(line, run[0], split.get() - 1));
                glyphs.add(glyph(line, split.get(), run[1]));
            } else {
                glyphs.add(glyph(line, run[0], run[1]));
            }
        }
        return glyphs;
    }

    private static int thinnestColumn(BufferedImage line, int[] run) {
        int width = run[1] - run[0] + 1;
        int best = run[0] + width / 2;
        int bestInk = Integer.MAX_VALUE;
        for (int x = run[0] + width / 3; x <= run[0] + 2 * width / 3; x++) {
            int ink = inkInColumn(line, x);
            if (ink < bestInk) {
                bestInk = ink;
                best = x;
            }
        }
        return best;
    }

    private static Glyph glyph(BufferedImage line, int left, int right) {
        BufferedImage image = line.getSubimage(left, 0, right - left + 1, line.getHeight());
        BufferedImage upright = uprightExclamation(image);
        return new Glyph(left, upright, upright != image);
    }

    /**
     * game_start.bmp의 느낌표 두 개는 위아래가 뒤집혀 있다(점이 위, 막대가 아래라 "ii"처럼 보인다).
     * 점과 막대는 밝은 테두리로 맞닿아 있어 빈 줄이 없으므로, 글자 안쪽에서 어두운 외곽선이 하나도 없는 줄을 두 덩어리의 경계로 본다
     * (S·T·A·R처럼 한 덩어리인 글자는 안쪽 모든 줄에 외곽선이 있다). 위 덩어리(점)가 아래(막대)보다 작으면 뒤집힌 느낌표로 보고
     * 잉크가 있는 줄 범위 안에서 위아래를 뒤집어 점이 아래 글자선에 오게 한다. 다른 글자는 그대로 돌려준다.
     */
    static BufferedImage uprightExclamation(BufferedImage glyph) {
        int top = 0;
        while (top < glyph.getHeight() && isBlankRow(glyph, top)) {
            top++;
        }
        int bottom = glyph.getHeight() - 1;
        while (bottom > top && isBlankRow(glyph, bottom)) {
            bottom--;
        }
        int separator = -1;
        // 맨 위·아래 몇 줄은 바깥 밝은 테두리뿐이라 빼고 본다
        for (int y = top + OUTER_RIM_ROWS; y <= bottom - OUTER_RIM_ROWS; y++) {
            if (!isBlankRow(glyph, y) && !hasOutline(glyph, y)) {
                separator = y;
                break;
            }
        }
        if (separator < 0 || separator - top >= bottom - separator) {
            return glyph;
        }
        BufferedImage upright = new BufferedImage(glyph.getWidth(), glyph.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < glyph.getHeight(); y++) {
            int from = y < top || y > bottom ? y : top + bottom - y;
            for (int x = 0; x < glyph.getWidth(); x++) {
                upright.setRGB(x, y, glyph.getRGB(x, from));
            }
        }
        return upright;
    }

    /** y줄에 어두운 외곽선 픽셀이 있는지 */
    private static boolean hasOutline(BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            if ((argb >>> 24) == 0) {
                continue;
            }
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            if ((r * 299 + g * 587 + b * 114) / 1000 < OUTLINE_LUMA) {
                return true;
            }
        }
        return false;
    }

    private static int inkInColumn(BufferedImage image, int x) {
        int ink = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if ((image.getRGB(x, y) >>> 24) != 0) {
                ink++;
            }
        }
        return ink;
    }

    private static boolean isBlankRow(BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 주황 화살표를 target 색 화살표로 칠한다. 채도가 있는 픽셀은 OKLCH 색상각을 모두 target 색상각으로 맞추고(주황 몸통과
     * 노란 반짝임이 같은 색 계열의 진하고 밝은 부분이 되게), 채도는 target/주황 비율로, 밝기는 그 차이만큼(채도가 있는 만큼) 옮긴다.
     * 흰 테두리처럼 채도가 거의 없는 픽셀은 그대로다.
     */
    static BufferedImage tint(BufferedImage source, Oklch target) {
        Oklch base = Oklch.fromRgb(ARROW_BASE_RGB);
        double chromaScale = target.c() / base.c();
        double lightShift = target.l() - base.l();
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }
                Oklch pixel = Oklch.fromRgb(argb & 0xFFFFFF);
                if (pixel.c() < NEUTRAL_CHROMA) {
                    out.setRGB(x, y, argb);
                    continue;
                }
                double weight = Math.min(1, pixel.c() / base.c());
                Oklch moved = new Oklch(
                        Math.max(0, Math.min(1, pixel.l() + lightShift * weight)),
                        pixel.c() * chromaScale,
                        target.h());
                out.setRGB(x, y, (alpha << 24) | moved.toRgb());
            }
        }
        return out;
    }
}
