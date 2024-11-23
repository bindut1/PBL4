package download;

import util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;


public class DownloadTorrent extends AbstractDownloadObject {

	private static final int TORRENT_UPLOAD_RATE_LIMIT = 0;
	private static final int TORRENT_DOWNLOAD_RATE_LIMIT = 0;

	public DownloadTorrent() {
		this.runningFlag = false;
		this.completedFlag = false;
		this.startTime = 0;
		this.progress = 0;
		this.detailText = "";
		this.lock = new ReentrantLock();
		this.pauseCondition = lock.newCondition();
	}

	@Override
	public void start(String urlInput, String path) {
		this.runningFlag = true;
		this.completedFlag = false;
		this.url = urlInput;
		this.path = path;
		this.detailText = "Đang chuẩn bị tải";
		try {
			excute();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.runningFlag = false;
		}
	}

	@Override
	public void cancel() {
		try {
			executor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		this.runningFlag = false;
	}

	@Override
	public void resume() {
		this.runningFlag = true;
		lock.lock();
		try {
			pauseCondition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean getRunningFlag() {
		return this.runningFlag;
	}

	private void excute() throws Exception {
		System.out.println(this.url);
		File torrentFile = new File(this.url);
		if (!torrentFile.exists()) {
			throw new FileNotFoundException("Torrent file not found");
		}

		File downloadDir = new File(this.path);
		if (!downloadDir.exists()) {
			downloadDir.mkdir();
		}

		SharedTorrent torrent = SharedTorrent.fromFile(torrentFile, downloadDir);
		torrent.setMaxUploadRate(TORRENT_UPLOAD_RATE_LIMIT);
		torrent.setMaxDownloadRate(TORRENT_DOWNLOAD_RATE_LIMIT);
		Client client = new Client(InetAddress.getLocalHost(), torrent);
		AtomicLong lastDownloaded = new AtomicLong(0);

		client.addObserver((o, arg) -> {
			Client.ClientState state = client.getState();
			float progress = client.getTorrent().getCompletion();
			double currentTime = 0;
			double elapsedTime = (currentTime - this.startTime) / 1000.0;
			long downloadedBytes = client.getTorrent().getDownloaded();
			long deltaDownloaded = downloadedBytes - lastDownloaded.getAndSet(downloadedBytes);
			double instantSpeed = deltaDownloaded / 1.0;
			updateProgress(progress, state.toString(), instantSpeed, downloadedBytes / elapsedTime,
					client.getPeers().size());
		});

		client.download();
		while (!client.getState().equals(Client.ClientState.SEEDING)) {
			Thread.sleep(1000);
		}
		client.stop();
		this.detailText = "Tải thành công";
		this.completedFlag = true;
	}

	public void updateProgress(double progress, String state, double instantSpeed, double averageSpeed, int peers) {
		this.progress = progress / 100;
		this.detailText = String.format(
				"Progress: %.2f%% - State: %s - Current Speed: %s/s - Average Speed: %s/s - Peers: %d", progress, state,
				FileHandle.formatFileSize((long) instantSpeed), FileHandle.formatFileSize((long) averageSpeed), peers);
		System.out.println(detailText);
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
