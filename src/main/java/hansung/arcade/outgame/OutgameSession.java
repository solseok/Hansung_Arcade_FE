package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.Room;
import hansung.arcade.outgame.lobby.RoomState;
import hansung.arcade.outgame.lobby.ServerLobbyService;
import hansung.arcade.outgame.lobby.SlotState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * 한 컴퓨터의 아웃게임 상태. 1P면 한 명, 2P면 두 명이 이 컴퓨터에서 함께 한다.
 * 서버 주소·포트는 사용자가 넣지 않고 설정(application.properties, .env)에서 읽는다.
 *
 * <p>오프라인({@link hansung.arcade.outgame.lobby.MockLobbyService})이면 대기실의 다른 인원은 준비된 목업 참가자다.
 * 온라인({@link ServerLobbyService})이면 방·자리·방장·준비는 서버의 ROOM_STATE를 따르고, 고르기·준비·맵·시작은 서버로 보낸다.
 * 이 컴퓨터 플레이어의 캐릭터·팀·준비도 서버가 알려 준 자리 값으로 맞춘다.</p>
 *
 * <p>캐릭터와 팀(캐릭터 색)은 이 컴퓨터의 플레이어마다 따로 고른다. 번호(local)는 0이 1P, 1이 2P이고,
 * 번호를 받지 않는 메서드는 1P의 것이다. 온라인이면 고른 플레이어 번호(playerId)를 붙여 보내고, 서버도 그 플레이어만 바꾼다.</p>
 *
 * <p>1P·2P가 마지막으로 고른 캐릭터·팀은 이 컴퓨터({@link LocalPicks})에 기억해 다음에 켜도 이어 쓴다. 온라인에서 방에 들어가면
 * 서버 자리가 기억한 값과 다를 때 맞춰 달라고 보낸다. 고른 적이 없으면 캐릭터는 둘 다 상상부기({@link #DEFAULT_CHARACTER_2P})이고,
 * 팀은 2P가 1P와 구별되게 파랑({@link #DEFAULT_TEAM_2P})이다.</p>
 */
public final class OutgameSession {
    /** 이 컴퓨터에서 함께 할 수 있는 최대 인원 */
    public static final int MAX_LOCAL_PLAYERS = 2;
    /** 2P가 고른 적이 없을 때의 캐릭터. 1P와 같은 상상부기다 */
    public static final Mascot DEFAULT_CHARACTER_2P = Mascot.DEFAULT;
    /** 2P가 고른 적이 없을 때의 팀. 1P(빨강)와 구별되게 파랑이다 */
    public static final Team DEFAULT_TEAM_2P = Team.BLUE;

    public final LobbyService service;
    /** 온라인이면 서버 방 API. 오프라인이면 null */
    private final ServerLobbyService online;
    /** 온라인 대기실의 마지막 ROOM_STATE. 방 밖이거나 오프라인이면 null */
    private RoomState roomState;
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
    /** 1P·2P가 마지막으로 고른 캐릭터·팀 (이 컴퓨터에 기억) */
    private final LocalPicks picks;
    /** 기억한 값으로 맞춰 달라고 이미 보낸 방 (온라인). 방 만들기와 대기실 화면이 같은 방으로 두 번 들어온다 */
    private Room pickedRoom;
    private String mapId = GameMaps.DEFAULT_ID;
    private boolean ready;
    /** 목업 참가자(이 컴퓨터 밖의 사람)의 준비 상태. 서버가 붙으면 서버에서 받은 값으로 바꾼다 */
    private final Map<Integer, Boolean> participantReady = new HashMap<>();

    /** 대기실 자리 하나를 그릴 내용. character가 null이면 랜덤(?)이다 */
    public record SeatView(int slot, String nickname, Mascot character, Team team, SlotState state) {
    }

    /** 고른 캐릭터·팀을 이번 실행 동안만 기억한다 */
    public OutgameSession(LobbyService service) {
        this(service, LocalPicks.inMemory());
    }

    /** picks에 기억해 둔 1P·2P의 캐릭터·팀으로 시작하고, 고를 때마다 picks에 적는다 */
    public OutgameSession(LobbyService service, LocalPicks picks) {
        this.service = service;
        this.picks = Objects.requireNonNull(picks);
        for (int local = 0; local < MAX_LOCAL_PLAYERS; local++) {
            String character = pickedCharacter(local);
            randomCharacters[local] = RoomState.RANDOM_CHARACTER.equals(character);
            if (!randomCharacters[local]) {
                characters[local] = Mascot.fromId(character);
            }
            teams[local] = pickedTeam(local);
        }
        this.online = service instanceof ServerLobbyService server ? server : null;
        if (online != null) {
            // 화면보다 먼저 등록해, 화면이 알림을 받을 때는 세션이 이미 새 상태다
            online.listen(new ServerLobbyService.Listener() {
                @Override
                public void roomStateChanged(RoomState state) {
                    applyRoomState(state);
                }
            });
        }
    }

    /** 서버의 방 API를 쓰는지 */
    public boolean isOnline() {
        return online != null;
    }

    /** 온라인이면 서버 방 API */
    public Optional<ServerLobbyService> online() {
        return Optional.ofNullable(online);
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
        room = null;
        roomState = null;
        service.signedIn(playerIds);
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

    /** 1P가 말한다. 온라인이면 서버에 보내고, 화면에는 서버가 돌려준 CHAT_MESSAGE로 뜬다 */
    public void chat(String text) {
        if (online != null) {
            online.chat(playerIds.getFirst(), text);
        }
    }

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

    /**
     * 방에 들어온 상태로 맞춘다. 목업 참가자는 모두 준비한 채로 시작한다. 온라인이면 서버가 알려 준 방 상태를 쓰고,
     * 이 컴퓨터에 기억한 1P·2P의 캐릭터·팀과 다른 자리는 맞춰 달라고 서버에 보낸다.
     */
    public void enter(Room room) {
        this.room = room;
        ready = false;
        participantReady.clear();
        if (online != null) {
            applyRoomState(online.state());
            usePicks();
            return;
        }
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

    /** 준비를 켜고 끈다. 온라인이면 바뀔 때만 서버에 READY를 보낸다 (방장은 보내지 않는다) */
    public void setReady(boolean value) {
        if (online != null && room != null && room.status() == Room.Status.WAITING && !isHost() && value != ready) {
            online.ready(value);
        }
        ready = value;
    }

    /**
     * 서버가 알려 준 방 상태로 맞춘다 (온라인). 방·맵과 함께 이 컴퓨터 플레이어(1P, 2P) 자리의 캐릭터·팀과 준비도 서버 값을 따른다.
     */
    public void applyRoomState(RoomState state) {
        if (state == null) {
            return;
        }
        roomState = state;
        room = state.room();
        mapId = state.mapId();
        for (int local = 0; local < playerIds.size() && local < MAX_LOCAL_PLAYERS; local++) {
            RoomState.Seat seat = state.seatOf(playerIds.get(local));
            if (seat == null) {
                continue;
            }
            randomCharacters[local] = seat.randomCharacter();
            if (!seat.randomCharacter()) {
                characters[local] = seat.mascot();
            }
            teams[local] = seat.team();
            if (local == 0) {
                // 준비는 연결(이 컴퓨터) 단위라 1P 자리 값을 쓴다
                ready = seat.ready();
            }
        }
    }

    /**
     * 방에 들어왔을 때 서버 자리의 캐릭터·팀이 이 컴퓨터에 기억한 값과 다르면 기억한 값으로 보이고, 그 플레이어 번호를 붙여
     * 서버에 보낸다 (온라인). 같은 방(같은 Room 객체)으로 다시 들어오면 이미 보냈으므로 보이기만 한다.
     */
    private void usePicks() {
        boolean send = room != pickedRoom;
        pickedRoom = room;
        if (roomState == null) {
            return;
        }
        for (int local = 0; local < localPlayers() && local < MAX_LOCAL_PLAYERS; local++) {
            RoomState.Seat seat = roomState.seatOf(playerIds.get(local));
            if (seat == null) {
                continue;
            }
            String character = pickedCharacter(local);
            if (!character.equals(seat.character())) {
                randomCharacters[local] = RoomState.RANDOM_CHARACTER.equals(character);
                if (!randomCharacters[local]) {
                    characters[local] = Mascot.fromId(character);
                }
                if (send) {
                    online.selectCharacter(playerIds.get(local), character);
                }
            }
            Team team = pickedTeam(local);
            if (team != seat.team()) {
                teams[local] = team;
                if (send) {
                    online.selectTeam(playerIds.get(local), team);
                }
            }
        }
    }

    /** local번째 플레이어가 기억한 캐릭터 id (랜덤이면 random). 고른 적이 없으면 상상부기 */
    private String pickedCharacter(int local) {
        return picks.character(local).orElse((local == 0 ? Mascot.DEFAULT : DEFAULT_CHARACTER_2P).id());
    }

    /** local번째 플레이어가 기억한 팀. 고른 적이 없으면 1P 빨강, 2P 파랑 */
    private Team pickedTeam(int local) {
        return picks.team(local).orElse(local == 0 ? Team.RED : DEFAULT_TEAM_2P);
    }

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
     * 온라인이면 그 플레이어 번호를 붙여 SELECT_CHARACTER를 보낸다. 고른 것은 이 컴퓨터에 기억한다.
     *
     * @throws IllegalArgumentException 이 컴퓨터에 그 플레이어가 없으면 (1P로 로그인했는데 2P를 고르는 등)
     */
    public void select(int local, Mascot character) {
        characters[checkPlaying(local)] = Objects.requireNonNull(character);
        randomCharacters[local] = false;
        picks.saveCharacter(local, character.id());
        if (online != null) {
            // 준비가 풀리는지는 서버가 정한다 (같은 캐릭터를 다시 고르면 서버는 준비를 그대로 둔다)
            online.selectCharacter(playerIds.get(local), character.id());
        } else {
            ready = false;
        }
    }

    /** local번째(0이 1P) 플레이어가 ?(랜덤 캐릭터)를 고른다. 게임을 시작할 때 다섯 캐릭터 중 하나로 정한다 */
    public void selectRandomCharacter(int local) {
        randomCharacters[checkPlaying(local)] = true;
        picks.saveCharacter(local, RoomState.RANDOM_CHARACTER);
        if (online != null) {
            online.selectCharacter(playerIds.get(local), RoomState.RANDOM_CHARACTER);
        } else {
            ready = false;
        }
    }

    /**
     * local번째(0이 1P) 플레이어의 팀(캐릭터 색)을 바꾼다. 바꾸면 준비가 풀린다. 온라인이면 SELECT_TEAM을 보낸다.
     * 고른 것은 이 컴퓨터에 기억한다.
     */
    public void selectTeam(int local, Team team) {
        teams[checkPlaying(local)] = Objects.requireNonNull(team);
        picks.saveTeam(local, team);
        if (online != null) {
            online.selectTeam(playerIds.get(local), team);
        } else {
            ready = false;
        }
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

    /**
     * 대기실 자리들 (자리 번호 순서, 빈자리는 없다). 온라인은 ROOM_STATE를, 오프라인은 이 컴퓨터의 플레이어를 앞에,
     * 목업 참가자를 뒤에 둔다. 방장과 같은 컴퓨터의 2P는 방장이 시작할 때 함께 들어가므로 늘 준비한 것으로 보인다.
     */
    public List<SeatView> seats() {
        List<SeatView> seats = new ArrayList<>();
        boolean meHost = isHost();
        if (roomState != null) {
            for (RoomState.Seat seat : roomState.seats()) {
                boolean mine = playerIds.contains(seat.playerId());
                SlotState state = seat.playerId() == roomState.hostId() ? SlotState.HOST
                        : (mine && meHost) || seat.ready() ? SlotState.READY : SlotState.WAITING;
                seats.add(new SeatView(seat.slot(), seat.nickname(), seat.mascot(), seat.team(), state));
            }
            return List.copyOf(seats);
        }
        if (room == null) {
            return List.of();
        }
        List<GameStartInfo.PlayerInfo> players = players();
        int host = hostIndex();
        for (int i = 0; i < players.size(); i++) {
            GameStartInfo.PlayerInfo player = players.get(i);
            boolean local = i < localPlayers();
            SlotState state = i == host ? SlotState.HOST
                    : (local && meHost) || isReady(i + 1) ? SlotState.READY : SlotState.WAITING;
            seats.add(new SeatView(i, player.nickname(), local && randomCharacters[i] ? null : player.character(),
                    Team.fromLabel(player.team()), state));
        }
        return List.copyOf(seats);
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
        if (roomState != null) {
            return isHost()
                    && room.status() == Room.Status.WAITING
                    && roomState.seats().size() >= 2
                    && roomState.seats().stream()
                    .filter(seat -> !playerIds.contains(seat.playerId()))
                    .allMatch(RoomState.Seat::ready);
        }
        return room != null
                && service.isHost()
                && room.status() == Room.Status.WAITING
                && participantReady.values().stream().allMatch(Boolean::booleanValue);
    }

    /** 경기가 끝나 대기방으로 돌아온다. 방장이면 방을 다시 대기 중으로 돌리고, 준비는 풀린다 (온라인은 서버가 돌린다) */
    public void returnToWaiting() {
        if (online != null) {
            ready = false;
            return;
        }
        if (room != null && service.isHost()) {
            room = service.updateRoom(room.map(), Room.Status.WAITING);
        }
        ready = false;
    }

    /**
     * 게임을 시작한다. 방은 게임 중이 되고, 랜덤 맵·랜덤 캐릭터는 여기서 실제 맵·캐릭터로 정한다.
     * 온라인이면 서버에 GAME_START만 보내고 null을 돌려준다. 맵·캐릭터는 서버가 정해 GAME_STARTED로 알린다({@link #startInfo}).
     *
     * @throws IllegalStateException 방장이 아니거나 시작할 수 없으면. 온라인이면 서버 문구(모두 준비 등)
     */
    public GameStartInfo start() {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }
        if (online != null) {
            online.startGame();
            return null;
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

    /**
     * 서버가 알린 GAME_STARTED를 게임 화면에 넘길 정보로 바꾼다. 이 컴퓨터의 플레이어(1P, 2P 순서)를 앞에 둔다.
     */
    public GameStartInfo startInfo(ServerLobbyService.GameStarted started) {
        List<GameStartInfo.PlayerInfo> local = new ArrayList<>();
        List<GameStartInfo.PlayerInfo> others = new ArrayList<>();
        for (long id : playerIds) {
            started.players().stream().filter(player -> player.playerId() == id).findFirst()
                    .ifPresent(player -> local.add(info(player)));
        }
        for (ServerLobbyService.StartedPlayer player : started.players()) {
            if (!playerIds.contains(player.playerId())) {
                others.add(info(player));
            }
        }
        if (local.isEmpty()) {
            throw new IllegalStateException("시작한 경기에 이 컴퓨터의 플레이어가 없습니다.");
        }
        if (!GameMaps.ids().contains(started.mapId())) {
            throw new IllegalStateException("이 게임에 없는 맵입니다: " + started.mapId());
        }
        local.addAll(others);
        return new GameStartInfo(started.mapId(), local, local.size() - others.size(), true);
    }

    private static GameStartInfo.PlayerInfo info(ServerLobbyService.StartedPlayer player) {
        return new GameStartInfo.PlayerInfo((int) player.playerId(), player.nickname(),
                Mascot.fromId(player.character()), Team.valueOf(player.team()).label(), player.spawn());
    }

    /**
     * 서버가 이 컴퓨터의 플레이어를 경기와 방에서 함께 뺐다 (로딩 시간 초과). 이미 빠졌으므로 GAME_EXIT·ROOM_LEAVE를
     * 보내지 않고 방만 잊는다. 나가기를 눌러 나올 때는 {@link #exitGame}이다.
     */
    public void droppedFromGame() {
        if (online != null) {
            online.forgetRoom();
        }
        roomState = null;
        room = null;
        participantReady.clear();
        ready = false;
    }

    /** 게임 화면에서 나간다 (온라인: GAME_EXIT). 서버는 방에서 빼므로 이 컴퓨터도 방을 떠난 상태가 된다 */
    public void exitGame() {
        online().ifPresent(ServerLobbyService::exitGame);
        roomState = null;
        room = null;
        participantReady.clear();
        ready = false;
    }

    /**
     * 방에서 나간다. 온라인이면 서버가 거절해도 이 컴퓨터는 방을 떠난 상태가 된다.
     *
     * @throws IllegalStateException 서버가 나가기를 거절했으면 (방은 이미 떠난 상태다)
     */
    public void leave() {
        try {
            service.leave();
        } finally {
            roomState = null;
            room = null;
            participantReady.clear();
            ready = false;
        }
    }
}
