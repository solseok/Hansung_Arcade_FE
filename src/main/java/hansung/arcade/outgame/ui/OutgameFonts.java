package hansung.arcade.outgame.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 아웃게임 글꼴은 두 가지다.
 * <ul>
 *   <li>{@link #gulimChe 굴림체}: 사용자가 바꿀 수 있는 문구(방 제목, 닉네임, 채팅). 윈도우에는 설치돼 있고, 맥에는 없지만
 *       MS 오피스가 앱 안에 넣어 둔 gulim.ttc(굴림·굴림체·돋움·돋움체)가 있으면 그것을 읽는다. 둘 다 없으면 기본 고정폭 글꼴.</li>
 *   <li>{@link #label 게임 글꼴}: 바뀌지 않는 문구(버튼·상태·안내). 참고 화면의 굵은 글자처럼 기본 산세리프 굵게를 쓰고,
 *       그리는 쪽에서 테두리·그러데이션을 입힌다.</li>
 * </ul>
 */
public final class OutgameFonts {

    public static final String FAMILY = "GulimChe";
    private static final List<String> COLLECTIONS = List.of(
            "/Applications/Microsoft Word.app/Contents/Resources/DFonts/gulim.ttc",
            "/Applications/Microsoft Excel.app/Contents/Resources/DFonts/gulim.ttc",
            "/Applications/Microsoft PowerPoint.app/Contents/Resources/DFonts/gulim.ttc",
            "C:/Windows/Fonts/gulim.ttc");
    private static final Font BASE;
    private static final boolean AVAILABLE;

    static {
        Font found = installed();
        if (found == null) {
            found = fromCollections();
        }
        AVAILABLE = found != null;
        BASE = found != null ? found : new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    private OutgameFonts() {
    }

    /** 굴림체(없으면 기본 고정폭) size 크기 */
    public static Font gulimChe(float size) {
        return BASE.deriveFont(Font.PLAIN, size);
    }

    /** 굵은 굴림체. 테두리를 두른 제목·닉네임처럼 획이 가늘면 테두리에 묻히는 글자에 쓴다 */
    public static Font gulimCheBold(float size) {
        return BASE.deriveFont(Font.BOLD, size);
    }

    /** 바뀌지 않는 문구에 쓰는 굵은 게임 글꼴 size 크기 */
    public static Font label(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, 12).deriveFont(size);
    }

    /** 굴림체를 찾았는지 */
    public static boolean available() {
        return AVAILABLE;
    }

    private static Font installed() {
        for (String name : List.of(FAMILY, "굴림체")) {
            Font font = new Font(name, Font.PLAIN, 12);
            if (FAMILY.equals(font.getFamily(Locale.ENGLISH))) {
                return font;
            }
        }
        return null;
    }

    private static Font fromCollections() {
        for (String path : COLLECTIONS) {
            File file = new File(path);
            if (!file.isFile()) {
                continue;
            }
            try {
                for (Font font : Font.createFonts(file)) {
                    if (FAMILY.equals(font.getFamily(Locale.ENGLISH))) {
                        return font.deriveFont(Font.PLAIN, 12f);
                    }
                }
            } catch (FontFormatException | IOException e) {
                // 다음 파일에서 찾는다
            }
        }
        return null;
    }
}
