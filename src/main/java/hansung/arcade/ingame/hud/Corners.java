package hansung.arcade.ingame.hud;

/**
 * 네 모서리의 둥글기 반지름(픽셀). 왼쪽 위부터 시계 방향.
 */
public record Corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {

    public static final Corners SQUARE = all(0);

    public Corners {
        if (topLeft < 0 || topRight < 0 || bottomRight < 0 || bottomLeft < 0) {
            throw new IllegalArgumentException("반지름은 음수일 수 없습니다.");
        }
    }

    public static Corners all(int radius) {
        return new Corners(radius, radius, radius, radius);
    }

    /** 위쪽 두 모서리만 둥글다 */
    public static Corners top(int radius) {
        return new Corners(radius, radius, 0, 0);
    }

    /**
     * 테두리 두께만큼 안쪽 도형의 반지름. 바깥 모서리와 중심이 같아서 테두리 굵기가 모서리에서도 일정하다.
     */
    public Corners shrink(int amount) {
        return new Corners(Math.max(0, topLeft - amount), Math.max(0, topRight - amount),
                Math.max(0, bottomRight - amount), Math.max(0, bottomLeft - amount));
    }
}
