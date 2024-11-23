package download;

import view.ProgressUI;

public class DownloadObject {
	public AbstractDownloadObject downloader;
	
	protected String urlInput;
	protected String path;
	
	public DownloadObject(String url, String path) {
		this.urlInput = url;
		this.path = path;
		if (urlInput.endsWith(".torrent"))
			this.downloader = new DownloadTorrent();
		else
			this.downloader = new DownloadDirectLink();
	}

	public boolean downloaderNotNull() {
		if (downloader == null)
			return false;
		else
			return true;
	}

	public void start() {
		this.downloader.start(this.urlInput, this.path);
	}

}
