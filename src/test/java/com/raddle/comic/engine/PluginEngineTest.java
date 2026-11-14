package com.raddle.comic.engine;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;

/**
 * 插件引擎的测试。
 */
public class PluginEngineTest {

	private ComicPluginEngine newEngine() throws Exception {
		ComicPluginEngine engine = new ComicPluginEngine();
		engine.init(new File("src/test/resources/channel/test-channel.js"));
		return engine;
	}

	@Test
	public void 取章节列表() throws Exception {
		ComicPluginEngine engine = newEngine();
		try {
			// 测试脚本按漫画id造章节，id是3就造三章
			ComicInfo comicInfo = engine.getSections("3");
			Assert.assertEquals("测试漫画", comicInfo.getComicName());
			Assert.assertEquals(3, comicInfo.getSections().size());
			// 章节列表是倒序的，最新的一话在最前面
			Assert.assertEquals("3", comicInfo.getSections().get(0).getSectionId());
			Assert.assertEquals("第1章", comicInfo.getSections().get(2).getName());
		} finally {
			engine.close();
		}
	}

	@Test
	public void 取页面列表() throws Exception {
		ComicPluginEngine engine = newEngine();
		try {
			List<PageInfo> pages = engine.getPages("1", "2");
			Assert.assertEquals(2, pages.size());
			Assert.assertEquals(Integer.valueOf(1), pages.get(0).getPageNo());
			Assert.assertEquals("http://test.local/2/1.jpg", pages.get(0).getPageUrl());
			Assert.assertEquals("1.jpg", pages.get(0).getFilename());
		} finally {
			engine.close();
		}
	}

	/**
	 * 换章的时候页面信息是在后台线程取的，Rhino 的 Context 是跟线程走的，
	 * 所以那个线程要自己进一个 Context，退出的时候也要退掉。
	 */
	@Test
	public void 后台线程里取页面列表() throws Exception {
		final ComicPluginEngine engine = newEngine();
		final AtomicReference<List<PageInfo>> result = new AtomicReference<List<PageInfo>>();
		final AtomicReference<Exception> error = new AtomicReference<Exception>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Context.enter();
					try {
						result.set(engine.getPages("1", "2"));
					} finally {
						Context.exit();
					}
				} catch (Exception e) {
					error.set(e);
				}
			}
		}, "load-section");
		thread.start();
		thread.join(10000);
		Assert.assertFalse("后台线程没有结束", thread.isAlive());
		if (error.get() != null) {
			throw error.get();
		}
		Assert.assertNotNull(result.get());
		Assert.assertEquals(2, result.get().size());
		Assert.assertEquals("http://test.local/2/1.jpg", result.get().get(0).getPageUrl());
	}
}
