import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Docling REST API 的 Java 客户端，零外部依赖（仅 JDK 11+ HttpClient）。
 *
 * <pre>{@code
 *   DoclingClient client = new DoclingClient("http://localhost:8080");
 *   String md = client.convertFile(Path.of("document.pdf"), "md");
 * }</pre>
 */
public class DoclingClient {

    private final HttpClient http;
    private final String baseUrl;

    public DoclingClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(120));
    }

    public DoclingClient(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    /** 上传本地文件并转换 */
    public String convertFile(Path file, String format) throws IOException, InterruptedException {
        String boundary = "docling-" + System.currentTimeMillis();
        byte[] body = buildMultipart(file, null, format, boundary);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/convert"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(300))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    /** 按 URL 转换（服务端自行下载） */
    public String convertUrl(String url, String format) throws IOException, InterruptedException {
        String boundary = "docling-" + System.currentTimeMillis();
        byte[] body = buildMultipart(null, url, format, boundary);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/convert"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(300))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    /** 异步转换本地文件 */
    public CompletableFuture<String> convertFileAsync(Path file, String format) {
        String boundary = "docling-" + System.currentTimeMillis();
        byte[] body = buildMultipart(file, null, format, boundary);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/convert"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(300))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
                    }
                    return resp.body();
                });
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /* ---------- internal ---------- */

    private byte[] buildMultipart(
            Path file, String url, String format, String boundary) {
        StringBuilder sb = new StringBuilder();
        final String EOL = "\r\n";
        final String DQ = "--";

        // ---- file part ----
        if (file != null) {
            sb.append(DQ).append(boundary).append(EOL);
            sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getFileName()).append("\"").append(EOL);
            sb.append("Content-Type: application/octet-stream").append(EOL);
            sb.append(EOL);
        } else {
            sb.append(DQ).append(boundary).append(EOL);
            sb.append("Content-Disposition: form-data; name=\"url\"").append(EOL);
            sb.append(EOL);
            sb.append(url).append(EOL);
        }

        // ---- "to" part ----
        sb.append(DQ).append(boundary).append(EOL);
        sb.append("Content-Disposition: form-data; name=\"to\"").append(EOL);
        sb.append(EOL);
        sb.append(format).append(EOL);

        // ---- end marker ----
        if (file != null) {
            sb.append(DQ).append(boundary).append(DQ).append(EOL);
        } else {
            sb.append(DQ).append(boundary).append(DQ).append(EOL);
        }

        byte[] textPart = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (file != null) {
            try {
                byte[] filePart = Files.readAllBytes(file);
                byte[] full = new byte[textPart.length + filePart.length];
                System.arraycopy(textPart, 0, full, 0, textPart.length);
                System.arraycopy(filePart, 0, full, textPart.length, filePart.length);
                return full;
            } catch (IOException e) {
                throw new RuntimeException("读取文件失败: " + file, e);
            }
        }

        return textPart;
    }
}
