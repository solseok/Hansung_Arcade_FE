package hansung.arcade.ingame.hud;

import java.awt.Graphics2D;

/**
 * 화면에 글자를 그리는 방법. 지금은 글꼴({@link FontTextArt})로 그린다.
 * TIMER·ITEM 글씨나 타이머 숫자를 디자인 이미지로 바꿀 때는 이 인터페이스를 이미지로 구현해
 * {@link HudSkin}에서 갈아 끼우면 되고, 그리는 쪽 코드는 고칠 필요가 없다.
 */
public interface TextArt {

    /** text를 area 안에 가로는 align대로, 세로는 가운데로 그린다. */
    default void draw(Graphics2D g, String text, Box area, Align align) {
        draw(g, text, area, align, VerticalAlign.MIDDLE);
    }

    /** text를 area 안에 가로는 align, 세로는 verticalAlign대로 그린다. 세로는 실제 글자 픽셀 기준이다. */
    void draw(Graphics2D g, String text, Box area, Align align, VerticalAlign verticalAlign);

    enum VerticalAlign {
        TOP,
        MIDDLE,
        /** 글자 아래 끝을 area 바닥에 붙인다 */
        BOTTOM
    }

    enum Align {
        LEFT,
        CENTER,
        /** 오른쪽 끝을 글자 폭 기준으로 맞춘다. 숫자가 바뀌어도 오른쪽 끝은 흔들리지 않는다. */
        RIGHT
    }
}
