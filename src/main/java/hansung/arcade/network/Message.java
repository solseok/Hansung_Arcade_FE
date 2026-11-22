package hansung.arcade.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 한 줄에 담기는 메시지 하나. 네프 API 공통 규칙의 다섯 가지 모양이다.
 *
 * <pre>
 * 요청       {"type":"LOGIN","id":7,"body":{"nicknames":["상상부기"]}}
 * 응답 성공  {"type":"LOGIN","id":7,"ok":true,"body":{...}}
 * 응답 실패  {"type":"LOGIN","id":7,"ok":false,"error":{"code":"...","message":"..."}}
 * 응답 없음  {"type":"MOVE","body":{...}}
 * 푸시       {"type":"ROOM_STATE","body":{...}}
 * </pre>
 *
 * @param type  늘 있다. 응답은 요청과 같은 type
 * @param id    요청과 그 응답에만 있다. 없으면 null
 * @param ok    응답에만 있다. 없으면 null
 * @param body  실패 응답이 아니면 늘 있다 (내용이 없어도 {@code {}})
 * @param error 실패 응답에만 있다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Message(String type, Long id, Boolean ok, JsonNode body, Error error) {

    /** 모르는 필드는 무시한다 (서버가 필드를 더해도 클라이언트가 깨지지 않게) */
    static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 실패 응답의 내용. message는 화면에 그대로 띄울 수 있는 한국어 한 줄이다 */
    public record Error(String code, String message) {
    }

    public Message {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type이 비어 있습니다.");
        }
    }

    /** 응답을 기다리는 요청 */
    public static Message request(String type, long id, Object body) {
        return new Message(type, id, null, toBody(body), null);
    }

    /** 응답이 없는 메시지 */
    public static Message notice(String type, Object body) {
        return new Message(type, null, null, toBody(body), null);
    }

    /** 요청에 대한 응답인지 (ok가 있는지) */
    public boolean isResponse() {
        return ok != null && id != null;
    }

    /** 실패 응답이나 ERROR 푸시의 code. 없으면 null */
    public String errorCode() {
        if (error != null) {
            return error.code();
        }
        return Protocol.ERROR.equals(type) && body.hasNonNull("code") ? body.get("code").asText() : null;
    }

    /**
     * 서버가 보낸 한 줄을 읽는다. body가 없으면 {@code {}}로 본다. 모르는 필드는 무시한다.
     *
     * @throws IllegalArgumentException JSON 객체가 아니거나 type이 없으면
     */
    public static Message parse(String line) {
        JsonNode root;
        try {
            root = MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 형식이 아닙니다: " + line, e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("메시지는 JSON 객체여야 합니다: " + line);
        }
        JsonNode type = root.get("type");
        if (type == null || !type.isTextual() || type.asText().isBlank()) {
            throw new IllegalArgumentException("type이 없습니다: " + line);
        }
        JsonNode idNode = root.get("id");
        Long id = idNode != null && idNode.canConvertToLong() ? idNode.asLong() : null;
        JsonNode okNode = root.get("ok");
        Boolean ok = okNode != null && okNode.isBoolean() ? okNode.asBoolean() : null;
        JsonNode body = root.get("body");
        if (body == null || !body.isObject()) {
            body = MAPPER.createObjectNode();
        }
        Error error = null;
        JsonNode errorNode = root.get("error");
        if (errorNode != null && errorNode.isObject()) {
            error = new Error(errorNode.path("code").asText(""), errorNode.path("message").asText(""));
        }
        return new Message(type.asText(), id, ok, body, error);
    }

    /** 한 줄 JSON. 줄 끝의 \n은 보내는 쪽이 붙인다 */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(type + " 메시지를 JSON으로 바꾸지 못했습니다.", e);
        }
    }

    private static JsonNode toBody(Object body) {
        if (body == null) {
            return MAPPER.createObjectNode();
        }
        JsonNode node = body instanceof JsonNode json ? json : MAPPER.valueToTree(body);
        if (!node.isObject()) {
            throw new IllegalArgumentException("body는 JSON 객체여야 합니다: " + node.getNodeType());
        }
        return node;
    }

    /** body를 record 등으로 읽는다. 모르는 필드는 무시한다 */
    public static <T> T read(JsonNode body, Class<T> type) {
        try {
            return MAPPER.treeToValue(body, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(type.getSimpleName() + "으로 읽을 수 없는 body입니다: " + body, e);
        }
    }

    /** 빈 body {@code {}} */
    public static ObjectNode emptyBody() {
        return MAPPER.createObjectNode();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
