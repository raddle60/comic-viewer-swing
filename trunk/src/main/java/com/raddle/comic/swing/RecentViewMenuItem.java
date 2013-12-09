/**
 * 
 */
package com.raddle.comic.swing;

import javax.swing.JMenuItem;

import org.apache.commons.lang.StringUtils;

import com.raddle.comic.RecentViewInfo;

/**
 * @author raddle
 * 
 */
public class RecentViewMenuItem extends JMenuItem {
	private static final long serialVersionUID = 1L;
	private RecentViewInfo viewInfo;

	public RecentViewMenuItem(RecentViewInfo viewInfo) {
		super(viewInfo.getComicId() + " - " + StringUtils.defaultIfBlank(viewInfo.getComicName(), viewInfo.getSectionId()) + " - "
				+ viewInfo.getPageNo() + "/" + viewInfo.getMaxPageNo());
		this.viewInfo = viewInfo;
	}

	public RecentViewInfo getViewInfo() {
		return viewInfo;
	}

}
