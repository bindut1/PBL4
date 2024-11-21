package download;

public class DownloadObject {
	public AbstractDownloadObject downloader;
	protected String urlInput;
	protected String path;
	protected String pathTorrentPre;

	public DownloadObject(String url, String path) {
		this.urlInput = url;
		this.path = path;
	}

	public boolean downloaderNotNull() {
		if (downloader == null)
			return false;
		else
			return true;
	}

	public void start() {
		if (urlInput.endsWith(".torrent"))
			this.downloader = new DownloadTorrent();
		else
			this.downloader = new DownloadDirectLink();
		
		this.downloader.start(this.urlInput, this.path);
	}
}
