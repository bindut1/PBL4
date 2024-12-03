package download;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

import util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTorrent extends AbstractDownloadObject {

	private static final int TORRENT_UPLOAD_RATE_LIMIT = 0;
	private static final int TORRENT_DOWNLOAD_RATE_LIMIT = 0;

	private SharedTorrent torrent;
	private Client client;

	public DownloadTorrent() {
		this.runningFlag = false;
		this.completedFlag = false;
		this.startTime = 0;
		this.progress = 0;
		this.detailText = "";
		this.lock = new ReentrantLock();
		this.pauseCondition = lock.newCondition();
		this.executor = Executors.newFixedThreadPool(5);
	}

	@Override
	public void start(String urlInput, String path) {
		this.runningFlag = true;
		this.completedFlag = false;
		this.url = urlInput;
		this.path = path;
		this.detailText = "Đang chuẩn bị tải";
		this.startTime = System.currentTimeMillis();

		CompletableFuture.runAsync(() -> {
			try {
				downloadTorrent();
			} catch (Exception e) {
				e.printStackTrace();
				this.detailText = "Lỗi: " + e.getMessage();
				this.runningFlag = false;
			}
		}, executor);
	}

	private void downloadTorrent() throws Exception {
		File torrentFile = new File(this.url);
		if (!torrentFile.exists()) {
			throw new FileNotFoundException("Torrent file not found");
		}

		File downloadDir = new File(this.path);
		if (!downloadDir.exists()) {
			downloadDir.mkdir();
		}

		setUpClient(torrentFile, downloadDir);
		client.download();
	}

	@Override
	public void cancel() {
		try {
			this.detailText = "Đã hủy tải";
			this.runningFlag = false;
			this.completedFlag = false;
			client.stop();
			executor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		this.detailText = "Tạm dừng tải";
		this.runningFlag = false;
		client.stop();
	}

	@Override
	public void resume() {
		executor.submit(() -> {
			try {
				this.runningFlag = true;
				downloadTorrent();
			} catch (Exception e) {
				e.printStackTrace();
			}
			lock.lock();
			try {
				pauseCondition.signalAll();
			} finally {
				lock.unlock();
			}
		});
	}

	@Override
	public boolean getRunningFlag() {
		return this.runningFlag;
	}

	public void updateProgress(double progress, double speed, double downloadedBytes, long fileSize,
			double averageSpeed, int peers) {
		this.progress = progress / 100;
		this.detailText = String.format("Progress: %s / %s (%.2f%%) - Speed: %s/s - Average Speed: %s/s - Peers: %d",
				FileHandle.formatFileSize((long) downloadedBytes), FileHandle.formatFileSize(fileSize), progress,
				FileHandle.formatFileSize((long) speed), FileHandle.formatFileSize((long) averageSpeed), peers);
		System.out.println(detailText);
	}

	public void setUpClient(File torrentFile, File downloadDir) throws Exception {
		torrent = SharedTorrent.fromFile(torrentFile, downloadDir);
		torrent.setMaxUploadRate(TORRENT_UPLOAD_RATE_LIMIT);
		torrent.setMaxDownloadRate(TORRENT_DOWNLOAD_RATE_LIMIT);
		client = new Client(InetAddress.getLocalHost(), torrent);

		long fileSize = client.getTorrent().getSize();

		long startTime = System.currentTimeMillis();
		AtomicLong lastDownloaded = new AtomicLong(0);
		AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());

		client.addObserver((o, arg) -> {
//			float progress = client.getTorrent().getCompletion();
			long currentTime = System.currentTimeMillis();
			long timeElapsed = currentTime - lastUpdateTime.get();
			if (timeElapsed >= 500) {
				double elapsedTime = currentTime - startTime;
				long downloadedBytes = client.getTorrent().getDownloaded();
				float progress = (float) (downloadedBytes * 100.0) / fileSize;
				long deltaDownloaded = downloadedBytes - lastDownloaded.get();
				double instantSpeed = deltaDownloaded / (timeElapsed / 1000.0);

				lastDownloaded.set(downloadedBytes);
				lastUpdateTime.set(currentTime);
				updateProgress(progress, instantSpeed, downloadedBytes, fileSize, downloadedBytes / elapsedTime,
						client.getPeers().size());

				if (client.getTorrent().isComplete()) {
					this.completedFlag = true;
					client.stop();
				}
			}
		});
	}

	@Override
	public boolean getCompletedFlag() {
		return this.completedFlag;
	}

	@Override
	public double getStartTime() {
		return this.startTime;
	}

}