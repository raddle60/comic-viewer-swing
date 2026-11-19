package com.raddle.comic.swing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.raddle.comic.RecentViewInfo;
import com.raddle.comic.engine.ComicPluginEngine;

/**
 * 「最近打开」菜单里，渠道脚本已经不在了的记录，菜单项要禁用掉。
 */
public class RecentViewChannelMissingTest {

	private RecentViewInfo viewInfo(String channelPath) {
		RecentViewInfo info = new RecentViewInfo();
		info.setChannelName("顶漫画");
		info.setChannelPath(channelPath);
		info.setComicName("测试漫画");
		info.setComicId("c1");
		info.setSectionId("1");
		info.setSectionName("第一章");
		info.setPageNo(1);
		info.setMaxPageNo(10);
		return info;
	}

	private Set<String> scriptNames(String... names) {
		Set<String> set = new HashSet<String>();
		for (String name : names) {
			set.add(name);
		}
		return set;
	}

	@Test
	public void 渠道还在就正常显示() {
		RecentViewMenuItem item = ComicViewer.createRecentViewMenuItem(viewInfo("/tmp/channels/dingmanhua.com.js"),
				new HashMap<String, ComicUpdate>(), scriptNames("dingmanhua.com.js"));
		Assert.assertTrue("渠道还在，菜单项要能点", item.isEnabled());
		Assert.assertFalse("不该提示渠道不存在：" + item.getText(), item.getText().contains("渠道不存在"));
	}

	@Test
	public void 渠道没了就禁用菜单项() {
		RecentViewMenuItem item = ComicViewer.createRecentViewMenuItem(viewInfo("/tmp/channels/dingmanhua.com.js"),
				new HashMap<String, ComicUpdate>(), scriptNames("other.com.js"));
		Assert.assertFalse("渠道没了，菜单项要禁用", item.isEnabled());
		Assert.assertTrue("要说明渠道不存在：" + item.getText(), item.getText().contains("渠道不存在"));
		Assert.assertTrue("要说明是哪个脚本：" + item.getToolTipText(), item.getToolTipText().contains("dingmanhua.com.js"));
	}

	@Test
	public void 列出渠道脚本名字() throws IOException {
		File dir = File.createTempFile("channels", "test");
		Assert.assertTrue(dir.delete());
		Assert.assertTrue(dir.mkdirs());
		try {
			File script = new File(dir, "demo.com.js");
			FileOutputStream out = new FileOutputStream(script);
			out.write("// test".getBytes("utf-8"));
			out.close();

			Set<String> names = ComicPluginEngine.getChannelScriptNames(dir);
			Assert.assertTrue("要能列出脚本：" + names, names.contains("demo.com.js"));
		} finally {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					file.delete();
				}
			}
			dir.delete();
		}
	}
}
