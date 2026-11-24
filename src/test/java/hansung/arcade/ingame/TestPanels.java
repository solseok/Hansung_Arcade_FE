package hansung.arcade.ingame;

import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.TestRenderers;
import hansung.arcade.ingame.render.Tileset;

/** 테스트에서 인게임 화면을 만든다. 넘기지 않은 그림은 {@link TestRenderers}가 읽어 둔 그림을 쓴다 */
final class TestPanels {

    private TestPanels() {
    }

    static InGamePanel panel(GameWorld world, Tileset tileset, HudSkin skin) {
        return panel(world, tileset, skin, TestRenderers.characters());
    }

    static InGamePanel panel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters) {
        return new InGamePanel(world, tileset, skin, characters, TestRenderers.balloons(), TestRenderers.items(),
                TestRenderers.airplanes());
    }
}
