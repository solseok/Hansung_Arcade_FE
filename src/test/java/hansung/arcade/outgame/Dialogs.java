package hansung.arcade.outgame;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 테스트에서 곧 뜰 JOptionPane 창을 찾아 답하는 도우미 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * 곧 뜰 확인 창을 찾아 answer로 답한다. 창에 쓰인 글자들을 돌려준다 (5초 안에 창이 안 뜨면 빈 목록).
     * 모달 창이 떠 있는 동안에도 EDT 타이머는 돌므로, 창을 띄우는 동작 바로 앞에서 부른다.
     */
    public static List<String> answer(Consumer<JOptionPane> answer) {
        List<String> texts = new ArrayList<>();
        int[] tries = {0};
        Timer timer = new Timer(100, null);
        timer.addActionListener(event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing()) {
                    List<JOptionPane> panes = SwingTree.all(dialog, JOptionPane.class);
                    if (!panes.isEmpty()) {
                        timer.stop();
                        JOptionPane pane = panes.getFirst();
                        texts.addAll(SwingTree.texts(dialog));
                        if (pane.getMessage() instanceof String message) {
                            texts.add(message);
                        } else if (pane.getMessage() instanceof Container message) {
                            texts.addAll(SwingTree.texts(message));
                        }
                        answer.accept(pane);
                        return;
                    }
                }
            }
            if (++tries[0] > 50) {
                timer.stop();
            }
        });
        timer.start();
        return texts;
    }
}
