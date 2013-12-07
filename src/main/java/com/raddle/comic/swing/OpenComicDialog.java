package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;
import com.raddle.comic.engine.ChannelInfo;
import com.raddle.comic.engine.ComicPluginEngine;
import com.raddle.comic.engine.PageInfo;

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

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			OpenComicDialog dialog = new OpenComicDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public OpenComicDialog() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel lblNewLabel = new JLabel("来源");
		lblNewLabel.setBounds(10, 10, 54, 15);
		contentPanel.add(lblNewLabel);

		channelBox = new JComboBox<ChannelInfo>();
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
		scrollPane.setBounds(10, 114, 330, 105);
		contentPanel.add(scrollPane);

		JTextArea descTxt = new JTextArea();
		scrollPane.setViewportView(descTxt);

		JButton getBtn = new JButton("获取");
		getBtn.addActionListener(new ActionListener() {
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
						List<PageInfo> pages = pluginEngine.getPages(comicIdTxt.getText(), sectionIdTxt.getText());
						pageNoBox.removeAllItems();
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
		getBtn.setBounds(341, 81, 93, 23);
		contentPanel.add(getBtn);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
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
					public void actionPerformed(ActionEvent e) {
						OpenComicDialog.this.setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		// 填充channel
		List<ChannelInfo> channelList = ComicPluginEngine.getChannelList(new File("D:\\workspaces\\raddle\\playlist\\src\\main\\resources"));
		channelBox.removeAllItems();
		for (ChannelInfo channelInfo : channelList) {
			channelBox.addItem(channelInfo);
		}
		if (channelBox.getSelectedItem() != null) {
			ChannelInfo selectedItem = (ChannelInfo) channelBox.getSelectedItem();
			descTxt.setText(selectedItem.getDesc());
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

	public String getSectionId() {
		return sectionIdTxt.getText();
	}
}