package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
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
	private JScrollPane sectionListScr;
	private final DefaultListModel<SectionInfo> sectionListModel = new DefaultListModel<SectionInfo>();
	/** 程序化设置选中项时，屏蔽列表选择事件，避免重复加载页面 */
	private boolean suppressSectionEvent = false;
	/** 要滚到列表中间的章节位置，等窗口显示出来之后再用 */
	private int pendingScrollIndex = -1;
	/** 列表还没布局好的时候要重试几次 */
	private int scrollRetry = 0;
	private static final int MAX_SCROLL_RETRY = 10;
	/** 选中的那一章往下滚几章，能让它落在可视区域的中间 */
	private static final int SCROLL_DOWN_ROWS = 6;
	/** 下载后续章节的按钮 */
	private JButton downloadBtn;
	/** 下载后续章节的进度，界面拿去显示在标题栏上 */
	private DownloadProgressListener progressListener;
	/** 下载后续章节的线程是不是在跑 */
	private volatile boolean downloading = false;
	/** 正在下载的漫画id，一个漫画同时只能有一条下载链 */
	private volatile String downloadingComicId = null;
	/** 连续失败多少页就停下来 */
	private static final int MAX_FAILED_IN_A_ROW = 3;

	/**
	 * 下载后续章节的进度回调。
	 */
	public interface DownloadProgressListener {
		/**
		 * @param progress 进度的字，比如"下载后续章节 3/10"，null表示下完了不显示了
		 */
		void onDownloadProgress(String progress);
	}

	/** 设置下载进度的回调 */
	public void setDownloadProgressListener(DownloadProgressListener listener) {
		this.progressListener = listener;
	}

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
							sectionIdTxt.setText(getLatestSection(sections).getSectionId());
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
							SectionInfo latestSection = getLatestSection(sections);
							lastSectionIdLeb.setText("最后章节：" + latestSection.getSectionId());
							if (StringUtils.isNotBlank(latestSection.getName())) {
								lastSectionIdLeb.setText(lastSectionIdLeb.getText() + "(名称：" + latestSection.getName() + ")");
							}
							for (SectionInfo sectionInfo : sections) {
								sectionListModel.addElement(sectionInfo);
							}
							if (matched) {
								suppressSectionEvent = true;
								try {
									sectionList.setSelectedIndex(matchedIndex);
								} finally {
									suppressSectionEvent = false;
								}
								// 把选中的章节滚到列表中间
								pendingScrollIndex = matchedIndex;
								scrollSelectedToMiddle();
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

		downloadBtn = new JButton("下载后续章节");
		downloadBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startDownloadChapters();
			}
		});
		downloadBtn.setBounds(446, 81, 120, 23);
		contentPanel.add(downloadBtn);

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
		this.sectionListScr = sectionListScr;
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

	/**
	 * 窗口每次显示出来都要做两件事：把选中的章节滚到中间，
	 * 再看一下下载的状态（下载是对话框关掉也在跑的，窗口再打开时按钮要接着是灰的）。
	 *
	 * 不能靠 windowOpened 那个事件，它只在窗口第一次显示的时候来一次，
	 * 第二次打开就滚不动了。setVisible 每次显示都会走这里。
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			// 模态窗口的 setVisible 会一直阻塞到窗口被关掉，
			// 所以要先把滚动的事丢到事件队列里，等窗口显示出来再处理
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					scrollSelectedToMiddle();
				}
			});
			updateDownloadButton();
		}
		super.setVisible(visible);
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
					SectionInfo latestSection = getLatestSection(sections);
					lastSectionIdLeb.setText("最后章节：" + latestSection.getSectionId());
					if (StringUtils.isNotBlank(latestSection.getName())) {
						lastSectionIdLeb.setText(lastSectionIdLeb.getText() + "(名称：" + latestSection.getName() + ")");
					}
					// 章节列表也填上，跟手动打开漫画时一样
					for (SectionInfo sectionInfo : sections) {
						sectionListModel.addElement(sectionInfo);
					}
					if (matchedIndex >= 0) {
						suppressSectionEvent = true;
						try {
							sectionList.setSelectedIndex(matchedIndex);
						} finally {
							suppressSectionEvent = false;
						}
						// 把选中的章节滚到列表中间
						pendingScrollIndex = matchedIndex;
						scrollSelectedToMiddle();
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
	 * 下载后续章节：从选中的章节开始，把后面所有章节的图片都下到本地缓存。
	 *
	 * 单独起一个线程，窗口还能用。某一页下不下来就跳过，接着下下一页；
	 * 连续失败三页了就停下来，剩下的不下了。
	 */
	private void startDownloadChapters() {
		if (downloading) {
			// 已经有一条下载链在跑了，一个漫画同时只能有一条
			return;
		}
		ChannelInfo channel = (ChannelInfo) channelBox.getSelectedItem();
		if (channel == null) {
			JOptionPane.showMessageDialog(this, "没有选择来源");
			return;
		}
		final String comicId = comicIdTxt.getText();
		if (StringUtils.isBlank(comicId)) {
			JOptionPane.showMessageDialog(this, "没有填写漫画id");
			return;
		}
		final List<SectionInfo> sections = getSections();
		if (sections.isEmpty()) {
			JOptionPane.showMessageDialog(this, "没有章节信息，先点获取");
			return;
		}
		// 从选中的那一章开始，后面的章节依次下
		int startIndex = -1;
		String sectionId = sectionIdTxt.getText();
		for (int i = 0; i < sections.size(); i++) {
			if (StringUtils.equals(sections.get(i).getSectionId(), sectionId)) {
				startIndex = i;
				break;
			}
		}
		if (startIndex < 0) {
			JOptionPane.showMessageDialog(this, "没有选中的章节");
			return;
		}
		// 告诉用户一共要下多少章，确认了再下
		int option = JOptionPane.showConfirmDialog(this, buildDownloadMessage(sections, startIndex), "提示", JOptionPane.YES_NO_OPTION);
		if (option != JOptionPane.YES_OPTION) {
			return;
		}
		downloading = true;
		downloadingComicId = comicId;
		updateDownloadButton();
		// 章节列表是倒序的（第一个是最新的一章），"往后的章节"在列表里
		// 是排在选中项前面的那些。这里从选中的那一章开始，一路往前下。
		final List<SectionInfo> toDownload = new ArrayList<SectionInfo>();
		for (int i = startIndex; i >= 0; i--) {
			toDownload.add(sections.get(i));
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				downloadChapters(channel, comicId, toDownload);
			}
		}, "download-chapters").start();
	}

	/**
	 * 下载中的时候按钮是灰的，标题也改一下，让用户看得出正在下载。
	 */
	private void updateDownloadButton() {
		downloadBtn.setEnabled(!downloading);
		downloadBtn.setText(downloading ? "下载中…" : "下载后续章节");
	}

	/**
	 * 把这些章节的图片都下到本地缓存。
	 *
	 * @param channel 渠道
	 * @param comicId 漫画id
	 * @param sections 要下的章节，从选中的那一章开始
	 */
	private void downloadChapters(ChannelInfo channel, String comicId, List<SectionInfo> sections) {
		ComicPluginEngine engine = new ComicPluginEngine();
		try {
			engine.init(channel.getScriptFile());
			int chaptersDone = downloadChapters(new EngineDownloader(engine), comicId, sections);
			if (chaptersDone >= sections.size()) {
				logger.log("后续章节下载完成 comicId[" + comicId + "] 共" + chaptersDone + "章");
			} else {
				logger.log("后续章节下载中断 comicId[" + comicId + "] 下了" + chaptersDone + "章，还剩" + (sections.size() - chaptersDone) + "章");
			}
		} catch (Exception e) {
			logger.log(e.getMessage(), e);
		} finally {
			engine.close();
			stopDownload();
		}
	}

	/**
	 * 下载这些章节的图片。
	 *
	 * 某一页下不下来就跳过，接着下下一页；连续失败三页了就停下来，剩下的不下了。
	 *
	 * @param downloader 下载用的
	 * @param comicId 漫画id
	 * @param sections 要下的章节
	 * @return 下完了多少章，比sections.size()小说明是中途停下来的
	 */
	int downloadChapters(ChapterDownloader downloader, String comicId, List<SectionInfo> sections) {
		int failedInARow = 0;
		int chaptersDone = 0;
		int pagesDone = 0;
		for (SectionInfo section : sections) {
			List<PageInfo> pages;
			try {
				pages = downloader.getPages(comicId, section.getSectionId());
			} catch (Exception e) {
				logger.log("获取页面信息失败 comicId[" + comicId + "] sectionId[" + section.getSectionId() + "] " + e.getMessage(), e);
				pages = new ArrayList<PageInfo>();
			}
			if (pages == null) {
				pages = new ArrayList<PageInfo>();
			}
			for (PageInfo page : pages) {
				boolean ok;
				try {
					ok = downloader.download(comicId, section.getSectionId(), page);
				} catch (Exception e) {
					ok = false;
					logger.log("下载图片失败，跳过 comicId[" + comicId + "] sectionId[" + section.getSectionId() + "] pageNo["
							+ page.getPageNo() + "] " + e.getMessage(), e);
				}
				if (ok) {
					failedInARow = 0;
					pagesDone++;
				} else {
					// 这一页下不来就跳过，接着下下一页
					failedInARow++;
				}
				if (failedInARow >= MAX_FAILED_IN_A_ROW) {
					// 连续失败这么多次了，停下来
					logger.log("连续失败" + failedInARow + "页，停止下载 comicId[" + comicId + "]");
					return chaptersDone;
				}
			}
			chaptersDone++;
			showDownloadProgress("下载后续章节 " + chaptersDone + "/" + sections.size() + " 已下载" + pagesDone + "页");
		}
		return chaptersDone;
	}

	/**
	 * 确认框上的话。
	 *
	 * 章节列表是倒序的（第一个是最新的一章），从选中的那一章往后下，
	 * 下到列表的第一个（最新的一章）为止。
	 *
	 * 数字是"一共要下多少章"，不是一个章号，所以要把从哪一章到哪一章也说清楚，
	 * 不然看到个数字会以为是下到第几章。
	 *
	 * @param sections 章节列表
	 * @param startIndex 选中的那一章在列表里的位置
	 * @return 确认框上的话
	 */
	static String buildDownloadMessage(List<SectionInfo> sections, int startIndex) {
		int count = startIndex + 1;
		SectionInfo selected = sections.get(startIndex);
		SectionInfo newest = sections.get(0);
		return "从「" + StringUtils.defaultIfBlank(selected.getName(), selected.getSectionId()) + "」开始，一直下到「"
				+ StringUtils.defaultIfBlank(newest.getName(), newest.getSectionId()) + "」，一共 " + count + " 章，是否继续？";
	}

	/**
	 * 下载用的东西。测试的时候可以换成假的。
	 */
	interface ChapterDownloader {
		/**
		 * 取这一章的页面信息。
		 */
		List<PageInfo> getPages(String comicId, String sectionId) throws Exception;

		/**
		 * 下载这一页，成功返回true，失败返回false。
		 */
		boolean download(String comicId, String sectionId, PageInfo page) throws Exception;
	}

	/** 用插件引擎来下载 */
	private static class EngineDownloader implements ChapterDownloader {

		private final ComicPluginEngine engine;

		EngineDownloader(ComicPluginEngine engine) {
			this.engine = engine;
		}

		@Override
		public List<PageInfo> getPages(String comicId, String sectionId) {
			// 后台线程里调引擎，Rhino的Context是跟线程走的，要自己进一个
			Context.enter();
			try {
				return engine.getPages(comicId, sectionId);
			} finally {
				Context.exit();
			}
		}

		@Override
		public boolean download(String comicId, String sectionId, PageInfo page) {
			// loadRemoteImage自己管Context，不用在外面进
			engine.loadRemoteImage(comicId, sectionId, page.getPageNo(), page.getPageUrl());
			return true;
		}
	}

	/** 下载完了（或者中途停了），恢复按钮，标题上不再显示进度 */
	private void stopDownload() {
		downloading = false;
		downloadingComicId = null;
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateDownloadButton();
				showDownloadProgress(null);
			}
		});
	}

	/** 把进度告诉界面，界面拿去显示在标题栏上 */
	private void showDownloadProgress(final String progress) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				// 对话框自己的标题栏上也显示一下
				setTitle(progress == null ? "" : progress);
				if (progressListener != null) {
					progressListener.onDownloadProgress(progress);
				}
			}
		});
	}

	/**
	 * 把选中的章节滚到列表可视区域的中间。
	 *
	 * 滚到选中的那一章后面几章，选中的那一章就大致在中间了。
	 * 一屏能看十几章，往下滚六章，选中的那一章差不多就在中间。
	 *
	 * 窗口刚显示出来的时候列表还没布局好，高度是0，这时滚不了，等一下再来。
	 */
	private void scrollSelectedToMiddle() {
		int index = pendingScrollIndex;
		if (index < 0 || index >= sectionListModel.size()) {
			return;
		}
		if (sectionListScr.getViewport().getExtentSize().height <= 0) {
			// 窗口还没显示出来，列表没有高度，滚不了，等一下再滚
			if (scrollRetry < MAX_SCROLL_RETRY) {
				scrollRetry++;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						scrollSelectedToMiddle();
					}
				});
			}
			return;
		}
		// 先回到顶部。ensureIndexIsVisible 只会滚最少的长度，
		// 列表原来停在哪里，滚完的位置就不一样，每次打开的滚动位置得一样才说得过去。
		sectionList.ensureIndexIsVisible(0);
		// 再往下滚几章，选中的那一章就落在可视区域的中间了
		int target = Math.min(index + SCROLL_DOWN_ROWS, sectionListModel.size() - 1);
		sectionList.ensureIndexIsVisible(target);
		pendingScrollIndex = -1;
		scrollRetry = 0;
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

	/**
	 * 章节列表，顺序跟窗口里显示的一样。
	 */
	public List<SectionInfo> getSections() {
		List<SectionInfo> sections = new ArrayList<SectionInfo>();
		for (int i = 0; i < sectionListModel.size(); i++) {
			sections.add(sectionListModel.get(i));
		}
		return sections;
	}

	/**
	 * 最新的一章。
	 * 站点给的章节列表是倒序的，最新的一话在最前面，所以第一个就是最新的。
	 *
	 * @param sections 章节列表
	 * @return 最新的一章
	 */
	private static SectionInfo getLatestSection(List<SectionInfo> sections) {
		return sections.get(0);
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
