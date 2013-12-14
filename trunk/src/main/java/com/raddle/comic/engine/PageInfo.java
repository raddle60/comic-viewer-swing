/**
 * 
 */
package com.raddle.comic.engine;

/**
 * @author raddle
 * 
 */
public class PageInfo {
	private Integer pageNo;
	private String filename;
	private String pageUrl;

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	@Override
	public String toString() {
		return pageNo + "";
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}
