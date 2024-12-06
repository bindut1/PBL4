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

import com.google.common.io.Files;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

import view.MainUI;
import view.UIObjectGeneral;
import view.objWaiting;

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
		// Thay thế chuỗi gạch dưới liên tiếp thành một gạch dưới duy nhất
		fileName = fileName.replaceAll("_+", "_");
		fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
		return fileName.replace("%20", " ");
	}
	
	public static String ensureUniqueFileName(String folderPath, String fileName) {
        File folder = new File(folderPath);
        int dotIndex = fileName.lastIndexOf(".");
        String name = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
        String extension = (dotIndex > 0) ? fileName.substring(dotIndex) : "";
        
        String newFileName = fileName;
        int counter = 1;
        while (new File(folder, newFileName).exists()||isFileNameInList(newFileName)) {
            newFileName = name + " (" + counter + ")" + extension;
            counter++;
        }
        return newFileName;
    }
	
	private static boolean isFileNameInList(String fileName) {
        for (objWaiting waiting : objWaiting.getListWaiting()) {
            if (waiting.getFileName().equals(fileName)) {
                return true;
            }
        }
        for (UIObjectGeneral obj : MainUI.listFileDownloadingGlobal) {
            if (obj.getFileName().equals(fileName)) {
                return true;
            }
        }
        return false;
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

	public static String getFileNameFromConnectHttp(String urlInput) throws IOException {
		URL url = new URL(urlInput);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		return getFileName(connection, urlInput);
	}

	public static long getFileSizeFromConnectHttp(String urlInput) throws IOException {
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
		String logEntry = String.format("%s, %s, %s, %s, %s, %s", fileName, fileSize, status, date, time, path);
		try (FileWriter fw = new FileWriter("CompletedFileTracking.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveFileWaitingToTxt(String url, String fileSize, String path, String time) {
		String logEntry = String.format("%s,%s,%s,%s", url, fileSize, path, time);
		try (FileWriter fw = new FileWriter("WaitingFileTracking.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean deleteLineFromTxtFile(String fileName, String lineToDelete) {
        File inputFile = new File(fileName);
        File tempFile = new File("temp.txt");
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.trim().equals(lineToDelete.trim())) {
                    found = true;
                    continue; 
                }
                writer.write(currentLine);
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!found) {
            tempFile.delete();
            return false;
        }

        if (!inputFile.delete()) {
            tempFile.delete();
            return false;
        }
        if (!tempFile.renameTo(inputFile)) {
            return false;
        }
        return true;
    }
}
