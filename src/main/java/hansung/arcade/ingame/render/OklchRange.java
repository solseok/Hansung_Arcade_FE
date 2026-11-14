package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;

/**
 * OKLCH 밝기·채도·색상각이 모두 범위 안인 색들. 경계값도 범위 안이다.
 * 색상각은 fromH 에서 시계 방향으로 toH 까지이며, fromH가 toH보다 크면 0도를 넘어 이어진다(예: 340~10).
 */
record OklchRange(double minL, double maxL, double minC, double maxC, double fromH, double toH) {

    boolean contains(Oklch color) {
        return color.l() >= minL && color.l() <= maxL
                && color.c() >= minC && color.c() <= maxC
                && hueInRange(color.h());
    }

    private boolean hueInRange(double h) {
        return fromH <= toH ? h >= fromH && h <= toH : h >= fromH || h <= toH;
    }
}
