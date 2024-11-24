package download;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.settings_pack;
import util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTorrent extends AbstractDownloadObject {
    private static final int TORRENT_UPLOAD_RATE_LIMIT = 0;
    private static final int TORRENT_DOWNLOAD_RATE_LIMIT = 0;
    
    private SessionManager sessionManager;
    private TorrentHandle torrentHandle;
    private final CountDownLatch signal;
    
    public DownloadTorrent() {
        this.runningFlag = false;
        this.completedFlag = false;
        this.startTime = 0;
        this.progress = 0;
        this.detailText = "";
        this.lock = new ReentrantLock();
        this.pauseCondition = lock.newCondition();
        this.signal = new CountDownLatch(1);
    }

    @Override
    public void start(String urlInput, String path) {
        this.runningFlag = true;
        this.completedFlag = false;
        this.url = urlInput;
        this.path = path;
        this.detailText = "Đang chuẩn bị tải";
        this.startTime = System.currentTimeMillis();
        
        try {
            execute();
        } catch (Exception e) {
            e.printStackTrace();
            this.detailText = "Lỗi: " + e.getMessage();
        } finally {
            this.runningFlag = false;
        }
    }

    @Override
    public void pause() {
        if (torrentHandle != null && sessionManager != null) {
            this.runningFlag = false;
            torrentHandle.pause();
            this.detailText = "Đã tạm dừng tải";
        }
    }

    @Override
    public void resume() {
        if (torrentHandle != null && sessionManager != null) {
            this.runningFlag = true;
            torrentHandle.resume();
            this.detailText = "Đang tiếp tục tải";
        }
    }

    @Override
    public void cancel() {
        if (sessionManager != null) {
            this.runningFlag = false;
            try {
                if (torrentHandle != null) {
                    torrentHandle.pause();
                }
                sessionManager.stop();
                this.detailText = "Đã hủy tải";
                if (executor != null) {
                    executor.shutdownNow();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void execute() throws Exception {
        File torrentFile = new File(this.url);
        if (!torrentFile.exists()) {
            throw new FileNotFoundException("Không tìm thấy file torrent");
        }

        File downloadDir = new File(this.path);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        // Initialize session with settings
        sessionManager = new SessionManager();
        
        SettingsPack settingsPack = new SettingsPack();
        settingsPack.setInteger(settings_pack.int_types.active_downloads.swigValue(), 4);
        settingsPack.setInteger(settings_pack.int_types.active_seeds.swigValue(), 4);
        settingsPack.setInteger(settings_pack.int_types.upload_rate_limit.swigValue(), TORRENT_UPLOAD_RATE_LIMIT);
        settingsPack.setInteger(settings_pack.int_types.download_rate_limit.swigValue(), TORRENT_DOWNLOAD_RATE_LIMIT);
        
        SessionParams params = new SessionParams(settingsPack);
        sessionManager.start(params);

        // Add torrent to session
        TorrentInfo ti = new TorrentInfo(torrentFile);
        
        // Use the simplified download method
//        torrentHandle = sessionManager.download(ti, downloadDir);
        
        // Set the priority after getting the handle
        if (torrentHandle != null) {
            Priority[] priorities = Priority.array(Priority.NORMAL, ti.numPieces());
//            torrentHandle.prioritize(priorities);
        }

        // Monitor download progress
        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return new int[] {
                    AlertType.STATE_UPDATE.swig(),
                    AlertType.TORRENT_FINISHED.swig()
                };
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert instanceof StateUpdateAlert) {
                    StateUpdateAlert stateAlert = (StateUpdateAlert) alert;
                    if (stateAlert.status().size() > 0) {
                        TorrentStatus status = stateAlert.status().get(0);
                        updateProgress(status);
                    }
                } else if (alert instanceof TorrentFinishedAlert) {
                    completedFlag = true;
                    detailText = "Tải thành công";
                    signal.countDown();
                }
            }
        });

        // Wait until download is complete
        signal.await();
        sessionManager.stop();
    }

    private void updateProgress(TorrentStatus status) {
        if (runningFlag) {
            double progress = status.progress() * 100;
            long downloadRate = status.downloadRate();
            long totalDownload = status.totalDownload();
            int numPeers = status.numPeers();
            
            double currentTime = System.currentTimeMillis();
            double elapsedTime = (currentTime - this.startTime) / 1000.0;
            double averageSpeed = totalDownload / elapsedTime;

            this.progress = progress / 100;
            this.detailText = String.format(
                "Progress: %.2f%% - State: %s - Current Speed: %s/s - Average Speed: %s/s - Peers: %d",
                progress,
                status.state().toString(),
                FileHandle.formatFileSize(downloadRate),
                FileHandle.formatFileSize((long)averageSpeed),
                numPeers
            );
            System.out.println(detailText);
        }
    }

    @Override
    public boolean getCompletedFlag() {
        return this.completedFlag;
    }

    @Override
    public double getStartTime() {
        return this.startTime;
    }

    @Override
    public boolean getRunningFlag() {
        return this.runningFlag;
    }
}