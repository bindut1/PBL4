package download;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import java.nio.file.Path;
import util.FileHandle;
import util.HttpConnection;
import util.TimeHandle;

public class DownloadDirectLink extends AbstractDownloadObject {
	private static int NUM_SEGMENTS = 5;
	private static int BUFFER = 1024 * 1000;
	private int num_segment;
	private int trunkSize;

	public DownloadDirectLink() {
		synchronized (DownloadDirectLink.class) {
			this.num_segment = DownloadDirectLink.NUM_SEGMENTS;
			this.trunkSize = DownloadDirectLink.BUFFER;
		}
		this.progress = 0;
		this.startTime = 0;
		this.runningFlag = false;
		this.completedFlag = false;
		this.lock = new ReentrantLock();
		this.pauseCondition = lock.newCondition();
		this.executor = Executors.newFixedThreadPool(this.num_segment + 1);
	}

	public void start() {
		this.detailText = "Đang chuẩn bị tải!";
		this.startTime = TimeHandle.getCurrentTime();
		try {
			URL url = new URL(this.getUrl());
			String protocol = url.getProtocol().toLowerCase();
			switch (protocol) {
			case "http":
			case "https":
				this.runningFlag = true;
				this.completedFlag = false;
				downloadDirectLink();
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

	private void downloadDirectLink() throws IOException {
		URL url = new URL(this.getUrl());
		HttpURLConnection connection = HttpConnection.openConnection(url);
		boolean acceptRanges = connection.getHeaderField("Accept-Ranges") != null;
		long fileSize = connection.getContentLengthLong();

		File outputFile = new File(this.path, this.fileName);
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
			long segmentSize = (long) Math.ceil((double) fileSize / this.num_segment);

			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < this.num_segment; i++) {
				long startByte = i * segmentSize;
				long endByte = (i == this.num_segment - 1) ? fileSize - 1 : (i + 1) * segmentSize - 1;
				final int segmentNumber = i;

				// Thêm các luồng tải phân đoạn vào executor
				futures.add(executor.submit(() -> {
					try {
						downloadSegment(this.url, startByte, endByte, outputFile, segmentNumber, totalBytesDownloaded);
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

//	private void downloadSegment(String fileUrl, long startByte, long endByte, File outputFile, int segmentNumber,
//			AtomicLong totalBytesDownloaded) throws IOException {
//		Object connectionOrFile = null;
//		RandomAccessFile raf = null;
//		InputStream in = null;
//		Path tempFile = Files.createTempFile(outputFile.getName() + "segment", ".tmp");
//		byte[] buffer = new byte[this.trunkSize];
//		byte[] http2Data = null;
//		try {
//			long currentPosition = startByte;
//			raf = new RandomAccessFile(outputFile, "rw");
//			int bytesRead;
//			long bytesDownloaded = 0;
//			double currentTime;
//			double lastUpdateTime = TimeHandle.getCurrentTime();
//
//			while (currentPosition <= endByte) {
//				if (Thread.currentThread().isInterrupted()) {
//					return;
//				}
//				lock.lock();
//				try {
//					while (!this.runningFlag) {
//						try {
//							if (connectionOrFile instanceof HttpURLConnection) {
//								((HttpURLConnection) connectionOrFile).disconnect();
//								connectionOrFile = null;
//								if (in != null) {
//									in.close();
//									in = null;
//								}
//							}
//							pauseCondition.await();
//						} catch (InterruptedException e) {
//							System.out.println(e);
//						}
//					}
//				} finally {
//					lock.unlock();
//				}
//
//				if (connectionOrFile == null) {
//					try {
//						connectionOrFile = HttpConnection.createSegmentConnection(fileUrl, currentPosition, endByte);
//					} catch (Exception e) {
//						e.printStackTrace();
//						return;
//					}
//				}
//
//				// http1
//				if (connectionOrFile instanceof HttpURLConnection) {
//					// System.out.println("Xử lý HTTP/1.1...");
//					if (in == null) {
//						in = ((HttpURLConnection) connectionOrFile).getInputStream();
//					}
//					bytesRead = in.read(buffer);
//					if (bytesRead == -1)
//						break;
//					try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile.toFile(), "rw")) {
//						tempRaf.seek(currentPosition - startByte);
//						tempRaf.write(buffer, 0, bytesRead);
//					}
//					bytesDownloaded += bytesRead;
//					currentPosition += bytesRead;
//					totalBytesDownloaded.addAndGet(bytesRead);
//
//					currentTime = TimeHandle.getCurrentTime();
//					if (currentTime - lastUpdateTime >= 1500) {
//						updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
//						lastUpdateTime = currentTime;
//					}
//				}
//
//				// http2
//				else if (connectionOrFile instanceof byte[]) {
//					System.out.println("Xử lý HTTP/2 (mảng byte)...");
//					http2Data = (byte[]) connectionOrFile;
//
//					try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile.toFile(), "rw")) {
//						int offset = 0; // Điểm bắt đầu đọc dữ liệu từ mảng byte
//						int length = http2Data.length; // Tổng độ dài của dữ liệu
//						int bytesToWrite; // Số byte sẽ được ghi trong mỗi vòng lặp
//
//						while (offset < length) {
//							bytesToWrite = Math.min(buffer.length, length - offset); // Số byte còn lại hoặc kích thước
//																						// buffer
//							tempRaf.seek(currentPosition - startByte); // Vị trí ghi trong file tạm
//							tempRaf.write(http2Data, offset, bytesToWrite); // Ghi dữ liệu vào file tạm
//							offset += bytesToWrite;
//							currentPosition += bytesToWrite;
//							bytesDownloaded += bytesToWrite;
//							totalBytesDownloaded.addAndGet(bytesToWrite);
//
//							// Cập nhật tiến trình
//							currentTime = TimeHandle.getCurrentTime();
//							if (currentTime - lastUpdateTime >= 1500) {
//								updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
//								lastUpdateTime = currentTime;
//							}
//						}
//					}
//					break;
//				}
//			}
//
//			try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile.toFile(), "r")) {
//				tempRaf.seek(0);
//				raf.seek(startByte);
//				while ((bytesRead = tempRaf.read(buffer)) != -1) {
//					raf.write(buffer, 0, bytesRead);
//				}
//			}
//
//			updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
//		} finally {
//			if (in != null)
//				try {
//					in.close();
//				} catch (IOException e) {
//				}
//			if (raf != null)
//				try {
//					raf.close();
//				} catch (IOException e) {
//				}
//			if (connectionOrFile instanceof HttpURLConnection) {
//				((HttpURLConnection) connectionOrFile).disconnect();
//			}
//			Files.deleteIfExists(tempFile);
//			buffer = null;
//		}
//	}

	private void downloadSegment(String fileUrl, long startByte, long endByte, File outputFile, int segmentNumber,
			AtomicLong totalBytesDownloaded) throws IOException {
		InputStream in = null;
		RandomAccessFile raf = null;
		Path tempFile = Files.createTempFile(outputFile.getName() + "_segment", ".tmp");
		byte[] buffer = new byte[this.trunkSize];
		long currentPosition = startByte;
		long bytesDownloaded = 0;
		double currentTime;
		int bytesRead;

		double lastUpdateTime = TimeHandle.getCurrentTime();

		try {
			raf = new RandomAccessFile(outputFile, "rw");
			while (currentPosition <= endByte) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				lock.lock();
				try {
					while (!this.runningFlag) {
						try {
							pauseCondition.await();
						} catch (Exception e) {
						}

					}
				} finally {
					lock.unlock();
				}

				if (in == null) {
					try {
						in = HttpConnection.createSegmentConnection1(fileUrl, currentPosition, endByte);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}

				bytesRead = in.read(buffer);
				if (bytesRead == -1) {
					break;
				}
				try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile.toFile(), "rw")) {
					tempRaf.seek(currentPosition - startByte);
					tempRaf.write(buffer, 0, bytesRead);
				}

				currentPosition += bytesRead;
				bytesDownloaded += bytesRead;
				totalBytesDownloaded.addAndGet(bytesRead);

				currentTime = TimeHandle.getCurrentTime();
				if (currentTime - lastUpdateTime >= 1500) {
					updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
					lastUpdateTime = currentTime;
				}
			}

			try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile.toFile(), "r")) {
				tempRaf.seek(0);
				raf.seek(startByte);
				while ((bytesRead = tempRaf.read(buffer)) != -1) {
					raf.write(buffer, 0, bytesRead);
				}
			}
			updateSegmentProgress(segmentNumber, bytesDownloaded, startByte, endByte);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Files.deleteIfExists(tempFile);
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
		double progress = (double) totalBytesDownloaded / fileSize * 100;
		double nowTime = TimeHandle.getCurrentTime();
		double elapsedTime = nowTime - this.startTime - this.totalPauseTime;
		double speed = totalBytesDownloaded / elapsedTime; // milisecond
		double estimatedTimeRemaining = (fileSize - totalBytesDownloaded) / speed;
		speed *= 1000; // tốc độ trên 1 giây
		String detailText = String.format("Overall Progress: %s / %s (%.2f%%) - Speed: %s/s - Elapsed: %s - ETA: %s \n",
				FileHandle.formatFileSize(totalBytesDownloaded), FileHandle.formatFileSize(fileSize), progress,
				FileHandle.formatFileSize((long) speed), TimeHandle.formatTime(elapsedTime),
				TimeHandle.formatTime(estimatedTimeRemaining));
		this.detailText = detailText;
		this.progress = (totalBytesDownloaded < fileSize) ? (progress / 100) : 1.0;
	}

	public void updateSegmentProgress(int segmentNumber, long bytesDownloaded, long startByte, long endByte) {
		{
			double currentTime = TimeHandle.getCurrentTime();
			double timeElapsed = currentTime - this.startTime - this.totalPauseTime;
			long segmentSize = endByte - startByte + 1;
			double speedInBytesPerSecond = (bytesDownloaded * 1000.0) / timeElapsed;
			double segmentProgress = Math.min((double) (bytesDownloaded * 100.0) / (segmentSize), 100.0);

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
			byte[] buffer = new byte[this.trunkSize];
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

	public static void SetNUM_SEGMENTS(int num) {
		synchronized (DownloadDirectLink.class) {
			DownloadDirectLink.NUM_SEGMENTS = num;
		}
	}

	public static void SetBUFFER(int buffer) {
		synchronized (DownloadDirectLink.class) {
			DownloadDirectLink.BUFFER = buffer;
		}
	}

}
