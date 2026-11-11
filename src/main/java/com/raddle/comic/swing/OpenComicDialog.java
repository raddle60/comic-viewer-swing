package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
	private JTextField lastSectionIdLeb;
	private JLabel comicNameLeb;
	private String sectionName;
	private JList<SectionInfo> sectionList;
	private final DefaultListModel<SectionInfo> sectionListModel = new DefaultListModel<SectionInfo>();
	/** 程序化设置选中项时，屏蔽列表选择事件，避免重复加载页面 */
	private boolean suppressSectionEvent = false;

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
		setBounds(100, 100, 773, 606);
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
						sectionListModel.clear();
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
		scrollPane.setBounds(10, 114, 751, 147);
		contentPanel.add(scrollPane);

		descTxt = new JTextArea();
		scrollPane.setViewportView(descTxt);

		JButton getBtn = new JButton("获取");
		getBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sectionListModel.clear();
				ChannelInfo selectedItem = (ChannelInfo) channelBox.getSelectedItem();
				if (selectedItem != null) {
					ComicPluginEngine pluginEngine = new ComicPluginEngine();
					try {
						pluginEngine.init(selectedItem.getScriptFile());
						if (StringUtils.isBlank(comicIdTxt.getText())) {
							JOptionPane.showMessageDialog(null, "没有填写漫画id");
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
						if(StringUtils.isEmpty(sectionIdTxt.getText())){
							sectionIdTxt.setText(sections.get(sections.size() - 1).getSectionId());
						}
						boolean matched = false;
						int matchedIndex = -1;
						for (int i = 0; i < sections.size(); i++) {
							SectionInfo sectionInfo = sections.get(i);
							if (sectionIdTxt.getText().equals(sectionInfo.getSectionId())) {
								sectionName = sectionInfo.getName();
								matched = true;
								matchedIndex = i;
								break;
							}
						}
						if (sections.size() > 0) {
							lastSectionIdLeb.setText("最后章节：" + sections.get(sections.size() - 1).getSectionId());
							if (StringUtils.isNotBlank(sections.get(sections.size() - 1).getName())) {
								lastSectionIdLeb.setText(lastSectionIdLeb.getText() + "(名称：" + sections.get(sections.size() - 1).getName() + ")");
							}
							for (SectionInfo sectionInfo : sections) {
								sectionListModel.addElement(sectionInfo);
							}
							if (matched) {
								suppressSectionEvent = true;
								try {
									sectionList.setSelectedIndex(matchedIndex);
									sectionList.ensureIndexIsVisible(matchedIndex);
								} finally {
									suppressSectionEvent = false;
								}
							}
						}
						if (!matched) {
							pageInfos = null;
							pageNoBox.removeAllItems();
							JOptionPane.showMessageDialog(null, "没有对应的章节信息");
							return;
						}
						if (!fillPageInfos(pluginEngine.getPages(comicIdTxt.getText(), sectionIdTxt.getText()))) {
							JOptionPane.showMessageDialog(null, "没有获得到页面信息");
							return;
						}
					} catch (Exception e1) {
						pageInfos = null;
						pageNoBox.removeAllItems();
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

		lastSectionIdLeb = new JTextField("");
		lastSectionIdLeb.setBounds(351, 57, 410, 21);
		lastSectionIdLeb.setEditable(false);
		contentPanel.add(lastSectionIdLeb);

		comicNameLeb = new JLabel("");
		comicNameLeb.setBounds(351, 35, 278, 15);
		contentPanel.add(comicNameLeb);
		
		JLabel sectionListLabel = new JLabel("章节列表（选中后自动填入章节id）");
		sectionListLabel.setBounds(10, 261, 300, 15);
		contentPanel.add(sectionListLabel);

		sectionList = new JList<SectionInfo>(sectionListModel);
		sectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sectionList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof SectionInfo) {
					SectionInfo sectionInfo = (SectionInfo) value;
					setText(sectionInfo.getSectionId() + " - " + StringUtils.defaultString(sectionInfo.getName()));
				}
				return this;
			}
		});
		sectionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() || suppressSectionEvent) {
					return;
				}
				SectionInfo selected = sectionList.getSelectedValue();
				if (selected != null) {
					// 只填入章节id，是否获取内容由用户点击"获取"决定
					sectionIdTxt.setText(selected.getSectionId());
					sectionName = selected.getName();
					// 章节已变，旧的页面信息不再对应当前章节，清空等待用户重新获取
					pageNoBox.removeAllItems();
					pageInfos = null;
				}
			}
		});

		JScrollPane sectionListScr = new JScrollPane(sectionList);
		sectionListScr.setBounds(10, 281, 751, 276);
		contentPanel.add(sectionListScr);
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
		sectionListModel.clear();
		ComicPluginEngine pluginEngine = new ComicPluginEngine();
		try {
			pluginEngine.init(((ChannelInfo) channelBox.getSelectedItem()).getScriptFile());
			ComicInfo comicInfo = pluginEngine.getSections(comicIdTxt.getText());
			if (comicInfo != null) {
				comicNameLeb.setText(StringUtils.defaultString(comicInfo.getComicName()));
				List<SectionInfo> sections = comicInfo.getSections();
				if (sections != null && sections.size() > 0) {
					int matchedIndex = -1;
					for (int i = 0; i < sections.size(); i++) {
						SectionInfo sectionInfo = sections.get(i);
						if (sectionIdTxt.getText().equals(sectionInfo.getSectionId())) {
							sectionName = sectionInfo.getName();
							matchedIndex = i;
						}
					}
					lastSectionIdLeb.setText("最后章节：" + sections.get(sections.size() - 1).getSectionId());
					if (StringUtils.isNotBlank(sections.get(sections.size() - 1).getName())) {
						lastSectionIdLeb.setText(lastSectionIdLeb.getText() + "(名称：" + sections.get(sections.size() - 1).getName() + ")");
					}
					for (SectionInfo sectionInfo : sections) {
						sectionListModel.addElement(sectionInfo);
					}
					if (matchedIndex >= 0) {
						suppressSectionEvent = true;
						try {
							sectionList.setSelectedIndex(matchedIndex);
							sectionList.ensureIndexIsVisible(matchedIndex);
						} finally {
							suppressSectionEvent = false;
						}
					}
				}
			}
			List<PageInfo> pages = pluginEngine.getPages(comicIdTxt.getText(), sectionIdTxt.getText());
			if (!fillPageInfos(pages)) {
				return;
			}
			for (int i = 0; i < pageNoBox.getItemCount(); i++) {
				PageInfo pageInfo = pageNoBox.getItemAt(i);
				if (pageInfo.getPageNo().equals(viewInfo.getPageNo())) {
					pageNoBox.setSelectedItem(pageInfo);
					break;
				}
			}
		} catch (Exception e) {
			logger.log(e.getMessage(), e);
		} finally {
			pluginEngine.close();
		}
	}

	/**
	 * 填充页码下拉框，返回是否有页面信息。
	 */
	private boolean fillPageInfos(List<PageInfo> pages) {
		pageNoBox.removeAllItems();
		pageInfos = null;
		if (pages == null || pages.isEmpty()) {
			return false;
		}
		pageInfos = pages;
		for (PageInfo pageInfo : pages) {
			pageNoBox.addItem(pageInfo);
		}
		return true;
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

	public String getSectionName() {
		return sectionName;
	}

	public Integer getPageNo() {
		return ((PageInfo) pageNoBox.getSelectedItem()).getPageNo();
	}
}
