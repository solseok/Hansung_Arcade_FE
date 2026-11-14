package hansung.arcade.ingame.render;

import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한 테마의 타일 그림 묶음. resources/image/{테마}/ 의 BMP를 파일 이름 규칙으로 찾아 읽는다.
 *
 * <p>모든 테마가 같은 파일 이름을 쓰고, 테마 폴더에는 그 테마에 필요한 파일만 두면 된다.
 * 한 파일에 40px 폭(여러 칸짜리 타일은 그 배수) 프레임을 가로로 이어 붙인다.
 * 블록 등 물체 그림은 둘레의 투명한 여백을 잘라 칸 너비를 꽉 채우게 키워 쓰므로({@link TileSprite#fitted}),
 * 그림 안에 여백이 조금 남아 있어도 이웃 블록과 틈 없이 붙는다.</p>
 *
 * <p>바닥은 tile_NN_이름.bmp (예: tile_01_grass, tile_02_road) 로 번호만 맞추면 이름은 자유다.
 * 지금은 가장 앞 번호(보통 tile_01)를 맵 전체 바닥으로 깐다. 앞 두 장을 밝은 장·어두운 장으로 나눠
 * 체스판처럼 번갈아 깔고, (0, 0) 칸이 밝은 장이다. 어느 장이 밝은지는 읽을 때 평균 밝기로 정하므로
 * 파일 안 순서는 상관없다. 한 장뿐이면 모든 칸에 같은 장을 깐다.
 * 바닥 파일이 하나도 없으면 렌더러가 단색 바닥을 깐다.
 * 두 번째 이후 바닥 파일은 아래 표처럼 기호로 칸마다 지정한다.</p>
 *
 * <pre>
 * tile_02_*       _ | =    [일반 길, 세로 점선 차선, 횡단보도] 순서
 * 01_block_normal B O      [B, O] 순서
 * 02_block_lr     -        여러 장이면 칸마다 섞어 쓴다 (이하 "변형")
 * 03_block_push   P        변형
 * 04_block_multi  c b a    [남은 3회, 2회, 1회] 순서. 장이 모자라면 마지막 장
 * 05_block_solid  #        변형
 * 06_block_oneway &lt; &gt; ^ v  [왼쪽, 오른쪽, 위, 아래] 순서. 가시가 난 쪽이 화살표 방향
 * 07_wire         W        변형
 * 08_floor_spike  !        변형
 * 09_bush         T        변형
 * 10_tree         Y        변형
 * 11_house        H J K    [H, J, K] 순서
 * 12_missile      M        변형. 2칸짜리라 프레임 폭 80px
 * </pre>
 */
public final class Tileset {

    private static final int T = TileMap.TILE_SIZE;
    private static final Pattern FLOOR_FILE = Pattern.compile("tile_(\\d+)_.+\\.bmp");
    private static final Map<Tile, Art> ART = new EnumMap<>(Tile.class);

    static {
        ART.put(Tile.ROAD, Art.frame(floorKey(2), 0));
        ART.put(Tile.ROAD_LANE, Art.frame(floorKey(2), 1));
        ART.put(Tile.CROSSWALK, Art.frame(floorKey(2), 2));
        ART.put(Tile.BLOCK, Art.frame("01_block_normal", 0));
        ART.put(Tile.BLOCK_2, Art.frame("01_block_normal", 1));
        ART.put(Tile.SIDE_BLOCK, Art.variants("02_block_lr"));
        ART.put(Tile.PUSH_BOX, Art.variants("03_block_push"));
        ART.put(Tile.HARD_BLOCK_3, Art.damage("04_block_multi", 0));
        ART.put(Tile.HARD_BLOCK_2, Art.damage("04_block_multi", 1));
        ART.put(Tile.HARD_BLOCK_1, Art.damage("04_block_multi", 2));
        ART.put(Tile.STATUE, Art.variants("05_block_solid"));
        ART.put(Tile.ONE_WAY_LEFT, Art.frame("06_block_oneway", 0));
        ART.put(Tile.ONE_WAY_RIGHT, Art.frame("06_block_oneway", 1));
        ART.put(Tile.ONE_WAY_UP, Art.frame("06_block_oneway", 2));
        ART.put(Tile.ONE_WAY_DOWN, Art.frame("06_block_oneway", 3));
        ART.put(Tile.WIRE, Art.variants("07_wire"));
        ART.put(Tile.SPIKE, Art.variants("08_floor_spike"));
        ART.put(Tile.BUSH, Art.variants("09_bush"));
        ART.put(Tile.TREE, Art.variants("10_tree"));
        ART.put(Tile.HOUSE_1, Art.frame("11_house", 0));
        ART.put(Tile.HOUSE_2, Art.frame("11_house", 1));
        ART.put(Tile.HOUSE_3, Art.frame("11_house", 2));
        ART.put(Tile.MISSILE, Art.variants("12_missile"));
    }

    /** 바닥 번호(tile_NN) → 그 파일의 체크무늬 한 쌍. 번호 순으로 정렬되어 있다. */
    private final TreeMap<Integer, Checker> floors;
    /** 파일 이름(바닥 파일은 tile_NN) → 그 파일의 프레임들. 테마 폴더에 있는 파일만 들어 있다. */
    private final Map<String, List<TileSprite>> sheets;

    private Tileset(TreeMap<Integer, Checker> floors, Map<String, List<TileSprite>> sheets) {
        this.floors = floors;
        this.sheets = sheets;
    }

    /** resources/image/{theme}/ 에 있는 그림을 읽는다. 그림이 하나도 없으면 테마 이름이 틀린 것으로 보고 실패한다. */
    public static Tileset forTheme(String theme) {
        String dir = "/image/" + theme + "/";
        TreeMap<Integer, Checker> floors = new TreeMap<>();
        Map<String, List<TileSprite>> sheets = new HashMap<>();
        for (String name : ResourceFiles.list(dir)) {
            Matcher m = FLOOR_FILE.matcher(name);
            if (m.matches()) {
                int number = Integer.parseInt(m.group(1));
                List<BufferedImage> frames = SpriteSheet.find(dir + name).orElseThrow().frames(T);
                if (floors.putIfAbsent(number, Checker.of(frames)) != null) {
                    throw new IllegalArgumentException("%s 에 바닥 번호 %s 파일이 둘 이상 있습니다."
                            .formatted(dir, m.group(1)));
                }
                sheets.put(floorKey(number), frames.stream().map(frame -> TileSprite.standing(frame, 1)).toList());
            }
        }


        ART.forEach((tile, art) -> {
            if (sheets.containsKey(art.file())) {
                return;
            }
            SpriteSheet.find(dir + art.file() + ".bmp").ifPresent(sheet -> sheets.put(art.file(),
                    sheet.frames(T * tile.width()).stream()
                            .map(frame -> TileSprite.fitted(frame, tile.width()))
                            .toList()));
        });

        if (floors.isEmpty() && sheets.isEmpty()) {
            throw new IllegalArgumentException("테마 '%s'의 그림이 없습니다. resources%s 폴더를 확인하세요."
                    .formatted(theme, dir));
        }
        return new Tileset(floors, sheets);
    }

    /** 기본 바닥(가장 앞 번호 tile_NN) 그림. 체스판처럼 (0, 0)부터 밝은 장·어두운 장을 번갈아 준다. 바닥 파일이 없으면 null. */
    public BufferedImage floor(int col, int row) {
        return floors.isEmpty() ? null : floors.firstEntry().getValue().at(col, row);
    }

    /** (col, row) 칸에 그릴 타일 그림. 바닥이거나 그림 파일·프레임이 없으면 null. */
    public TileSprite sprite(Tile tile, int col, int row) {
        Art art = ART.get(tile);
        List<TileSprite> frames = art == null ? null : sheets.get(art.file());
        if (frames == null) {
            return null;
        }
        return switch (art.pick()) {
            case VARIANT -> frames.get(variantIndex(col, row, frames.size()));
            case EXACT -> art.frame() < frames.size() ? frames.get(art.frame()) : null;
            case UP_TO -> frames.get(Math.min(art.frame(), frames.size() - 1));
        };
    }

    /** 맵에 쓰였는데 이 테마에 그림이 없는 타일들. 렌더러는 이 타일을 임시 표시로 그린다. */
    public Set<Tile> missingArt(TileMap map) {
        Set<Tile> missing = EnumSet.noneOf(Tile.class);
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.columns(); col++) {
                Tile tile = map.tileAt(col, row);
                if (tile != Tile.FLOOR && sprite(tile, col, row) == null) {
                    missing.add(tile);
                }
            }
        }
        return missing;
    }

    public boolean hasFloor() {
        return !floors.isEmpty();
    }

    /** 바닥 파일을 이름 뒷부분과 상관없이 번호로 가리키는 키. tile_02_road → tile_02 */
    private static String floorKey(int number) {
        return "tile_%02d".formatted(number);
    }

    /** 칸마다 고정된, 하지만 규칙적이지 않은 변형 번호. 같은 맵은 언제 그려도 같은 모습이다. */
    static int variantIndex(int col, int row, int count) {
        if (count == 1) {
            return 0;
        }
        // 그냥 곱해서 나머지를 내면 2장일 때 체크무늬가 되므로 비트를 한 번 섞는다.
        int h = col * 73_856_093 ^ row * 19_349_663;
        h ^= h >>> 16;
        h *= 0x45D9F3B;
        h ^= h >>> 16;
        return Math.floorMod(h, count);
    }

    /** 체스판 바닥 한 쌍. (col + row)가 짝수인 칸에 light, 홀수인 칸에 dark. */
    private record Checker(BufferedImage light, BufferedImage dark) {

        /** 앞 두 장 중 평균 밝기가 높은 장을 light 로 둔다. 셋째 장부터는 쓰지 않는다. */
        static Checker of(List<BufferedImage> frames) {
            BufferedImage first = frames.get(0);
            if (frames.size() == 1) {
                return new Checker(first, first);
            }
            BufferedImage second = frames.get(1);
            return brightness(first) >= brightness(second) ? new Checker(first, second) : new Checker(second, first);
        }

        BufferedImage at(int col, int row) {
            return (col + row) % 2 == 0 ? light : dark;
        }

        private static double brightness(BufferedImage image) {
            double sum = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    sum += 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
                }
            }
            return sum / (image.getWidth() * image.getHeight());
        }
    }

    private enum Pick {
        /** 여러 장 중 칸 위치로 하나 고른다 */
        VARIANT,
        /** 정해진 장. 없으면 그림 없음 */
        EXACT,
        /** 정해진 장. 모자라면 마지막 장 */
        UP_TO
    }

    private record Art(String file, Pick pick, int frame) {
        static Art variants(String file) {
            return new Art(file, Pick.VARIANT, 0);
        }

        static Art frame(String file, int frame) {
            return new Art(file, Pick.EXACT, frame);
        }

        static Art damage(String file, int frame) {
            return new Art(file, Pick.UP_TO, frame);
        }
    }
}
