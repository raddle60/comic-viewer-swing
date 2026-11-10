package com.raddle.comic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * 有些站点的图片是 webp，JDK 自带的 ImageIO 不支持，靠 TwelveMonkeys 的插件解码
 */
public class WebpTest {

	@Test
	public void testReadWebp() throws Exception {
		InputStream inputStream = WebpTest.class.getResourceAsStream("/webp-sample.webp");
		assertNotNull(inputStream);
		BufferedImage image = ImageIO.read(inputStream);
		assertNotNull(image);
		assertEquals(64, image.getWidth());
		assertEquals(64, image.getHeight());
	}
}
