package hansung.arcade.ingame.map;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 텍스트 맵 파일을 읽어 {@link TileMap}을 만든다.
 *
 * <pre>
 * name=맵 이름        메타데이터 (key=value)
 * 1.BB#..             타일 한 줄. 글자는 {@link Tile} 참고, 숫자 1~8은 스폰 지점(바닥).
 *                     스폰 번호는 1부터 빠짐없이, 번호마다 한 번만 쓴다
 * </pre>
 *
 * <p>맵 id는 resources/maps 아래 경로다. 앞쪽 폴더 이름이 테마가 된다 (camp/camp08 → camp).</p>
 */
public final class MapLoader {

    private static final String MAP_DIR = "/maps/";

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
        List<String> rows = new ArrayList<>();
        for (String raw : lines) {
            // 메모장 등이 파일 맨 앞에 붙이는 BOM(U+FEFF)은 strip()으로 지워지지 않는다.
            String line = raw.replace("\uFEFF", "").strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("name=")) {
                name = line.substring("name=".length()).strip();
                continue;
            }
            rows.add(line);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("맵에 타일이 없습니다: " + name);
        }

        int columns = rows.get(0).length();
        Tile[][] tiles = new Tile[rows.size()][columns];
        List<SpawnPoint> spawns = new ArrayList<>();

        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            if (line.length() != columns) {
                throw new IllegalArgumentException(
                        "맵 %s의 %d번째 줄 길이가 %d입니다. 모든 줄은 %d칸이어야 합니다."
                                .formatted(name, row + 1, line.length(), columns));
            }
            for (int col = 0; col < columns; col++) {
                char c = line.charAt(col);
                if (c >= '1' && c <= '8') {
                    int order = c - '0';
                    if (spawns.stream().anyMatch(spawn -> spawn.order() == order)) {
                        throw new IllegalArgumentException(
                                "맵 %s의 (%d, %d)에 스폰 지점 %d이(가) 또 있습니다. 스폰 번호는 한 번씩만 써야 합니다."
                                        .formatted(name, col, row, order));
                    }
                    spawns.add(new SpawnPoint(order, new Point(col, row)));
                    tiles[row][col] = Tile.FLOOR;
                    continue;
                }
                Tile tile = Tile.fromSymbol(c);
                if (tile == null) {
                    throw new IllegalArgumentException(
                            "맵 %s의 (%d, %d)에 알 수 없는 글자 '%c'가 있습니다.".formatted(name, col, row, c));
                }
                tiles[row][col] = tile;
            }
            checkWideTiles(name, row, tiles[row]);
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
        return new TileMap(name, themeOf(id), tiles, spawns.stream().map(SpawnPoint::position).toList());
    }

    /** 여러 칸짜리 타일(미사일 MM 등)이 한 줄에 칸 수만큼 빠짐없이 이어져 있는지 확인한다. */
    private static void checkWideTiles(String name, int row, Tile[] line) {
        int col = 0;
        while (col < line.length) {
            Tile tile = line[col];
            int start = col;
            while (col < line.length && line[col] == tile) {
                col++;
            }
            if ((col - start) % tile.width() != 0) {
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
