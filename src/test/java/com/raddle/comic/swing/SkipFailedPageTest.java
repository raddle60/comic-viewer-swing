package com.raddle.comic.swing;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

/**
 * 一页图片连续下载失败多次，就问用户要不要跳过。
 * 跳过以后用一张 not found 的图代替，图片存到本地，下次不再去远程下载。
 */
public class SkipFailedPageTest {

	private static final int PAGE_COUNT = 20;

	/**
	 * 连续失败三次才问用户，失败两次不打扰用户。
	 */
	@Test
	public void 连续失败三次才问用户() throws Exception {
		final List<Integer> notified = Collections.synchronizedList(new ArrayList<Integer>());
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, alwaysFailLoader(), null);
		model.setDownloadFailedListener(new ContinuousScrollModel.DownloadFailedListener() {
			@Override
			public void onDownloadFailed(int pageNo, int failedCount) {
				notified.add(failedCount);
			}
		});

		model.loadPage(3);
		model.loadPage(3);
		Assert.assertEquals("失败两次还不该问用户", 0, notified.size());
		Assert.assertEquals(2, model.getFailedCount(3));

		model.loadPage(3);
		Assert.assertEquals("失败三次就该问用户了", 1, notified.size());
		Assert.assertEquals(Integer.valueOf(3), notified.get(0));

		// 用户不想跳过，把失败次数清掉
		model.resetFailedCount(3);
		Assert.assertEquals(0, model.getFailedCount(3));
	}

	/**
	 * 下好了以后失败次数要清掉，重新算。
	 */
	@Test
	public void 下好了失败次数清掉() throws Exception {
		final boolean[] ok = new boolean[] { false };
		ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
			@Override
			public boolean isCached(int pageNo) {
				return false;
			}

			@Override
			public Image load(int pageNo) throws Exception {
				if (ok[0]) {
					return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
				}
				throw new java.io.IOException("下载失败");
			}
		}, null);
		model.loadPage(3);
		model.loadPage(3);
		Assert.assertEquals(2, model.getFailedCount(3));

		ok[0] = true;
		Assert.assertNotNull(model.loadPage(3));
		Assert.assertEquals("下好了失败次数要清掉", 0, model.getFailedCount(3));
	}

	/**
	 * 跳过以后用本地存的 not found 图显示，不会再去远程下载。
	 */
	@Test
	public void 跳过以后不再去远程下载() throws Exception {
		final File notFoundFile = File.createTempFile("comic-notfound", ".png");
		try {
			ComicViewer.writeNotFoundImage(notFoundFile);
			final List<Integer> requested = Collections.synchronizedList(new ArrayList<Integer>());
			final boolean[] skipped = new boolean[] { false };
			final ContinuousScrollModel[] holder = new ContinuousScrollModel[1];
			ContinuousScrollModel model = new ContinuousScrollModel(PAGE_COUNT, new ContinuousScrollModel.PageLoader() {
				@Override
				public boolean isCached(int pageNo) {
					return skipped[0];
				}

				@Override
				public Image load(int pageNo) throws Exception {
					if (skipped[0]) {
						// 本地已经有 not found 的图了，从本地读
						return ImageIO.read(notFoundFile);
					}
					requested.add(pageNo);
					throw new java.io.IOException("下载失败");
				}
			}, null);
			holder[0] = model;
			model.setDownloadFailedListener(new ContinuousScrollModel.DownloadFailedListener() {
				@Override
				public void onDownloadFailed(int pageNo, int failedCount) {
					if (failedCount >= 3) {
						// 用户选择跳过：本地有 not found 的图了
						skipped[0] = true;
						holder[0].reloadPage(pageNo);
					}
				}
			});

			model.loadPage(3);
			model.loadPage(3);
			model.loadPage(3);
			Assert.assertEquals("失败三次以后就不该再请求了", 3, requested.size());

			// 这一页现在能显示了，用的是本地的 not found 图
			Assert.assertNotNull("跳过以后这一页要能显示", model.loadPage(3));
			Assert.assertEquals("跳过以后不再去远程下载", 3, requested.size());
		} finally {
			if (notFoundFile.exists()) {
				notFoundFile.delete();
			}
		}
	}

	/**
	 * not found 的图要能当图片读出来。
	 */
	@Test
	public void notfound图片能正常保存() throws Exception {
		File file = File.createTempFile("comic-notfound", ".jpg");
		try {
			ComicViewer.writeNotFoundImage(file);
			Assert.assertTrue("图片要存下来", file.exists());
			BufferedImage image = ImageIO.read(file);
			Assert.assertNotNull("存下来的要是一张能读的图片", image);
			Assert.assertEquals(600, image.getWidth());
			Assert.assertEquals(200, image.getHeight());
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}
	}

	private ContinuousScrollModel.PageLoader alwaysFailLoader() {
		return new ContinuousScrollModel.PageLoader() {
			@Override
			public boolean isCached(int pageNo) {
				return false;
			}

			@Override
			public Image load(int pageNo) throws Exception {
				throw new java.io.IOException("下载失败");
			}
		};
	}
}
