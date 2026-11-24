package hansung.arcade.core.log;

import hansung.arcade.core.ui.HsuColor;

/**
 * 터미널 글자 스타일: 24비트 글자색(0xRRGGBB)과 굵게 여부.
 * HSU 팔레트 색은 {@link #of(HsuColor)}, 팔레트 밖의 색은 {@link #rgb(int)}로 만든다.
 */
public record AnsiStyle(int rgb, boolean bold) {

    public static AnsiStyle of(HsuColor color) {
        return new AnsiStyle(color.rgb(), false);
    }

    public static AnsiStyle rgb(int rgb) {
        return new AnsiStyle(rgb, false);
    }

    public AnsiStyle withBold() {
        return new AnsiStyle(rgb, true);
    }
}
