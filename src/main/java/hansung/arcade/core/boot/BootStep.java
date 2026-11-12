package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;

/**
 * 애플리케이션 구동 단계 하나. 필요한 객체를 만들어 {@link GameContext}에 등록한다.
 */
public interface BootStep {

    /** 로그와 오류 메시지에 쓰는 단계 이름 */
    String name();

    void run(GameContext context) throws Exception;
}
