package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;
import hansung.arcade.ingame.entity.Mascot;

import java.util.List;

/**
 * 캐릭터 그림에서 고정색으로 칠해진 부분을 고르는 기준. 그림이 8비트 팔레트 BMP라 쓰인 색이 몇십 개뿐이어서,
 * 캐릭터·포즈별 색 목록을 OKLCH로 늘어놓고 고정색 부분의 색만 들어가는 밝기·채도·색상각 범위를 정했다.
 *
 * <p>캐릭터 몸 외곽선, 피부(흰 얼굴·배), 눈, 볏·부리·발, 물풍선 물빛, 죽을 때 별·번개 같은 이펙트는 범위 밖이다.
 * 다만 고정색 부분을 따로 둘러싼 같은 계열의 어두운 색(빨간 가방의 검붉은 테두리 등)은 그 부분의 그림자로 보고 넣었다.
 * 물풍선에 갇힌 그림(bubble, needle 앞 장)은 캐릭터가 물빛과 섞여 색이 달라지므로 그 색들을 따로 넣었다.</p>
 *
 * @param anchorRgb 고정색 부분을 대표하는 원래 색. 다시 칠하면 이 색이 고른 색과 똑같아지고, 나머지 색은 이 색과의
 *                  밝기·채도 차이를 지킨다. 정면 그림의 고정색 부분에서 가장 넓게 칠해진 색인데, 꼭꼬만은 나비넥타이 테두리
 *                  #102468 보다 나비넥타이·공의 바탕색 #4078E0 을 골랐다
 * @param ranges    고정색 부분의 색 범위들
 */
record FixedColorRegion(int anchorRgb, List<OklchRange> ranges) {

    FixedColorRegion(int anchorRgb, OklchRange... ranges) {
        this(anchorRgb, List.of(ranges));
    }

    Oklch anchor() {
        return Oklch.fromRgb(anchorRgb);
    }

    static FixedColorRegion of(Mascot mascot) {
        return switch (mascot) {
            // 남색 후드(모자·몸)와 등딱지. 등딱지 윗부분이 후드와 같은 색이라 등딱지도 같이 칠한다.
            // 파란 볼터치 #9CBCEE, 몸 외곽선 #101838·#0C162C(밝기 0.22 아래), 얼굴 외곽선 #46507A(채도 0.070)는 빠진다.
            case SANGSANGBUGI -> new FixedColorRegion(0x324E94,
                    // 후드 #4E6EB8 #324E94 #243976 #1A2858
                    new OklchRange(0.25, 0.60, 0.085, 0.13, 262, 272),
                    // 후드 꼭대기 하이라이트 #A0BAEA
                    new OklchRange(0.75, 0.82, 0.07, 0.08, 261, 265),
                    // 조금 더 청록빛인 등딱지·소매 끝 #142846 #1E385E #2A4C7A #406698
                    new OklchRange(0.26, 0.52, 0.055, 0.095, 255, 259),
                    // 등딱지 하이라이트 #80A2CA
                    new OklchRange(0.69, 0.72, 0.065, 0.075, 252, 254),
                    // 물풍선 속 후드 #4373B3 #5789CC #3A649E #335889. 물풍선 테두리 #0E4A9C 와 #1C7CD4 는 채도가 0.14 넘는다
                    new OklchRange(0.45, 0.70, 0.09, 0.13, 253, 258),
                    // 물풍선 속 후드 하이라이트 #90BEEF. 물풍선 속 볼터치 #8EC0F2 는 h 249.0
                    new OklchRange(0.78, 0.80, 0.08, 0.09, 249.8, 251.5),
                    // 물풍선 속 후드 아랫자락 #294B6A #2C4D72 #2E587C #35638D #3E71A0. 물풍선 속 몸 외곽선 #33506F 는 채도 0.063
                    new OklchRange(0.40, 0.54, 0.064, 0.095, 245, 253),
                    // 옅어진 물풍선 속 후드 #4884AC #4B8BB6 #4985B0 #5399C9 #4E91BF #4D8BBC #5192C8 #569AD3 #61A6E1.
                    // 사이에 낀 #5E9AC9(h 243.5, 얼굴)와 채도가 0.087 아래인 #4D87AF·#8AC6EF(몸 외곽선·얼굴)는 빠진다.
                    // 볼터치 #7EC3F5 와 후드 하이라이트 #80C3F4 는 색이 거의 같아서 둘 다 물빛 그대로 둔다(밝기 0.79)
                    new OklchRange(0.585, 0.75, 0.087, 0.115, 239.5, 243),
                    new OklchRange(0.60, 0.75, 0.095, 0.115, 243.8, 247));
            // 빨간 가방·멜빵·가슴 글자. 분홍 귀·꼬리(채도 0.15 아래)는 그대로 둔다.
            case HANSUNGNYANGI -> new FixedColorRegion(0xE43A3E,
                    // #941824 #C0242E #E43A3E #F86C66
                    new OklchRange(0.40, 0.75, 0.155, 0.25, 15, 30),
                    // 옆모습 가방 하이라이트 #FFAAA0. 귀 안쪽 #FCA496 은 밝기 0.80, #FFB6B6 은 채도 0.085
                    new OklchRange(0.81, 0.83, 0.095, 0.11, 26, 28),
                    // 가방·멜빵 테두리 #640E1A. 귀·꼬리 테두리 #802C32 는 밝기 0.42 다
                    new OklchRange(0.30, 0.35, 0.11, 0.13, 17, 23),
                    // 물풍선 속 글자·멜빵 #C06576 #A7556B
                    new OklchRange(0.50, 0.65, 0.10, 0.13, 0, 12),
                    // 옅어진 물풍선 속 글자·멜빵 #9A92B3 #8C8AAC
                    new OklchRange(0.62, 0.70, 0.04, 0.06, 285, 300));
            // 파란 나비넥타이·손에 든 공·양말 줄무늬. 빨간 볏·주황 부리는 그대로 둔다.
            case KKOKKO -> new FixedColorRegion(0x4078E0,
                    // #C0D8FF #76A6F6 #4078E0 #2C58BC #1E3C94 #102468 #324E94 #243976. 물풍선 테두리 #0E4A9C 는 h 258.3
                    new OklchRange(0.28, 0.90, 0.055, 0.18, 259.5, 268.5),
                    // 물풍선 속 #4D90E8 #3F7ACF #3566B3 #2C5594 #73B0F7 #4373B3 #3A649E
                    new OklchRange(0.44, 0.80, 0.10, 0.155, 253, 260),
                    // 옅어진 물풍선 속 #70BBF8 #5BAAF0 #549DE2 #4E93D3 #498AC3 #569AD3 #5192C8
                    new OklchRange(0.60, 0.78, 0.10, 0.135, 244.5, 250));
            // 갈색 깃털. 노란 목도리는 죽을 때 머리 위 별과 같은 색이라 고르지 않았다. 부리·발·볏도 그대로 둔다.
            // 옅어진 물풍선(bubble_2) 속 깃털은 원래 그림에서도 갈색이 다 빠진 물빛 회색(h 210~235)이라 그대로 둔다.
            // 물빛에서 나온 채도를 고른 색의 채도로 옮기면 bubble_1 보다 오히려 진해진다.
            case KKUKKU -> new FixedColorRegion(0xAC683C,
                    // 어두운 깃털 #66361E #884C2C. 몸 외곽선 #401E10(밝기 0.28), 부리 그림자 #76300C(채도 0.108)는 빠진다
                    new OklchRange(0.35, 0.50, 0.07, 0.10, 43, 50),
                    // 밝은 깃털 #AC683C #CE8E5C #EEBE90 과 물풍선 속 #CB9E75. 별 테두리 #7C4808 은 밝기 0.45 다
                    new OklchRange(0.55, 0.85, 0.075, 0.106, 50, 66),
                    // 물풍선 속 깃털 #80716A #998575 #735E53 #B19F8B
                    new OklchRange(0.48, 0.75, 0.015, 0.04, 40, 72),
                    // 물풍선 속 머리 하이라이트 #C7C1B0. 물풍선 속 목도리(채도 0.04 넘음)는 빠진다
                    new OklchRange(0.80, 0.82, 0.02, 0.03, 85, 95));
            // 초록 몸. 크림색 배와 눈은 그대로 둔다.
            case SANGZZI -> new FixedColorRegion(0x70BE40,
                    // #D6F4A4 #9CDC60 #70BE40 #4E9430 #306C28. 몸 외곽선 #164022 는 빠진다
                    new OklchRange(0.40, 0.95, 0.10, 0.18, 124, 142),
                    // 물풍선 속 몸 #8ED68E #6FC178 #57A46D #428867
                    new OklchRange(0.55, 0.85, 0.085, 0.14, 143, 162),
                    // 물풍선 속 머리 하이라이트 #B6E7BE
                    new OklchRange(0.87, 0.90, 0.07, 0.08, 147, 152),
                    // 옅어진 물풍선 속 몸 #7ED0BF #6EC4B3 #61B4AD #94D9D9 #55A5AA #4B94A8
                    new OklchRange(0.60, 0.85, 0.06, 0.09, 175, 219));
        };
    }

    /** rgb(0xRRGGBB)가 고정색 부분의 색인지 */
    boolean contains(int rgb) {
        Oklch color = Oklch.fromRgb(rgb);
        for (OklchRange range : ranges) {
            if (range.contains(color)) {
                return true;
            }
        }
        return false;
    }
}
