/**
 * 
 */
package com.raddle.comic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

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
				config.load(new InputStreamReader(new FileInputStream(configFile), "utf-8"));
			} catch (Exception e) {
				LoggerFactory.getLogger(RecentViewHelper.class).error(e.getMessage(), e);
			}
		}
	}

	private static void save() {
		try {
			Writer os = new OutputStreamWriter(new FileOutputStream(configFile), "utf-8");
			config.save(os);
			os.close();
		} catch (Exception e) {
			LoggerFactory.getLogger(RecentViewHelper.class).error(e.getMessage(), e);
		}
	}

	public static void updateRecentView(ChannelInfo channelInfo, String comicId, String sectionId, Integer pageNo, Integer maxPageNo) {
		List<HierarchicalConfiguration> views = config.configurationsAt("recents.views.page");
		if (views == null) {
			addNewViewInfo(channelInfo, comicId, sectionId, pageNo, maxPageNo);
		} else {
			for (HierarchicalConfiguration view : views) {
				if (channelInfo.getScriptFile().getName().equals(FilenameUtils.getName(view.getProperty("channel.path") + ""))
						&& comicId.equals(view.getProperty("comicId"))) {
					view.setProperty("pageNo", pageNo);
					view.setProperty("sectionId", sectionId);
					view.setProperty("time", System.currentTimeMillis());
					save();
					return;
				}
			}
			// 没有匹配的就新增
			addNewViewInfo(channelInfo, comicId, sectionId, pageNo, maxPageNo);
		}
		save();
	}

	private static void addNewViewInfo(ChannelInfo channelInfo, String comicId, String sectionId, Integer pageNo, Integer maxPageNo) {
		config.addProperty("recents.views.page(-1).channel.path", channelInfo.getScriptFile().getAbsolutePath());
		config.addProperty("recents.views.page.channel.name", channelInfo.getName());
		config.addProperty("recents.views.page.comicId", comicId);
		config.addProperty("recents.views.page.sectionId", sectionId);
		config.addProperty("recents.views.page.pageNo", pageNo);
		config.addProperty("recents.views.page.maxPageNo", maxPageNo);
		config.addProperty("recents.views.page.time", System.currentTimeMillis());
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
				info.setMaxPageNo(view.getInt("maxPageNo"));
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
		if (list.size() > 30) {
			list = list.subList(0, 30);
		}
		return list;
	}
}
