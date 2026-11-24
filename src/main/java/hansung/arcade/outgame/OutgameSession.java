package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.Room;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * 한 컴퓨터의 아웃게임 상태. 1P면 한 명, 2P면 두 명이 이 컴퓨터에서 함께 한다.
 * 로그인은 온라인이면 서버가 받아들인 플레이어 번호를 들고 있고, 대기실의 다른 인원은 아직 준비된 목업 참가자다.
 * 서버 주소·포트는 사용자가 넣지 않고 설정(application.properties, .env)에서 읽는다.
 *
 * <p>캐릭터와 팀(캐릭터 색)은 이 컴퓨터의 플레이어마다 따로 고른다. 번호(local)는 0이 1P, 1이 2P이고,
 * 번호를 받지 않는 메서드는 1P의 것이다. 2P는 처음에 1P와 구별되게 다른 캐릭터({@link #DEFAULT_CHARACTER_2P})·
 * 다른 팀({@link #DEFAULT_TEAM_2P})으로 시작한다.</p>
 */
public final class OutgameSession {
    /** 이 컴퓨터에서 함께 할 수 있는 최대 인원 */
    public static final int MAX_LOCAL_PLAYERS = 2;
    /** 2P가 처음 고른 캐릭터 */
    public static final Mascot DEFAULT_CHARACTER_2P = Mascot.HANSUNGNYANGI;
    /** 2P가 처음 고른 팀 */
    public static final Team DEFAULT_TEAM_2P = Team.BLUE;

    public final LobbyService service;
    private List<String> nicknames = List.of("나");
    /** 로그인한 플레이어 번호 (1P부터). 오프라인이면 1, 2 */
    private List<Long> playerIds = List.of(1L);
    private Room room;
    /** 이 컴퓨터 플레이어마다(1P, 2P) 고른 캐릭터. 랜덤을 골랐으면 마지막으로 고른 캐릭터 */
    private final Mascot[] characters = {Mascot.DEFAULT, DEFAULT_CHARACTER_2P};
    /** ?(랜덤)를 골랐으면 true. 실제 캐릭터는 게임을 시작할 때 정한다 */
    private final boolean[] randomCharacters = new boolean[MAX_LOCAL_PLAYERS];
    private final Random random = new Random();
    private final Team[] teams = {Team.RED, DEFAULT_TEAM_2P};
    private String mapId = GameMaps.DEFAULT_ID;
    private boolean ready;
    /** 목업 참가자(이 컴퓨터 밖의 사람)의 준비 상태. 서버가 붙으면 서버에서 받은 값으로 바꾼다 */
    private final Map<Integer, Boolean> participantReady = new HashMap<>();

    public OutgameSession(LobbyService service) {
        this.service = service;
    }

    /**
     * 1P는 닉네임 하나, 2P는 둘로 서버 없이 로그인한다. 규칙은 {@link #checkNicknames}와 같다.
     */
    public void login(String... nicknames) {
        signedIn(OfflineLoginService.players(checkNicknames(nicknames)));
    }

    /**
     * 로그인하기 전에 이 컴퓨터에서 하는 검사. 닉네임은 {@link NicknameRule}을 따르고, 2P의 두 닉네임은 달라야 한다.
     *
     * @return 다듬은 닉네임 (1P부터)
     * @throws IllegalArgumentException 어긋나면. 메시지를 그대로 화면에 보여 줄 수 있다
     */
    public static List<String> checkNicknames(String... nicknames) {
        if (nicknames.length < 1 || nicknames.length > 2) {
            throw new IllegalArgumentException("1P 또는 2P로만 로그인할 수 있습니다.");
        }
        List<String> valid = new ArrayList<>();
        for (String nickname : nicknames) {
            valid.add(NicknameRule.validate(nickname));
        }
        if (valid.size() == 2 && valid.get(0).equals(valid.get(1))) {
            throw new IllegalArgumentException("1P와 2P 닉네임이 같습니다. 서로 다르게 입력해 주세요.");
        }
        return List.copyOf(valid);
    }

    /** 로그인이 받아들여진 플레이어(1P부터)로 맞춘다. 닉네임은 서버가 다듬어 돌려준 값을 쓴다 */
    public void signedIn(List<LoginService.LoginPlayer> players) {
        if (players.isEmpty() || players.size() > 2) {
            throw new IllegalArgumentException("로그인한 플레이어는 1~2명이어야 합니다: " + players.size());
        }
        nicknames = players.stream().map(LoginService.LoginPlayer::nickname).toList();
        playerIds = players.stream().map(LoginService.LoginPlayer::playerId).toList();
    }

    /** 이 컴퓨터의 플레이어 닉네임. 1P부터 */
    public List<String> nicknames() { return nicknames; }

    /** 이 컴퓨터의 플레이어 번호. 1P부터. 온라인이면 서버가 정한 번호다 */
    public List<Long> playerIds() { return playerIds; }

    /** 1P 닉네임 */
    public String nickname() { return nicknames.getFirst(); }

    /** 이 컴퓨터에서 함께 하는 사람 수 (1P면 1, 2P면 2) */
    public int localPlayers() { return nicknames.size(); }

    public boolean isHost() { return service.isHost(); }

    /** 방에 들어간다. 2P면 두 자리를 차지한다. */
    public Room join(int roomNumber) {
        Room joined = service.join(roomNumber, localPlayers());
        enter(joined);
        return joined;
    }

    public Room create(String title, int capacity) {
        Room created = service.create(title, capacity, localPlayers());
        enter(created);
        return created;
    }

    public Room quickJoin() {
        Room joined = service.quickJoin(localPlayers());
        enter(joined);
        return joined;
    }

    /** 방에 들어온 상태로 맞춘다. 목업 참가자는 모두 준비한 채로 시작한다. */
    public void enter(Room room) {
        this.room = room;
        ready = false;
        participantReady.clear();
        for (int id = localPlayers() + 1; id <= room.players(); id++) {
            participantReady.put(id, true);
        }
        mapId = GameMaps.byName(room.map()).id();
    }

    public Room room() { return room; }
    /** 1P가 고른 캐릭터. 랜덤을 골랐으면 마지막으로 고른 캐릭터이고, 실제 캐릭터는 {@link #start}에서 정한다 */
    public Mascot character() { return character(0); }
    public boolean randomCharacter() { return randomCharacter(0); }
    public Team team() { return team(0); }

    /** local번째(0이 1P) 플레이어가 고른 캐릭터. 랜덤을 골랐으면 마지막으로 고른 캐릭터다 */
    public Mascot character(int local) { return characters[checkLocal(local)]; }

    /** local번째(0이 1P) 플레이어가 ?(랜덤)를 골랐는지 */
    public boolean randomCharacter(int local) { return randomCharacters[checkLocal(local)]; }

    /** local번째(0이 1P) 플레이어가 고른 팀 */
    public Team team(int local) { return teams[checkLocal(local)]; }
    public String mapId() { return mapId; }
    public boolean ready() { return ready; }
    public void setReady(boolean value) { ready = value; }

    /** 1P의 캐릭터를 바꾼다. 바꾸면 준비가 풀리고 랜덤도 풀린다. */
    public void select(Mascot character) {
        select(0, character);
    }

    /** 1P가 ?(랜덤 캐릭터)를 고른다. 게임을 시작할 때 다섯 캐릭터 중 하나로 정한다 */
    public void selectRandomCharacter() {
        selectRandomCharacter(0);
    }

    /** 1P의 팀을 바꾼다. 바꾸면 준비가 풀린다. */
    public void selectTeam(Team team) {
        selectTeam(0, team);
    }

    /**
     * local번째(0이 1P) 플레이어의 캐릭터를 바꾼다. 바꾸면 준비가 풀리고 그 플레이어의 랜덤도 풀린다.
     *
     * @throws IllegalArgumentException 이 컴퓨터에 그 플레이어가 없으면 (1P로 로그인했는데 2P를 고르는 등)
     */
    public void select(int local, Mascot character) {
        characters[checkPlaying(local)] = Objects.requireNonNull(character);
        randomCharacters[local] = false;
        ready = false;
    }

    /** local번째(0이 1P) 플레이어가 ?(랜덤 캐릭터)를 고른다. 게임을 시작할 때 다섯 캐릭터 중 하나로 정한다 */
    public void selectRandomCharacter(int local) {
        randomCharacters[checkPlaying(local)] = true;
        ready = false;
    }

    /** local번째(0이 1P) 플레이어의 팀(캐릭터 색)을 바꾼다. 바꾸면 준비가 풀린다. */
    public void selectTeam(int local, Team team) {
        teams[checkPlaying(local)] = Objects.requireNonNull(team);
        ready = false;
    }

    private static int checkLocal(int local) {
        if (local < 0 || local >= MAX_LOCAL_PLAYERS) {
            throw new IllegalArgumentException("이 컴퓨터의 플레이어 번호는 0(1P) 또는 1(2P)입니다: " + local);
        }
        return local;
    }

    private int checkPlaying(int local) {
        checkLocal(local);
        if (local >= localPlayers()) {
            throw new IllegalArgumentException("%dP는 이 컴퓨터에서 함께 하고 있지 않습니다.".formatted(local + 1));
        }
        return local;
    }

    /**
     * 방장이 맵을 고른다(랜덤 포함). 맵 최대 인원보다 사람이 많으면 고를 수 없고, 고르면 방 정원이 그 맵에 맞게 바뀐다.
     */
    public void selectMap(String mapId) {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 맵을 선택할 수 있습니다.");
        }
        GameMaps.GameMap map;
        try {
            map = GameMaps.byId(mapId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 맵입니다.", e);
        }
        if (room != null && room.players() > map.maxPlayers()) {
            throw new IllegalStateException("%s은(는) 최대 %d명이라 지금 인원 %d명으로는 고를 수 없습니다."
                    .formatted(map.name(), map.maxPlayers(), room.players()));
        }
        if (room != null) {
            room = service.updateRoom(map.name(), Room.Status.WAITING);
        }
        this.mapId = mapId;
    }

    /** 방장 자리의 순서. 내가 방장이면 0(1P), 아니면 내 다음 첫 목업 참가자다. */
    public int hostIndex() {
        return service.isHost() ? 0 : localPlayers();
    }

    /** 방 인원. 이 컴퓨터의 플레이어가 앞에, 목업 참가자가 뒤에 온다. */
    public List<GameStartInfo.PlayerInfo> players() {
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < nicknames.size(); i++) {
            players.add(new GameStartInfo.PlayerInfo(i + 1, nicknames.get(i), characters[i], teams[i].label()));
        }
        for (int id = nicknames.size() + 1; id <= room.players(); id++) {
            players.add(new GameStartInfo.PlayerInfo(id, "목업 " + id,
                    Mascot.values()[(id - 1) % Mascot.values().length],
                    (id % 2 == 0 ? Team.BLUE : Team.RED).label()));
        }
        return List.copyOf(players);
    }

    /** 목업 참가자의 준비 상태를 바꾼다. 서버가 붙으면 서버에서 받은 준비 상태도 이 메서드로 반영한다. */
    public void setParticipantReady(int playerId, boolean value) {
        if (!participantReady.containsKey(playerId)) {
            throw new IllegalArgumentException("방에 없는 참가자입니다: " + playerId);
        }
        participantReady.put(playerId, value);
    }

    /** playerId번 사람이 준비했는지. 이 컴퓨터의 플레이어는 함께 준비한다 */
    public boolean isReady(int playerId) {
        return playerId <= localPlayers() ? ready : participantReady.getOrDefault(playerId, false);
    }

    /** 방장이고, 방이 대기 중이고, 다른 참가자가 모두 준비했으면 시작할 수 있다 */
    public boolean canStart() {
        return room != null
                && service.isHost()
                && room.status() == Room.Status.WAITING
                && participantReady.values().stream().allMatch(Boolean::booleanValue);
    }

    /** 경기가 끝나 대기방으로 돌아온다. 방장이면 방을 다시 대기 중으로 돌리고, 준비는 풀린다 */
    public void returnToWaiting() {
        if (room != null && service.isHost()) {
            room = service.updateRoom(room.map(), Room.Status.WAITING);
        }
        ready = false;
    }

    /** 게임을 시작한다. 방은 게임 중이 되고, 랜덤 맵·랜덤 캐릭터는 여기서 실제 맵·캐릭터로 정한다 */
    public GameStartInfo start() {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }
        if (!canStart()) {
            throw new IllegalStateException("모든 참가자가 준비해야 시작할 수 있습니다.");
        }
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>(players());
        // 랜덤을 고른 사람만 각자 따로 뽑는다
        for (int i = 0; i < localPlayers(); i++) {
            if (randomCharacters[i]) {
                Mascot picked = Mascot.values()[random.nextInt(Mascot.values().length)];
                GameStartInfo.PlayerInfo local = players.get(i);
                players.set(i, new GameStartInfo.PlayerInfo(local.id(), local.nickname(), picked, local.team()));
            }
        }
        String map = GameMaps.RANDOM_ID.equals(mapId) ? GameMaps.pick(players.size(), random).id() : mapId;
        GameStartInfo info = new GameStartInfo(map, players, localPlayers());
        room = service.updateRoom(room.map(), Room.Status.PLAYING);
        return info;
    }

    public void leave() {
        service.leave();
        room = null;
        participantReady.clear();
        ready = false;
    }
}
