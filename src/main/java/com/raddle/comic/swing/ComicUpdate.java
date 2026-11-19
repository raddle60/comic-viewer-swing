package com.raddle.comic.swing;

/**
 * 漫画的更新情况：最新的一章是哪一章。
 *
 * "最近打开"菜单里要显示最新的一章，
 * 光有章节id不够，章节名字更好看，所以两个都留着。
 */
public class ComicUpdate {

	/** 正在检查更新 */
	public static final String STATE_CHECKING = "updating";
	/** 检查失败 */
	public static final String STATE_FAILED = "failed";
	/** 查到了最新的一章 */
	public static final String STATE_LATEST = "latest";

	private final String state;
	/** 最新的一章的id */
	private final String sectionId;
	/** 最新的一章的名字 */
	private final String sectionName;
	/** 检查失败时的说明 */
	private final String message;

	private ComicUpdate(String state, String sectionId, String sectionName, String message) {
		this.state = state;
		this.sectionId = sectionId;
		this.sectionName = sectionName;
		this.message = message;
	}

	/** 正在检查更新 */
	public static ComicUpdate checking() {
		return new ComicUpdate(STATE_CHECKING, null, null, null);
	}

	/** 检查失败 */
	public static ComicUpdate failed(String message) {
		return new ComicUpdate(STATE_FAILED, null, null, message);
	}

	/**
	 * 查到了最新的一章。
	 *
	 * @param sectionId 最新的一章的id
	 * @param sectionName 最新的一章的名字，可以是null
	 */
	public static ComicUpdate latest(String sectionId, String sectionName) {
		return new ComicUpdate(STATE_LATEST, sectionId, sectionName, null);
	}

	public String getState() {
		return state;
	}

	/** 最新的一章的id */
	public String getSectionId() {
		return sectionId;
	}

	/** 最新的一章的名字 */
	public String getSectionName() {
		return sectionName;
	}

	/** 检查失败时的说明 */
	public String getMessage() {
		return message;
	}
}
