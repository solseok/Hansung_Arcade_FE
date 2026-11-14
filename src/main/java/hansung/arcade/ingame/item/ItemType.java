package hansung.arcade.ingame.item;

/**
 * 맵에 떨어지는 아이템 종류. 그림은 resources/item/{passive|active}/{id}.bmp (38 x 38, 마젠타 배경)이다.
 *
 * <p>먹는 순간 능력치가 바로 바뀌는 패시브 아이템과, 들고 있다가 아이템 키로 쓰는 액티브 아이템이 있다.
 * 먹었을 때 하는 일은 {@link ItemEffects}가 정한다.</p>
 */
public enum ItemType {
    /** 물풍선 개수 +1 */
    BUBBLE("bubble", Kind.PASSIVE),
    /** 물줄기 길이 +1 */
    FLUID("fluid", Kind.PASSIVE),
    /** 물줄기 길이를 캐릭터 최댓값으로 */
    ULTRA("ultra", Kind.PASSIVE),
    /** 이동속도 +1 */
    ROLLER_SHOE("roller_shoe", Kind.PASSIVE),
    /** 물줄기 길이를 최댓값으로 하고 물풍선을 밀 수 있게 */
    RED_DEVIL("red_devil", Kind.PASSIVE),
    /** 물풍선을 던질 수 있게 */
    GLOVE("glove", Kind.PASSIVE),
    /** 물풍선을 밀 수 있게 */
    SHOE("shoe", Kind.PASSIVE),
    /** 물줄기를 한 번 막아 주는 탱크 */
    TANK("tank", Kind.PASSIVE),
    /** 갇혔을 때 스스로 빠져나오는 바늘 */
    NEEDLE("needle", Kind.ACTIVE),
    /** 잠깐 물줄기를 막는 보호막 */
    SHIELD("shield", Kind.ACTIVE),
    /** 앞 칸을 뛰어넘는 스프링 신발 */
    SPRING_SHOE("spring_shoe", Kind.ACTIVE);

    /** 아이템 그림 한 장의 가로·세로 크기 */
    public static final int IMAGE_SIZE = 38;

    private final String id;
    private final Kind kind;

    ItemType(String id, Kind kind) {
        this.id = id;
        this.kind = kind;
    }

    /** 그림 파일 이름 앞부분 (예: roller_shoe) */
    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** 들고 있다가 아이템 키로 쓰는 아이템인지 */
    public boolean isActive() {
        return kind == Kind.ACTIVE;
    }

    /** 그림 파일의 클래스패스 경로 (예: /item/passive/bubble.bmp) */
    public String imagePath() {
        return "/item/" + kind.directory + "/" + id + ".bmp";
    }

    public enum Kind {
        /** 먹는 순간 효과가 나는 아이템 */
        PASSIVE("passive"),
        /** 들고 있다가 아이템 키로 쓰는 아이템 */
        ACTIVE("active");

        private final String directory;

        Kind(String directory) {
            this.directory = directory;
        }
    }
}
