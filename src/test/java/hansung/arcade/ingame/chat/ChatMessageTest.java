package hansung.arcade.ingame.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMessageTest {

    @Test
    void 영문과_숫자는_1바이트_한글은_2바이트로_센다() {
        assertEquals(0, ChatMessage.byteLength(""));
        assertEquals(5, ChatMessage.byteLength("Hi 12"));
        assertEquals(10, ChatMessage.byteLength("안녕하세요"));
        assertEquals(9, ChatMessage.byteLength("ㅋㅋ gg~!"));
    }

    @Test
    void 바이트_제한을_넘는_뒷부분은_잘린다() {
        assertEquals("abc", ChatMessage.truncateToBytes("abc", 64));
        assertEquals("abcd", ChatMessage.truncateToBytes("abcdef", 4));
        assertEquals("가나", ChatMessage.truncateToBytes("가나다", 4));
        // 한글이 반만 들어갈 자리가 남으면 그 글자는 넣지 않는다
        assertEquals("가나", ChatMessage.truncateToBytes("가나다", 5));
        assertEquals("a가", ChatMessage.truncateToBytes("a가나", 4));
        assertEquals("", ChatMessage.truncateToBytes("가", 1));
        assertEquals("", ChatMessage.truncateToBytes("abc", 0));
        assertEquals("", ChatMessage.truncateToBytes("abc", -3));
    }

    @Test
    void 최대_길이는_한글_32자_또는_영문_64자다() {
        assertEquals(32, ChatMessage.truncateToBytes("가".repeat(50), ChatMessage.MAX_BYTES).length());
        assertEquals(64, ChatMessage.truncateToBytes("a".repeat(100), ChatMessage.MAX_BYTES).length());
    }

    @Test
    void 보낼_내용은_한_줄로_만들고_앞뒤_공백을_떼고_64바이트에서_자른다() {
        assertEquals("안녕 하세요", ChatMessage.clean("  안녕\n하세요\t "));
        assertEquals("", ChatMessage.clean(" \n\t "));
        assertEquals("가".repeat(32), ChatMessage.clean("가".repeat(40)));
        // 잘린 자리가 공백이면 그 공백도 뗀다
        assertEquals("a".repeat(63), ChatMessage.clean("a".repeat(63) + " b"));
    }

    @Test
    void 빈칸으로_보이는_글자뿐인_글은_빈_글로_다듬는다() {
        // 한글 채움 문자, 폭 없는 공백, 줄 바꿈 없는 공백
        assertEquals("", ChatMessage.clean("\u3164"));
        assertEquals("", ChatMessage.clean("\u200B\u200B"));
        assertEquals("", ChatMessage.clean("\u00A0 \u3164"));
        // 글 사이의 공백은 그대로 두고 앞뒤만 뗀다
        assertEquals("가 나", ChatMessage.clean("\u3164가 나\u00A0"));
    }

    @Test
    void 줄을_나누거나_글자_방향을_바꾸는_글자는_남기지_않는다() {
        // 줄 구분, 문단 구분은 공백으로
        assertEquals("a b", ChatMessage.clean("a\u2028b"));
        assertEquals("a b", ChatMessage.clean("a\u2029b"));
        // 글자 방향 뒤집기와 폭 없는 공백은 지운다
        assertEquals("abc", ChatMessage.clean("a\u202Eb\u200Bc"));
        // 짝이 맞지 않는 서로게이트
        assertEquals("ab", ChatMessage.clean("a\uD83Db"));
    }

    @Test
    void 자모를_풀어쓴_한글은_한_글자로_합쳐서_센다() {
        // ㅎ + ㅏ + ㄴ 을 따로 적은 "한" (맥의 파일 이름을 붙여넣으면 이렇게 들어온다)
        String decomposed = "\u1112\u1161\u11AB";
        assertEquals("한", ChatMessage.clean(decomposed));
        assertEquals(2, ChatMessage.byteLength(ChatMessage.clean(decomposed)));
    }

    @Test
    void 이모지_같은_서로게이트_쌍의_가운데는_자르지_않는다() {
        String smile = "😀";
        assertEquals(4, ChatMessage.byteLength(smile));
        assertEquals("a", ChatMessage.truncateToBytes("a" + smile, 4));
        assertEquals("a" + smile, ChatMessage.truncateToBytes("a" + smile, 5));
    }
}
