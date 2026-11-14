package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DirectionInputTest {

    @Test
    void 아무것도_안_누르면_방향이_없다() {
        assertNull(new DirectionInput().current());
    }

    @Test
    void 마지막에_누른_방향을_따른다() {
        DirectionInput input = new DirectionInput();
        input.press(Direction.UP);
        input.press(Direction.RIGHT);

        assertEquals(Direction.RIGHT, input.current());
    }

    @Test
    void 나중_키를_떼면_아직_누르고_있는_이전_키로_돌아간다() {
        DirectionInput input = new DirectionInput();
        input.press(Direction.UP);
        input.press(Direction.RIGHT);
        input.release(Direction.RIGHT);

        assertEquals(Direction.UP, input.current());
    }

    @Test
    void 키_반복_입력이_와도_순서가_꼬이지_않는다() {
        DirectionInput input = new DirectionInput();
        input.press(Direction.UP);
        input.press(Direction.LEFT);
        input.press(Direction.UP); // OS 키 반복

        assertEquals(Direction.UP, input.current());
        input.release(Direction.UP);
        assertEquals(Direction.LEFT, input.current());
    }

    @Test
    void 방향키_코드를_방향으로_바꾸고_다른_키는_무시한다() {
        assertEquals(Direction.UP, DirectionInput.directionOf(KeyEvent.VK_UP));
        assertEquals(Direction.DOWN, DirectionInput.directionOf(KeyEvent.VK_DOWN));
        assertEquals(Direction.LEFT, DirectionInput.directionOf(KeyEvent.VK_LEFT));
        assertEquals(Direction.RIGHT, DirectionInput.directionOf(KeyEvent.VK_RIGHT));
        assertNull(DirectionInput.directionOf(KeyEvent.VK_SPACE));
        assertNull(DirectionInput.directionOf(KeyEvent.VK_SHIFT));
    }

    @Test
    void 포커스를_잃으면_모두_비운다() {
        DirectionInput input = new DirectionInput();
        input.press(Direction.DOWN);
        input.clear();

        assertNull(input.current());
    }
}
