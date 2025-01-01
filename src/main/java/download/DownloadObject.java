package download;

public class DownloadObject {
	public AbstractDownloadObject downloader;
		
	public DownloadObject(String url, String path) {
		if (url.endsWith(".torrent"))
			this.downloader = new DownloadTorrent();
		else
			this.downloader = new DownloadDirectLink();
		this.downloader.setPath(path);
		this.downloader.setUrl(url);
	}

	public boolean downloaderNotNull() {
		if (downloader == null)
			return false;
		else
			return true;
	}

	public void start() {
		this.downloader.start();
	}

}
