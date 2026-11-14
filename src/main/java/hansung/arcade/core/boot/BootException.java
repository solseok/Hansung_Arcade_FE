package hansung.arcade.core.boot;

/**
 * 구동 단계가 실패했을 때 어느 단계였는지 함께 알려 주는 예외.
 */
public final class BootException extends RuntimeException {

    private final String stepName;

    public BootException(String stepName, Throwable cause) {
        super("[%s] 단계에서 실패했습니다: %s".formatted(stepName, describe(cause)), cause);
        this.stepName = stepName;
    }

    /** 메시지 없이 던져진 예외(NullPointerException 등)는 "null" 대신 예외 이름을 보여 준다. */
    private static String describe(Throwable cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    public String stepName() {
        return stepName;
    }
}
