package com.raddle.comic.swing;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * 连续显示滚动的回归测试。
 *
 * 重点验证：滚动位置以"页码+页内偏移"保存，图片加载完、页面加载完，位置都不会变，画面也就不会跳。
 */
public class ContinuousScrollModelTest {

	private static final int PAGE_COUNT = 20;
	private static final int PAGE_HEIGHT = 1000;
	private static final int PAGE_WIDTH = 100;
	private static final int PANE_WIDTH = 800;
	private static final int PANE_HEIGHT = 800;

	/** 假页面，所有页一样高，读取瞬间完成 */
	private ContinuousScrollModel.PageLoader loader() {
		return new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) {
				return new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
			}
		};
	}

	/** 视口顶部在整章里的绝对位置 */
	private int absoluteOffset(ContinuousScrollModel model) {
		int page = model.getCurPageNo();
		return (page - 1) * (PAGE_HEIGHT + ContinuousScrollModel.INTERVAL_HEIGHT) + model.getScrollOffset();
	}

	@Test
	public void 连续向下滚动位置不跳跃() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, loader(), null);
		model.reset(3);
		model.loadPage(3);

		int prev = absoluteOffset(model);
		for (int i = 0; i < 100; i++) {
			model.scroll(50);
			// 下一页准备好了（真实流程里是后台预加载做的）
			model.loadPage(model.getCurPageNo() + 1);
			int now = absoluteOffset(model);
			Assert.assertEquals("第" + i + "步向下滚出现跳跃", prev + 50, now);
			prev = now;
		}
		// 一路滚到了后面的页
		Assert.assertTrue(model.getCurPageNo() > 3);
	}

	@Test
	public void 连续向上滚动位置不跳跃() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, loader(), null);
		model.reset(8);
		model.loadPage(8);

		int prev = absoluteOffset(model);
		for (int i = 0; i < 100; i++) {
			model.scroll(-50);
			// 上一页准备好了（真实流程里是后台预加载做的）
			model.loadPage(model.getCurPageNo() - 1);
			int now = absoluteOffset(model);
			Assert.assertEquals("第" + i + "步向上滚出现跳跃", prev - 50, now);
			prev = now;
		}
		Assert.assertTrue(model.getCurPageNo() < 8);
	}

	@Test
	public void 页加载出来后位置不变() {
		final List<Integer> loaded = new ArrayList<Integer>();
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) {
				// 只有先放入loaded的页才加载得出来
				if (!loaded.contains(pageNo)) {
					return null;
				}
				return new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
			}
		}, null);
		model.reset(1);
		loaded.add(1);
		model.loadPage(1);

		// 滚到第1页底部，第2页还没加载，位置先停住
		int start = absoluteOffset(model);
		for (int i = 0; i < 40; i++) {
			model.scroll(50);
		}
		Assert.assertEquals("没加载的页不该被翻过去", 1, model.getCurPageNo());
		int before = absoluteOffset(model);
		Assert.assertTrue("位置应该停在第一页底部", before > start);

		// 第2页加载出来了，之前滚过去的距离要补上，一分都不能少
		loaded.add(2);
		model.loadPage(2);
		Assert.assertEquals("页加载出来后用户滚的距离丢了", start + 40 * 50, absoluteOffset(model));
		Assert.assertEquals(2, model.getCurPageNo());
	}

	@Test
	public void 缺页时不会露出错位的画面() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) {
				// 只加载得到第1页，后面的页都拿不到
				return pageNo == 1 ? new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB) : null;
			}
		}, null);
		model.reset(1);
		model.loadPage(1);

		for (int i = 0; i < 40; i++) {
			model.scroll(50);
			List<ContinuousScrollModel.VisiblePage> visible = model.getVisiblePages(PANE_WIDTH, PANE_HEIGHT, 0);
			Assert.assertFalse("一 页都没画出来", visible.isEmpty());
			// 画出来的页要从当前页开始，且连续
			Assert.assertEquals("画出来的页不是从当前页开始的", model.getCurPageNo(), visible.get(0).page.pageNo);
			for (int k = 1; k < visible.size(); k++) {
				Assert.assertEquals("画出来的页不连续", visible.get(k - 1).page.pageNo + 1, visible.get(k).page.pageNo);
			}
			// 要么盖满窗口，要么是因为后面的页还没加载出来
			int lastBottom = visible.get(visible.size() - 1).y + visible.get(visible.size() - 1).page.height;
			if (lastBottom < PANE_HEIGHT) {
				Assert.assertNotNull("下面露了空白，又没有页在加载", model.getMissingPage());
			}
		}
	}

	@Test
	public void 加载失败的页不会自动重试() throws Exception {
		final List<Integer> requested = new ArrayList<Integer>();
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) throws Exception {
				requested.add(pageNo);
				if (pageNo == 2) {
					throw new java.io.IOException("Read timed out");
				}
				return new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
			}
		}, null);
		model.reset(1);
		model.loadPage(1);

		// 第2页加载失败
		model.loadPage(2);
		Assert.assertNotNull("加载失败没有记录原因", model.getFailedReason());
		Assert.assertTrue("失败原因里应该说明超时: " + model.getFailedReason(),
				model.getFailedReason().contains("超时"));

		// 失败后预加载不能再自动去试它
		int before = count(requested, 2);
		for (int i = 0; i < 20; i++) {
			model.preload();
		}
		Thread.sleep(200);
		Assert.assertEquals("失败的页被自动重试了", before, count(requested, 2));
	}

	@Test
	public void 用户移动图片后才重试失败的页() throws Exception {
		final List<Integer> requested = new ArrayList<Integer>();
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) throws Exception {
				requested.add(pageNo);
				if (pageNo == 2) {
					throw new java.io.IOException("Read timed out");
				}
				return new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
			}
		}, null);
		model.reset(1);
		model.loadPage(1);
		model.loadPage(2);
		int before = count(requested, 2);

		Assert.assertNotNull("加载失败应该记录原因", model.getFailedReason());

		// 用户移动了图片，报错要马上更新成"正在加载"，失败的页也可以再试一次
		model.scroll(50);
		Assert.assertNull("用户移动图片后报错没有更新", model.getFailedReason());
		model.preload();
		Thread.sleep(200);
		Assert.assertTrue("用户移动图片后应该重试一次", count(requested, 2) > before);
	}

	@Test
	public void 默认页与页之间有间隔() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, loader(), null);
		model.reset(1);
		model.loadPage(1);
		model.loadPage(2);
		// 窗口要够高才能同时看到两页
		List<ContinuousScrollModel.VisiblePage> visible = model.getVisiblePages(PANE_WIDTH, PAGE_HEIGHT * 3, 0);
		Assert.assertTrue(visible.size() >= 2);
		Assert.assertEquals("两页之间应该有间隔", visible.get(0).y + PAGE_HEIGHT + ContinuousScrollModel.INTERVAL_HEIGHT,
				visible.get(1).y);
	}

	@Test
	public void 无缝显示时页与页之间没有间隔() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, loader(), null);
		model.reset(1);
		model.loadPage(1);
		model.loadPage(2);
		model.setSeamless(true);
		Assert.assertEquals(0, model.getIntervalHeight());
		// 窗口要够高才能同时看到两页
		List<ContinuousScrollModel.VisiblePage> visible = model.getVisiblePages(PANE_WIDTH, PAGE_HEIGHT * 3, 0);
		Assert.assertTrue(visible.size() >= 2);
		// 一页的底部就是下一页的顶部，中间不留空
		Assert.assertEquals("两页之间不该有间隔", visible.get(0).y + PAGE_HEIGHT, visible.get(1).y);
	}

	@Test
	public void 窗口比图片宽时居中显示() {
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, loader(), null);
		model.reset(1);
		model.loadPage(1);
		List<ContinuousScrollModel.VisiblePage> visible = model.getVisiblePages(PANE_WIDTH, PANE_HEIGHT, 0);
		Assert.assertFalse(visible.isEmpty());
		// 图片宽100，窗口宽800，应该居中
		Assert.assertEquals((PANE_WIDTH - PAGE_WIDTH) / 2, visible.get(0).x);
	}

	private int count(List<Integer> list, int value) {
		int n = 0;
		for (Integer i : list) {
			if (i.intValue() == value) {
				n++;
			}
		}
		return n;
	}

	@Test
	public void 预加载会请求没加载的页() {
		final List<Integer> requested = new ArrayList<Integer>();
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public Image load(int pageNo) {
				requested.add(pageNo);
				return new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
			}
		}, null);
		model.reset(5);
		model.preload();
		// 预加载是后台线程，等它们都跑完
		for (int i = 0; i < 200 && requested.size() < 7; i++) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				break;
			}
		}
		Assert.assertTrue("预加载没有请求后面的页: " + requested, requested.contains(6));
		Assert.assertTrue("预加载没有请求前面的页: " + requested, requested.contains(4));
	}
}
