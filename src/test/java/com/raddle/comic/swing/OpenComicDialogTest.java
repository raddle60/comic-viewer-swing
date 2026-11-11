package com.raddle.comic.swing;

import java.lang.reflect.Field;

import javax.swing.JList;
import javax.swing.JTextField;

import org.junit.Assert;
import org.junit.Test;

import com.raddle.comic.engine.SectionInfo;

public class OpenComicDialogTest {

	@Test
	public void 章节列表填充并回填章节id() throws Exception {
		OpenComicDialog dialog = new OpenComicDialog();
		Field modelField = OpenComicDialog.class.getDeclaredField("sectionListModel");
		modelField.setAccessible(true);
		@SuppressWarnings("unchecked")
		javax.swing.DefaultListModel<SectionInfo> model = (javax.swing.DefaultListModel<SectionInfo>) modelField.get(dialog);
		Field listField = OpenComicDialog.class.getDeclaredField("sectionList");
		listField.setAccessible(true);
		@SuppressWarnings("unchecked")
		JList<SectionInfo> list = (JList<SectionInfo>) listField.get(dialog);
		Field sectionIdField = OpenComicDialog.class.getDeclaredField("sectionIdTxt");
		sectionIdField.setAccessible(true);
		JTextField sectionIdTxt = (JTextField) sectionIdField.get(dialog);

		// 模拟“获取”之后填充的章节
		SectionInfo s1 = new SectionInfo();
		s1.setSectionId("1");
		s1.setName("第一章");
		SectionInfo s2 = new SectionInfo();
		s2.setSectionId("2");
		s2.setName("第二章");
		model.addElement(s1);
		model.addElement(s2);

		Assert.assertEquals(2, model.size());
		Assert.assertEquals(javax.swing.ListSelectionModel.SINGLE_SELECTION, list.getSelectionMode());

		list.setSelectedIndex(1);
		// 只填入章节id，不触发获取
		Assert.assertEquals("2", sectionIdTxt.getText());
		Assert.assertNull(dialog.getPageInfo());
	}
}
