package com.raddle.comic.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Field;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.junit.Assert;
import org.junit.Test;

import com.raddle.comic.RecentViewInfo;
import com.raddle.comic.engine.ChannelInfo;
import com.raddle.comic.engine.SectionInfo;

/**
 * 打开对话框时，章节列表要滚到选中的那一章，而且每次打开都得一样。
 */
public class OpenComicDialogScrollTest {

	@Test
	public void 选中的章节滚到列表中间() throws Exception {
		checkSelectedInMiddle("50", "25", 25);
	}

	/**
	 * 连续打开两个漫画，第二个也要滚到选中的那一章。
	 * 列表原来停在哪里会影响滚动位置，所以每次打开都得重新算。
	 */
	@Test
	public void 第二次打开也一样滚到中间() throws Exception {
		checkSelectedInMiddle("50", "25", 25);
		checkSelectedInMiddle("30", "15", 15);
	}

	/**
	 * 按"从最近打开菜单打开对话框"的流程走一遍，看选中的那一章在不在中间。
	 *
	 * @param comicId 漫画id，测试脚本按这个数字造章节
	 * @param sectionId 最近看的那一章
	 * @param expectedIndex 这一章在列表里的下标
	 */
	private void checkSelectedInMiddle(String comicId, String sectionId, int expectedIndex) throws Exception {
		final OpenComicDialog dialog = new OpenComicDialog();
		// 测试里不能模态，模态的setVisible会一直阻塞
		dialog.setModal(false);
		try {
			final JComboBox<ChannelInfo> channelBox = (JComboBox<ChannelInfo>) getField(dialog, "channelBox");
			final DefaultListModel<SectionInfo> model = (DefaultListModel<SectionInfo>) getField(dialog, "sectionListModel");
			final JList<SectionInfo> list = (JList<SectionInfo>) getField(dialog, "sectionList");

			ChannelInfo testChannel = new ChannelInfo();
			testChannel.setName("测试渠道");
			testChannel.setScriptFile(new File("src/test/resources/channel/test-channel.js"));
			channelBox.addItem(testChannel);

			RecentViewInfo viewInfo = new RecentViewInfo();
			viewInfo.setChannelPath("src/test/resources/channel/test-channel.js");
			viewInfo.setComicId(comicId);
			viewInfo.setSectionId(sectionId);
			viewInfo.setPageNo(1);

			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.initRecentView(viewInfo);
					dialog.setVisible(true);
				}
			});
			// 等窗口显示出来，列表布局好
			Thread.sleep(800);

			Assert.assertEquals("选中的章节不对", expectedIndex, list.getSelectedIndex());

			final int[] values = new int[4];
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					// 每一行的高度，用渲染器算，和列表自己画的时候一样
					Component c = list.getCellRenderer().getListCellRendererComponent(list, model.get(0), 0, false, false);
					int rowHeight = c.getPreferredSize().height;
					// 选中的那一章在列表里的位置
					int selectedY = rowHeight * list.getSelectedIndex();
					// 当前能看到的那一段
					Rectangle visible = list.getVisibleRect();
					values[0] = rowHeight;
					values[1] = selectedY;
					values[2] = visible.y;
					values[3] = visible.height;
				}
			});
			int rowHeight = values[0];
			int selectedY = values[1];
			int visibleY = values[2];
			int visibleHeight = values[3];
			Assert.assertTrue("列表要有可视高度", visibleHeight > 0);
			Assert.assertTrue("选中的章节不在可视范围内", selectedY >= visibleY && selectedY + rowHeight <= visibleY + visibleHeight);
			// 大概在中间就行，差三行以内都算
			int offset = (selectedY - visibleY) - (visibleHeight - rowHeight) / 2;
			Assert.assertTrue("选中的章节不在中间，差了" + offset + "像素", Math.abs(offset) <= rowHeight * 3);
		} finally {
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.setVisible(false);
					dialog.dispose();
				}
			});
		}
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = OpenComicDialog.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	/**
	 * 从最近打开的菜单进来时，章节列表只填一遍，
	 * 选中的是最近看的那一章。
	 */
	@Test
	public void 最近打开的章节列表不重复() throws Exception {
		final OpenComicDialog dialog = new OpenComicDialog();
		try {
			final JComboBox<ChannelInfo> channelBox = (JComboBox<ChannelInfo>) getField(dialog, "channelBox");
			final DefaultListModel<SectionInfo> model = (DefaultListModel<SectionInfo>) getField(dialog, "sectionListModel");
			final JList<SectionInfo> list = (JList<SectionInfo>) getField(dialog, "sectionList");

			ChannelInfo testChannel = new ChannelInfo();
			testChannel.setName("测试渠道");
			testChannel.setScriptFile(new File("src/test/resources/channel/test-channel.js"));
			channelBox.addItem(testChannel);

			RecentViewInfo viewInfo = new RecentViewInfo();
			viewInfo.setChannelPath("src/test/resources/channel/test-channel.js");
			viewInfo.setComicId("2");
			viewInfo.setSectionId("1");
			viewInfo.setPageNo(1);

			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.initRecentView(viewInfo);
				}
			});

			// 测试脚本里有两章，列表里就该是两条，不能重复填
			Assert.assertEquals("章节列表重复了", 2, model.size());
			Assert.assertEquals(2, dialog.getSections().size());
			// 最近看的是第一章，列表里第二章在最前面，所以第一章的下标是1
			Assert.assertEquals(1, list.getSelectedIndex());
			Assert.assertEquals("第1章", dialog.getSectionName());
		} finally {
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					dialog.dispose();
				}
			});
		}
	}
}
