package hansung.arcade.core.ui;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 구동 단계(메인 스레드)에서 Swing 작업을 EDT로 넘겨 끝날 때까지 기다린다.
 * EDT에서 난 예외는 감싸지 않고 그대로 다시 던져서 실패 원인이 잘 보이게 한다.
 */
public final class SwingThread {

    private SwingThread() {
    }

    public static void runAndWait(Runnable task) {
        callAndWait(() -> {
            task.run();
            return null;
        });
    }

    public static <T> T callAndWait(Supplier<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(task.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Swing 작업을 기다리다 중단되었습니다.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
        return result.get();
    }
}
