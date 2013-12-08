/**
 * 
 */
package com.raddle.comic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.raddle.comic.engine.ChannelInfo;

/**
 * @author raddle
 * 
 */
public class RecentViewHelper {
	private static XMLConfiguration config = new XMLConfiguration();
	private static File configFile = new File(System.getProperty("user.home") + "/.comic-view/recent-view.xml");
	static {
		if (!configFile.getParentFile().exists()) {
			configFile.mkdirs();
		}
		if (!configFile.exists()) {
			save();
		} else {
			try {
				config.load(configFile);
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	private static void save() {
		try {
			config.save(configFile);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static void updateRecentView(ChannelInfo channelInfo, String comicId, String sectionId, Integer pageNo) {
		List<HierarchicalConfiguration> views = config.configurationsAt("recents.views.page");
		if (views == null) {
			config.addProperty("recents.views.page(-1).channel.path", channelInfo.getScriptFile().getAbsolutePath());
			config.addProperty("recents.views.page.channel.name", channelInfo.getName());
			config.addProperty("recents.views.page.comicId", comicId);
			config.addProperty("recents.views.page.sectionId", pageNo);
			config.addProperty("recents.views.page.pageNo", pageNo);
			config.addProperty("recents.views.page.time", System.currentTimeMillis());
		} else {
			for (HierarchicalConfiguration view : views) {
				if (channelInfo.getScriptFile().getAbsolutePath().equals(view.getProperty("channel.path"))
						&& comicId.equals(view.getProperty("comicId"))) {
					view.setProperty("pageNo", pageNo);
					view.setProperty("sectionId", sectionId);
					view.setProperty("time", System.currentTimeMillis());
					save();
					return;
				}
			}
			// 没有匹配的就新增
			config.addProperty("recents.views.page(-1).channel.path", channelInfo.getScriptFile().getAbsolutePath());
			config.addProperty("recents.views.page.channel.name", channelInfo.getName());
			config.addProperty("recents.views.page.comicId", comicId);
			config.addProperty("recents.views.page.sectionId", pageNo);
			config.addProperty("recents.views.page.pageNo", pageNo);
			config.addProperty("recents.views.page.time", System.currentTimeMillis());
		}
		save();
	}

	public static List<RecentViewInfo> getRecentViews() {
		List<HierarchicalConfiguration> views = config.configurationsAt("recents.views.page");
		List<RecentViewInfo> list = new ArrayList<RecentViewInfo>();
		if (views != null) {
			for (HierarchicalConfiguration view : views) {
				RecentViewInfo info = new RecentViewInfo();
				info.setChannelName(view.getString("channel.name"));
				info.setChannelPath(view.getString("channel.path"));
				info.setComicId(view.getString("comicId"));
				info.setSectionId(view.getString("sectionId"));
				info.setPageNo(view.getInt("pageNo"));
				info.setTime(view.getLong("time"));
				list.add(info);
			}
		}
		Collections.sort(list, new Comparator<RecentViewInfo>() {

			@Override
			public int compare(RecentViewInfo o1, RecentViewInfo o2) {
				return new Long(o2.getTime()).compareTo(new Long(o1.getTime()));
			}
		});
		return list;
	}
}
