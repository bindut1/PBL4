package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.Tika;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

public class FileHandle {
	private static final DecimalFormat df = new DecimalFormat("#.##");

	public static String formatFileSize(long size) {
		String[] units = { "B", "KB", "MB", "GB", "TB" };
		int unitIndex = 0;
		double fileSize = size;

		while (fileSize > 1024 && unitIndex < units.length - 1) {
			fileSize /= 1024;
			unitIndex++;
		}
		return df.format(fileSize) + " " + units[unitIndex];
	}

	public static String getFileName(HttpURLConnection connection, String fileUrl) {
		String fileName = null;
		Tika tika = new Tika();

		String disposition = connection.getHeaderField("Content-Disposition");
		if (disposition != null && disposition.contains("filename=")) {
			Pattern pattern = Pattern.compile("filename=[\"']?([^\"']+)[\"']?");
			Matcher matcher = pattern.matcher(disposition);
			if (matcher.find()) {
				fileName = matcher.group(1);
			}
		}

		if (fileName == null) {
			String path = new File(fileUrl).getName();
			int queryIndex = path.indexOf('?');
			if (queryIndex > 0) {
				path = path.substring(0, queryIndex);
			}
			if (!path.isEmpty()) {
				fileName = path;
			}
		}

		if (fileName == null || fileName.trim().isEmpty()) {
			fileName = "downloaded_file";
			String contentType = connection.getContentType();
			if (contentType != null) {
				String extension = tika.detect(contentType);
				if (extension != null && !extension.isEmpty()) {
					fileName += extension;
				}
			}
		}
		return sanitizeFileName(fileName);
	}

	private static String sanitizeFileName(String fileName) {
		return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	public static String getFileNameTorrent(String url, String path) throws Exception {
		File torrentFile = new File(url);
		File downloadDir = new File(path);
		SharedTorrent torrent = SharedTorrent.fromFile(torrentFile, downloadDir);
		return torrent.getName();
	}

	public static long getFileSizeTorrent(String url, String path) throws Exception {
		File torrentFile = new File(url);
		File downloadDir = new File(path);
		SharedTorrent torrent = SharedTorrent.fromFile(torrentFile, downloadDir);
		Client client = new Client(InetAddress.getLocalHost(), torrent);
		return client.getTorrent().getSize();
	}
	
	public static String getFileNameFromConnectHttp(String urlInput) throws IOException{
		URL url = new URL(urlInput);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		return getFileName(connection, urlInput);
	}
	
	public static long getFileSizeFromConnectHttp(String urlInput) throws IOException{
		URL url = new URL(urlInput);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		return connection.getContentLengthLong();
	}
	
	public static List<String> readFileFromTxt(String txtFileName) {
	    List<String> listFile = new ArrayList<>();
	    try (BufferedReader reader = new BufferedReader(new FileReader(txtFileName))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            if (!line.trim().isEmpty()) {
	            	listFile.add(line.trim());
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return listFile;
	}
	
	public static void saveFileCompletedToTxt(String fileName, String fileSize, String status, String date, String time,
			String path) {
		String logEntry = String.format("%s, %s, %s, %s, %s, %s\n", fileName, fileSize, status, date, time, path);
		try (FileWriter fw = new FileWriter("CompletedFileTracking.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveFileWaitingToTxt(String fileName, long fileSize, String path) {
		String formattedSize = FileHandle.formatFileSize(fileSize);
		String logEntry = String.format("%s, %s, %s\n", fileName, formattedSize, path);
		try (FileWriter fw = new FileWriter("WaitingFileTracking.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
