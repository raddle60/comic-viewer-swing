package com.raddle.comic.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

/**
 * 放大图片时不能出现马赛克。
 */
public class ZoomRenderTest {

	/**
	 * 画一张两个像素的棋格图。
	 * 按像素点取样放大会把棋格变成大块，插值会产生中间色调。
	 */
	private BufferedImage sourceImage(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				g.setColor(((x + y) % 2 == 0) ? Color.BLACK : Color.WHITE);
				g.fillRect(x, y, 1, 1);
			}
		}
		g.dispose();
		return image;
	}

	/** 绘制一张放大后的图 */
	private BufferedImage render(BufferedImage src, int target, boolean smooth) {
		BufferedImage dst = new BufferedImage(target, target, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dst.createGraphics();
		if (smooth) {
			ContinuousScrollModel.Page page = new ContinuousScrollModel.Page(1, src);
			ContinuousScrollModel.VisiblePage visible = new ContinuousScrollModel.VisiblePage(page, 0, 0, target, target);
			ComicViewer.drawPage(g, visible, target, target);
		} else {
			// 以前的画法：默认按像素点取样
			g.drawImage(src, 0, 0, target, target, null);
		}
		g.dispose();
		return dst;
	}

	/**
	 * 相邻两个像素颜色一样的次数。
	 * 按像素点取样是把原图的一个像素复制好几个，所以会出现大量相同的像素（即像素块）。
	 */
	private int countColors(BufferedImage img) {
		java.util.Set<Integer> colors = new java.util.HashSet<Integer>();
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				colors.add(img.getRGB(x, y) & 0xFFFFFF);
			}
		}
		return colors.size();
	}

	@Test
	public void 放大后不出现马赛克() throws Exception {
		BufferedImage src = sourceImage(200, 200);
		int zoom = 150;
		int target = 200 * zoom / 100;

		BufferedImage smooth = render(src, target, true);
		BufferedImage blocky = render(src, target, false);
		int smoothColors = countColors(smooth);
		int blockyColors = countColors(blocky);
		System.out.println("插值后 " + smoothColors + " 种颜色，按像素点取样 " + blockyColors + " 种颜色");
		File out = new File("target/zoom-sample.png");
		ImageIO.write(smooth, "png", out);
		ImageIO.write(blocky, "png", new File("target/zoom-sample-blocky.png"));
		System.out.println("样图: " + out.getAbsolutePath());
		Assert.assertTrue("插值后应该比按点取样平滑", smoothColors > 10 && smoothColors > blockyColors * 5);
	}
}
