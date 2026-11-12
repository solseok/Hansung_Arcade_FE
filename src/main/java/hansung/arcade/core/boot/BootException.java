package hansung.arcade.core.boot;

/**
 * 구동 단계가 실패했을 때 어느 단계였는지 함께 알려 주는 예외.
 */
public final class BootException extends RuntimeException {

    private final String stepName;

    public BootException(String stepName, Throwable cause) {
        super("[%s] 단계에서 실패했습니다: %s".formatted(stepName, cause.getMessage()), cause);
        this.stepName = stepName;
    }

    public String stepName() {
        return stepName;
    }
}
