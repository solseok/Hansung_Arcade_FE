package hansung.arcade.outgame;

import hansung.arcade.outgame.ui.OutgamePalette;

import java.awt.Color;
import java.util.Arrays;

/**
 * 맥 스크린샷(Display P3)에서 잰 색과 비교하는 테스트 도구.
 * 사용자가 알려 주는 색과 참고 화면 PNG는 맥 스크린샷이라 Display P3 값이다. Java는 sRGB로 그리고 macOS가 화면 색 공간으로
 * 바꿔 보여 주므로, 토큰(sRGB)을 Display P3로 바꿨을 때 잰 값과 같아야 스크린샷에서 같은 값으로 보인다.
 */
public final class P3 {

    private P3() {
    }

    /** color를 Display P3로 바꾼 값이 (r, g, b)와 채널마다 1 이하로 다른지 */
    public static void assertP3(int r, int g, int b, Color color) {
        int[] p3 = OutgamePalette.toDisplayP3(color);
        int[] expected = {r, g, b};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(p3[i] - expected[i]) > 1) {
                throw new AssertionError("Display P3 %s 이어야 하는데 %s 입니다 (sRGB %d,%d,%d)".formatted(
                        Arrays.toString(expected), Arrays.toString(p3), color.getRed(), color.getGreen(), color.getBlue()));
            }
        }
    }
}
