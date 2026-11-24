package hansung.arcade.core.game;

import javax.swing.JComponent;

/** 화면 생명주기는 EDT에서 호출한다. */
public interface Scene {
    JComponent view();

    default void enter(Object data) {
    }

    default void exit() {
    }
}
