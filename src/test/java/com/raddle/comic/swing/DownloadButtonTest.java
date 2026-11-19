package com.raddle.comic.swing;

import java.awt.EventQueue;
import java.lang.reflect.Field;

import javax.swing.JButton;

import org.junit.Assert;
import org.junit.Test;

/**
 * 下载后续章节：一个漫画同时只能有一条下载链，
 * 下载中的时候按钮是灰的。
 */
public class DownloadButtonTest {

	private JButton downloadButton(OpenComicDialog dialog) throws Exception {
		Field field = OpenComicDialog.class.getDeclaredField("downloadBtn");
		field.setAccessible(true);
		return (JButton) field.get(dialog);
	}

	private void setDownloading(OpenComicDialog dialog, boolean downloading, String comicId) throws Exception {
		Field field = OpenComicDialog.class.getDeclaredField("downloading");
		field.setAccessible(true);
		field.setBoolean(dialog, downloading);
		Field comicField = OpenComicDialog.class.getDeclaredField("downloadingComicId");
		comicField.setAccessible(true);
		comicField.set(dialog, comicId);
	}

	@Test
	public void 没有下载的时候按钮能用() throws Exception {
		final OpenComicDialog dialog = new OpenComicDialog();
		dialog.setModal(false);
		try {
			setDownloading(dialog, false, null);
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.setVisible(true);
				}
			});
			Assert.assertTrue("没有下载的时候按钮能用", downloadButton(dialog).isEnabled());
			Assert.assertEquals("下载后续章节", downloadButton(dialog).getText());
		} finally {
			dispose(dialog);
		}
	}

	@Test
	public void 下载中的时候按钮是灰的() throws Exception {
		final OpenComicDialog dialog = new OpenComicDialog();
		dialog.setModal(false);
		try {
			setDownloading(dialog, true, "c1");
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.setVisible(true);
				}
			});
			// 对话框关掉再打开，按钮还是要灰的，不能让用户又起一条下载链
			Assert.assertFalse("下载中的时候按钮要是灰的", downloadButton(dialog).isEnabled());
			Assert.assertEquals("下载中…", downloadButton(dialog).getText());
		} finally {
			dispose(dialog);
		}
	}

	private void dispose(final OpenComicDialog dialog) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	}
}
