package com.raddle.comic.swing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * 缓存文件损坏（没下完整）时的处理：读不出来就把文件删掉，
 * 下次才会重新下载，不然会一直读这个坏文件。
 */
public class CacheFileTest {

	@Test
	public void 坏文件读不出来就删掉() throws Exception {
		File file = File.createTempFile("comic-broken", ".webp");
		try {
			OutputStream os = new FileOutputStream(file);
			try {
				// 不是图片的内容，读的时候肯定会失败
				os.write("this is not an image".getBytes());
			} finally {
				os.close();
			}
			try {
				ComicViewer.readCachedImage(file);
				Assert.fail("坏文件应该读不出来");
			} catch (IOException e) {
				// 预期的
			}
			Assert.assertFalse("损坏的缓存文件应该被删掉", file.exists());
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void 空文件读不出来就删掉() throws Exception {
		File file = File.createTempFile("comic-empty", ".webp");
		try {
			try {
				ComicViewer.readCachedImage(file);
				Assert.fail("空文件应该读不出来");
			} catch (IOException e) {
				// 预期的
			}
			Assert.assertFalse("空的缓存文件应该被删掉", file.exists());
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}
	}
}
