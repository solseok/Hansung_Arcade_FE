package hansung.arcade.ingame.map;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 텍스트 맵 파일을 읽어 {@link TileMap}을 만든다. 파일은 바닥·블럭·스폰 세 층으로 나뉜다.
 *
 * <pre>
 * name=맵 이름        메타데이터 (key=value)
 * [floor]             바닥 층: 한 글자 = 한 칸. 블럭 밑에 깔린 바닥까지 빠짐없이 적는다
 * ..._|_.             '.' 풀·흙 바닥, '_' 길, '|' 차선, '=' 횡단보도, '!' 바닥 가시
 * ..._|_.
 * [block]             블럭 층: 바닥 층과 같은 크기. 바닥 위에 놓인 것을 적고 '.' = 아무것도 없음
 * .B..P#.             블럭·상자·덤불·집·나무 등 바닥이 아닌 글자만 쓴다 ({@link Tile} 참고)
 * .B.T..T             (위 줄의 상자 P 밑은 바닥 층의 '_' 길이다)
 * [spawn]             스폰 층: 바닥 층과 같은 크기. 숫자 1~8 = 스폰 지점, '.' = 없음
 * 1......             블럭이 없는 칸이나 덤불처럼 지나갈 수 있는 칸이면 어디든 된다
 * ...2...
 * </pre>
 *
 * <p>블럭이 부서지거나 밀려 나가면 그 칸의 바닥 층 글자가 드러난다. 층 순서는 상관없지만 층마다 한 번만 쓴다.</p>
 *
 * <p>테스트처럼 작은 맵은 층 이름 없이 한 층(타일 층)에 바로 적어도 된다. 바닥 글자는 바닥이 되고, 블럭은
 * 일반 바닥('.') 위에 놓인 것으로 본다. 숫자 1~8은 "바닥('.') + 스폰"이다. 이 줄임 형식 뒤에도 [spawn] 층을 붙일 수
 * 있지만, 타일 층의 숫자와 함께 쓰면 오류다. 줄임 형식과 [floor]·[block] 층도 한 맵에 섞어 쓸 수 없다.</p>
 *
 * <p>스폰 번호는 1부터 빠짐없이, 번호마다 한 번만 쓴다.</p>
 *
 * <p>맵 id는 resources/maps 아래 경로다. 앞쪽 폴더 이름이 테마가 된다 (camp/camp08 → camp).</p>
 */
public final class MapLoader {

    private static final String MAP_DIR = "/maps/";
    /** 이 줄 다음부터 바닥 층 */
    static final String FLOOR_LAYER = "[floor]";
    /** 이 줄 다음부터 블럭 층 */
    static final String BLOCK_LAYER = "[block]";
    /** 이 줄 다음부터 스폰 층 */
    static final String SPAWN_LAYER = "[spawn]";
    private static final List<String> LAYERS = List.of(FLOOR_LAYER, BLOCK_LAYER, SPAWN_LAYER);
    /** 줄임 형식(층 이름 없이 적은 한 층)을 오류 문구에서 부르는 이름 */
    private static final String TILE_LAYER = "타일";
    /** 블럭 층과 스폰 층에서 "아무것도 없음" */
    private static final char EMPTY = '.';
    /** 바닥 층에 쓸 수 있는 글자들. 예) ". _ | = !" */
    private static final String GROUND_SYMBOLS = Arrays.stream(Tile.values())
            .filter(Tile::isGround)
            .map(tile -> String.valueOf(tile.symbol()))
            .collect(Collectors.joining(" "));

    private MapLoader() {
    }

    /** resources/maps/{id}.txt 를 읽는다. 예) camp/camp08 */
    public static TileMap loadResource(String id) {
        String path = MAP_DIR + id + ".txt";
        try (InputStream in = MapLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        "맵 파일을 찾을 수 없습니다: %s (맵 id는 테마/맵 형식입니다. 예: camp/camp08)".formatted(path));
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parse(id, reader.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException("맵 파일을 읽지 못했습니다: " + path, e);
        }
    }

    /** @param id 맵 id. name= 이 없을 때 이름으로 쓰고, 폴더 부분은 테마가 된다. */
    public static TileMap parse(String id, List<String> lines) {
        String name = id;
        List<String> plainRows = new ArrayList<>();
        Map<String, List<String>> layers = new LinkedHashMap<>();
        List<String> current = plainRows;
        for (String raw : lines) {
            // 메모장 등이 파일 맨 앞에 붙이는 BOM(U+FEFF)은 strip()으로 지워지지 않는다.
            String line = raw.replace("﻿", "").strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[")) {
                if (!LAYERS.contains(line)) {
                    throw new IllegalArgumentException("맵 %s에 모르는 층 이름 %s이(가) 있습니다. 층 이름은 %s 중 하나입니다."
                            .formatted(name, line, String.join(" ", LAYERS)));
                }
                if (layers.containsKey(line)) {
                    throw new IllegalArgumentException("맵 %s에 %s 줄이 두 번 있습니다.".formatted(name, line));
                }
                current = new ArrayList<>();
                layers.put(line, current);
                continue;
            }
            if (line.startsWith("name=")) {
                name = line.substring("name=".length()).strip();
                continue;
            }
            current.add(line);
        }

        Tile[][] ground;
        Tile[][] blocks;
        String sizeLayer;
        List<SpawnPoint> spawns = new ArrayList<>();
        if (layers.containsKey(FLOOR_LAYER) || layers.containsKey(BLOCK_LAYER)) {
            requireLayers(name, plainRows, layers);
            ground = readFloorLayer(name, layers.get(FLOOR_LAYER));
            blocks = readBlockLayer(name, layers.get(BLOCK_LAYER), ground);
            sizeLayer = FLOOR_LAYER + " 층";
        } else {
            if (plainRows.isEmpty()) {
                throw new IllegalArgumentException("맵에 타일이 없습니다: " + name);
            }
            int rows = plainRows.size();
            int columns = plainRows.get(0).length();
            ground = new Tile[rows][columns];
            blocks = new Tile[rows][columns];
            readTileLayer(name, plainRows, ground, blocks, spawns);
            sizeLayer = TILE_LAYER + " 층";
        }
        if (layers.containsKey(SPAWN_LAYER)) {
            if (!spawns.isEmpty()) {
                throw new IllegalArgumentException(("맵 %s의 타일 층에 스폰 숫자가 있는데 %s 층도 있습니다. "
                        + "스폰은 한 곳에만 적어야 하니 타일 층의 숫자를 원래 타일 기호로 바꾸세요.").formatted(name, SPAWN_LAYER));
            }
            readSpawnLayer(name, layers.get(SPAWN_LAYER), sizeLayer, ground, blocks, spawns);
        }
        if (spawns.isEmpty()) {
            throw new IllegalArgumentException("맵에 스폰 지점(1~8)이 없습니다: " + name);
        }
        spawns.sort(Comparator.comparingInt(SpawnPoint::order));
        for (int i = 0; i < spawns.size(); i++) {
            // 스폰 목록은 번호순 위치로 꺼내 쓰므로, 번호가 빠지면 뒤 번호가 앞당겨져 엉뚱한 자리가 된다.
            if (spawns.get(i).order() != i + 1) {
                throw new IllegalArgumentException(
                        "맵 %s에 스폰 지점 %d이(가) 없습니다. 스폰 번호는 1부터 빠짐없이 써야 합니다."
                                .formatted(name, i + 1));
            }
        }
        return new TileMap(name, themeOf(id), ground, blocks, spawns.stream().map(SpawnPoint::position).toList());
    }

    /** [floor]·[block] 층을 쓰는 맵은 두 층과 [spawn] 층이 모두 있어야 하고, 층 이름 없는 줄이 없어야 한다. */
    private static void requireLayers(String name, List<String> plainRows, Map<String, List<String>> layers) {
        if (!plainRows.isEmpty()) {
            throw new IllegalArgumentException(("맵 %s에 층 이름 없이 적은 줄 '%s'이(가) 있습니다. "
                    + "%s·%s 층을 쓰는 맵은 모든 줄을 층 이름 아래에 적어야 합니다.")
                    .formatted(name, plainRows.get(0), FLOOR_LAYER, BLOCK_LAYER));
        }
        for (String layer : LAYERS) {
            if (!layers.containsKey(layer)) {
                throw new IllegalArgumentException("맵 %s에 %s 층이 없습니다. %s 세 층을 모두 적어야 합니다."
                        .formatted(name, layer, String.join(" ", LAYERS)));
            }
        }
        if (layers.get(FLOOR_LAYER).isEmpty()) {
            throw new IllegalArgumentException("맵 %s의 %s 층이 비어 있습니다.".formatted(name, FLOOR_LAYER));
        }
    }

    /** 바닥 층: 바닥 글자({@link Tile#isGround()})만 쓴다. */
    private static Tile[][] readFloorLayer(String name, List<String> rows) {
        int columns = rows.get(0).length();
        Tile[][] ground = new Tile[rows.size()][columns];
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            checkLength(name, FLOOR_LAYER, row, line, columns);
            for (int col = 0; col < columns; col++) {
                char c = line.charAt(col);
                Tile tile = Tile.fromSymbol(c);
                if (tile == null || !tile.isGround()) {
                    throw new IllegalArgumentException("맵 %s의 %s 층 (%d, %d)에 '%c'가 있습니다. 바닥 층에는 바닥 글자(%s)만 쓸 수 있습니다.%s"
                            .formatted(name, FLOOR_LAYER, col, row, c, GROUND_SYMBOLS, hintOutsideFloor(c, tile)));
                }
                ground[row][col] = tile;
            }
        }
        return ground;
    }

    private static String hintOutsideFloor(char c, Tile tile) {
        if (isSpawnDigit(c)) {
            return " 스폰 번호는 %s 층에 적으세요.".formatted(SPAWN_LAYER);
        }
        return tile == null ? " 알 수 없는 글자입니다." : " '%c'는 %s 층에 적으세요.".formatted(c, BLOCK_LAYER);
    }

    /** 블럭 층: 바닥과 같은 크기. '.'은 빈 칸(null)이고 나머지는 바닥이 아닌 타일이다. */
    private static Tile[][] readBlockLayer(String name, List<String> rows, Tile[][] ground) {
        checkRowCount(name, BLOCK_LAYER, rows.size(), FLOOR_LAYER + " 층", ground.length);
        int columns = ground[0].length;
        Tile[][] blocks = new Tile[ground.length][columns];
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            checkLength(name, BLOCK_LAYER, row, line, columns);
            for (int col = 0; col < columns; col++) {
                char c = line.charAt(col);
                if (c == EMPTY) {
                    continue;
                }
                Tile tile = Tile.fromSymbol(c);
                if (tile == null || tile.isGround()) {
                    throw new IllegalArgumentException("맵 %s의 %s 층 (%d, %d)에 '%c'가 있습니다.%s"
                            .formatted(name, BLOCK_LAYER, col, row, c, hintOutsideBlock(c, tile)));
                }
                blocks[row][col] = tile;
            }
            checkWideTiles(name, row, blocks[row]);
        }
        return blocks;
    }

    private static String hintOutsideBlock(char c, Tile tile) {
        if (isSpawnDigit(c)) {
            return " 스폰 번호는 %s 층에 적으세요.".formatted(SPAWN_LAYER);
        }
        if (tile == null) {
            return " 알 수 없는 글자입니다.";
        }
        return " 바닥 글자는 %s 층에 적고, 블럭 층의 빈 칸은 '%c'로 둡니다.".formatted(FLOOR_LAYER, EMPTY);
    }

    /** 줄임 형식의 타일 층. 바닥 글자는 바닥으로, 나머지는 일반 바닥 위의 블럭으로, 숫자는 "바닥 + 스폰"으로 읽는다. */
    private static void readTileLayer(String name, List<String> rows, Tile[][] ground, Tile[][] blocks,
                                      List<SpawnPoint> spawns) {
        int columns = ground[0].length;
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            if (line.length() != columns) {
                throw new IllegalArgumentException(
                        "맵 %s의 %d번째 줄 길이가 %d입니다. 모든 줄은 %d칸이어야 합니다."
                                .formatted(name, row + 1, line.length(), columns));
            }
            for (int col = 0; col < columns; col++) {
                char c = line.charAt(col);
                ground[row][col] = Tile.FLOOR;
                if (isSpawnDigit(c)) {
                    addSpawn(name, spawns, c - '0', col, row);
                    continue;
                }
                Tile tile = Tile.fromSymbol(c);
                if (tile == null) {
                    throw new IllegalArgumentException(
                            "맵 %s의 (%d, %d)에 알 수 없는 글자 '%c'가 있습니다.".formatted(name, col, row, c));
                }
                if (tile.isGround()) {
                    ground[row][col] = tile;
                } else {
                    blocks[row][col] = tile;
                }
            }
            checkWideTiles(name, row, blocks[row]);
        }
    }

    /** 스폰 층: 바닥과 같은 크기, 숫자 1~8은 스폰 지점, '.'은 없음. 스폰 칸은 지나갈 수 있어야 한다. */
    private static void readSpawnLayer(String name, List<String> rows, String sizeLayer, Tile[][] ground,
                                       Tile[][] blocks, List<SpawnPoint> spawns) {
        checkRowCount(name, SPAWN_LAYER, rows.size(), sizeLayer, ground.length);
        int columns = ground[0].length;
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            checkLength(name, SPAWN_LAYER, row, line, columns);
            for (int col = 0; col < columns; col++) {
                char c = line.charAt(col);
                if (c == EMPTY) {
                    continue;
                }
                if (!isSpawnDigit(c)) {
                    throw new IllegalArgumentException("맵 %s의 %s 층 (%d, %d)에 '%c'가 있습니다. 1~8 또는 '%c'만 쓸 수 있습니다."
                            .formatted(name, SPAWN_LAYER, col, row, c, EMPTY));
                }
                Tile block = blocks[row][col];
                if (block != null && block.isSolid()) {
                    throw new IllegalArgumentException("맵 %s의 스폰 지점 %c (%d, %d)는 '%c' 칸이라 지나갈 수 없습니다."
                            .formatted(name, c, col, row, block.symbol()));
                }
                addSpawn(name, spawns, c - '0', col, row);
            }
        }
    }

    private static void checkRowCount(String name, String layer, int rowCount, String sizeLayer, int expected) {
        if (rowCount != expected) {
            throw new IllegalArgumentException("맵 %s의 %s 층이 %d줄입니다. %s과 같은 %d줄이어야 합니다."
                    .formatted(name, layer, rowCount, sizeLayer, expected));
        }
    }

    private static void checkLength(String name, String layer, int row, String line, int columns) {
        if (line.length() != columns) {
            throw new IllegalArgumentException("맵 %s의 %s 층 %d번째 줄 길이가 %d입니다. %d칸이어야 합니다."
                    .formatted(name, layer, row + 1, line.length(), columns));
        }
    }

    private static void addSpawn(String name, List<SpawnPoint> spawns, int order, int col, int row) {
        if (spawns.stream().anyMatch(spawn -> spawn.order() == order)) {
            throw new IllegalArgumentException(
                    "맵 %s의 (%d, %d)에 스폰 지점 %d이(가) 또 있습니다. 스폰 번호는 한 번씩만 써야 합니다."
                            .formatted(name, col, row, order));
        }
        spawns.add(new SpawnPoint(order, new Point(col, row)));
    }

    private static boolean isSpawnDigit(char c) {
        return c >= '1' && c <= '8';
    }

    /** 여러 칸짜리 타일(미사일 MM 등)이 한 줄에 칸 수만큼 빠짐없이 이어져 있는지 확인한다. null은 빈 칸이다. */
    private static void checkWideTiles(String name, int row, Tile[] line) {
        int col = 0;
        while (col < line.length) {
            Tile tile = line[col];
            int start = col;
            while (col < line.length && line[col] == tile) {
                col++;
            }
            if (tile != null && (col - start) % tile.width() != 0) {
                throw new IllegalArgumentException(
                        "맵 %s의 (%d, %d)부터 '%c'가 %d칸 있습니다. 이 타일은 가로 %d칸씩 붙여 써야 합니다."
                                .formatted(name, start, row, tile.symbol(), col - start, tile.width()));
            }
        }
    }

    private static String themeOf(String id) {
        int slash = id.lastIndexOf('/');
        return slash < 0 ? "" : id.substring(0, slash);
    }

    private record SpawnPoint(int order, Point position) {
    }
}
