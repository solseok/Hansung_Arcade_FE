package hansung.arcade.outgame.ui;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static hansung.arcade.outgame.P3.assertP3;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 맥 스크린샷은 Display P3 색 공간이라, Java가 sRGB로 그린 색이 스크린샷에서는 다른 값으로 재진다.
 * 예) 로비 배경 토큰 sRGB(60,128,235)를 사용자가 스크린샷에서 재면 78,126,228이었다.
 * 그래서 스크린샷에서 잰 값(사용자가 알려 준 색, 참고 화면 PNG)은 {@link OutgamePalette#p3}로 sRGB로 바꿔 토큰에 넣는다.
 */
class DisplayP3Test {

    @Test
    void 사용자가_스크린샷에서_잰_값은_sRGB_토큰의_Display_P3_값이다() {
        assertP3(84, 185, 230, new Color(0x00, 0xBC, 0xEB));   // 처음 로비 배경 0x00BCEB
        assertP3(78, 126, 228, new Color(60, 128, 235));       // 로비 배경
        assertP3(116, 190, 234, new Color(88, 192, 239));      // 방 목록
        assertP3(104, 166, 227, new Color(82, 168, 233));      // 인원 라벨
        assertP3(84, 140, 201, new Color(63, 142, 206));       // 방 제목줄
    }

    @Test
    void p3로_정한_색은_스크린샷에서_그_값으로_보인다() {
        int[][] measured = {{46, 107, 192}, {88, 192, 242}, {82, 168, 233}, {67, 148, 216}, {215, 208, 76},
                {103, 219, 251}, {78, 167, 248}, {69, 147, 227}, {98, 96, 98}};
        for (int[] c : measured) {
            assertP3(c[0], c[1], c[2], OutgamePalette.p3(c[0], c[1], c[2]));
        }
    }

    @Test
    void sRGB로_나타낼_수_없는_P3_색은_가장_가까운_sRGB로_자른다() {
        Color red = OutgamePalette.p3(255, 0, 0);

        assertTrue(red.getRed() == 255 && red.getGreen() == 0 && red.getBlue() == 0, red.toString());
    }
}
