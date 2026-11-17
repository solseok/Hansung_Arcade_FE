package hansung.arcade.outgame.lobby;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 3. 방만들기·바로시작 버튼 그림은 원래 하늘색 바탕 위에서 잘라낸 것이라 네 모서리에 하늘색 픽셀이 남아 있었다.
 * 둥근 모서리 밖은 투명하게 지워서 어떤 바탕에 놓아도 하늘색이 보이지 않아야 한다.
 */
@ExtendWith(RunOnEdt.class)
class LobbyButtonArtTest {

    /** 모서리에서 살펴볼 칸 크기. 그림의 둥근 모서리(반지름 약 5px)를 덮는다 */
    private static final int CORNER = 7;

    @ParameterizedTest
    @ValueSource(strings = {"방만들기", "바로시작"})
    void 그림_버튼의_모서리에_하늘색이_남지_않는다(String text) {
        JButton button = LobbyArtwork.button(text, true);
        button.setSize(size(text));

        BufferedImage image = SwingTree.render(button);
        int w = image.getWidth();
        int h = image.getHeight();

        assertEquals(0, SwingTree.alpha(image, 0, 0), "왼쪽 위 모서리가 투명하지 않습니다.");
        assertEquals(0, SwingTree.alpha(image, w - 1, h - 1), "오른쪽 아래 모서리가 투명하지 않습니다.");
        int[][] origins = {{0, 0}, {w - CORNER, 0}, {0, h - CORNER}, {w - CORNER, h - CORNER}};
        for (int[] origin : origins) {
            for (int y = origin[1]; y < origin[1] + CORNER; y++) {
                for (int x = origin[0]; x < origin[0] + CORNER; x++) {
                    int argb = image.getRGB(x, y);
                    assertFalse(isSkyBlue(argb), text + " (" + x + "," + y + ")에 하늘색이 남아 있습니다: "
                            + Integer.toHexString(argb));
                }
            }
        }
    }

    /** 그림 원래 크기 (방만들기 134x61, 바로시작 123x61) */
    private static Dimension size(String text) {
        return text.equals("방만들기") ? new Dimension(134, 61) : new Dimension(123, 61);
    }

    /** 보이는(알파가 있는) 픽셀 중 원래 바탕이던 하늘색 계열 (빨강이 적고 파랑이 많은 밝은 청록) */
    private static boolean isSkyBlue(int argb) {
        int alpha = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return alpha > 40 && r < 90 && g > 120 && b > 190;
    }
}
