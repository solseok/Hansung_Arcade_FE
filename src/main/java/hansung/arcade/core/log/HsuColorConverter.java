package hansung.arcade.core.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import hansung.arcade.core.ui.HsuColor;

import java.util.Locale;

/**
 * logback 패턴에서 감싼 부분을 지정한 HSU 색으로 칠한다.
 *
 * <pre>%hsu(%logger{20}){SKY_BLUE}</pre>
 */
public final class HsuColorConverter extends CompositeConverter<ILoggingEvent> {

    private HsuColor color;

    @Override
    public void start() {
        String option = getFirstOption();
        if (option == null) {
            addError("%hsu 에 색 이름이 없습니다. 예) %hsu(%logger){SKY_BLUE}");
        } else {
            try {
                color = HsuColor.valueOf(option.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                addError("알 수 없는 HSU 색 이름입니다: " + option);
            }
        }
        super.start();
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return color == null ? in : Ansi.paint(in, color);
    }
}
