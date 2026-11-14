package hansung.arcade.ingame.hud;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HUD 디자인 글꼴. HY견고딕·HY윤고딕은 한컴 오피스 등과 함께 설치되는 글꼴이라 PC마다 있을 수도 없을 수도 있고,
 * Michroma는 resources/fonts/Michroma-Regular.ttf 를 두면 그것을 쓴다.
 * 찾지 못한 글꼴은 Java 기본 산세리프 굵게로 대신하고 {@link #missing()}에 이름을 남긴다.
 */
public final class HudFonts {

    public static final String MICHROMA_RESOURCE = "/fonts/Michroma-Regular.ttf";

    private final Font gyeonGothic;
    private final Font yunGothic;
    private final Font michroma;
    private final List<String> missing;

    private HudFonts(Font gyeonGothic, Font yunGothic, Font michroma, List<String> missing) {
        this.gyeonGothic = gyeonGothic;
        this.yunGothic = yunGothic;
        this.michroma = michroma;
        this.missing = List.copyOf(missing);
    }

    /** 설치된 글꼴을 훑어 찾는다. 글꼴 목록을 읽는 데 시간이 걸리므로 구동 중 한 번만 부른다. */
    public static HudFonts load() {
        Font[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        List<String> missing = new ArrayList<>();
        Font gyeon = installed(installed, "HY견고딕", "HYGothic-Extra", "HYGothicExtra");
        if (gyeon == null) {
            missing.add("HY견고딕");
        }
        Font yun = installed(installed, "HY윤고딕", "HYYunGothic", "HYYGO");
        if (yun == null) {
            missing.add("HY윤고딕");
        }
        Font michroma = bundled(MICHROMA_RESOURCE);
        if (michroma == null) {
            michroma = installed(installed, "Michroma");
        }
        if (michroma == null) {
            missing.add("Michroma");
        }
        return new HudFonts(orFallback(gyeon), orFallback(yun), orFallback(michroma), missing);
    }

    /** 찾지 못해 기본 글꼴로 대신한 디자인 글꼴 이름들 */
    public List<String> missing() {
        return missing;
    }

    public Font gyeonGothic(float size) {
        return gyeonGothic.deriveFont(size);
    }

    public Font yunGothic(float size) {
        return yunGothic.deriveFont(size);
    }

    public Font michroma(float size) {
        return michroma.deriveFont(size);
    }

    /** sample 글자의 실제 높이(픽셀)가 inkHeight에 가장 가까워지는 크기로 맞춘 글꼴 */
    public static Font fitInkHeight(Font font, String sample, int inkHeight) {
        return fit(font, size -> inkBounds(font.deriveFont(size), sample)[1], inkHeight);
    }

    /** sample 글자의 실제 폭(픽셀)이 inkWidth에 가장 가까워지는 크기로 맞춘 글꼴 */
    public static Font fitInkWidth(Font font, String sample, int inkWidth) {
        return fit(font, size -> inkBounds(font.deriveFont(size), sample)[0], inkWidth);
    }

    private static Font fit(Font font, Measure measure, int target) {
        float best = 1;
        double bestError = Double.MAX_VALUE;
        for (float size = 4; size <= 72; size += 0.5f) {
            double error = Math.abs(measure.at(size) - target);
            if (error < bestError) {
                bestError = error;
                best = size;
            }
        }
        return font.deriveFont(best);
    }

    /** [폭, 높이] */
    private static double[] inkBounds(Font font, String sample) {
        FontRenderContext frc = new FontRenderContext(null, true, false);
        var bounds = font.createGlyphVector(frc, sample).getVisualBounds();
        return new double[]{bounds.getWidth(), bounds.getHeight()};
    }

    private static Font installed(Font[] fonts, String... names) {
        for (String name : names) {
            String wanted = normalize(name);
            for (Font font : fonts) {
                if (normalize(font.getFamily(Locale.KOREAN)).contains(wanted)
                        || normalize(font.getFamily(Locale.ENGLISH)).contains(wanted)
                        || normalize(font.getFontName(Locale.KOREAN)).contains(wanted)
                        || normalize(font.getFontName(Locale.ENGLISH)).contains(wanted)) {
                    return font.deriveFont(Font.PLAIN, 12f);
                }
            }
        }
        return null;
    }

    private static Font bundled(String resource) {
        try (InputStream in = HudFonts.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(12f);
        } catch (FontFormatException e) {
            throw new IllegalArgumentException("글꼴 파일 형식이 잘못되었습니다: " + resource, e);
        } catch (IOException e) {
            throw new UncheckedIOException("글꼴 파일을 읽지 못했습니다: " + resource, e);
        }
    }

    private static Font orFallback(Font font) {
        return font != null ? font : new Font(Font.SANS_SERIF, Font.BOLD, 12);
    }

    private static String normalize(String name) {
        return name.replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface Measure {
        double at(float size);
    }
}
