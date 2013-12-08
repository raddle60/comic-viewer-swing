package com.raddle.comic;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.raddle.comic.engine.ChannelInfo;

public class RecentViewHelperTest {

	@Test
	public void testUpdateRecentView() {
		ChannelInfo channelInfo = new ChannelInfo();
		channelInfo.setName("gg");
		channelInfo.setScriptFile(new File("d:/xxx.js"));
		RecentViewHelper.updateRecentView(channelInfo, "xx", "ff", 1);
		List<RecentViewInfo> recentViews = RecentViewHelper.getRecentViews();
		System.out.println(recentViews.get(0).getTime());
	}

}
