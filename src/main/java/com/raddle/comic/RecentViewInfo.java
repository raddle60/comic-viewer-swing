package com.raddle.comic;

public class RecentViewInfo {
	private String channelName;
	private String channelPath;
	private String comicId;
	private String sectionId;
	private Integer pageNo;
	private long time;

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getChannelPath() {
		return channelPath;
	}

	public void setChannelPath(String channelPath) {
		this.channelPath = channelPath;
	}

	public String getComicId() {
		return comicId;
	}

	public void setComicId(String comicId) {
		this.comicId = comicId;
	}

	public String getSectionId() {
		return sectionId;
	}

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}