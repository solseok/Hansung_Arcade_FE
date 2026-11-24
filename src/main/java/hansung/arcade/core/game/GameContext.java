package hansung.arcade.core.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 구동 단계들이 만든 객체를 타입별로 보관하는 곳.
 * 앞 단계가 {@link #register}한 객체를 뒤 단계가 {@link #get}으로 꺼내 쓴다.
 */
public final class GameContext {

    private final Class<?> primarySource;
    private final List<String> args;
    private final Map<Class<?>, Object> components = new HashMap<>();

    public GameContext(Class<?> primarySource, List<String> args) {
        this.primarySource = Objects.requireNonNull(primarySource);
        this.args = List.copyOf(args);
    }

    /** main을 가진 클래스. 리소스를 찾을 기준과 기본 이름으로 쓴다. */
    public Class<?> primarySource() {
        return primarySource;
    }

    public List<String> args() {
        return args;
    }

    public <T> void register(Class<T> type, T component) {
        Objects.requireNonNull(component, () -> type.getSimpleName() + "에 null을 등록할 수 없습니다.");
        if (components.putIfAbsent(type, component) != null) {
            throw new IllegalStateException(type.getSimpleName() + "이(가) 이미 등록되어 있습니다.");
        }
    }

    public <T> T get(Class<T> type) {
        Object component = components.get(type);
        if (component == null) {
            throw new IllegalStateException(
                    type.getSimpleName() + "이(가) 아직 준비되지 않았습니다. 구동 단계 순서를 확인하세요.");
        }
        return type.cast(component);
    }

    public boolean contains(Class<?> type) {
        return components.containsKey(type);
    }
}
