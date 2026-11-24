package hansung.arcade.outgame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 서버 없이 바로 로그인한다. 플레이어 번호는 1P부터 1, 2다. */
public final class OfflineLoginService implements LoginService {

    @Override
    public CompletableFuture<List<LoginPlayer>> login(List<String> nicknames) {
        return CompletableFuture.completedFuture(players(nicknames));
    }

    static List<LoginPlayer> players(List<String> nicknames) {
        List<LoginPlayer> players = new ArrayList<>();
        for (int i = 0; i < nicknames.size(); i++) {
            players.add(new LoginPlayer(i + 1, nicknames.get(i)));
        }
        return List.copyOf(players);
    }
}
