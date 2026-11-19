package com.raddle.comic.swing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.raddle.comic.engine.PageInfo;
import com.raddle.comic.engine.SectionInfo;

/**
 * 下载后续章节：失败的页跳过，接着下下一页；连续失败三页就停。
 */
public class DownloadChaptersTest {

	/** 假的下载器，记下下载了哪些页，指定哪些页会失败 */
	private class FakeDownloader implements OpenComicDialog.ChapterDownloader {

		/** 每一章有几页 */
		int pagesPerSection = 2;
		/** 哪些页会失败，页码从1开始 */
		List<Integer> failedPages = new ArrayList<Integer>();
		/** 实际请求下载的页 */
		List<String> requested = new ArrayList<String>();

		@Override
		public List<PageInfo> getPages(String comicId, String sectionId) {
			List<PageInfo> pages = new ArrayList<PageInfo>();
			for (int i = 1; i <= pagesPerSection; i++) {
				PageInfo page = new PageInfo();
				page.setPageNo(i);
				page.setPageUrl("http://test.local/" + sectionId + "/" + i + ".jpg");
				pages.add(page);
			}
			return pages;
		}

		@Override
		public boolean download(String comicId, String sectionId, PageInfo page) throws Exception {
			requested.add(sectionId + "/" + page.getPageNo());
			if (failedPages.contains(page.getPageNo())) {
				throw new java.io.IOException("下载失败");
			}
			return true;
		}
	}

	private List<SectionInfo> sections(int count) {
		List<SectionInfo> list = new ArrayList<SectionInfo>();
		for (int i = 0; i < count; i++) {
			SectionInfo section = new SectionInfo();
			section.setSectionId(String.valueOf(i + 1));
			section.setName("第" + (i + 1) + "章");
			list.add(section);
		}
		return list;
	}

	@Test
	public void 全部章节都下载() {
		FakeDownloader downloader = new FakeDownloader();
		OpenComicDialog dialog = new OpenComicDialog();
		int done = dialog.downloadChapters(downloader, "c1", sections(3));
		Assert.assertEquals("三章都要下完", 3, done);
		Assert.assertEquals("每一章两页", 6, downloader.requested.size());
	}

	@Test
	public void 失败的页跳过接着下() {
		FakeDownloader downloader = new FakeDownloader();
		// 第1页失败，其它都成功
		downloader.failedPages.add(1);
		OpenComicDialog dialog = new OpenComicDialog();
		int done = dialog.downloadChapters(downloader, "c1", sections(2));
		Assert.assertEquals("失败的页跳过，不能算连续失败", 2, done);
		// 每一章的第2页都要下
		Assert.assertTrue(downloader.requested.contains("1/2"));
		Assert.assertTrue(downloader.requested.contains("2/2"));
	}

	@Test
	public void 连续失败三页就停下来() {
		FakeDownloader downloader = new FakeDownloader();
		downloader.pagesPerSection = 5;
		// 开头连续三页失败
		downloader.failedPages.add(1);
		downloader.failedPages.add(2);
		downloader.failedPages.add(3);
		OpenComicDialog dialog = new OpenComicDialog();
		int done = dialog.downloadChapters(downloader, "c1", sections(1));
		Assert.assertEquals("连续失败三页就要停，这一章都没下完", 0, done);
		// 失败到第三页就不往下试了
		Assert.assertEquals("停在第三个失败的页", 3, downloader.requested.size());
	}

	@Test
	public void 连续失败三页中间成功就清零() {
		FakeDownloader downloader = new FakeDownloader();
		// 只有第一页会失败，其它都成功 → 永远不会连续失败三页
		downloader.failedPages.add(1);
		OpenComicDialog dialog = new OpenComicDialog();
		int done = dialog.downloadChapters(downloader, "c1", sections(4));
		Assert.assertEquals("成功一次失败次数就清零", 4, done);
	}

	@Test
	public void 章节列表为空就没什么可下的() {
		FakeDownloader downloader = new FakeDownloader();
		OpenComicDialog dialog = new OpenComicDialog();
		int done = dialog.downloadChapters(downloader, "c1", new ArrayList<SectionInfo>());
		Assert.assertEquals(0, done);
		Assert.assertEquals(0, downloader.requested.size());
	}

	@Test
	public void 确认框上的话要写清楚多少章() {
		// 章节列表是倒序的：第一个是最新的一章
		List<SectionInfo> list = new ArrayList<SectionInfo>();
		for (int i = 0; i < 954; i++) {
			SectionInfo section = new SectionInfo();
			section.setSectionId(String.valueOf(954 - i));
			section.setName("第" + (954 - i) + "章");
			list.add(section);
		}
		// 选中倒数第二章（故事里的第2章），往后下就是下到最新的一章
		String message = OpenComicDialog.buildDownloadMessage(list, list.size() - 2);
		Assert.assertTrue("要说明是一共多少章：" + message, message.contains("一共 953 章"));
		// 从哪一章到哪一章也要说清楚
		Assert.assertTrue("要说明从哪一章开始：" + message, message.contains("第2章"));
		Assert.assertTrue("要说明下到哪一章：" + message, message.contains("第954章"));

		// 选中第953章（列表里的第二个），往后下只有它和最新的一章，一共2章
		String message2 = OpenComicDialog.buildDownloadMessage(list, 1);
		Assert.assertTrue("要说明是一共多少章：" + message2, message2.contains("一共 2 章"));
		Assert.assertTrue("要说明从哪一章开始：" + message2, message2.contains("第953章"));
		Assert.assertTrue("要说明下到哪一章：" + message2, message2.contains("第954章"));
	}
}
