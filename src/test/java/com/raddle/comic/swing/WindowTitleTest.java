package com.raddle.comic.swing;

import org.junit.Assert;
import org.junit.Test;

/**
 * 窗口标题由几个部分拼起来，各处只改自己关心的那一部分，
 * 改完统一按属性拼出来显示，不能把别的部分弄丢。
 */
public class WindowTitleTest {

	private static final String BASIC = "顶漫画 - 测试漫画 - 第一章 - 3/20";

	@Test
	public void 什么都没有就显示基本信息() {
		WindowTitle title = new WindowTitle();
		Assert.assertEquals(BASIC, ComicViewer.buildTitle(BASIC, title));
	}

	@Test
	public void 正在做的事会显示出来() {
		WindowTitle title = new WindowTitle();
		title.setStatus("正在翻页");
		Assert.assertEquals(BASIC + " - 正在翻页", ComicViewer.buildTitle(BASIC, title));
	}

	@Test
	public void 下载进度会显示出来() {
		WindowTitle title = new WindowTitle();
		title.setDownloadProgress("已下载 5/20");
		Assert.assertEquals(BASIC + " - 已下载 5/20", ComicViewer.buildTitle(BASIC, title));
	}

	@Test
	public void 失败原因盖掉正在做的事() {
		WindowTitle title = new WindowTitle();
		title.setStatus("loading");
		title.setFailedReason("第8页加载失败：网络超时");
		Assert.assertEquals(BASIC + " - 第8页加载失败：网络超时", ComicViewer.buildTitle(BASIC, title));
	}

	@Test
	public void 各部分都在的时候都要显示() {
		WindowTitle title = new WindowTitle();
		title.setStatus("正在翻页");
		title.setDownloadProgress("已下载 5/20");
		Assert.assertEquals(BASIC + " - 正在翻页 - 已下载 5/20", ComicViewer.buildTitle(BASIC, title));
	}
}
