package hansung.arcade.core.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import hansung.arcade.core.ui.HsuColor;

/**
 * logback 패턴에서 감싼 부분을 로그 레벨에 맞는 색으로 칠한다.
 * ERROR·WARN은 눈에 잘 띄도록 HSU 팔레트 밖의 빨강·주황을 쓴다.
 *
 * <pre>
 * %levelColor(%-5level)       모든 레벨에 색
 * %levelColor(%msg){alert}    WARN, ERROR일 때만 색 (평소 메시지는 터미널 기본색)
 * </pre>
 */
public final class LevelColorConverter extends CompositeConverter<ILoggingEvent> {

    static final AnsiStyle ERROR = AnsiStyle.rgb(0xFF5F5F).withBold();
    static final AnsiStyle WARN = AnsiStyle.rgb(0xFFAF00);

    private boolean alertOnly;

    @Override
    public void start() {
        alertOnly = "alert".equalsIgnoreCase(getFirstOption());
        super.start();
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        Level level = event.getLevel();
        if (alertOnly && !isAlert(level)) {
            return in;
        }
        return Ansi.paint(in, styleOf(level));
    }

    static boolean isAlert(Level level) {
        return level.isGreaterOrEqual(Level.WARN);
    }

    static AnsiStyle styleOf(Level level) {
        return switch (level.toInt()) {
            case Level.ERROR_INT -> ERROR;
            case Level.WARN_INT -> WARN;
            case Level.INFO_INT -> AnsiStyle.of(HsuColor.LIGHT_GREEN);
            case Level.DEBUG_INT -> AnsiStyle.of(HsuColor.LIGHT_BLUE);
            default -> AnsiStyle.of(HsuColor.LIGHT_GRAY);
        };
    }
}
