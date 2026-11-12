package com.raddle.comic.swing;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;

/**
 * 连续显示的滚动模型。
 *
 * 连续显示把每一页直接画在窗口里，页面之间留一点间隔，看起来就是一整条很长的内容。
 * 滚动位置用"页码 + 页内偏移"表示，它只跟内容有关，跟窗口里画了多少页没有关系，
 * 所以图片加载完、页面加载完，位置都不会变，画面也就不会跳。
 *
 * 没有加载出来的页不画，位置留在原地等它加载完，这样不会露出错位的画面。
 */
class ContinuousScrollModel {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ContinuousScrollModel.class));

	/** 页与页之间的间隔 */
	static final int INTERVAL_HEIGHT = 5;
	/** 实际用的间隔，"无缝显示"时为0 */
	private int intervalHeight = INTERVAL_HEIGHT;
	/** 预加载半径，当前页前后各预加载几页 */
	private int preloadRadius = 3;

	/** 加载某一页的图片 */
	interface PageLoader {
		/**
		 * @param pageNo 页码
		 * @return 图片
		 * @throws Exception 加载失败，异常信息就是给用户看的失败原因
		 */
		Image load(int pageNo) throws Exception;
	}

	/** 一页图片 */
	static class Page {
		final int pageNo;
		final Image image;
		final int width;
		final int height;

		Page(int pageNo, Image image) {
			this.pageNo = pageNo;
			this.image = image;
			this.width = image.getWidth(null);
			this.height = image.getHeight(null);
		}
	}

	/** 视口里要画的一页，带上它在窗口里的位置 */
	static class VisiblePage {
		final Page page;
		/** 相对窗口左边的位置 */
		final int x;
		/** 相对窗口上边的位置 */
		final int y;

		VisiblePage(Page page, int x, int y) {
			this.page = page;
			this.x = x;
			this.y = y;
		}
	}

	/** 本章总页数 */
	private int pageCount;
	/** 加载器 */
	private final PageLoader loader;
	/** 页加载好后的回调，用来重画界面 */
	private final Runnable onPageLoaded;
	/** 已加载的页缓存 */
	private final Map<Integer, Page> pageCache = new ConcurrentHashMap<Integer, Page>();
	/** 正在加载中的页，避免重复请求 */
	private final Set<Integer> loadingPages = ConcurrentHashMap.newKeySet();
	/** 连续下载：勾选后下载完一页接着下载下一页 */
	private volatile boolean continuousDownload = false;
	/** 连续下载的线程是否已经在跑了，避免起多条链 */
	private final AtomicBoolean continuousDownloading = new AtomicBoolean(false);
	/** 加载失败的页和失败原因 */
	private final Map<Integer, String> failedPages = new ConcurrentHashMap<Integer, String>();

	/** 视口顶部所在页 */
	private int scrollPage;
	/** 视口顶部在 scrollPage 页内的偏移 */
	private int scrollOffset;
	/**
	 * 下一页还没加载出来时，多滚出去的那点距离。
	 * 先记着，等下一页加载完再补上，用户的滚动就不会被吞掉。
	 */
	private int pendingOffset = 0;
	/** 渲染缩放比例，"适合高度"时把每页缩放到窗口高度 */
	private double scale = 1.0;

	ContinuousScrollModel(int pageCount, PageLoader loader, Runnable onPageLoaded) {
		this.pageCount = pageCount;
		this.loader = loader;
		this.onPageLoaded = onPageLoaded;
	}

	void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	/** 打开新章节时重置 */
	void reset(int pageNo) {
		pageCache.clear();
		loadingPages.clear();
		failedPages.clear();
		scrollPage = pageNo;
		scrollOffset = 0;
	}

	/** 跳到指定页，不在当前页时定位到该页顶部 */
	void gotoPage(int pageNo) {
		if (pageNo != scrollPage) {
			scrollOffset = 0;
		}
		scrollPage = pageNo;
	}

	/**
	 * 滚动，delta 为正表示向下滚（看下面的内容）。
	 */
	synchronized void scroll(int delta) {
		int oldPage = scrollPage;
		int oldOffset = scrollOffset;
		scrollOffset = scrollOffset + (int) (delta / scale);
		normalizeScroll();
		// 失败的页不会自动重试，网站有防刷，一直重试也成功不了。
		// 等用户又移动了图片，才把失败的标记清掉重试一次，
		// 这样画面上的报错也会跟着变成"正在加载"。
		if (scrollPage != oldPage || scrollOffset != oldOffset) {
			for (int p = scrollPage; p <= scrollPage + preloadRadius && p <= pageCount; p++) {
				failedPages.remove(p);
			}
		}
	}

	/** 把滚动位置调整到已加载的页面范围内 */
	private void normalizeScroll() {
		// 向下超出当前页
		while (true) {
			Page cur = pageCache.get(scrollPage);
			if (cur == null) {
				break;
			}
			if (scrollOffset < cur.height + intervalHeight) {
				break;
			}
			if (!pageCache.containsKey(scrollPage + 1)) {
				// 下一页还没加载出来，先停在这一页底部。
				// 多滚出去的距离记下来，等下一页加载完再补上，滚动不会被吞掉。
				int maxOffset = cur.height + intervalHeight - 1;
				if (scrollOffset > maxOffset) {
					pendingOffset = pendingOffset + (scrollOffset - maxOffset);
					scrollOffset = maxOffset;
				}
				break;
			}
			scrollOffset = scrollOffset - cur.height - intervalHeight;
			scrollPage = scrollPage + 1;
		}
		// 向上超出当前页
		while (scrollOffset < 0) {
			Page prev = pageCache.get(scrollPage - 1);
			if (prev == null) {
				// 上一页还没加载出来，先停在这一页顶部。
				// 多滚出去的距离记下来，等上一页加载完再补上，滚动不会被吞掉。
				pendingOffset = pendingOffset + scrollOffset;
				scrollOffset = 0;
				break;
			}
			scrollPage = scrollPage - 1;
			scrollOffset = scrollOffset + prev.height + intervalHeight;
		}
	}

	/** 视口顶部所在页，也就是当前正在看的页 */
	int getCurPageNo() {
		return scrollPage;
	}

	/** 视口顶部在当前页内的偏移 */
	int getScrollOffset() {
		return scrollOffset;
	}

	/**
	 * 设置页与页之间的间隔，"无缝显示"时传0，页和页就紧挨着拼在一起。
	 */
	void setSeamless(boolean seamless) {
		intervalHeight = seamless ? 0 : INTERVAL_HEIGHT;
	}

	/** 当前页与页之间的间隔 */
	int getIntervalHeight() {
		return intervalHeight;
	}

	/** 设置渲染缩放比例 */
	void setScale(double scale) {
		this.scale = scale <= 0 ? 1.0 : scale;
	}

	/**
	 * 视口里要画的页，每页给出它在窗口里的位置。
	 *
	 * 从当前页开始往下找，找到没加载出来的页就停，后面还没加载的页也画不出来。
	 *
	 * @param paneWidth 窗口宽度
	 * @param paneHeight 窗口高度
	 * @param panX 横向平移量
	 */
	List<VisiblePage> getVisiblePages(int paneWidth, int paneHeight, int panX) {
		List<VisiblePage> visible = new ArrayList<VisiblePage>();
		int maxWidth = getContentWidth();
		// 窗口比图片宽就居中显示
		int baseX = paneWidth > maxWidth ? (paneWidth - maxWidth) / 2 : 0;
		int y = 0 - (int) (scrollOffset * scale);
		int pageNo = scrollPage;
		while (y < paneHeight && pageNo <= pageCount) {
			Page page = pageCache.get(pageNo);
			if (page == null) {
				// 这一页还没加载出来，等加载完再画
				break;
			}
			visible.add(new VisiblePage(page, baseX + panX + (maxWidth - page.width) / 2, y));
			y = y + (int) ((page.height + intervalHeight) * scale);
			pageNo++;
		}
		return visible;
	}

	/** 当前页，已经加载出来才有 */
	Page getCurPage() {
		return pageCache.get(scrollPage);
	}

	/**
	 * 下一页加载出来后，把之前记下的滚动距离补上，位置接着往下走。
	 */
	private synchronized void applyPendingOffset() {
		if (pendingOffset == 0) {
			return;
		}
		Page cur = pageCache.get(scrollPage);
		if (cur == null) {
			return;
		}
		if (pendingOffset > 0) {
			// 往下滚：下一页加载出来了才能接着走
			if (!pageCache.containsKey(scrollPage + 1)) {
				return;
			}
			int extra = pendingOffset;
			pendingOffset = 0;
			// 记下的距离是从这一页的最后一个位置算起的，接着往下走
			scrollOffset = cur.height + intervalHeight - 1 + extra;
		} else {
			// 往上滚：上一页加载出来了才能接着走
			if (!pageCache.containsKey(scrollPage - 1)) {
				return;
			}
			int extra = pendingOffset;
			pendingOffset = 0;
			scrollOffset = extra;
		}
		normalizeScroll();
	}

	/** 内容的最宽宽度，用来横向居中和限制横向平移 */
	int getContentWidth() {
		int maxWidth = 0;
		for (Page page : pageCache.values()) {
			maxWidth = Math.max(maxWidth, page.width);
		}
		return maxWidth;
	}

	/**
	 * 当前页往后第一个还没加载出来的页，没有则返回 null。
	 * 用来提示"正在加载"。
	 */
	Integer getMissingPage() {
		for (int pageNo = scrollPage; pageNo <= pageCount && pageNo < scrollPage + preloadRadius; pageNo++) {
			if (!pageCache.containsKey(pageNo)) {
				return pageNo;
			}
		}
		return null;
	}

	/** 是否已经翻到本章最后一页的底部 */
	boolean isAtBottom() {
		if (scrollPage < pageCount) {
			return false;
		}
		Page cur = pageCache.get(scrollPage);
		if (cur == null) {
			return false;
		}
		return scrollOffset >= cur.height;
	}

	/** 是否已经翻到本章第一页的顶部 */
	boolean isAtTop() {
		return scrollPage <= 1 && scrollOffset <= 0;
	}

	/**
	 * 加载指定页，拿到就放进缓存。加载不到返回 null。
	 */
	Page loadPage(int pageNo) {
		Page page = pageCache.get(pageNo);
		if (page != null) {
			return page;
		}
		if (pageNo < 1 || pageNo > pageCount) {
			return null;
		}
		try {
			Image image = loader.load(pageNo);
			if (image == null) {
				// 加载失败要记下来，不然会一直重试
				failedPages.put(pageNo, "图片下载失败");
				notifyPageChanged();
				return null;
			}
			page = new Page(pageNo, image);
			pageCache.put(pageNo, page);
			failedPages.remove(pageNo);
			applyPendingOffset();
			if (onPageLoaded != null) {
				// 页放进缓存了，让界面重画一下
				onPageLoaded.run();
			}
			// 连续下载：这一页下完了接着下下一页
			startContinuousDownload(pageNo + 1);
			return page;
		} catch (Exception e) {
			logger.log(e.getMessage(), e);
			failedPages.put(pageNo, shortReason(e));
			notifyPageChanged();
			return null;
		}
	}

	/** 页的状态变了（加载好了或者失败了），通知界面更新 */
	private void notifyPageChanged() {
		if (onPageLoaded != null) {
			onPageLoaded.run();
		}
	}

	/** 把异常信息整理成一句短的话，给用户看 */
	private static String shortReason(Exception e) {
		String message = e.getMessage();
		if (message == null) {
			return e.getClass().getSimpleName();
		}
		if (message.contains("timed out")) {
			return "网络超时";
		}
		if (message.length() > 30) {
			return message.substring(0, 30) + "...";
		}
		return message;
	}

	/**
	 * 当前往后第一个加载失败的页和原因，没有则返回 null。
	 * 失败的页不会自动重试，要等用户滚动到它才会再试。
	 */
	String getFailedReason() {
		for (int pageNo = scrollPage; pageNo <= pageCount && pageNo < scrollPage + preloadRadius + 1; pageNo++) {
			String reason = failedPages.get(pageNo);
			if (reason != null) {
				return "第" + pageNo + "页加载失败：" + reason;
			}
		}
		return null;
	}

	/** 有没有页正在加载 */
	boolean isLoading() {
		return !loadingPages.isEmpty();
	}

	/**
	 * 设置连续下载。勾选后，远程下载完一页会接着下载下一页，
	 * 直到本章下载完或者中途出错。
	 */
	void setContinuousDownload(boolean continuousDownload) {
		this.continuousDownload = continuousDownload;
	}

	/**
	 * 连续下载：从 fromPage 开始接着往下下，一直下到本章最后一页。
	 * 中途出错就停下来，把错误交给界面提示。
	 */
	private void startContinuousDownload(int fromPage) {
		if (!continuousDownload || fromPage > pageCount) {
			return;
		}
		if (!continuousDownloading.compareAndSet(false, true)) {
			// 已经有一条链在跑了，不用再起一条
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				logger.log("start continuous download");
				try {
					for (int pageNo = fromPage; pageNo <= pageCount; pageNo++) {
						if (!continuousDownload) {
							// 用户把连续下载关掉了
							break;
						}
						if (!loadingPages.add(pageNo)) {
							// 别的线程正在下这一页，跳过
							continue;
						}
						boolean ok;
						try {
							logger.log("continuous download ：" + pageNo);
							ok = downloadPage(pageNo);
						} finally {
							loadingPages.remove(pageNo);
						}
						if (!ok) {
							// 出错了，停下来，错误信息界面上会显示
							break;
						}
					}
				} finally {
					continuousDownloading.set(false);
					logger.log("end continuous download");
				}
			}
		}, "continuous-download").start();
	}

	/**
	 * 连续下载用：把这一页下到磁盘缓存里，但不留在内存中。
	 * 整章图片都留在内存里太占地方了，之后翻到这页再从磁盘读就行。
	 *
	 * @return 下载成功返回true，失败返回false
	 */
	private boolean downloadPage(int pageNo) {
		if (pageCache.containsKey(pageNo)) {
			// 内存里已经有了，不用再下
			return true;
		}
		try {
			Image image = loader.load(pageNo);
			if (image == null) {
				failedPages.put(pageNo, "图片下载失败");
				notifyPageChanged();
				return false;
			}
			failedPages.remove(pageNo);
			return true;
		} catch (Exception e) {
			logger.log(e.getMessage(), e);
			failedPages.put(pageNo, shortReason(e));
			notifyPageChanged();
			return false;
		}
	}

	/**
	 * 后台预加载当前页前后若干页，滚到边界时就不用等网络。
	 */
	void preload() {
		int center = scrollPage;
		for (int p = center - preloadRadius; p <= center + preloadRadius; p++) {
			if (p < 1 || p > pageCount) {
				continue;
			}
			if (pageCache.containsKey(p) || !loadingPages.add(p)) {
				continue;
			}
			// 失败过的页不自动重试，网站有防刷，一直重试也成功不了。
			// 等用户滚动到这一页时再试。
			if (failedPages.containsKey(p)) {
				loadingPages.remove(p);
				continue;
			}
			final int page = p;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						loadPage(page);
					} finally {
						loadingPages.remove(page);
					}
				}
			}, "preload-page-" + page).start();
		}
	}
}
