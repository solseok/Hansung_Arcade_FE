package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 4단계: 화면에 필요한 리소스를 미리 읽어 등록한다. 파일이 깨져 있으면 창을 띄우기 전에 실패한다.
 * 맵과 맵 테마의 타일 그림(BMP)을 읽는다. 사운드 로더도 준비되면 이 단계에 추가한다.
 */
public final class ResourceStep implements BootStep {

    private static final Logger log = LoggerFactory.getLogger(ResourceStep.class);

    @Override
    public String name() {
        return "리소스 로드";
    }

    @Override
    public void run(GameContext context) {
        String mapId = context.get(GameProperties.class).get("game.start-map");
        TileMap map = MapLoader.loadResource(mapId);
        Tileset tileset = Tileset.forTheme(map.theme());
        context.register(TileMap.class, map);
        context.register(Tileset.class, tileset);
        log.info("시작 맵: {} ({}, {}x{}, 테마 {})", map.name(), mapId, map.columns(), map.rows(), map.theme());
        warnMissingArt(map, tileset);
    }

    /** 그림이 빠져 있어도 게임은 띄우고(임시 표시), 무엇이 빠졌는지 로그로 알린다. */
    private static void warnMissingArt(TileMap map, Tileset tileset) {
        if (!tileset.hasFloor()) {
            log.warn("테마 {}에 바닥 그림(tile_NN_이름.bmp)이 없어 단색 바닥으로 그립니다.", map.theme());
        }
        for (Tile tile : tileset.missingArt(map)) {
            log.warn("테마 {}에 '{}'({}) 그림이 없어 임시 상자로 그립니다.", map.theme(), tile.symbol(), tile);
        }
    }
}
