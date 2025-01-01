package util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpConnection {
	private static final int MAX_REDIRECTS = 5;

	public static HttpURLConnection openConnection(URL url) throws IOException {

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.setInstanceFollowRedirects(true);

		int redirectCount = 0;
		while (redirectCount < MAX_REDIRECTS) {
			int status = connection.getResponseCode();
			if (status != HttpURLConnection.HTTP_MOVED_TEMP && status != HttpURLConnection.HTTP_MOVED_PERM
					&& status != HttpURLConnection.HTTP_SEE_OTHER) {
				break;
			}
			String newUrl = connection.getHeaderField("Location");
			connection.disconnect();
			connection = (HttpURLConnection) new URL(newUrl).openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			redirectCount++;
		}

		return connection;
	}

	public static HttpURLConnection createSegmentConnection(URL url, long startByte, long endByte) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		if (startByte >= 0 && endByte >= 0) {
			connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
		}
		return connection;
	}

	public static Object createSegmentConnection(String fileUrl, long currentPosition, long endByte) throws Exception {
		try {
			// Sử dụng HttpClient để kiểm tra và sử dụng HTTP/2
			HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2) // Ưu tiên HTTP/2
					.connectTimeout(Duration.ofSeconds(10)).build();

			HttpRequest request = HttpRequest.newBuilder().uri(new URI(fileUrl))
					.header("Range", "bytes=" + currentPosition + "-" + endByte).GET().build();

			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

			// Kiểm tra mã trạng thái
			if (response.statusCode() == 206 || response.statusCode() == 200) {
				System.out.println("HTTP/2 được sử dụng. Dữ liệu đã được tải về dưới dạng byte array.");
				return response.body(); // Trả về mảng byte chứa dữ liệu
			} else {
				throw new IOException("HTTP/2 không trả về phản hồi hợp lệ. Fallback sang HTTP/1.1.");
			}
		} catch (Exception e) {
			// Fallback sang HTTP/1.1 nếu HTTP/2 không được hỗ trợ
			System.out.println("Fallback sang HTTP/1.1: " + e.getMessage());
			URL url = new URL(fileUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endByte);
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(15000);
			connection.setDoInput(true);
			return connection; // Trả về HttpURLConnection
		}
	}

}
