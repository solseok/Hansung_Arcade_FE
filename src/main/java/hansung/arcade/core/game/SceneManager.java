package hansung.arcade.core.game;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** 기존 GameWindow를 통해 화면을 교체하고 진입 데이터와 정리 호출을 관리한다. */
public final class SceneManager {
    private final Consumer<JComponent> display;
    private final Map<String, Scene> scenes = new HashMap<>();
    private Scene current;
    private Consumer<String> onShow = name -> { };

    public void onShow(Consumer<String> listener) {
        onShow = Objects.requireNonNull(listener);
    }

    public SceneManager(Consumer<JComponent> display) {
        this.display = Objects.requireNonNull(display);
    }

    public void register(String name, Scene scene) {
        if (scenes.putIfAbsent(name, Objects.requireNonNull(scene)) != null) {
            throw new IllegalArgumentException("이미 등록된 화면: " + name);
        }
    }

    public void show(String name, Object data) {
        Scene next = scenes.get(name);
        if (next == null) {
            throw new IllegalArgumentException("등록되지 않은 화면: " + name);
        }
        if (current != null) {
            current.exit();
        }
        current = next;
        display.accept(next.view());
        next.enter(data);
        onShow.accept(name);
    }
}
