package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;

/**
 * 1단계: application.properties와 실행 인자를 읽어 {@link GameProperties}를 등록한다.
 */
public final class PropertiesStep implements BootStep {

    @Override
    public String name() {
        return "설정 로드";
    }

    @Override
    public void run(GameContext context) {
        context.register(GameProperties.class, GameProperties.load(context.primarySource(), context.args()));
    }
}
