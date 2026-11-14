package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceStepTest {

    @Test
    void 시작_화면에_필요한_맵과_그림을_읽어_등록한다() {
        GameContext context = new GameContext(ResourceStepTest.class, List.of());
        context.register(GameProperties.class,
                GameProperties.of(Map.of("game.start-map", "camp/camp08"), List.of()));

        new ResourceStep().run(context);

        assertEquals("camp", context.get(TileMap.class).theme());
        assertTrue(context.contains(Tileset.class));
        // 캐릭터 그림은 화면을 조립하는 단계가 아니라 이 단계에서 읽는다
        assertTrue(context.contains(CharacterSprites.class));
        assertTrue(context.contains(HudSkin.class));
    }
}
