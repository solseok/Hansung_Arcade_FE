package hansung.arcade.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 서버가 보낸 줄을 하나씩 읽는다. 한 줄은 {@link Protocol#MAX_LINE_BYTES}(UTF-8 바이트, \n 제외)까지만 받는다.
 * 서버도 같은 한도를 쓰므로, 넘는 줄은 잘못된 연결로 보고 끊는다 (끝없이 긴 줄로 메모리를 다 쓰지 않게).
 */
final class LineInput {

    private final InputStream in;
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();

    /** @param in 버퍼가 있는 스트림 (한 바이트씩 읽는다) */
    LineInput(InputStream in) {
        this.in = in;
    }

    /**
     * 다음 줄. 끝의 \r\n·\n은 뗀다.
     *
     * @return 연결이 끝났으면 null (끝나기 전의 덜 받은 줄은 버린다)
     * @throws IOException 줄이 한도를 넘거나 읽지 못하면 (시간 초과는 {@link java.net.SocketTimeoutException})
     */
    String readLine() throws IOException {
        line.reset();
        while (true) {
            int b = in.read();
            if (b == -1) {
                return null;
            }
            if (b == '\n') {
                byte[] bytes = line.toByteArray();
                int length = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
                return new String(bytes, 0, length, StandardCharsets.UTF_8);
            }
            if (line.size() >= Protocol.MAX_LINE_BYTES) {
                throw new IOException("서버가 보낸 한 줄이 " + Protocol.MAX_LINE_BYTES + "바이트를 넘습니다.");
            }
            line.write(b);
        }
    }
}
