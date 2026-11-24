package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;

import java.awt.Graphics2D;

/**
 * HUD 한 덩어리(맵 프레임, 플레이어 목록 ...). 그리는 층이 세 개라 필요한 것만 구현한다.
 *
 * <ol>
 *   <li>{@link #paintBase}: 바뀌지 않는 바탕. 한 번 그려 두고 매 프레임 맵보다 먼저 붙인다.</li>
 *   <li>{@link #paint}: 매 프레임 바뀌는 내용 (타이머 숫자, 플레이어, 아이템).</li>
 *   <li>{@link #paintOverlay}: 바뀌지 않지만 맵과 매 프레임 내용보다 위에 있어야 하는 것 (맵 이름표, 단축키 번호).</li>
 * </ol>
 */
interface HudPart {

    default void paintBase(PixelCanvas canvas) {
    }

    default void paint(Graphics2D g, GameWorld world) {
    }

    default void paintOverlay(PixelCanvas canvas) {
    }
}
