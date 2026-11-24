package hansung.arcade.core.ui;

import java.awt.Color;

/**
 * 한성대학교 전용색상 (HSU UI 가이드). 로그·배너·화면에서 같은 값을 쓴다.
 */
public enum HsuColor {
    BLUE(0x013B90),
    DARK_BLUE(0x001655),
    SKY_BLUE(0x227FC4),
    GRAY(0x666565),
    LIGHT_GRAY(0xC9C9CA),
    LIGHT_BLUE(0x9ED8F6),
    LIGHT_PINK(0xF7C7CD),
    LIGHT_GREEN(0xBEDEAE),
    LIGHT_YELLOW(0xFFF3C2);

    private final int rgb;

    HsuColor(int rgb) {
        this.rgb = rgb;
    }

    /** 0xRRGGBB */
    public int rgb() {
        return rgb;
    }

    public int red() {
        return (rgb >> 16) & 0xFF;
    }

    public int green() {
        return (rgb >> 8) & 0xFF;
    }

    public int blue() {
        return rgb & 0xFF;
    }

    public Color toAwt() {
        return new Color(rgb);
    }
}
