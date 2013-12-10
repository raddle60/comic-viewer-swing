package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;
import com.raddle.comic.RecentViewHelper;
import com.raddle.comic.RecentViewInfo;
import com.raddle.comic.engine.ChannelInfo;
import com.raddle.comic.engine.ComicInfo;
import com.raddle.comic.engine.ComicPluginEngine;
import com.raddle.comic.engine.PageInfo;
import com.raddle.comic.engine.SectionInfo;

public class ComicViewer {
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
	}
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ComicViewer.class));
	private JFrame frame;
	private JPanel picPane;
	private Point pressedPoint;
	private Point picStartPoint = new Point();
	private OpenComicDialog openComicDialog = new OpenComicDialog();
	private Image image;
	private ComicPluginEngine picEngine;
	private Map<Integer, PageInfo> pageMap = new HashMap<Integer, PageInfo>();
	private List<SectionInfo> sectionList;
	private String comicId;
	private String comicName;
	private String sectionId;
	private String sectionName;
	private ChannelInfo channelInfo;
	private Integer pageNo;
	private boolean loading = false;
	private boolean isFullScreen = false;
	private JMenuBar menuBar;
	private JMenuItem mntmNewMenuItem;
	private JMenu recentViewmenu;
	private JMenu menu_1;
	private JMenuItem menuItem_1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final ComicViewer window = new ComicViewer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ComicViewer() {
		createFrame();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void createFrame() {
		frame = new JFrame();
		frame.setBounds(100, 100, 651, 467);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);

		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu menu = new JMenu("文件");
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem("打开");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openComicDialog.setModal(true);
				openComicDialog.setVisible(true);
				openComic();
			}
		});
		menu.add(menuItem);

		recentViewmenu = new JMenu("最近打开");
		recentViewmenu.addMenuListener(new MenuListener() {
			@Override
			public void menuCanceled(MenuEvent e) {
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuSelected(MenuEvent e) {
				if (recentViewmenu.getComponentCount() > 0) {
					RecentViewMenuItem component = (RecentViewMenuItem) recentViewmenu.getComponent(0);
					List<RecentViewInfo> recentViews = RecentViewHelper.getRecentViews();
					if (recentViews.size() > 0) {
						if (recentViews.get(0).getTime() > component.getViewInfo().getTime()) {
							recentViewmenu.removeAll();
							for (RecentViewInfo recentViewInfo : recentViews) {
								final RecentViewMenuItem recentItem = new RecentViewMenuItem(recentViewInfo);
								recentItem.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										openComicDialog.initRecentView(recentItem.getViewInfo());
										openComicDialog.setModal(true);
										openComicDialog.setVisible(true);
										openComic();
									}
								});
								recentViewmenu.add(recentItem);
							}
						}
					}
				} else {
					recentViewmenu.removeAll();
					List<RecentViewInfo> recentViews = RecentViewHelper.getRecentViews();
					for (RecentViewInfo recentViewInfo : recentViews) {
						final RecentViewMenuItem recentItem = new RecentViewMenuItem(recentViewInfo);
						recentItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								openComicDialog.initRecentView(recentItem.getViewInfo());
								openComicDialog.setModal(true);
								openComicDialog.setVisible(true);
								openComic();
							}
						});
						recentViewmenu.add(recentItem);
					}
				}
			}
		});
		menu.add(recentViewmenu);

		mntmNewMenuItem = new JMenuItem("退出");
		mntmNewMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(1);
			}
		});
		menu.add(mntmNewMenuItem);

		menu_1 = new JMenu("帮助");
		menuBar.add(menu_1);

		menuItem_1 = new JMenuItem("操作说明");
		menuItem_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "上下箭头，空格，回车，左键和右键点击自动滚屏。鼠标拖拽，移动图片\n" + "左右箭头，PageUp和PageDown翻页\n" + "Home第一页End最后一页\n"
						+ "Ctrl+Enter全屏，Esc推出全屏");
			}
		});
		menu_1.add(menuItem_1);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		picPane = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				if (image != null) {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());
					g.drawImage(image, picStartPoint.x, picStartPoint.y, this);
					// 全屏显示页
					if (isFullScreen) {
						g.setXORMode(Color.WHITE);
						g.drawString(pageNo + "/" + pageMap.size(), this.getWidth() - 50, this.getHeight() - 10);
						g.drawString(StringUtils.defaultString(sectionName, sectionId), 20, this.getHeight() - 10);
					}
				} else {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());
				}
			}

		};
		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_RIGHT) {
					changePage(true);
				}
				if (e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_LEFT) {
					changePage(false);
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_SPACE
						|| (!e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER)) {
					moveViewDown();
				}
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					moveViewUp();
				}
				if (e.getKeyCode() == KeyEvent.VK_F5) {
					showImage(true);
				}
				if (e.getKeyCode() == KeyEvent.VK_HOME) {
					pageNo = 1;
					showImage(true);
				}
				if (e.getKeyCode() == KeyEvent.VK_END) {
					pageNo = pageMap.size();
					showImage(true);
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					exitFullScreen();
				}
				if (e.getKeyCode() == KeyEvent.VK_M) {
					if (!isFullScreen) {
						menuBar.setVisible(!menuBar.isVisible());
					}
				}
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (!isFullScreen) {
						isFullScreen = true;
						// 释放前一个frame
						frame.dispose();
						// 创建新的frame
						createFrame();
						frame.setUndecorated(true);
						menuBar.setVisible(false);
						frame.setVisible(true);
						GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
						// 通过调用GraphicsEnvironment的getDefaultScreenDevice方法获得当前的屏幕设备了
						GraphicsDevice gd = ge.getDefaultScreenDevice();
						// 全屏设置
						gd.setFullScreenWindow(frame);
					} else {
						exitFullScreen();
					}
				}
			}
		});
		picPane.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() == 1) {
					movePic(0, -50);
				}
				if (e.getWheelRotation() == -1) {
					movePic(0, 50);
				}
				picPane.repaint();
			}
		});
		frame.getContentPane().add(picPane, BorderLayout.CENTER);
		picPane.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int changedX = e.getPoint().x - pressedPoint.x;
				int changedY = e.getPoint().y - pressedPoint.y;
				movePic(changedX, changedY);
				pressedPoint = e.getPoint();
				picPane.repaint();
			}
		});
		picPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				picPane.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				pressedPoint = e.getPoint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				picPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					moveViewDown();
				} else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					moveViewUp();
				}
			}
		});
		//
		picPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void movePic(int changedX, int changedY) {
		if (image != null) {
			// 移动图片，图片边界不能超出窗口
			int x = picStartPoint.x + changedX;
			if (x < 0) {
				if (picPane.getWidth() > image.getWidth(null)) {
					picStartPoint.x = 0;
				} else {
					picStartPoint.x = Math.max(x, picPane.getWidth() - image.getWidth(null));
				}
			} else {
				if (picPane.getWidth() > image.getWidth(null)) {
					picStartPoint.x = Math.min(x, picPane.getWidth() - image.getWidth(null));
				} else {
					picStartPoint.x = 0;
				}
			}
			int y = picStartPoint.y + changedY;
			if (y < 0) {
				if (picPane.getHeight() > image.getHeight(null)) {
					picStartPoint.y = 0;
				} else {
					picStartPoint.y = Math.max(y, picPane.getHeight() - image.getHeight(null));
				}
			} else {
				if (picPane.getHeight() > image.getHeight(null)) {
					picStartPoint.y = Math.min(y, picPane.getHeight() - image.getHeight(null));
				} else {
					picStartPoint.y = 0;
				}
			}
			// 窗口大于图片，图片剧中
			if (picPane.getWidth() > image.getWidth(null)) {
				picStartPoint.x = (int) ((picPane.getWidth() - image.getWidth(null)) / 2.0);
			}
			if (picPane.getHeight() > image.getHeight(null)) {
				picStartPoint.y = (int) ((picPane.getHeight() - image.getHeight(null)) / 2.0);
			}
		}
	}

	private synchronized void showImage(final boolean isNextPage) {
		if (!loading) {
			loading = true;
			frame.setTitle(getBasicTitle() + " - loading");
			new Thread() {

				@Override
				public void run() {
					try {
						if (pageNo != null) {
							PageInfo pageInfo = pageMap.get(pageNo);
							if (pageInfo == null || StringUtils.isBlank(pageInfo.getPageUrl())) {
								JOptionPane.showMessageDialog(null, "没有第" + pageNo + "页的图片信息");
								return;
							}
							try {
								image = loadImage(pageInfo);
								picStartPoint = new Point();
								if (isNextPage) {
									// 下一页从右上角开始
									picStartPoint.x = picPane.getWidth() - image.getWidth(null);
								} else {
									// 上一页从左下角开始
									picStartPoint.y = picPane.getHeight() - image.getHeight(null);
								}
								movePic(0, 0);
								picPane.repaint();
								try {
									RecentViewHelper
											.updateRecentView(channelInfo, comicId, comicName, sectionId, sectionName, pageNo, pageMap.size());
								} catch (Exception e) {
									logger.log(e.getMessage(), e);
								}
							} catch (Exception e) {
								logger.log(e.getMessage(), e);
								image = null;
								picPane.repaint();
								JOptionPane.showMessageDialog(null, "加载图片失败," + e.getMessage());
								return;
							}
						}
					} finally {
						frame.setTitle(getBasicTitle());
						loading = false;
					}
				}
			}.start();
		}
	}

	private String getBasicTitle() {
		return channelInfo.getName() + " - " + StringUtils.defaultString(comicName, comicId) + " - "
				+ StringUtils.defaultString(sectionName, sectionId) + " - " + pageNo + "/" + pageMap.size();
	}

	private void changePage(boolean isNextPage) {
		if (pageNo != null && !loading && pageMap.size() > 0) {
			loading = true;
			boolean success = false;
			frame.setTitle(getBasicTitle() + " - 正在翻页");
			try {
				int nextPageNo = isNextPage ? pageNo + 1 : pageNo - 1;
				PageInfo pageInfo = pageMap.get(nextPageNo);
				if (pageInfo != null) {
					pageNo = nextPageNo;
					success = true;
				} else {
					if (sectionList == null || sectionList.size() == 0) {
						try {
							ComicInfo comicInfo = picEngine.getSections(comicId);
							sectionList = comicInfo.getSections();
						} catch (Exception e1) {
							logger.log(e1.getMessage(), e1);
							JOptionPane.showMessageDialog(null, "获取章节信息失败 , " + e1.getMessage());
						}
					}
					if (sectionList == null || sectionList.size() == 0) {
						JOptionPane.showMessageDialog(null, "没有获取到章节信息");
						return;
					}
					for (int i = 0; i < sectionList.size(); i++) {
						SectionInfo sectionInfo = sectionList.get(i);
						if (StringUtils.equals(sectionInfo.getSectionId(), sectionId)) {
							if (isNextPage ? i < sectionList.size() - 1 : i > 0) {
								sectionId = sectionList.get(isNextPage ? i + 1 : i - 1).getSectionId();
								pageMap.clear();
								try {
									List<PageInfo> pages = picEngine.getPages(comicId, sectionId);
									for (PageInfo pageInfo1 : pages) {
										pageMap.put(pageInfo1.getPageNo(), pageInfo1);
									}
									pageNo = isNextPage ? 1 : pageMap.size();
									success = true;
								} catch (Exception e1) {
									logger.log(e1.getMessage(), e1);
									JOptionPane.showMessageDialog(null, "获取页面信息失败 , " + e1.getMessage());
								}
							} else {
								if (isFullScreen) {
									exitFullScreen();
								} else {
									JOptionPane.showMessageDialog(null, isNextPage ? "已是最后一章" : "已是第一章");
								}
							}
							return;
						}
					}
					JOptionPane.showMessageDialog(null, "没有匹配到章节信息");
					return;
				}
			} finally {
				loading = false;
				frame.setTitle(getBasicTitle());
				if (success) {
					showImage(isNextPage);
				}
			}
		}
	}

	private Image loadImage(PageInfo pageInfo) throws IOException {
		Image img = null;
		// 本地文件 c:/xxx 或者 /xx/xx xxx/xxx
		if (pageInfo.getPageUrl().charAt(1) == ':' || pageInfo.getPageUrl().indexOf(':') == -1) {
			img = ImageIO.read(new File(pageInfo.getPageUrl()));
		} else {
			// 缓存
			File cacheFile = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + channelInfo.getName() + "/" + comicId + "/"
					+ sectionId + "/" + FilenameUtils.getName(pageInfo.getPageUrl()));
			if (cacheFile.exists()) {
				img = ImageIO.read(cacheFile);
			} else {
				if (!cacheFile.getParentFile().exists()) {
					cacheFile.getParentFile().mkdirs();
				}
				picEngine.loadRemoteImage(comicId, sectionId, pageNo, pageInfo.getPageUrl());
				if (cacheFile.exists()) {
					img = ImageIO.read(cacheFile);
				}
			}
		}
		return img;
	}

	private void moveViewDown() {
		if (image != null) {
			if (image.getHeight(null) + picStartPoint.y > picPane.getHeight()) {
				// 没有到底,往下翻3/4个高度
				movePic(0, 0 - (int) (picPane.getHeight() * 0.9));
				picPane.repaint();
			} else if (picStartPoint.x < 0) {
				// 到底了，没在最左边，往左翻3/4个宽度
				picStartPoint.y = 0;
				movePic((int) (picPane.getWidth() * 0.9), 0);
				picPane.repaint();
			} else {
				// 本页看完了，下一页
				changePage(true);
			}
		}
	}

	private void openComic() {
		if (openComicDialog.getPageInfo() != null && openComicDialog.getPageInfo().size() > 0) {
			// 清理掉以前的引擎
			if (picEngine != null) {
				picEngine.close();
				picEngine = null;
			}
			try {
				picEngine = new ComicPluginEngine();
				picEngine.init(openComicDialog.getChannelInfo().getScriptFile());
			} catch (IOException e1) {
				logger.log(e1.getMessage(), e1);
				JOptionPane.showMessageDialog(null, "打开脚本失败," + e1.getMessage());
				return;
			}
			pageMap.clear();
			for (PageInfo pageInfo : openComicDialog.getPageInfo()) {
				pageMap.put(pageInfo.getPageNo(), pageInfo);
			}
			comicId = openComicDialog.getComicId();
			comicName = openComicDialog.getComicName();
			sectionId = openComicDialog.getSectionId();
			sectionName = openComicDialog.getSectionName();
			pageNo = openComicDialog.getPageNo();
			channelInfo = openComicDialog.getChannelInfo();
			sectionList = null;
			showImage(true);
		}
	}

	private void exitFullScreen() {
		if (isFullScreen) {
			isFullScreen = false;
			// 释放前一个frame
			frame.dispose();
			// 创建新的frame
			createFrame();
			frame.setVisible(true);
		}
	}

	private void moveViewUp() {
		if (image != null) {
			if (picStartPoint.y < 0) {
				// 没有到最上面,往上翻3/4个高度
				movePic(0, (int) (picPane.getHeight() * 0.9));
				picPane.repaint();
			} else if (image.getWidth(null) + picStartPoint.x > picPane.getWidth()) {
				// 到最上面，没在最右边，往右翻3/4个宽度,移动到右上
				movePic(0 - (int) (picPane.getWidth() * 0.9), 0 - image.getHeight(null));
				picPane.repaint();
			} else {
				// 本页看完了，上一页
				changePage(false);
			}
		}
	}
}
