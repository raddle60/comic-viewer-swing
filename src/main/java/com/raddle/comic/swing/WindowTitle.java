package com.raddle.comic.swing;

/**
 * 窗口标题的各个部分。
 *
 * 标题由几部分组成：基本信息（渠道、漫画、章节、页码），
 * 加载失败的原因，正在做的事，连续下载的进度。
 *
 * 以前各处都是直接 frame.setTitle(...)，改一处就把别处的信息冲掉了，
 * 比如下载进度被"正在翻页"顶掉，失败原因被普通标题盖住。
 * 这里把标题拆成几个部分，各处只改自己关心的那一部分，
 * 改完统一按属性拼出来显示（ComicViewer.refreshTitle）。
 */
class WindowTitle {

	/** 正在做的事，比如"正在翻页"、"loading"，没有就是null */
	private String status;
	/** 加载失败的原因 */
	private String failedReason;
	/** 连续下载的进度，比如"已下载 12/30" */
	private String downloadProgress;

	String getStatus() {
		return status;
	}

	void setStatus(String status) {
		this.status = status;
	}

	String getFailedReason() {
		return failedReason;
	}

	void setFailedReason(String failedReason) {
		this.failedReason = failedReason;
	}

	String getDownloadProgress() {
		return downloadProgress;
	}

	void setDownloadProgress(String downloadProgress) {
		this.downloadProgress = downloadProgress;
	}
}
