package hansung.arcade.ingame.match;

import hansung.arcade.ingame.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 한 판의 진행 상태: 경기 시계, 플레이어별 kill·save, 승패. Swing에 의존하지 않는다.
 * 게임 상태(GameWorld)가 갱신마다 그 갱신의 탈락·kill·save를 모두 반영한 뒤 {@link #update}를 부른다.
 *
 * <p>승패는 "편"으로 가린다. 개인전은 플레이어 한 명이 한 편, 팀전은 팀 번호가 같은 플레이어끼리 한 편이다.</p>
 * <ul>
 *   <li>시작할 때 편이 둘 이상이었는데 살아 있는 편이 하나 이하가 되면 바로 끝난다.</li>
 *   <li>그 전에 시간이 다 되면 그때 끝난다. 시작부터 한 편뿐이면(혼자 실행 등) 일찍 끝나지 않고 시간이 다 될 때 끝난다.</li>
 *   <li>끝날 때 살아 있는 편이 하나면 그 편 전원(먼저 탈락한 팀원 포함)이 WIN, 나머지는 LOSE다.
 *       살아 있는 편이 없거나(동시에 전멸) 둘 이상이면(시간 종료) 모두 DRAW다.</li>
 * </ul>
 *
 * <p>끝나면 시계가 멈추고 결과({@link #result()})가 정해져 더는 바뀌지 않는다. 결과는 로그로 한 번 남긴다.</p>
 */
public final class Match {

    private static final Logger log = LoggerFactory.getLogger(Match.class);

    private final MatchMode mode;
    /** 이번 판의 플레이어. 번호 순 */
    private final List<Player> players;
    private final MatchClock clock = new MatchClock();
    private final Map<Integer, Integer> kills = new LinkedHashMap<>();
    private final Map<Integer, Integer> saves = new LinkedHashMap<>();
    private final int startingSides;
    private MatchResult result;

    /** @param players 이번 판의 플레이어 전원. 판 중간에 들어오는 플레이어는 없다 */
    public Match(MatchMode mode, List<Player> players) {
        this.mode = Objects.requireNonNull(mode);
        this.players = players.stream().sorted(Comparator.comparingInt(Player::id)).toList();
        for (Player player : this.players) {
            if (kills.put(player.id(), 0) != null) {
                throw new IllegalArgumentException("플레이어 번호가 겹칩니다: " + player.id());
            }
            saves.put(player.id(), 0);
        }
        this.startingSides = (int) this.players.stream().map(this::sideOf).distinct().count();
        log.info("경기 시작: {}, {}명 ({}편), {}초", mode.label(), this.players.size(), startingSides,
                MatchClock.DURATION_SECONDS);
    }

    /** 시계를 dt초만큼 흘리고 승패를 가린다. 끝난 뒤에는 시계도 결과도 바뀌지 않는다. */
    public void update(double dt) {
        clock.advance(dt);
        if (result == null) {
            judge();
        }
    }

    /**
     * killerId 플레이어가 물풍선에 갇힌 상대를 터뜨렸다고 기록한다.
     * 경기가 끝난 뒤의 기록은 이미 정해진 결과에 들어가지 않으므로 무시한다.
     */
    public void recordKill(int killerId) {
        record(kills, killerId);
    }

    /**
     * saverId 플레이어가 물풍선에 갇힌 팀원을 구했다고 기록한다.
     * 경기가 끝난 뒤의 기록은 이미 정해진 결과에 들어가지 않으므로 무시한다.
     */
    public void recordSave(int saverId) {
        record(saves, saverId);
    }

    /** 지금까지 playerId 플레이어의 kill 수 */
    public int kills(int playerId) {
        return countOf(kills, playerId);
    }

    /** 지금까지 playerId 플레이어의 save 수 */
    public int saves(int playerId) {
        return countOf(saves, playerId);
    }

    public MatchMode mode() {
        return mode;
    }

    public MatchClock clock() {
        return clock;
    }

    /** 승패가 정해져 경기가 끝났는지 */
    public boolean isOver() {
        return result != null;
    }

    /** 끝난 경기의 결과. 아직 진행 중이면 비어 있다 */
    public Optional<MatchResult> result() {
        return Optional.ofNullable(result);
    }

    private void judge() {
        Set<Integer> aliveSides = new HashSet<>();
        for (Player player : players) {
            if (player.isAlive()) {
                aliveSides.add(sideOf(player));
            }
        }
        boolean lastStanding = startingSides >= 2 && aliveSides.size() <= 1;
        if (!lastStanding && !clock.isTimeUp()) {
            return;
        }
        Integer winner = aliveSides.size() == 1 ? aliveSides.iterator().next() : null;
        List<PlayerResult> records = players.stream()
                .map(player -> new PlayerResult(player.id(), outcomeOf(player, winner),
                        kills.get(player.id()), saves.get(player.id())))
                .toList();
        clock.stop();
        result = new MatchResult(mode, clock.elapsedSeconds(), records);
        log.info("경기 종료: {}, {}초 진행, {}", mode.label(),
                String.format(Locale.ROOT, "%.2f", result.playedSeconds()), summary(records));
    }

    /** 살아남은 편이 winner일 때 player의 승패. winner가 null이면 살아남은 편이 하나가 아니라 모두 비긴다 */
    private MatchOutcome outcomeOf(Player player, Integer winner) {
        if (winner == null) {
            return MatchOutcome.DRAW;
        }
        return sideOf(player) == winner ? MatchOutcome.WIN : MatchOutcome.LOSE;
    }

    /** 승패를 함께하는 편. 개인전은 플레이어 번호, 팀전은 팀 번호다 */
    private int sideOf(Player player) {
        return mode == MatchMode.SOLO ? player.id() : player.profile().team();
    }

    private void record(Map<Integer, Integer> counts, int playerId) {
        countOf(counts, playerId);
        if (result == null) {
            counts.merge(playerId, 1, Integer::sum);
        }
    }

    private static int countOf(Map<Integer, Integer> counts, int playerId) {
        Integer count = counts.get(playerId);
        if (count == null) {
            throw new IllegalArgumentException("이번 판에 없는 플레이어입니다: " + playerId);
        }
        return count;
    }

    private static String summary(List<PlayerResult> records) {
        return records.stream()
                .map(r -> "%d번 %s (kill %d, save %d)".formatted(r.playerId(), r.outcome(), r.kills(), r.saves()))
                .collect(Collectors.joining(", "));
    }
}
