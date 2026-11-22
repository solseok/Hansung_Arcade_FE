package hansung.arcade.outgame;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 로그인. 오프라인이면 바로 받아들이고({@link OfflineLoginService}), 온라인이면 서버에 LOGIN을 보낸다({@link ServerLoginService}).
 */
public interface LoginService {

    /** 서버가 정한 플레이어 번호와 닉네임 */
    record LoginPlayer(long playerId, String nickname) {
    }

    /**
     * 규칙 검사({@link OutgameSession#checkNicknames})를 마친 닉네임(1P부터)으로 로그인한다.
     * 성공하면 플레이어를 1P부터 돌려준다. 실패하면 화면에 띄울 문구를 담은 예외로 끝난다
     * ({@link hansung.arcade.network.ServerException#from}으로 한 줄로 바꾼다).
     * 결과는 EDT가 아닌 스레드에서 끝날 수 있다.
     */
    CompletableFuture<List<LoginPlayer>> login(List<String> nicknames);
}
