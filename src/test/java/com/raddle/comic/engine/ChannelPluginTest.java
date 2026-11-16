package com.raddle.comic.engine;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * 渠道脚本都要能正常加载。脚本写错了这里就会报错。
 */
public class ChannelPluginTest {

	@Test
	public void 所有渠道脚本都能加载() {
		List<ChannelInfo> channels = ComicPluginEngine.getChannelList(new File("channels"));
		Assert.assertFalse("没有加载到渠道脚本", channels.isEmpty());
		boolean found = false;
		for (ChannelInfo channel : channels) {
			if ("漫画160".equals(channel.getName())) {
				found = true;
			}
		}
		Assert.assertTrue("漫画160的渠道脚本没加载到", found);
	}
}
