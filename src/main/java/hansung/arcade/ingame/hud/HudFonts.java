package hansung.arcade.ingame.hud;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.io.File;
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
 *
 * <p>채팅에 쓰는 돋움체는 윈도우 기본 글꼴이다. 맥에는 없지만 MS 오피스가 앱 안에 넣어 둔 gulim.ttc
 * (굴림·굴림체·돋움·돋움체가 한 파일에 들어 있다)가 있으면 그것을 읽어 쓴다. 없으면 Java 기본 고정폭 글꼴로 대신한다.</p>
 */
public final class HudFonts {

    public static final String MICHROMA_RESOURCE = "/fonts/Michroma-Regular.ttf";

    /** 맥용 MS 오피스가 앱 안에 넣어 둔 글꼴 모음(굴림·굴림체·돋움·돋움체) */
    private static final List<String> GULIM_COLLECTIONS = List.of(
            "/Applications/Microsoft Word.app/Contents/Resources/DFonts/gulim.ttc",
            "/Applications/Microsoft Excel.app/Contents/Resources/DFonts/gulim.ttc",
            "/Applications/Microsoft PowerPoint.app/Contents/Resources/DFonts/gulim.ttc");
    public static final String DOTUMCHE = "돋움체";

    private final Font gyeonGothic;
    private final Font yunGothic;
    private final Font michroma;
    /** 찾지 못했으면 null */
    private final Font dotumChe;
    private final List<String> missing;

    private HudFonts(Font gyeonGothic, Font yunGothic, Font michroma, Font dotumChe, List<String> missing) {
        this.gyeonGothic = gyeonGothic;
        this.yunGothic = yunGothic;
        this.michroma = michroma;
        this.dotumChe = dotumChe;
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
        Font dotumChe = installed(installed, DOTUMCHE, "DotumChe");
        if (dotumChe == null) {
            dotumChe = fromFiles(GULIM_COLLECTIONS, "DotumChe");
        }
        if (dotumChe == null) {
            missing.add(DOTUMCHE);
        }
        return new HudFonts(orFallback(gyeon), orFallback(yun), orFallback(michroma), dotumChe, missing);
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

    /**
     * 글자 칸 높이가 cellHeight px인 돋움체. 돋움체는 작은 크기마다 도트 글자(비트맵)가 들어 있고
     * 글자 칸 높이가 글꼴 크기와 같아서(12px이면 위아래 12px), 크기를 칸 높이 그대로 준다.
     * 받침이 복잡한 일부 글자(갱, 냉 등)만 칸 위로 1px 올라간다.
     * 돋움체가 없으면 고정폭 기본 글꼴을 글자가 칸을 넘지 않는 가장 큰 크기로 맞춰 대신 쓴다 ({@link OutlineText#fitCell}).
     */
    public Font dotumChe(int cellHeight) {
        if (dotumChe != null) {
            return dotumChe.deriveFont((float) cellHeight);
        }
        return OutlineText.fitCell(new Font(Font.MONOSPACED, Font.PLAIN, cellHeight), cellHeight);
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

    /**
     * 글꼴 모음 파일(.ttc)들을 차례로 열어 family 글꼴을 찾는다. 다른 앱에 딸린 파일이라 없거나 읽지 못할 수 있고,
     * 그때는 찾지 못한 것으로 친다 (부르는 쪽이 {@link #missing()}에 남긴다).
     */
    private static Font fromFiles(List<String> paths, String family) {
        for (String path : paths) {
            File file = new File(path);
            if (!file.isFile()) {
                continue;
            }
            try {
                for (Font font : Font.createFonts(file)) {
                    if (normalize(font.getFamily(Locale.ENGLISH)).equals(normalize(family))) {
                        return font.deriveFont(Font.PLAIN, 12f);
                    }
                }
            } catch (FontFormatException | IOException e) {
                // 다음 파일에서 찾는다
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
