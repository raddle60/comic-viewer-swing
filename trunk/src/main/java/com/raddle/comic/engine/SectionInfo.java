/**
 * 
 */
package com.raddle.comic.engine;

/**
 * @author raddle
 * 
 */
public class SectionInfo {
	private String sectionId;
	private String name;

	public String getSectionId() {
		return sectionId;
	}

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name + "";
	}
}
