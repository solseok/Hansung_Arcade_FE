package hansung.arcade.core.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 테스트 메서드와 그 준비 메서드를 EDT에서 돌린다. {@code @ExtendWith(RunOnEdt.class)}로 붙인다.
 *
 * <p>Swing 컴포넌트는 EDT에서 다뤄야 게임에서와 똑같이 움직인다. 예를 들어 JTextField의 커서는
 * 글이 EDT 밖에서 바뀌면 따라 움직이지 않는다.</p>
 */
public final class RunOnEdt implements InvocationInterceptor {

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext,
                                          ExtensionContext extensionContext) throws Throwable {
        proceedOnEdt(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        proceedOnEdt(invocation);
    }

    private static void proceedOnEdt(Invocation<Void> invocation) throws Throwable {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
    }
}
