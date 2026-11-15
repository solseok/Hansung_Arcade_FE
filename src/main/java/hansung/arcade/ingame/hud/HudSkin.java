package hansung.arcade.ingame.hud;

import java.awt.Font;
import java.util.Objects;

/**
 * HUD에 들어가는 글자 그리는 방법과 그림 묶음. 글자를 디자인 이미지로 바꿀 때는 해당 칸의
 * {@link TextArt}만 이미지로 그리는 구현으로 바꿔 새로 만들면 된다 (세로 정렬은 그림의 실제 픽셀 기준으로 맞춘다).
 * 예) TIMER 글씨만 이미지로:
 *
 * <pre>
 * HudSkin skin = HudSkin.load(fonts);
 * TextArt timerImage = ...; // TextArt를 이미지로 구현한 것
 * skin = new HudSkin(timerImage, skin.digits(), skin.itemLabel(), ...);
 * </pre>
 *
 * @param timerLabel   "TIMER" 글씨 (Michroma)
 * @param digits       타이머 MM:ss 와 맵 번호 숫자
 * @param itemLabel    아이템 상자 위 "ITEM" 글씨 (Michroma)
 * @param itemBarLabel 아래 바의 "내 아이템" (HY견고딕)
 * @param keyNumber    아이템 칸 단축키 번호
 * @param exitLabel    "나가기" (HY윤고딕)
 * @param nickname     플레이어 목록 닉네임
 * @param chatText     맵 위 채팅 글자 (돋움체, 흰 글자에 검은 테두리). 입력칸·다른 플레이어의 채팅·말풍선에 쓴다
 * @param myChatText   채팅 목록에서 내가 친 줄 (같은 글꼴, #DFDB86)
 * @param portraits    플레이어 목록에 넣을 캐릭터 얼굴. 고정색별로 칠한 얼굴도 여기서 얻는다
 * @param itemSlotLabel 2인 모드 아이템 칸 위 "1P"·"2P" (Michroma)
 * @param itemKey      아이템 칸 안에 흐리게 적는 아이템 키 이름
 * @param itemCount    아이템 칸 오른쪽 아래 액티브 아이템 개수 "×02" (Michroma, 노란 글자에 테두리)
 */
public record HudSkin(
        TextArt timerLabel,
        TextArt digits,
        TextArt itemLabel,
        TextArt itemBarLabel,
        TextArt keyNumber,
        TextArt exitLabel,
        TextArt nickname,
        OutlineText chatText,
        OutlineText myChatText,
        Portraits portraits,
        TextArt itemSlotLabel,
        TextArt itemKey,
        TextArt itemCount) {

    public HudSkin {
        Objects.requireNonNull(portraits);
    }

    /** 디자인 글꼴로 만든 기본 모양과 캐릭터 얼굴 그림 */
    public static HudSkin load(HudFonts fonts) {
        Font timerLabelFont = HudFonts.fitInkWidth(fonts.michroma(10), "TIMER", HudLayout.TIMER_LABEL_WIDTH - 1);
        Font digitFont = HudFonts.fitInkHeight(fonts.michroma(12), "0", 11);
        Font itemLabelFont = HudFonts.fitInkHeight(fonts.michroma(10), "ITEM", HudLayout.ITEM_LABEL_HEIGHT);
        Font itemBarFont = HudFonts.fitInkHeight(fonts.gyeonGothic(12), "내 아이템", 12);
        Font keyFont = HudFonts.fitInkHeight(fonts.gyeonGothic(8), "0", 7);
        Font exitFont = HudFonts.fitInkHeight(fonts.yunGothic(18), "나가기", HudLayout.EXIT_TEXT_HEIGHT);
        Font nicknameFont = HudFonts.fitInkHeight(fonts.gyeonGothic(12), "가", HudLayout.NICKNAME_HEIGHT);
        Font chatFont = fonts.dotumChe(HudLayout.CHAT_TEXT_HEIGHT);
        Font itemSlotFont = HudFonts.fitInkHeight(fonts.michroma(8), "1P", HudLayout.ITEM_SLOT_LABEL_HEIGHT);
        Font itemKeyFont = HudFonts.fitInkHeight(fonts.gyeonGothic(8), "L", HudLayout.ITEM_KEY_HEIGHT);
        Font itemCountFont = HudFonts.fitInkHeight(fonts.michroma(10), "0", HudLayout.ITEM_COUNT_HEIGHT)
                .deriveFont(Font.BOLD);

        return new HudSkin(
                new FontTextArt(timerLabelFont, HudPalette.DISPLAY_LABEL),
                new FontTextArt(digitFont, HudPalette.DISPLAY_DIGIT),
                new FontTextArt(itemLabelFont, HudPalette.ITEM_LABEL),
                new FontTextArt(itemBarFont, HudPalette.ITEM_BAR_LABEL),
                new FontTextArt(keyFont, HudPalette.KEY_DIGIT),
                new FontTextArt(exitFont, HudPalette.EXIT_TEXT, HudPalette.EXIT_BUTTON_EDGE),
                new FontTextArt(nicknameFont, HudPalette.NICKNAME, HudPalette.edgeOf(HudPalette.PLAYER_SLOT)),
                new OutlineText(chatFont, HudLayout.CHAT_TEXT_HEIGHT, HudPalette.CHAT_TEXT, HudPalette.CHAT_OUTLINE),
                new OutlineText(chatFont, HudLayout.CHAT_TEXT_HEIGHT, HudPalette.CHAT_TEXT_MINE, HudPalette.CHAT_OUTLINE),
                Portraits.load(),
                new FontTextArt(itemSlotFont, HudPalette.ITEM_SLOT_LABEL, HudPalette.edgeOf(HudPalette.ITEM_INNER)),
                new FontTextArt(itemKeyFont, HudPalette.ITEM_KEY),
                FontTextArt.outlined(itemCountFont, HudPalette.ITEM_COUNT, HudPalette.ITEM_COUNT_OUTLINE));
    }
}
