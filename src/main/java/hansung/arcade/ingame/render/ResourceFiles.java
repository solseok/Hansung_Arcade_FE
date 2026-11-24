package hansung.arcade.ingame.render;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 클래스패스 폴더 안의 파일 이름 목록. IDE·gradle run(폴더)과 배포용 jar 양쪽에서 동작한다.
 */
final class ResourceFiles {

    private ResourceFiles() {
    }

    /** dir(예: /image/camp/) 바로 아래 파일 이름들. 폴더가 없으면 빈 목록. */
    static List<String> list(String dir) {
        URL url = ResourceFiles.class.getResource(dir);
        if (url == null) {
            return List.of();
        }
        try {
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                return listInJar(uri, dir);
            }
            return names(Path.of(uri));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("리소스 경로를 해석할 수 없습니다: " + url, e);
        } catch (IOException e) {
            throw new UncheckedIOException("리소스 폴더를 읽지 못했습니다: " + dir, e);
        }
    }

    private static List<String> listInJar(URI uri, String dir) throws IOException {
        try {
            return names(FileSystems.getFileSystem(uri).getPath(dir));
        } catch (FileSystemNotFoundException notOpenYet) {
            try (FileSystem jar = FileSystems.newFileSystem(uri, Map.of())) {
                return names(jar.getPath(dir));
            }
        }
    }

    private static List<String> names(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
}
