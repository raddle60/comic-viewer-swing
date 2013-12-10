package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;
import com.raddle.comic.RecentViewInfo;
import com.raddle.comic.engine.ChannelInfo;
import com.raddle.comic.engine.ComicInfo;
import com.raddle.comic.engine.ComicPluginEngine;
import com.raddle.comic.engine.PageInfo;
import com.raddle.comic.engine.SectionInfo;

public class OpenComicDialog extends JDialog {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(OpenComicDialog.class));
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField comicIdTxt;
	private JTextField sectionIdTxt;
	private JComboBox<ChannelInfo> channelBox;
	private JComboBox<PageInfo> pageNoBox;
	private List<PageInfo> pageInfos;
	private JTextArea descTxt;
	private JLabel lastSectionIdLeb;
	private JLabel comicNameLeb;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			OpenComicDialog dialog = new OpenComicDialog();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public OpenComicDialog() {
		setBounds(100, 100, 655, 421);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel lblNewLabel = new JLabel("来源");
		lblNewLabel.setBounds(10, 10, 54, 15);
		contentPanel.add(lblNewLabel);

		channelBox = new JComboBox<ChannelInfo>();
		channelBox.addItemListener(new ItemListener() {
			@Override
            public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (channelBox.getSelectedItem() != null) {
						ChannelInfo selectedItem = (ChannelInfo) channelBox.getSelectedItem();
						String msg = "主页：" + selectedItem.getHome();
						descTxt.setText(msg + "\n描述：\n" + selectedItem.getDesc());
						comicIdTxt.setText("");
						sectionIdTxt.setText("");
						pageNoBox.removeAllItems();
						pageInfos = null;
					}
				}
			}
		});
		channelBox.setBounds(74, 7, 267, 21);
		contentPanel.add(channelBox);

		JLabel lblid = new JLabel("漫画id");
		lblid.setBounds(10, 35, 54, 15);
		contentPanel.add(lblid);

		comicIdTxt = new JTextField();
		comicIdTxt.setBounds(74, 32, 267, 21);
		contentPanel.add(comicIdTxt);
		comicIdTxt.setColumns(10);

		JLabel lblid_1 = new JLabel("章节id");
		lblid_1.setBounds(10, 60, 54, 15);
		contentPanel.add(lblid_1);

		sectionIdTxt = new JTextField();
		sectionIdTxt.setBounds(74, 57, 267, 21);
		contentPanel.add(sectionIdTxt);
		sectionIdTxt.setColumns(10);

		JLabel label = new JLabel("页码");
		label.setBounds(10, 85, 54, 15);
		contentPanel.add(label);

		pageNoBox = new JComboBox<PageInfo>();
		pageNoBox.setBounds(74, 82, 267, 21);
		contentPanel.add(pageNoBox);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 114, 619, 229);
		contentPanel.add(scrollPane);

		descTxt = new JTextArea();
		scrollPane.setViewportView(descTxt);

		JButton getBtn = new JButton("获取");
		getBtn.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				ChannelInfo selectedItem = (ChannelInfo) channelBox.getSelectedItem();
				if (selectedItem != null) {
					ComicPluginEngine pluginEngine = new ComicPluginEngine();
					try {
						pluginEngine.init(selectedItem.getScriptFile());
						if (StringUtils.isBlank(comicIdTxt.getText())) {
							JOptionPane.showMessageDialog(null, "没有填写漫画id");
							return;
						}
						if (StringUtils.isBlank(sectionIdTxt.getText())) {
							JOptionPane.showMessageDialog(null, "没有填写章节id");
							return;
						}
						ComicInfo comicInfo = pluginEngine.getSections(comicIdTxt.getText());
						if (comicInfo == null) {
							JOptionPane.showMessageDialog(null, "没获得到漫画信息");
							return;
						}
						comicNameLeb.setText(StringUtils.defaultString(comicInfo.getComicName()));
						List<SectionInfo> sections = comicInfo.getSections();
						if (sections.size() == 0) {
							JOptionPane.showMessageDialog(null, "没获得到章节信息");
							return;
						}
						boolean matched = false;
						for (SectionInfo sectionInfo : sections) {
							if (sectionIdTxt.getText().equals(sectionInfo.getSectionId())) {
								matched = true;
								break;
							}
						}
						if (sections.size() > 0) {
                            lastSectionIdLeb.setText("最后章节：" + sections.get(sections.size() - 1).getSectionId());
                            if (StringUtils.isNotBlank(sections.get(sections.size() - 1).getName())) {
                                lastSectionIdLeb.setText(lastSectionIdLeb.getText() + "(" + sections.get(sections.size() - 1).getName() + ")");
                            }
						}
						if (!matched) {
							pageInfos = null;
							pageNoBox.removeAllItems();
							JOptionPane.showMessageDialog(null, "没有对应的章节信息");
							return;
						}
						List<PageInfo> pages = pluginEngine.getPages(comicIdTxt.getText(), sectionIdTxt.getText());
						pageNoBox.removeAllItems();
						pageInfos = null;
						if (pages != null && pages.size() == 0) {
							JOptionPane.showMessageDialog(null, "没有获得到页面信息");
							return;
						}
						pageInfos = pages;
						for (PageInfo pageInfo : pages) {
							pageNoBox.addItem(pageInfo);
						}
					} catch (Exception e1) {
						logger.log(e1.getMessage(), e1);
						JOptionPane.showMessageDialog(null, "获取页面信息失败," + e1.getMessage());
					} finally {
						pluginEngine.close();
					}
				} else {
					JOptionPane.showMessageDialog(null, "没有选择来源");
				}
			}
		});
		getBtn.setBounds(346, 81, 93, 23);
		contentPanel.add(getBtn);

		lastSectionIdLeb = new JLabel("");
		lastSectionIdLeb.setBounds(351, 60, 278, 15);
		contentPanel.add(lastSectionIdLeb);

		comicNameLeb = new JLabel("");
		comicNameLeb.setBounds(351, 35, 278, 15);
		contentPanel.add(comicNameLeb);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
                    public void actionPerformed(ActionEvent e) {
						if (pageNoBox.getSelectedItem() == null) {
							JOptionPane.showMessageDialog(null, "没有页码信息");
							return;
						}
						OpenComicDialog.this.setVisible(false);
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					@Override
                    public void actionPerformed(ActionEvent e) {
						pageInfos = null;
						OpenComicDialog.this.setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		// 填充channel
		List<ChannelInfo> channelList = ComicPluginEngine.getChannelList(new File("channels"));
		channelBox.removeAllItems();
		for (ChannelInfo channelInfo : channelList) {
			channelBox.addItem(channelInfo);
		}
		if (channelBox.getSelectedItem() != null) {
			ChannelInfo selectedItem = (ChannelInfo) channelBox.getSelectedItem();
			String msg = "主页：" + selectedItem.getHome();
			descTxt.setText(msg + "\n描述：\n" + selectedItem.getDesc());
		}
	}

	public void initRecentView(RecentViewInfo viewInfo) {
		for (int i = 0; i < channelBox.getItemCount(); i++) {
			ChannelInfo item = channelBox.getItemAt(i);
			if (item.getScriptFile().getName().equals(FilenameUtils.getName(viewInfo.getChannelPath()))) {
				channelBox.setSelectedItem(item);
				break;
			}
		}
		comicIdTxt.setText(viewInfo.getComicId());
		sectionIdTxt.setText(viewInfo.getSectionId());
		ComicPluginEngine pluginEngine = new ComicPluginEngine();
		try {
			pluginEngine.init(((ChannelInfo) channelBox.getSelectedItem()).getScriptFile());
			ComicInfo comicInfo = pluginEngine.getSections(comicIdTxt.getText());
			if (comicInfo != null) {
				comicNameLeb.setText(StringUtils.defaultString(comicInfo.getComicName()));
				List<SectionInfo> sections = comicInfo.getSections();
				if (sections != null && sections.size() > 0) {
					lastSectionIdLeb.setText("最后章节：" + sections.get(sections.size() - 1).getSectionId());
				}
			}
			List<PageInfo> pages = pluginEngine.getPages(comicIdTxt.getText(), sectionIdTxt.getText());
			pageNoBox.removeAllItems();
			if (pages != null && pages.size() == 0) {
				return;
			}
			pageInfos = pages;
			for (PageInfo pageInfo : pages) {
				pageNoBox.addItem(pageInfo);
				if (pageInfo.getPageNo().equals(viewInfo.getPageNo())) {
					pageNoBox.setSelectedItem(pageInfo);
				}
			}
		} catch (Exception e) {
			logger.log(e.getMessage(), e);
		} finally {
			pluginEngine.close();
		}
	}

	public ChannelInfo getChannelInfo() {
		return (ChannelInfo) channelBox.getSelectedItem();
	}

	public List<PageInfo> getPageInfo() {
		return pageInfos;
	}

	public String getComicId() {
		return comicIdTxt.getText();
	}

	public String getComicName() {
		return comicNameLeb.getText();
	}

	public String getSectionId() {
		return sectionIdTxt.getText();
	}

	public Integer getPageNo() {
		return ((PageInfo) pageNoBox.getSelectedItem()).getPageNo();
	}
}
