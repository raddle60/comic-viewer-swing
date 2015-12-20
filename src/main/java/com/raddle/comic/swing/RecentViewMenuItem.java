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

    public RecentViewMenuItem(RecentViewInfo viewInfo, Map<String, String> comicUpdate) {
        String name = viewInfo.getChannelName() + " - " + StringUtils.defaultIfBlank(viewInfo.getComicName(), viewInfo.getComicId()) + " - " + StringUtils.defaultIfBlank(viewInfo.getSectionName(), viewInfo.getSectionId()) + " - "
                + viewInfo.getPageNo() + "/" + viewInfo.getMaxPageNo();
        setForeground(Color.BLACK);
        if (comicUpdate.containsKey(viewInfo.getComicId())) {
            String updateResult = comicUpdate.get(viewInfo.getComicId());
            if (updateResult.startsWith("updating") || updateResult.startsWith("failed")) {
                name = name + " " + updateResult;
            } else if (StringUtils.isNotEmpty(updateResult)) {
                if (!updateResult.equals(viewInfo.getSectionId())) {
                    name = name + " 有更新";
                    setForeground(Color.RED);
                }
            }
        }
        setText(name);
        this.viewInfo = viewInfo;
    }

    public RecentViewInfo getViewInfo() {
        return viewInfo;
    }

}
