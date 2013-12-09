/**
 * 
 */
package com.raddle.comic.engine;

import java.util.List;

/**
 * @author raddle
 * 
 */
public class ComicInfo {
	private String comicId;
	private String comicName;
	private List<SectionInfo> sections;

	public String getComicId() {
		return comicId;
	}

	public void setComicId(String comicId) {
		this.comicId = comicId;
	}

	public String getComicName() {
		return comicName;
	}

	public void setComicName(String comicName) {
		this.comicName = comicName;
	}

	public List<SectionInfo> getSections() {
		return sections;
	}

	public void setSections(List<SectionInfo> sections) {
		this.sections = sections;
	}
}
