package com.raddle.comic.swing;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.raddle.comic.RecentViewInfo;

/**
 * "最近打开"菜单里显示更新情况。
 */
public class RecentViewMenuItemTest {

	private RecentViewInfo viewInfo(String sectionId, String sectionName) {
		RecentViewInfo info = new RecentViewInfo();
		info.setChannelName("顶漫画");
		info.setComicName("测试漫画");
		info.setComicId("c1");
		info.setSectionId(sectionId);
		info.setSectionName(sectionName);
		info.setPageNo(1);
		info.setMaxPageNo(10);
		return info;
	}

	private Map<String, ComicUpdate> updateMap(ComicUpdate update) {
		Map<String, ComicUpdate> map = new HashMap<String, ComicUpdate>();
		map.put("c1", update);
		return map;
	}

	@Test
	public void 有更新显示最新章节() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("1", "第一章"),
				updateMap(ComicUpdate.latest("2", "第2话")));
		Assert.assertTrue("要显示最新章节：" + item.getText(), item.getText().contains("最新章节 第2话"));
		Assert.assertEquals("有更新要显示成红色", Color.RED, item.getForeground());
	}

	@Test
	public void 最新章节没有名字就用id() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("1", "第一章"),
				updateMap(ComicUpdate.latest("2", null)));
		Assert.assertTrue("要显示最新章节：" + item.getText(), item.getText().contains("最新章节 2"));
	}

	@Test
	public void 已经是最新的那一章就不显示更新() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("2", "第2话"),
				updateMap(ComicUpdate.latest("2", "第2话")));
		Assert.assertFalse("不该显示最新章节：" + item.getText(), item.getText().contains("最新章节"));
		Assert.assertEquals(Color.BLACK, item.getForeground());
	}

	@Test
	public void 正在检查更新() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("1", "第一章"), updateMap(ComicUpdate.checking()));
		Assert.assertTrue("要显示检查状态：" + item.getText(), item.getText().contains("updating"));
	}

	@Test
	public void 检查失败() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("1", "第一章"), updateMap(ComicUpdate.failed("网络超时")));
		Assert.assertTrue("要显示失败状态：" + item.getText(), item.getText().contains("failed"));
		Assert.assertTrue("要显示失败原因：" + item.getText(), item.getText().contains("网络超时"));
	}

	@Test
	public void 没有检查结果就正常显示() {
		RecentViewMenuItem item = new RecentViewMenuItem(viewInfo("1", "第一章"), new HashMap<String, ComicUpdate>());
		Assert.assertFalse("不该显示更新：" + item.getText(), item.getText().contains("最新章节"));
		Assert.assertEquals(Color.BLACK, item.getForeground());
	}
}
