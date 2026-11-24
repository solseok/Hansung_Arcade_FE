package hansung.arcade.ingame.render;

/**
 * 테스트에서 쓰는 그림과 맵 렌더러. 운영 코드의 {@link WorldRenderer}·인게임 화면은 그림을 모두 받기만 하므로,
 * 그림을 따로 준비하지 않는 테스트는 여기서 처음 쓸 때 한 번 읽어 둔 그림을 같이 쓴다.
 */
public final class TestRenderers {

    private TestRenderers() {
    }

    public static CharacterSprites characters() {
        return Loaded.CHARACTERS;
    }

    public static BalloonSprites balloons() {
        return Loaded.BALLOONS;
    }

    public static ItemSprites items() {
        return Loaded.ITEMS;
    }

    public static AirplaneSprites airplanes() {
        return Loaded.AIRPLANES;
    }

    /** 읽어 둔 캐릭터·물풍선·아이템 그림으로 tileset 맵을 그리는 렌더러 */
    public static WorldRenderer worldRenderer(Tileset tileset) {
        return worldRenderer(tileset, characters());
    }

    /** characters 그림과 읽어 둔 물풍선·아이템 그림으로 tileset 맵을 그리는 렌더러 */
    public static WorldRenderer worldRenderer(Tileset tileset, CharacterSprites characters) {
        return new WorldRenderer(tileset, characters, balloons(), items());
    }

    /** 처음 쓸 때 한 번만 읽는다 */
    private static final class Loaded {
        static final CharacterSprites CHARACTERS = CharacterSprites.load();
        static final BalloonSprites BALLOONS = BalloonSprites.load();
        static final ItemSprites ITEMS = ItemSprites.load();
        static final AirplaneSprites AIRPLANES = AirplaneSprites.load();
    }
}
