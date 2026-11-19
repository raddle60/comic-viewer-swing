/**
 * 
 */
package com.raddle.comic.swing;

import java.awt.Color;
import java.util.Map;

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

    public RecentViewMenuItem(RecentViewInfo viewInfo, Map<String, ComicUpdate> comicUpdate) {
        String name = viewInfo.getChannelName() + " - " + StringUtils.defaultIfBlank(viewInfo.getComicName(), viewInfo.getComicId()) + " - " + StringUtils.defaultIfBlank(viewInfo.getSectionName(), viewInfo.getSectionId()) + " - "
                + viewInfo.getPageNo() + "/" + viewInfo.getMaxPageNo();
        setForeground(Color.BLACK);
        ComicUpdate update = comicUpdate.get(viewInfo.getComicId());
        if (update != null) {
            if (ComicUpdate.STATE_CHECKING.equals(update.getState())) {
                name = name + " " + update.getState();
            } else if (ComicUpdate.STATE_FAILED.equals(update.getState())) {
                name = name + " " + update.getState() + ": " + update.getMessage();
            } else if (StringUtils.isNotEmpty(update.getSectionId())
                    && !update.getSectionId().equals(viewInfo.getSectionId())) {
                // 有更新，把最新的一章显示出来
                name = name + " 最新章节 " + StringUtils.defaultIfBlank(update.getSectionName(), update.getSectionId());
                setForeground(Color.RED);
            }
        }
        setText(name);
        this.viewInfo = viewInfo;
    }

    public RecentViewInfo getViewInfo() {
        return viewInfo;
    }

}
