package hansung.arcade.core.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import hansung.arcade.core.ui.HsuColor;

/**
 * 스택 트레이스를 줄 단위로 칠한다.
 * 예외 종류·메시지 줄(첫 줄, Caused by:)은 레벨 색으로 강조하고, at ... 호출 경로는 회색으로 흐리게 둔다.
 *
 * <pre>%traceColor(%ex)</pre>
 */
public final class StackTraceColorConverter extends CompositeConverter<ILoggingEvent> {

    private static final AnsiStyle FRAME = AnsiStyle.of(HsuColor.GRAY);

    @Override
    protected String transform(ILoggingEvent event, String in) {
        AnsiStyle header = LevelColorConverter.isAlert(event.getLevel())
                ? LevelColorConverter.styleOf(event.getLevel())
                : null;
        return paintTrace(in, header);
    }

    /** @param header 예외 줄 스타일. null이면 예외 줄은 칠하지 않는다. */
    static String paintTrace(String trace, AnsiStyle header) {
        if (trace.isEmpty()) {
            return trace;
        }
        StringBuilder sb = new StringBuilder(trace.length() + 256);
        for (String piece : trace.split("(?<=\n)")) {
            String newline = piece.endsWith("\r\n") ? "\r\n" : piece.endsWith("\n") ? "\n" : "";
            String line = piece.substring(0, piece.length() - newline.length());
            if (isFrame(line)) {
                sb.append(Ansi.paint(line, FRAME));
            } else {
                sb.append(header == null ? line : Ansi.paint(line, header));
            }
            sb.append(newline);
        }
        return sb.toString();
    }

    /** at ... 호출 줄과 "... 3 common frames omitted" 같은 생략 줄 */
    static boolean isFrame(String line) {
        String trimmed = line.strip();
        return trimmed.startsWith("at ") || trimmed.startsWith("... ");
    }
}
