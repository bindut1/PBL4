package download;

import java.io.*;
import java.net.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import util.FileHandle;
import util.HttpConnection;
import util.TimeHandle;

public class DownloadDirectLink extends AbstractDownloadObject {
	private static final int NUM_SEGMENTS = 5;

	public DownloadDirectLink() {
		this.progress = 0;
		this.startTime = 0;
		this.runningFlag = false;
		this.completedFlag = false;
		this.lock = new ReentrantLock();
		this.pauseCondition = lock.newCondition();
		this.executor = Executors.newFixedThreadPool(NUM_SEGMENTS + 1);
	}

	public void start(String urlInput, String pathInput) {
		this.detailText = "Đang chuẩn bị tải!";
		this.url = urlInput;
		this.startTime = TimeHandle.getCurrentTime();
		try {
			URL url = new URL(urlInput);
			String protocol = url.getProtocol().toLowerCase();
			switch (protocol) {
			case "http":
			case "https":
				this.runningFlag = true;
				this.completedFlag = false;
				downloadDirectLink(urlInput, pathInput);
				break;
			default:
				this.detailText = "Unsupported protocol: " + protocol;
			}
		} catch (Exception e) {
			this.detailText = "Error occurred: " + e.getMessage();
			e.printStackTrace();
		} finally {
			executor = null;
		}
	}

	public void cancel() {
		try {
			this.detailText = "Đã hủy tải";
			this.runningFlag = false;
			this.completedFlag = false;
			if (executor != null)
				executor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pause() {
		this.detailText = "Đã tạm dừng tải";
		this.lastPauseTime = TimeHandle.getCurrentTime();
		this.runningFlag = false;
	}

	public void resume() {
		this.detailText = "Đang tiếp tục tải";
		this.totalPauseTime += TimeHandle.getCurrentTime() - this.lastPauseTime;
		this.runningFlag = true;
		lock.lock();
		try {
			pauseCondition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public boolean getRunningFlag() {
		return this.runningFlag;
	}

	private void downloadDirectLink(String fileUrl, String path) throws IOException {
		URL url = new URL(fileUrl);
		HttpURLConnection connection = HttpConnection.openConnection(url);
		boolean acceptRanges = connection.getHeaderField("Accept-Ranges") != null;
		long fileSize = connection.getContentLengthLong();
		String fileName = FileHandle.getFileName(connection, fileUrl);

		File outputFile = new File(path, fileName);
		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
			synchronized (outputFile) {

			}
		}
		try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
			raf.setLength(fileSize);
		} catch (IOException e) {
			this.detailText = "Error setting file length: " + e.getMessage();
			e.printStackTrace();
		}
		AtomicLong totalBytesDownloaded = new AtomicLong(0);
		if (acceptRanges && fileSize > 0) {
			long segmentSize = (long) Math.ceil((double) fileSize / NUM_SEGMENTS);

			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < NUM_SEGMENTS; i++) {
				long startByte = i * segmentSize;
				long endByte = (i == NUM_SEGMENTS - 1) ? fileSize - 1 : (i + 1) * segmentSize - 1;
				final int segmentNumber = i;

				// Thêm các luồng tải phân đoạn vào executor
				futures.add(executor.submit(() -> {
					try {
						downloadSegment(fileUrl, startByte, endByte, outputFile, segmentNumber, totalBytesDownloaded);
					} catch (IOException e) {
						e.printStackTrace();
						this.detailText = "Error in downloading segment: " + e.getMessage();
					}
				}));
			}

			executor.submit(() -> monitorObserver(totalBytesDownloaded, fileSize));
			if (!this.completedFlag && this.runningFlag)
				completeDownload(futures, fileSize);
		} else {
			// tải thông thường nếu không cho phép tải phân đoạn
			performSingleThreadDownload(connection, outputFile, totalBytesDownloaded);
		}
	}

	private void downloadSegment(String fileUrl, long startByte, long endByte, File outputFile, int segmentNumber,
			AtomicLong totalBytesDownloaded) throws IOException {
		HttpURLConnection connection = null;
		RandomAccessFile raf = null;
		InputStream in = null;
		try {
			URL url = new URL(fileUrl);
			long currentPosition = startByte;
			raf = new RandomAccessFile(outputFile, "rw");
			byte[] buffer = new byte[25600];
			int bytesRead;
			long bytesDownloaded = 0;
			double currentTime;
			double lastUpdateTime = TimeHandle.getCurrentTime();

			while (currentPosition <= endByte) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				lock.lock();
				try {
					while (!this.runningFlag) {
						try {
							if (connection != null) {
								connection.disconnect();
								connection = null;
								if (in != null) {
									in.close();
									in = null;
								}
							}
							pauseCondition.await();
						} catch (InterruptedException e) {
							System.out.println(e);
						}
					}
				} finally {
					lock.unlock();
				}

				if (connection == null) {
					connection = HttpConnection.createSegmentConnection(url, currentPosition, endByte);
					in = connection.getInputStream();
					raf.seek(currentPosition);
				}

				bytesRead = in.read(buffer);
				if (bytesRead == -1)
					break;

				raf.write(buffer, 0, bytesRead);
				bytesDownloaded += bytesRead;
				currentPosition += bytesRead;
				totalBytesDownloaded.addAndGet(bytesRead);

				currentTime = TimeHandle.getCurrentTime();
				if (currentTime - lastUpdateTime >= 1500) {
					updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
					lastUpdateTime = currentTime;
				}
			}
			updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
			if (raf != null)
				try {
					raf.close();
				} catch (IOException e) {
				}
			if (connection != null)
				connection.disconnect();
		}
	}

	private void monitorObserver(AtomicLong totalBytesDownloaded, Long fileSize) {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Xử lý pause
				lock.lock();
				try {
					while (!this.runningFlag) {
						try {
							pauseCondition.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				} finally {
					lock.unlock();
				}
				updateOverallProgress(totalBytesDownloaded.get(), fileSize);
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void completeDownload(List<Future<?>> futures, long fileSize) throws IOException {
		try {
			// future.get() : đợi một luồng chạy xong
			for (Future<?> future : futures) {
				future.get();
			}
			updateOverallProgress(fileSize, fileSize);
			this.detailText = "Download completed successfully!";
			this.completedFlag = true;
		} catch (InterruptedException | ExecutionException e) {
			this.detailText = "Download failed: " + e.getMessage();
		} finally {
			this.runningFlag = false;
			executor.shutdownNow();
		}
	}

	public void updateOverallProgress(long totalBytesDownloaded, long fileSize) {
		{
			double progress = (double) totalBytesDownloaded / fileSize * 100;
			double nowTime = TimeHandle.getCurrentTime();
			double elapsedTime = nowTime - this.startTime - this.totalPauseTime;
			double speed = totalBytesDownloaded / elapsedTime; // milisecond
			double estimatedTimeRemaining = (fileSize - totalBytesDownloaded) / speed;
			speed *= 1000; // tốc độ trên 1 giây
			String detailText = String.format(
					"Overall Progress: %s / %s (%.2f%%) - Speed: %s/s - Elapsed: %s - ETA: %s \n",
					FileHandle.formatFileSize(totalBytesDownloaded), FileHandle.formatFileSize(fileSize), progress,
					FileHandle.formatFileSize((long) speed), TimeHandle.formatTime(elapsedTime),
					TimeHandle.formatTime(estimatedTimeRemaining));
			this.detailText = detailText;
			this.progress = progress / 100;
		}
	}

	public void updateSegmentProgress(int segmentNumber, long bytesDownloaded, long startByte, long endByte) {
		{
			double currentTime = TimeHandle.getCurrentTime();
			double timeElapsed = currentTime - this.startTime - this.totalPauseTime;
			long segmentSize = endByte - startByte + 1;
			double speedInBytesPerSecond = (bytesDownloaded * 1000.0) / timeElapsed;
			double segmentProgress = (double) (bytesDownloaded * 100.0) / (segmentSize);

			String detailText = String.format("Segment %d: %s / %s (%.2f%%) - Speed: %s/s - Elapsed: %s\n",
					segmentNumber + 1, FileHandle.formatFileSize(bytesDownloaded),
					FileHandle.formatFileSize(segmentSize), segmentProgress,
					FileHandle.formatFileSize((long) speedInBytesPerSecond), TimeHandle.formatTime(timeElapsed));
			this.detailText += detailText;
		}
	}

	private void performSingleThreadDownload(HttpURLConnection connection, File outputFile,
			AtomicLong totalBytesDownloaded) throws IOException {
		this.detailText = "Kích thước file không xác định, hệ thống sẽ thực hiện tải thông thường!";
		this.detailText = ("Vui lòng đợi giây lát . . .");
		try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(outputFile)) {
			byte[] buffer = new byte[25600];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				totalBytesDownloaded.addAndGet(bytesRead);
			}
		}
		this.detailText = "Download completed successfully!";
		this.completedFlag = true;
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
