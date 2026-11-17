package hansung.arcade.outgame.ui;

import java.awt.*;
import java.util.*;

public final class OutgameFonts {
    private static final Font GULIM =
            Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
                    .filter(f -> f.getFamily(Locale.ENGLISH).equals("GulimChe"))
                    .findFirst()
                    .orElse(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    public static Font gulimChe(float size) {
        return GULIM.deriveFont(size);
    }

    public static Font label(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, 12).deriveFont(size);
    }

    public static boolean available() {
        return GULIM.getFamily(Locale.ENGLISH).equals("GulimChe");
    }
}
