package hansung.arcade.ingame;

import hansung.arcade.ingame.airplane.AirDrop;
import hansung.arcade.ingame.airplane.DropZone;
import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.Explosion;
import hansung.arcade.ingame.block.BoxPusher;
import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Obstacles;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.CellOccupancy;
import hansung.arcade.ingame.item.CellOccupancy.Occupant;
import hansung.arcade.ingame.item.FieldItems;
import hansung.arcade.ingame.item.ItemActions;
import hansung.arcade.ingame.item.ItemActions.BalloonKeyResult;
import hansung.arcade.ingame.item.ItemDrops;
import hansung.arcade.ingame.item.ItemEffects;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.Match;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.StartIntro;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * 인게임 상태(맵 + 플레이어 + 물풍선 + 바닥 아이템 + 올라온 채팅 + 경기 진행). Swing에 의존하지 않아서 테스트와 이후 네트워크 동기화에 그대로 쓸 수 있다.
 *
 * <p>물풍선 싸움 규칙은 {@link #update} 한 틱 안에서 이 순서로 처리한다.</p>
 * <ol>
 *   <li>스프링 신발로 뛰는 중인 플레이어가 공중을 지나간다. 공중에서는 이동 입력을 받지 않는다 ({@link PlayerGear}).</li>
 *   <li>플레이어가 움직인다. 놓인 물풍선은 막힌 칸이다 (놓는 순간 그 칸에 있던 사람은 빠져나갈 수 있다).
 *       상자에 막힌 채 계속 밀면 상자가 한 칸 밀린다 ({@link BoxPusher}). 신발이 있으면 막힌 앞 칸의 물풍선을 민다.</li>
 *   <li>밀린 물풍선이 미끄러지고 던진 물풍선이 날아가며, 때가 된 물풍선이 터지고, 다 보인 물줄기가 사라진다 ({@link BalloonField}).</li>
 *   <li>물풍선 키를 막 누른 플레이어가 선 칸에 물풍선을 놓는다. 장갑이 있으면 선 칸이나 앞 칸의 물풍선을 던진다.
 *       아이템 키를 막 누른 플레이어는 아이템 칸의 액티브 아이템을 쓴다 ({@link ItemActions}).</li>
 *   <li>이번 틱에 터진 물줄기가 막혀 멈춘 블럭을 맞힌다. 한 틱에 여럿이 같은 블럭을 맞혀도 한 번만 맞는다.
 *       다 부서진 블럭 자리에는 {@link ItemDrops} 확률로 아이템이 나온다.</li>
 *   <li>물줄기가 덮은 칸의 아이템은 사라진다. 블럭 자리는 물줄기가 들어가지 않은 칸이라 방금 나온 아이템은 남는다.</li>
 *   <li>물줄기 싸움을 가린다 ({@link BattleRules}). 물줄기가 덮은 칸에 선 플레이어를 가두고, 갇힌 사람에게 닿은 플레이어가
 *       같은 편이면 구하고(save) 상대면 터뜨리며(kill, 탈락), 갇힌 채 5초가 지난 사람을 탈락시킨다.</li>
 *   <li>보통 상태인 플레이어가 선 칸의 아이템을 먹는다 ({@link ItemEffects}). 갇히거나 탈락한 사람은 먹지 못한다.</li>
 *   <li>이 틱의 탈락·kill·save를 모두 반영한 뒤 경기 시계를 흘리고 승패를 가린다.</li>
 *   <li>남은 시간이 출현 시각이 됐으면 비행기가 나오고, 날고 있는 비행기가 아이템을 떨어뜨린다 ({@link AirDrop}).</li>
 * </ol>
 *
 * <p>시작 연출({@link #beginIntro()}, "Ready... Start!")을 켰으면 START!! 글자가 다 사라질 때까지는 위 규칙을 하나도 돌리지 않는다.
 * 키를 눌러도 움직이거나 물풍선을 놓지 않는다. 경기 시계는 START!! 글자가 뜬 뒤부터 흐른다 ({@link StartIntro}).
 * 채팅은 그대로 올라가고 사라진다.</p>
 *
 * <p>한 틱에 일어난 일 가운데 소리를 낼 일(물풍선 놓기·터짐, 갇힘, 탈락, 아이템 나옴·먹음 등)은 {@link GameSound}로 모아 두고,
 * 화면이 {@link #takeSounds()}로 꺼내 소리를 낸다.</p>
 *
 * <p>경기({@link Match})가 끝나면 이동 입력과 물풍선 놓기는 더 받지 않고, 물줄기도 보이기만 할 뿐 아무도 가두지 않는다.
 * 그래도 시간({@link #elapsedSeconds()})은 계속 흘러서 채팅이 사라지고, 이미 놓인 물풍선이 터지고, 탈락 연출도 끝까지 이어진다.
 * 끝날 때 갇혀 있던 사람은 결과가 바뀌지 않도록 갇힌 채로 남는다.</p>
 *
 * <p>온라인 경기({@link #online})는 판정을 서버가 한다. 위 규칙 가운데 이 컴퓨터의 플레이어가 움직이는 것만 여기서 하고,
 * 물풍선·물줄기·블럭·아이템·갇힘·승패는 서버가 알려 준 대로 맞춘다 ({@link ServerRules}).</p>
 */
public final class GameWorld {

    /**
     * 밀린 블럭이 들어갈 수 없게 하는 것: 물풍선(미끄러져 들어오거나 던져져 내려앉을 칸 포함), 아이템,
     * 몸이 조금이라도 걸친 플레이어(사라진 사람 빼고), 점프해 내려앉을 칸, 물줄기. 물줄기가 남은 칸에 들어간 블럭은 바로 부서질 수 있는데,
     * 거기서 나온 아이템은 그 물줄기에 바로 사라지므로 밀지 않는다.
     */
    private static final Set<Occupant> BOX_BLOCKERS = Collections.unmodifiableSet(EnumSet.allOf(Occupant.class));

    private final TileMap map;
    private final BalloonField balloonField;
    private final FieldItems items = new FieldItems();
    private final CellOccupancy occupancy;
    private final ItemDrops itemDrops;
    private final BoxPusher boxPusher;
    private final ItemActions itemActions;
    /** 플레이어 번호 → 아이템으로 얻은 능력과 아이템 칸 */
    private final Map<Integer, PlayerGear> gears = new HashMap<>();
    private final AirDrop airDrop;
    private final DropZone dropZone = new WorldDropZone();
    /** 이 컴퓨터의 첫 플레이어(1P). 채팅은 이 플레이어가 친 것으로 올라간다 */
    private final Player localPlayer;
    /** 이 컴퓨터에서 조작하는 플레이어들. 1P, 2P 순서 */
    private final List<Player> localPlayers;
    /** 맵 위의 모든 플레이어. 오프라인이면 이 컴퓨터의 플레이어뿐이다 */
    private final List<Player> allPlayers;
    /** 온라인 경기면 서버가 정한 일을 맞추는 규칙. 오프라인이면 null */
    private final ServerRules server;
    private final ChatLog chatLog = new ChatLog();
    private final SpeechBubbles speechBubbles = new SpeechBubbles();
    private final Match match;
    private final BattleRules battleRules;
    private final StartIntro intro = new StartIntro();
    /** 아직 화면이 꺼내 가지 않은 소리. 같은 소리는 한 번만 담는다 */
    private final EnumSet<GameSound> sounds = EnumSet.noneOf(GameSound.class);
    private double elapsedSeconds;

    /** 개인전으로 시작한다. */
    public GameWorld(TileMap map, PlayerProfile localProfile) {
        this(map, List.of(localProfile), MatchMode.SOLO);
    }

    /** @param localProfile 이 컴퓨터에서 조작하는 플레이어의 닉네임·캐릭터·팀 */
    public GameWorld(TileMap map, PlayerProfile localProfile, MatchMode mode) {
        this(map, List.of(localProfile), mode);
    }

    /** 한 컴퓨터에서 여럿이 하는 개인전. */
    public GameWorld(TileMap map, List<PlayerProfile> localProfiles) {
        this(map, localProfiles, MatchMode.SOLO);
    }

    /**
     * 한 컴퓨터에서 여럿이 하는 게임(한 키보드 2인 모드).
     *
     * @param localProfiles 이 컴퓨터에서 조작하는 플레이어들의 닉네임·캐릭터·팀. 1P, 2P 순서이고,
     *                      n번째(1부터) 플레이어는 번호 n을 받아 맵의 n번 스폰에서 시작한다
     * @param mode          개인전·팀전
     */
    public GameWorld(TileMap map, List<PlayerProfile> localProfiles, MatchMode mode) {
        this(map, localProfiles, mode, RandomGenerator.getDefault());
    }

    /**
     * 난수를 정해 둔 게임. 블럭에서 아이템이 나오는지·무엇인지와 비행기가 떨어뜨릴 칸·아이템을 이 난수로 정하므로,
     * 테스트에서 결과를 고정할 때 쓴다.
     *
     * @param random 아이템 드롭과 비행기에 쓸 난수
     */
    public GameWorld(TileMap map, List<PlayerProfile> localProfiles, MatchMode mode, RandomGenerator random) {
        List<Point> spawns = map.spawnPoints();
        if (localProfiles.isEmpty()) {
            throw new IllegalArgumentException("플레이어가 한 명은 있어야 합니다.");
        }
        if (localProfiles.size() > spawns.size()) {
            throw new IllegalArgumentException("맵 %s에는 스폰이 %d곳뿐이라 %d명이 시작할 수 없습니다."
                    .formatted(map.name(), spawns.size(), localProfiles.size()));
        }
        this.map = map;
        this.balloonField = new BalloonField(map);
        this.itemDrops = new ItemDrops(random);
        this.boxPusher = new BoxPusher(map);
        this.itemActions = new ItemActions(map, balloonField);
        this.airDrop = new AirDrop(map, random);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < localProfiles.size(); i++) {
            Point spawn = spawns.get(i);
            players.add(new Player(i + 1, localProfiles.get(i), spawn.x, spawn.y));
            gears.put(i + 1, new PlayerGear());
        }
        this.localPlayers = List.copyOf(players);
        this.allPlayers = localPlayers;
        this.localPlayer = localPlayers.get(0);
        this.occupancy = new CellOccupancy(balloonField, items, localPlayers, this::gear);
        // 플레이어를 모두 만든 뒤에 경기를 연다 (경기 중에 플레이어를 더하지 않는다)
        this.match = new Match(mode, players());
        this.battleRules = new BattleRules(localPlayers, match, this::gear, balloonField::wetOwnersAt);
        this.server = null;
    }

    /**
     * 서버가 판정하는 온라인 경기. 플레이어는 서버가 정한 번호·스폰 자리에서 시작하고, 이 컴퓨터의 플레이어(local)만 키로 움직인다.
     * 승패는 팀으로 가린다 (서버 규칙). 서버가 라운드를 열 때(ROUND_BEGIN)까지는 검은 화면에서 기다린다.
     *
     * @param seats  경기에 들어온 모든 플레이어 (GAME_STARTED). 이 컴퓨터의 플레이어는 1P, 2P 순서로 있어야 한다
     * @param outbox 이 컴퓨터의 플레이어가 한 일을 서버로 보낼 곳
     * @throws IllegalArgumentException 이 컴퓨터의 플레이어가 없거나, 번호가 겹치거나, 맵에 없는 스폰 번호면
     */
    public static GameWorld online(TileMap map, List<OnlineSeat> seats, ServerOutbox outbox) {
        return new GameWorld(map, seats, outbox);
    }

    private GameWorld(TileMap map, List<OnlineSeat> seats, ServerOutbox outbox) {
        List<Point> spawns = map.spawnPoints();
        if (seats.stream().noneMatch(OnlineSeat::local)) {
            throw new IllegalArgumentException("이 컴퓨터의 플레이어가 경기에 없습니다.");
        }
        this.map = map;
        this.balloonField = new BalloonField(map);
        this.itemDrops = new ItemDrops(RandomGenerator.getDefault());
        this.boxPusher = new BoxPusher(map);
        this.itemActions = new ItemActions(map, balloonField);
        this.airDrop = new AirDrop(map, RandomGenerator.getDefault());
        List<Player> players = new ArrayList<>();
        List<Player> locals = new ArrayList<>();
        for (OnlineSeat seat : seats) {
            if (seat.spawn() < 1 || seat.spawn() > spawns.size()) {
                throw new IllegalArgumentException("맵 %s에 %d번 스폰이 없습니다.".formatted(map.name(), seat.spawn()));
            }
            if (gears.containsKey(seat.playerId())) {
                throw new IllegalArgumentException("플레이어 번호가 겹칩니다: " + seat.playerId());
            }
            Point spawn = spawns.get(seat.spawn() - 1);
            Player player = new Player(seat.playerId(), seat.profile(), spawn.x, spawn.y);
            players.add(player);
            gears.put(player.id(), new PlayerGear());
            if (seat.local()) {
                locals.add(player);
            }
        }
        this.localPlayers = List.copyOf(locals);
        this.allPlayers = List.copyOf(players);
        this.localPlayer = localPlayers.get(0);
        this.occupancy = new CellOccupancy(balloonField, items, allPlayers, this::gear);
        this.match = Match.judgedByServer(MatchMode.TEAM, allPlayers);
        this.battleRules = new BattleRules(localPlayers, match, this::gear, balloonField::wetOwnersAt);
        this.server = new ServerRules(this, outbox);
        // 서버가 라운드를 열 때까지 검은 화면(Ready 앞)에 멈춰 둔다. 소리는 연출을 실제로 시작할 때 낸다
        intro.begin();
    }

    /**
     * 게임을 dt초(한 틱) 진행한다.
     *
     * @param commands 플레이어 번호마다 이번 틱에 할 일. 없는 플레이어는 아무것도 하지 않는다 ({@link PlayerCommand#IDLE})
     */
    public void update(double dt, Map<Integer, PlayerCommand> commands) {
        if (server != null) {
            updateOnline(dt, commands);
            return;
        }
        elapsedSeconds += dt;
        StartIntro.Phase introPhase = advanceIntro(dt);
        if (introPhase != StartIntro.Phase.DONE) {
            // 연출 중에는 아무도 조작하지 못한다. START!! 글자가 뜬 뒤부터는 경기 시계만 흐른다
            if (introPhase == StartIntro.Phase.START) {
                match.update(dt);
            }
            chatLog.expire(elapsedSeconds);
            speechBubbles.expire(elapsedSeconds);
            return;
        }
        boolean playing = !match.isOver();
        List<Player> players = players();
        Map<Integer, PlayerSnapshot> before = snapshots(players);
        for (Player player : players) {
            PlayerGear gear = gear(player);
            if (gear.advance(dt, player)) {
                // 물풍선이 놓인 칸에 내려앉았으면 갇히지 않고 걸어 나갈 수 있다
                balloonField.letLeave(player);
            }
            // 경기가 끝나면 누르고 있어도 멈춰 서고, 점프해 공중에 있는 동안에도 걷지 않는다
            PlayerCommand command = playing ? commands.getOrDefault(player.id(), PlayerCommand.IDLE) : PlayerCommand.IDLE;
            Direction move = gear.isAirborne() ? null : command.move();
            player.update(move, dt, obstaclesFor(player));
            boxPusher.push(player, move, dt, (col, row) -> occupancy.hasAny(col, row, BOX_BLOCKERS));
            if (playing && itemActions.kick(player, gear, move)) {
                sounds.add(GameSound.KICK);
            }
        }
        balloonField.update(dt, players);
        if (playing) {
            for (Player player : players) {
                PlayerCommand command = commands.getOrDefault(player.id(), PlayerCommand.IDLE);
                if (command.placeBalloon()) {
                    BalloonKeyResult result = itemActions.pressBalloonKey(player, gear(player), players);
                    if (result == BalloonKeyResult.PLACED) {
                        sounds.add(GameSound.BALLOON_PLACED);
                    } else if (result == BalloonKeyResult.THROWN) {
                        sounds.add(GameSound.KICK);
                    }
                }
                if (command.useItem()) {
                    useItem(player);
                }
            }
        }
        // 경기가 끝나도 이미 놓인 물풍선은 터지므로 블럭도 계속 부서진다
        List<Explosion> explosions = balloonField.takeNewExplosions();
        if (explosions.size() == 1) {
            sounds.add(GameSound.EXPLODE);
        } else if (explosions.size() > 1) {
            sounds.add(GameSound.EXPLODE_MULTI);
        }
        breakBlocks(explosions);
        items.removeWhere(balloonField::isWet);
        boxPusher.advance(dt);
        if (playing) {
            battleRules.settle();
            pickUpItems();
        }
        collectPlayerSounds(players, before);
        // 이 틱의 탈락·kill·save를 모두 반영한 뒤에 시계를 흘리고 승패를 가린다
        match.update(dt);
        // 시계를 흘린 뒤라야 이번 틱에 출현 시각이 됐는지 안다
        // 목록 크기는 같은 틱에 하나가 나오고 하나가 사라지면 그대로라, 지금까지 띄운·떨어뜨린 수로 본다
        int launches = airDrop.launches();
        int releases = airDrop.releases();
        airDrop.update(dt, dropZone);
        if (airDrop.launches() > launches) {
            sounds.add(GameSound.AIRPLANE);
        }
        if (airDrop.releases() > releases) {
            sounds.add(GameSound.AIR_DROP);
        }
        chatLog.expire(elapsedSeconds);
        speechBubbles.expire(elapsedSeconds);
    }

    /**
     * 온라인 경기의 한 틱. 이 컴퓨터의 플레이어를 움직이고, 다른 플레이어를 서버가 알려 준 자리로 따라가게 하고,
     * 물풍선 키·아이템 칸은 서버에 보낸다. 물줄기가 사라지는 것처럼 보이기만 하는 일은 여기서 흘린다.
     */
    private void updateOnline(double dt, Map<Integer, PlayerCommand> commands) {
        elapsedSeconds += dt;
        boolean controls = server.advanceRound(dt);
        Map<Integer, PlayerSnapshot> before = snapshots(localPlayers);
        for (Player player : allPlayers) {
            // 보호막 시간을 흘리고, 스프링 신발로 뛰는 중이면 점프가 자리를 옮긴다
            if (gear(player).advance(dt, player)) {
                balloonField.letLeave(player);
            }
        }
        for (Player player : localPlayers) {
            PlayerCommand command = controls ? commands.getOrDefault(player.id(), PlayerCommand.IDLE) : PlayerCommand.IDLE;
            // 점프해 공중에 있는 동안에는 걷지 않는다
            player.update(gear(player).isAirborne() ? null : command.move(), dt, obstaclesFor(player));
        }
        server.moveRemotePlayers(dt);
        balloonField.update(dt, allPlayers);
        if (controls) {
            for (Player player : localPlayers) {
                PlayerCommand command = commands.getOrDefault(player.id(), PlayerCommand.IDLE);
                if (command.placeBalloon()) {
                    server.requestBalloon(player);
                }
                if (command.useItem()) {
                    server.requestItemUse(player);
                }
            }
            server.requestPickups();
        }
        // 서버가 띄운 비행기를 날리고, 서버가 떨어뜨린 아이템을 내린다
        airDrop.update(dt, dropZone);
        List<Explosion> explosions = balloonField.takeNewExplosions();
        if (explosions.size() == 1) {
            sounds.add(GameSound.EXPLODE);
        } else if (explosions.size() > 1) {
            sounds.add(GameSound.EXPLODE_MULTI);
        }
        server.sendMoves(dt);
        // 갇힘·탈락 소리는 서버가 알릴 때 낸다. 여기서는 덤불에 들어간 소리만 고른다
        collectPlayerSounds(localPlayers, before);
        chatLog.expire(elapsedSeconds);
        speechBubbles.expire(elapsedSeconds);
    }

    /** 온라인 경기면 서버가 알린 일을 넣는 곳. 오프라인이면 비어 있다 */
    public Optional<ServerRules> server() {
        return Optional.ofNullable(server);
    }

    /** playerId번 플레이어. 이 경기에 없으면 null */
    Player player(int playerId) {
        for (Player player : allPlayers) {
            if (player.id() == playerId) {
                return player;
            }
        }
        return null;
    }

    /** 소리를 담는다. 같은 소리는 화면이 꺼내 갈 때까지 한 번만 담긴다 */
    void addSound(GameSound sound) {
        sounds.add(sound);
    }

    /**
     * 경기 시작 연출("Ready... Start!")을 처음부터 시작한다. READY 소리를 내고, START!! 글자가 다 사라질 때까지
     * 아무도 조작할 수 없다. 화면에 띄우기 전에 한 번 부른다.
     */
    public void beginIntro() {
        intro.begin();
        sounds.add(GameSound.READY);
    }

    /** 경기 시작 연출. 부르지 않았으면 처음부터 끝난 상태다 */
    public StartIntro intro() {
        return intro;
    }

    /**
     * 지난번 꺼낸 뒤 모인 소리를 꺼내고 비운다. 한 프레임에 같은 소리가 여러 번 일어나도 한 번만 담긴다.
     * 화면이 프레임마다 부른다.
     */
    public List<GameSound> takeSounds() {
        List<GameSound> taken = List.copyOf(sounds);
        sounds.clear();
        return taken;
    }

    /**
     * 연출 시간을 흘린다 (연출이 끝나도 화살표가 사라질 때까지 센다). START!! 글자가 뜨는 틱에 START 소리를 낸다.
     *
     * @return 이번 틱이 시작될 때의 단계. DONE이 아니면 이번 틱은 아직 연출이다
     */
    StartIntro.Phase advanceIntro(double dt) {
        StartIntro.Phase phase = intro.phase();
        intro.advance(dt);
        if (phase == StartIntro.Phase.READY && intro.phase() != StartIntro.Phase.READY) {
            sounds.add(GameSound.START);
        }
        return phase;
    }

    /** 아이템 키를 막 누른 player가 칸의 액티브 아이템을 쓴다. 쓰면 아이템에 맞는 소리를 낸다 */
    private void useItem(Player player) {
        PlayerGear gear = gear(player);
        Optional<ItemType> item = gear.activeItem();
        if (item.isEmpty() || !itemActions.useItem(player, gear)) {
            return;
        }
        switch (item.get()) {
            case SHIELD -> sounds.add(GameSound.SHIELD);
            case SPRING_SHOE -> sounds.add(GameSound.SPRING);
            default -> { }
        }
    }

    /** 틱이 시작될 때 플레이어마다의 상태. 틱이 끝난 뒤와 견줘 갇힘·탈락·탱크 부서짐·덤불 들어감 소리를 고른다 */
    private record PlayerSnapshot(PlayerState state, boolean alive, boolean tank, int col, int row) {
    }

    private Map<Integer, PlayerSnapshot> snapshots(List<Player> players) {
        Map<Integer, PlayerSnapshot> snapshots = new HashMap<>();
        for (Player player : players) {
            snapshots.put(player.id(), new PlayerSnapshot(player.state(), player.isAlive(), gear(player).hasTank(),
                    player.col(), player.row()));
        }
        return snapshots;
    }

    private void collectPlayerSounds(List<Player> players, Map<Integer, PlayerSnapshot> before) {
        for (Player player : players) {
            PlayerSnapshot was = before.get(player.id());
            PlayerGear gear = gear(player);
            if (was.state() != PlayerState.TRAPPED && player.state() == PlayerState.TRAPPED) {
                sounds.add(GameSound.TRAPPED);
            }
            if (was.alive() && !player.isAlive()) {
                sounds.add(GameSound.DIE);
            }
            if (was.tank() && !gear.hasTank()) {
                sounds.add(GameSound.TANK_BROKEN);
            }
            boolean moved = was.col() != player.col() || was.row() != player.row();
            if (moved && player.isAlive() && !gear.isAirborne() && map.isInside(player.col(), player.row())
                    && map.tileAt(player.col(), player.row()) == Tile.BUSH) {
                sounds.add(GameSound.BUSH);
            }
        }
    }

    /** player가 들어갈 수 없는 칸: 맵의 막힌 칸과, player가 지나갈 수 없는 물풍선 칸 */
    private Obstacles obstaclesFor(Player player) {
        return (col, row) -> map.isSolid(col, row) || balloonField.blocks(player, col, row);
    }

    /**
     * 이번 틱에 터진 물줄기들이 막혀 멈춘 칸의 블럭을 한 번씩 맞힌다. 물줄기는 처음 만난 막힌 칸에서 멈추므로
     * 블럭이 여럿 이어져 있어도 맨 앞 하나만 맞는다. 같은 틱의 연쇄 폭발은 모두 이 틱이 시작될 때의 맵으로 물줄기를 뻗었으므로,
     * 여럿이 같은 블럭을 맞혀도 한 번만 맞는다. 다 부서진 블럭 자리에는 아이템이 나올 수 있다.
     */
    private void breakBlocks(List<Explosion> explosions) {
        Set<Point> hit = new LinkedHashSet<>();
        for (Explosion explosion : explosions) {
            hit.addAll(explosion.blockedCells());
        }
        for (Point cell : hit) {
            if (map.hitBlock(cell.x, cell.y)) {
                itemDrops.rollForBrokenBlock().ifPresent(item -> {
                    items.place(cell.x, cell.y, item);
                    sounds.add(GameSound.ITEM_POP);
                });
            }
        }
    }

    /** 보통 상태인 플레이어가 선(충돌 박스 가운데가 있는) 칸의 아이템을 먹는다. 점프해 공중에 있으면 먹지 않는다 */
    private void pickUpItems() {
        for (Player player : players()) {
            PlayerGear gear = gear(player);
            if (player.state() == PlayerState.NORMAL && !gear.isAirborne()) {
                items.take(player.col(), player.row()).ifPresent(item -> {
                    ItemEffects.apply(player, gear, item);
                    sounds.add(GameSound.ITEM_EATEN);
                });
            }
        }
    }

    /**
     * 내가 친 채팅을 올린다. 오프라인이면 바로 내 화면에 올린다. 온라인이면 서버로 보내고,
     * 서버가 돌려준 채팅(나와 다른 플레이어 모두)을 {@link #receiveChat}으로 올린다.
     */
    public void sendChat(String text) {
        if (server != null) {
            server.sendChat(localPlayer, text);
            return;
        }
        receiveChat(new ChatMessage(localPlayer.id(), localPlayer.profile().nickname(), text));
    }

    /**
     * 누군가의 채팅을 채팅 목록에 올리고, 친 플레이어 머리 위에 말풍선으로 띄운다.
     * 내용은 {@link ChatMessage#clean 한 줄로 다듬어} 올리고, 다듬고 나서 비어 있으면 올리지 않는다.
     * 내가 친 글뿐 아니라 서버가 보내 준 글도 같은 규칙을 거친다 (빈 말풍선이나 화면을 덮는 긴 글을 막는다).
     *
     * <p>다른 상태와 마찬가지로 게임 루프가 도는 스레드에서만 부른다. 네트워크 스레드에서 받은 채팅은 그 스레드로 넘겨서 올린다.</p>
     */
    public void receiveChat(ChatMessage message) {
        String content = ChatMessage.clean(message.text());
        if (content.isEmpty()) {
            return;
        }
        ChatMessage shown = content.equals(message.text())
                ? message
                : new ChatMessage(message.senderId(), message.sender(), content);
        chatLog.add(shown, elapsedSeconds);
        speechBubbles.show(shown, elapsedSeconds);
    }

    public ChatLog chatLog() {
        return chatLog;
    }

    public SpeechBubbles speechBubbles() {
        return speechBubbles;
    }

    /** 이번 판의 경기 시계·kill·save·승패 */
    public Match match() {
        return match;
    }

    public TileMap map() {
        return map;
    }

    /** 맵 위의 물풍선과 물줄기 */
    public BalloonField balloonField() {
        return balloonField;
    }

    /** 몸으로 미는 블럭과, 밀려 미끄러지는 중인 블럭 */
    public BoxPusher boxPusher() {
        return boxPusher;
    }

    /** player가 아이템으로 얻은 능력과 액티브 아이템 칸 */
    public PlayerGear gear(Player player) {
        PlayerGear gear = gears.get(player.id());
        if (gear == null) {
            throw new IllegalArgumentException("이 게임의 플레이어가 아닙니다: " + player.id());
        }
        return gear;
    }

    /** 맵 위를 나는 비행기와 비행기에서 떨어지는 중인 아이템 */
    public AirDrop airDrop() {
        return airDrop;
    }

    /** 맵 바닥에 놓인 아이템. 비행기처럼 아이템을 떨어뜨리는 쪽은 여기에 {@link FieldItems#place 놓는다} */
    public FieldItems items() {
        return items;
    }

    /** 이 컴퓨터의 첫 플레이어(1P). 한 키보드 2인 모드에서도 채팅은 1P가 친 것으로 올라간다. */
    public Player localPlayer() {
        return localPlayer;
    }

    /** 이 컴퓨터에서 조작하는 플레이어들. 1P, 2P 순서. 혼자 하면 1P 한 명이다. */
    public List<Player> localPlayers() {
        return localPlayers;
    }

    /** 맵 위의 모든 플레이어. 오프라인이면 이 컴퓨터의 플레이어뿐이고, 온라인이면 다른 컴퓨터의 플레이어도 있다 */
    public List<Player> players() {
        return allPlayers;
    }

    /** 게임을 시작한 뒤 흐른 시간(초). 경기가 끝나도 계속 흐른다. 경기의 남은 시간은 {@link Match#clock()}을 본다 */
    public double elapsedSeconds() {
        return elapsedSeconds;
    }

    /** 비행기에게 넘기는 창구. 이 게임의 경기 시계·끝났는지, 칸 점유, 바닥 아이템만 이어 준다 */
    private final class WorldDropZone implements DropZone {

        @Override
        public MatchClock clock() {
            return match.clock();
        }

        @Override
        public boolean isMatchOver() {
            return match.isOver();
        }

        @Override
        public boolean holdsAny(int col, int row, Set<Occupant> occupants) {
            return occupancy.hasAny(col, row, occupants);
        }

        @Override
        public void placeItem(int col, int row, ItemType item) {
            items.place(col, row, item);
        }
    }
}
