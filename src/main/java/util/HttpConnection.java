package util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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
	
	public static String getFileNameFromConnectHttp(String urlInput) throws IOException{
		URL url = new URL(urlInput);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		return FileHandle.getFileName(connection, urlInput);
	}
	
	public static long getFileSizeFromConnectHttp(String urlInput) throws IOException{
		URL url = new URL(urlInput);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		return connection.getContentLengthLong();
	}

}
