package com.raddle.comic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Assert;
import org.junit.Test;

/**
 * 配置文件的写入。
 *
 * 以前用的是 commons-configuration 自带的 save()，它每存一次文件就多出一批空行，
 * 程序每启动一次文件就胖一圈（recent-view.xml 曾经涨到一千多行，大部分是空行）。
 */
public class XmlConfigTest {

	@Test
	public void 存出来的文件没有空行() throws Exception {
		File file = File.createTempFile("comic-xmlconfig", ".xml");
		try {
			XmlConfig config = new XmlConfig();
			config.setProperty("view.isContinueView", "true");
			config.setProperty("view.zoomPercent", "100");
			config.saveTo(file);
			String content = read(file);
			Assert.assertFalse("文件里不该有空行：\n" + content, hasBlankLine(content));
			Assert.assertTrue("连续显示的值要写进去：\n" + content, content.contains("<isContinueView>true</isContinueView>"));
			Assert.assertTrue("缩放的值要写进去：\n" + content, content.contains("<zoomPercent>100</zoomPercent>"));
		} finally {
			file.delete();
		}
	}

	@Test
	public void 反复读写文件不会越长() throws Exception {
		File file = File.createTempFile("comic-xmlconfig", ".xml");
		try {
			XmlConfig config = new XmlConfig();
			config.addProperty("recents.views.page(-1).channel.path", "d:\\channels\\a.com.js");
			config.addProperty("recents.views.page.channel.name", "渠道A");
			config.addProperty("recents.views.page.comicId", "1");
			config.addProperty("recents.views.page.comicName", "漫画一");
			config.addProperty("recents.views.page.pageNo", "3");
			config.saveTo(file);

			int lastLength = -1;
			for (int i = 0; i < 5; i++) {
				XmlConfig c = new XmlConfig();
				c.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
				c.saveTo(file);
				int length = (int) file.length();
				if (lastLength >= 0) {
					Assert.assertEquals("第 " + i + " 次读写之后文件变大了", lastLength, length);
				}
				lastLength = length;
			}
			Assert.assertFalse("文件里不该有空行：\n" + read(file), hasBlankLine(read(file)));
		} finally {
			file.delete();
		}
	}

	@Test
	public void 存进去的值能读回来() throws Exception {
		File file = File.createTempFile("comic-xmlconfig", ".xml");
		try {
			XmlConfig config = new XmlConfig();
			config.addProperty("recents.views.page(-1).channel.path", "d:\\channels\\a.com.js");
			config.addProperty("recents.views.page.channel.name", "渠道A");
			config.addProperty("recents.views.page.comicId", "1");
			config.addProperty("recents.views.page.comicName", "漫画一");
			config.addProperty("recents.views.page.pageNo", "3");
			config.saveTo(file);

			XmlConfig c = new XmlConfig();
			c.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			List<HierarchicalConfiguration> pages = c.configurationsAt("recents.views.page");
			Assert.assertEquals("应该只有一条记录", 1, pages.size());
			Assert.assertEquals("漫画一", pages.get(0).getString("comicName"));
			Assert.assertEquals("渠道A", pages.get(0).getString("channel.name"));
			Assert.assertEquals("3", pages.get(0).getString("pageNo"));
		} finally {
			file.delete();
		}
	}

	@Test
	public void 新增和修改记录都写得对() throws Exception {
		File file = File.createTempFile("comic-xmlconfig", ".xml");
		try {
			XmlConfig config = new XmlConfig();
			config.addProperty("recents.views.page(-1).comicId", "1");
			config.addProperty("recents.views.page.comicName", "漫画一");
			config.saveTo(file);

			// 再加一条
			XmlConfig c = new XmlConfig();
			c.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			c.addProperty("recents.views.page(-1).comicId", "2");
			c.addProperty("recents.views.page.comicName", "漫画二");
			c.saveTo(file);

			// 改第一条
			XmlConfig c2 = new XmlConfig();
			c2.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			List<HierarchicalConfiguration> pages = c2.configurationsAt("recents.views.page");
			Assert.assertEquals("应该有两条记录", 2, pages.size());
			pages.get(0).setProperty("comicName", "改过的名字");
			c2.saveTo(file);

			XmlConfig c3 = new XmlConfig();
			c3.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			List<HierarchicalConfiguration> pages2 = c3.configurationsAt("recents.views.page");
			Assert.assertEquals("应该有两条记录", 2, pages2.size());
			Assert.assertEquals("改过的名字", pages2.get(0).getString("comicName"));
			Assert.assertEquals("漫画二", pages2.get(1).getString("comicName"));
			Assert.assertFalse("文件里不该有空行：\n" + read(file), hasBlankLine(read(file)));
		} finally {
			file.delete();
		}
	}

	@Test
	public void 旧版本写出来的空行文件会被清理掉() throws Exception {
		File file = File.createTempFile("comic-xmlconfig-old", ".xml");
		try {
			// 模拟老版本 save() 写出来的文件：标签之间一堆空行
			Writer w = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
			try {
				w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n");
				w.write("<configuration>\r\n    <recents>\r\n        \r\n        \r\n        <views>\r\n");
				w.write("            \r\n            <page>\r\n                <comicId>7</comicId>\r\n");
				w.write("            </page>\r\n            \r\n            <page>\r\n");
				w.write("                <comicId>8</comicId>\r\n            </page>\r\n");
				w.write("        </views>\r\n    </recents>\r\n</configuration>\r\n");
			} finally {
				w.close();
			}
			XmlConfig c = new XmlConfig();
			c.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			c.saveTo(file);
			String content = read(file);
			Assert.assertFalse("老文件里的空行应该被清掉：\n" + content, hasBlankLine(content));
			Assert.assertTrue("值要还在：\n" + content, content.contains("<comicId>7</comicId>"));
			Assert.assertTrue("值要还在：\n" + content, content.contains("<comicId>8</comicId>"));

			// 清理完再读回来，记录数不能变
			XmlConfig c2 = new XmlConfig();
			c2.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			List<HierarchicalConfiguration> pages = c2.configurationsAt("recents.views.page");
			Assert.assertEquals("记录数不能变", 2, pages.size());
			Assert.assertEquals("7", pages.get(0).getString("comicId"));
			Assert.assertEquals("8", pages.get(1).getString("comicId"));
		} finally {
			file.delete();
		}
	}

	@Test
	public void 读进来再改再存值都还在() throws Exception {
		File file = File.createTempFile("comic-xmlconfig", ".xml");
		try {
			// 先写一个已经有的配置文件
			XmlConfig first = new XmlConfig();
			first.setProperty("view.isContinueView", "true");
			first.setProperty("view.zoomPercent", "100");
			first.saveTo(file);

			// 读进来，改一个值，再存回去（程序里改配置的流程）
			XmlConfig c = new XmlConfig();
			c.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			c.setProperty("view.isSeamlessView", "true");
			c.saveTo(file);

			XmlConfig c2 = new XmlConfig();
			c2.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
			Assert.assertEquals("true", c2.getString("view.isContinueView"));
			Assert.assertEquals("true", c2.getString("view.isSeamlessView"));
			Assert.assertEquals("100", c2.getString("view.zoomPercent"));
			Assert.assertFalse("文件里不该有空行：\n" + read(file), hasBlankLine(read(file)));
		} finally {
			file.delete();
		}
	}

	private static String read(File file) throws Exception {		InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "utf-8");
		try {
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				sb.append((char) c);
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	private static boolean hasBlankLine(String content) {
		String[] lines = content.split("\r\n|\n");
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
