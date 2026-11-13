package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
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
import com.raddle.comic.ViewConfigHelper;
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
	private Image complexImage;
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
	private JMenu menu_2;
	private JMenuItem menuItem_2;
	private JCheckBoxMenuItem continueViewItem;
	/** 连续显示时加载页面图片 */
	private final ContinuousScrollModel.PageLoader continuePageLoader = new ContinuousScrollModel.PageLoader() {
		@Override
		public boolean isCached(int pageNo) {
			PageInfo pageInfo = pageMap.get(pageNo);
			if (pageInfo == null) {
				return false;
			}
			try {
				return getLocalFile(pageInfo).exists();
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public Image load(int pageNo) throws Exception {
			PageInfo pageInfo = pageMap.get(pageNo);
			if (pageInfo == null) {
				throw new IOException("没有第" + pageNo + "页的图片信息");
			}
			try {
				Image image = loadImage(pageInfo, false);
				if (image == null) {
					// 抛出去让模型记下失败，不然会一直重试
					throw new IOException("图片下载失败");
				}
				return image;
			} catch (IOException e) {
				logger.log(e.getMessage(), e);
				throw e;
			} catch (Exception e) {
				logger.log(e.getMessage(), e);
				throw new IOException(e.getMessage(), e);
			}
		}
	};
	/** 连续显示的滚动模型 */
	private ContinuousScrollModel continueModel = new ContinuousScrollModel(0, continuePageLoader, new Runnable() {
		@Override
		public void run() {
			// 页在后台加载好了，重画一下，不然要等下次滚动才看到
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					// 之前滚过去但没滚到的那段距离，平滑地补上，
					// 不然页一下载好画面就突然跳一下
					int pending = continueModel.takePendingApply();
					if (pending != 0) {
						smoothScroll(pending);
					}
					updateContinueTitle();
					picPane.repaint();
				}
			});
		}
	});
	private JCheckBoxMenuItem suiteWidthMenuItem;
	private JCheckBoxMenuItem seamlessViewItem;
	private JCheckBoxMenuItem continuousDownloadItem;
	/** 图片缩放比例，100是原图大小 */
	private int zoomPercent = 100;
	/** 单页模式下原图，缩放后的图片从这里算出来 */
	private Image originImage;
	private Map<String,String> comicUpdate = new HashMap<String, String>();
	/** 已经提示过要不要继续下一章了，位置动了就重新提示 */
	private boolean nextChapterPromptShown = false;
	/** 滚轮一格滚动的距离 */
	private static final int WHEEL_SCROLL_DISTANCE = 150;
	/** 平滑滚动时每一帧最多移动多少像素 */
	private static final int SMOOTH_SCROLL_SPEED = 40;
	/** 平滑滚动还没走完的距离 */
	private int smoothScrollTarget = 0;
	/** 平滑滚动的动画定时器 */
	private javax.swing.Timer smoothScrollTimer;

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
                                final RecentViewMenuItem recentItem = new RecentViewMenuItem(recentViewInfo, comicUpdate);
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
                        final RecentViewMenuItem recentItem = new RecentViewMenuItem(recentViewInfo, comicUpdate);
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

		menu_2 = new JMenu("查看");
		menuBar.add(menu_2);

		menuItem_2 = new JMenuItem("跳转到...");
		menuItem_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gotoPage();
			}
		});
		menu_2.add(menuItem_2);

		continueViewItem = new JCheckBoxMenuItem("连续显示");
		continueViewItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ViewConfigHelper.setContinueView(continueViewItem.isSelected());
				showImage(true);
			}
		});
		continueViewItem.setSelected(ViewConfigHelper.getIsContinueView());
		menu_2.add(continueViewItem);

		seamlessViewItem = new JCheckBoxMenuItem("无缝显示");
		seamlessViewItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ViewConfigHelper.setSeamlessView(seamlessViewItem.isSelected());
				continueModel.setSeamless(seamlessViewItem.isSelected());
				if (continueViewItem.isSelected() && pageMap.size() > 0) {
					picPane.repaint();
				}
			}
		});
		seamlessViewItem.setSelected(ViewConfigHelper.getIsSeamlessView());
		continueModel.setSeamless(seamlessViewItem.isSelected());
		menu_2.add(seamlessViewItem);

		continuousDownloadItem = new JCheckBoxMenuItem("连续下载");
		continuousDownloadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ViewConfigHelper.setContinuousDownload(continuousDownloadItem.isSelected());
				continueModel.setContinuousDownload(continuousDownloadItem.isSelected());
				updateContinueTitle();
			}
		});
		continuousDownloadItem.setSelected(ViewConfigHelper.getIsContinuousDownload());
		continueModel.setContinuousDownload(continuousDownloadItem.isSelected());
		menu_2.add(continuousDownloadItem);
		
		JMenu zoomMenu = new JMenu("缩放");
		ButtonGroup zoomGroup = new ButtonGroup();
		zoomPercent = ViewConfigHelper.getZoomPercent();
		// 150%放最上面，往下依次减小
		for (int percent : new int[] { 150, 125, 100, 75, 50, 25, 0 }) {
			final int zoom = percent;
			JRadioButtonMenuItem zoomItem = new JRadioButtonMenuItem(percent + "%");
			zoomItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ViewConfigHelper.setZoomPercent(zoom);
					zoomPercent = zoom;
					applyZoom();
				}
			});
			zoomItem.setSelected(percent == zoomPercent);
			zoomGroup.add(zoomItem);
			zoomMenu.add(zoomItem);
		}
		menu_2.add(zoomMenu);

		suiteWidthMenuItem = new JCheckBoxMenuItem("适合高度");
		suiteWidthMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ViewConfigHelper.setFitHeigthView(suiteWidthMenuItem.isSelected());
				applyZoom();
			}
		});
		suiteWidthMenuItem.setSelected(ViewConfigHelper.getIsFitHeigthView());
		menu_2.add(suiteWidthMenuItem);

		menu_1 = new JMenu("帮助");
		menuBar.add(menu_1);

		menuItem_1 = new JMenuItem("操作说明");
		menuItem_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "上下箭头，空格，回车，左键和右键点击自动滚屏。鼠标拖拽，移动图片\n" + "Ctrl+上下左右箭头移动图片\n" + "左右箭头，PageUp和PageDown翻页\n"
						+ "Home第一页End最后一页\n" + "Ctrl+Enter全屏，Esc推出全屏");
			}
		});
		menu_1.add(menuItem_1);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		picPane = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
				if (continueViewItem.isSelected() && pageMap.size() > 0) {
					// 连续显示：一页一页直接画，位置由滚动模型算好
					paintContinueView(g);
				} else if (complexImage != null) {
					g.drawImage(complexImage, picStartPoint.x, picStartPoint.y, this);
					// 全屏显示页
					if (isFullScreen) {
						g.setXORMode(Color.WHITE);
                        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
						g.drawString(pageNo + "/" + pageMap.size(), this.getWidth() - 50, this.getHeight() - 10);
						g.drawString(StringUtils.defaultString(sectionName, sectionId), 20, this.getHeight() - 10);
					}
				}
			}

		};
		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int interval = 30;
				boolean isContinue = continueViewItem.isSelected() && pageMap.size() > 0;
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_UP) {
					// 连续显示时纵向由滚动模型管，否则直接改偏移会和模型打架
					if (isContinue) {
						scrollContinue(0 - interval);
					} else {
						movePic(0, interval);
					}
					picPane.repaint();
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (isContinue) {
						scrollContinue(interval);
					} else {
						movePic(0, 0 - interval);
					}
					picPane.repaint();
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_LEFT) {
					if (isContinue) {
						movePicHorizontalContinue(interval);
					} else {
						movePicHorizontal(interval);
					}
					picPane.repaint();
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_RIGHT) {
					if (isContinue) {
						movePicHorizontalContinue(0 - interval);
					} else {
						movePicHorizontal(0 - interval);
					}
					picPane.repaint();
				} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_RIGHT) {
					changePage(true);
				} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_LEFT) {
					changePage(false);
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_SPACE
						|| (!e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER)) {
					moveViewDown();
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					moveViewUp();
				} else if (e.getKeyCode() == KeyEvent.VK_F5) {
					showImage(true);
				} else if (e.getKeyCode() == KeyEvent.VK_HOME) {
					pageNo = 1;
					showImage(true);
				} else if (e.getKeyCode() == KeyEvent.VK_END) {
					pageNo = pageMap.size();
					showImage(true);
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					exitFullScreen();
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_G) {
					gotoPage();
				} else if (e.getKeyCode() == KeyEvent.VK_M) {
					if (!isFullScreen) {
						menuBar.setVisible(!menuBar.isVisible());
					}
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
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
				// 高精度滚轮一格会分成很多小事件，用precise的才准
				double rotation = e.getPreciseWheelRotation();
				if (rotation == 0) {
					rotation = e.getWheelRotation();
				}
				if (rotation == 0) {
					return;
				}
				// 正数表示滚轮向下
				wheelScroll((int) Math.round(rotation * WHEEL_SCROLL_DISTANCE));
			}
		});
		frame.getContentPane().add(picPane, BorderLayout.CENTER);
		picPane.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int changedX = e.getPoint().x - pressedPoint.x;
				int changedY = e.getPoint().y - pressedPoint.y;
				pressedPoint = e.getPoint();
				// 拖动的时候停掉滚轮的平滑滚动，免得两个一起动
				stopSmoothScroll();
				if (continueViewItem.isSelected() && pageMap.size() > 0) {
					// 连续显示：纵向按"页码+页内偏移"滚动，横向单独处理
					movePicHorizontalContinue(changedX);
					scrollContinue(-changedY);
				} else {
					movePic(changedX, changedY);
				}
				picPane.repaint();
			}
		});
		picPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				picPane.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				pressedPoint = e.getPoint();
				// 按下鼠标就停掉滚轮的平滑滚动
				stopSmoothScroll();
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
		// 检查漫画更新
        new Thread(new Runnable() {

            @Override
            public void run() {
                List<RecentViewInfo> recentViews = RecentViewHelper.getRecentViews();
                for (RecentViewInfo recentViewInfo : recentViews) {
                    comicUpdate.put(recentViewInfo.getComicId(), "updating");
                    ComicPluginEngine updateSessionEngine = null;
                    try {
                        updateSessionEngine = new ComicPluginEngine();
                        List<ChannelInfo> channelList = ComicPluginEngine.getChannelList(new File("channels"));
                        for (ChannelInfo channelInfo : channelList) {
                            if (FilenameUtils.getName(recentViewInfo.getChannelPath()).equals(channelInfo.getScriptFile().getName())) {
                                updateSessionEngine.init(channelInfo.getScriptFile());
                                break;
                            }
                        }
                        ComicInfo comicInfo = updateSessionEngine.getSections(recentViewInfo.getComicId());
                        comicUpdate.put(recentViewInfo.getComicId(), comicInfo.getSections().get(comicInfo.getSections().size() - 1).getSectionId());
                    } catch (Exception e) {
                        comicUpdate.put(recentViewInfo.getComicId(), "failed: " + StringUtils.defaultIfEmpty(e.getMessage(), e.getClass().getSimpleName()));
                    } finally {
                        if (updateSessionEngine != null) {
                            updateSessionEngine.close();
                        }
                    }
                }
            }
        }, "check-comic-update").start();
    }

	private void movePic(int changedX, int changedY) {
		if (complexImage == null) {
			return;
		}
		movePicHorizontal(changedX);
		int y = picStartPoint.y + changedY;
		if (y < 0) {
			if (picPane.getHeight() > complexImage.getHeight(null)) {
				picStartPoint.y = 0;
			} else {
				picStartPoint.y = Math.max(y, picPane.getHeight() - complexImage.getHeight(null));
			}
		} else {
			if (picPane.getHeight() > complexImage.getHeight(null)) {
				picStartPoint.y = Math.min(y, picPane.getHeight() - complexImage.getHeight(null));
			} else {
				picStartPoint.y = 0;
			}
		}
		// 窗口大于图片，图片剧中
		if (picPane.getHeight() > complexImage.getHeight(null)) {
			picStartPoint.y = (int) ((picPane.getHeight() - complexImage.getHeight(null)) / 2.0);
		}
	}

	/**
	 * 连续显示的横向平移。窗口比图片宽的时候居中显示，不用平移。
	 */
	private void movePicHorizontalContinue(int changedX) {
		int imageWidth = continueModel.getDisplayWidth();
		if (imageWidth <= 0) {
			return;
		}
		if (picPane.getWidth() > imageWidth) {
			// 比窗口窄，居中显示就行
			picStartPoint.x = 0;
			return;
		}
		int x = picStartPoint.x + changedX;
		picStartPoint.x = x > 0 ? 0 : Math.max(x, picPane.getWidth() - imageWidth);
	}

	/** 横向移动图片，图片边界不能超出窗口 */
	private void movePicHorizontal(int changedX) {
		int imageWidth = getDisplayWidth();
		if (imageWidth <= 0) {
			return;
		}
		int x = picStartPoint.x + changedX;
		if (x < 0) {
			if (picPane.getWidth() > imageWidth) {
				picStartPoint.x = 0;
			} else {
				picStartPoint.x = Math.max(x, picPane.getWidth() - imageWidth);
			}
		} else {
			if (picPane.getWidth() > imageWidth) {
				picStartPoint.x = Math.min(x, picPane.getWidth() - imageWidth);
			} else {
				picStartPoint.x = 0;
			}
		}
		// 窗口大于图片，图片居中
		if (picPane.getWidth() > imageWidth) {
			picStartPoint.x = (int) ((picPane.getWidth() - imageWidth) / 2.0);
		}
	}

	/** 当前显示的图片宽度 */
	private int getDisplayWidth() {
		return complexImage == null ? 0 : complexImage.getWidth(null);
	}

	/**
	 * 滚轮滚动。距离累加起来，用动画慢慢走过去，比一格一格跳顺。
	 *
	 * @param distance 正数表示向下滚
	 */
	private void wheelScroll(int distance) {
		smoothScroll(distance);
	}

	/**
	 * 平滑滚动。距离累加起来，用动画慢慢走过去。
	 * 滚轮和"页加载好了要补的距离"都用它。
	 *
	 * @param distance 正数表示向下滚
	 */
	private void smoothScroll(int distance) {
		smoothScrollTarget = smoothScrollTarget + distance;
		startSmoothScroll();
	}

	/** 启动平滑滚动的动画 */
	private void startSmoothScroll() {
		if (smoothScrollTimer == null) {
			smoothScrollTimer = new javax.swing.Timer(16, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (smoothScrollTarget == 0) {
						smoothScrollTimer.stop();
						return;
					}
					int step = smoothScrollTarget / 4;
					if (step > SMOOTH_SCROLL_SPEED) {
						step = SMOOTH_SCROLL_SPEED;
					}
					if (step < 0 - SMOOTH_SCROLL_SPEED) {
						step = 0 - SMOOTH_SCROLL_SPEED;
					}
					if (step == 0) {
						// 剩的不多了，直接到位
						step = smoothScrollTarget;
					}
					smoothScrollTarget = smoothScrollTarget - step;
					if (continueViewItem.isSelected() && pageMap.size() > 0) {
						scrollContinue(step);
					} else {
						// 非连续显示：往下滚内容要往上移
						movePic(0, 0 - step);
						picPane.repaint();
					}
				}
			});
		}
		if (!smoothScrollTimer.isRunning()) {
			smoothScrollTimer.start();
		}
	}

	/** 停止平滑滚动，鼠标拖动的时候用 */
	private void stopSmoothScroll() {
		smoothScrollTarget = 0;
		if (smoothScrollTimer != null && smoothScrollTimer.isRunning()) {
			smoothScrollTimer.stop();
		}
	}

	/**
	 * 连续显示：把当前视口里的页一页页画出来。
	 * 没加载出来的页不画，下面留黑并提示正在加载，加载完直接补上，位置不会变。
	 */
	private void paintContinueView(Graphics g) {
		int paneWidth = picPane.getWidth();
		int paneHeight = picPane.getHeight();
		List<ContinuousScrollModel.VisiblePage> visible = continueModel.getVisiblePages(paneWidth, paneHeight, picStartPoint.x);
		for (ContinuousScrollModel.VisiblePage visiblePage : visible) {
			ContinuousScrollModel.Page page = visiblePage.page;
			g.drawImage(page.image, visiblePage.x, visiblePage.y, visiblePage.width, visiblePage.height, picPane);
			g.setColor(Color.BLACK);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, pageFontSize()));
			g.drawString(page.pageNo + "/" + pageMap.size(), visiblePage.x + visiblePage.width - 80,
					visiblePage.y + visiblePage.height - 10);
		}
		// 加载失败或者还有页没加载出来，提示一下
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		String failed = continueModel.getFailedReason();
		if (failed != null) {
			g.setColor(Color.RED);
			g.drawString(failed + "（继续滚动可重试）", 20, paneHeight - 20);
		} else {
			Integer missing = continueModel.getMissingPage();
			if (missing != null) {
				g.setColor(Color.GRAY);
				g.drawString("正在加载第" + missing + "页...", 20, paneHeight - 20);
			}
		}
	}

	/**
	 * 连续显示模式下的滚动。delta 为正表示向下滚（看下面的内容）。
	 *
	 * 滚动位置由 continueModel 以"页码+页内偏移"保存，和画了多少页无关，
	 * 所以图片加载完、页面加载完，位置都不会变，画面也就不会跳。
	 */
	private void scrollContinue(int delta) {
		updateContinueScale();
		int oldPageNo = continueModel.getCurPageNo();
		int oldOffset = continueModel.getScrollOffset();
		continueModel.scroll(delta);
		boolean moved = continueModel.getCurPageNo() != oldPageNo || continueModel.getScrollOffset() != oldOffset;
		int curPageNo = continueModel.getCurPageNo();
		if (curPageNo != pageNo) {
			pageNo = curPageNo;
			frame.setTitle(getBasicTitle());
			try {
				RecentViewHelper.updateRecentView(channelInfo, comicId, comicName, sectionId, sectionName, pageNo, pageMap.size());
			} catch (Exception e) {
				logger.log(e.getMessage(), e);
			}
		}
		if (moved) {
			// 位置动了，下次到底了还能再提示
			nextChapterPromptShown = false;
		}
		if (continueModel.isBlockedAtEnd() && !nextChapterPromptShown) {
			// 已经到本章最后一页了，用户还在往章末外面拉，问一下要不要继续下一章
			nextChapterPromptShown = true;
			promptNextChapter();
		}
		continueModel.preload(picPane.getHeight());
		updateContinueTitle();
		picPane.repaint();
	}

	/**
	 * 更新标题。加载中和加载失败都体现在标题上，
	 * 跟以前打开漫画时标题显示"loading"是一个做法。
	 */
	private void updateContinueTitle() {
		if (channelInfo == null) {
			return;
		}
		String title = getBasicTitle();
		String failed = continueModel.getFailedReason();
		if (failed != null) {
			frame.setTitle(title + " - " + failed);
		} else if (continueModel.isLoading()) {
			// 跟 showImage 里的写法保持一致
			frame.setTitle(title + " - loading");
		} else {
			frame.setTitle(title);
		}
	}

	/** 页码文字的大小，图片缩放了文字也跟着缩放 */
	private int pageFontSize() {
		int size = (int) (24 * zoomPercent / 100.0);
		return size < 12 ? 12 : size;
	}

	/**
	 * 按当前的缩放比例和"适合高度"重新显示。
	 */
	private void applyZoom() {
		if (continueViewItem.isSelected() && pageMap.size() > 0) {
			// 连续显示：改一下比例重画就行
			updateContinueScale();
			picPane.repaint();
			return;
		}
		// 单页模式：从原图重新缩放
		if (originImage != null) {
			complexImage = originImage;
			if (suiteWidthMenuItem.isSelected()) {
				scaleToHeight();
			}
			scaleByZoom();
		}
		picPane.repaint();
	}

	/** 按缩放比例把当前图片缩小或放大 */
	private void scaleByZoom() {
		if (complexImage == null || zoomPercent == 100) {
			return;
		}
		double zoom = zoomPercent / 100.0;
		int width = (int) (complexImage.getWidth(null) * zoom);
		int height = (int) (complexImage.getHeight(null) * zoom);
		if (width <= 0 || height <= 0) {
			return;
		}
		complexImage = complexImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
	}

	/** 更新连续显示的缩放比例，"适合高度"时把每页缩放到窗口高度，再乘上缩放比例 */
	private void updateContinueScale() {
		ContinuousScrollModel.Page page = continueModel.getCurPage();
		double scale = 1.0;
		if (suiteWidthMenuItem.isSelected() && page != null) {
			scale = (double) picPane.getHeight() / page.height;
		}
		continueModel.setScale(scale * zoomPercent / 100.0);
	}

	private synchronized void showImage(final boolean isNextPage) {
		if (!loading && pageMap.size() > 0) {
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
								if (continueViewItem.isSelected()) {
									// 连续显示：定位到当前页，把这一页加载出来就能画
									continueModel.gotoPage(pageNo);
									continueModel.loadPage(pageNo);
									continueModel.preload(picPane.getHeight());
									updateContinueScale();
									picPane.repaint();
								} else {
									originImage = loadImage(pageInfo);
									complexImage = originImage;
									if (complexImage != null) {
										if(suiteWidthMenuItem.isSelected()){
											scaleToHeight();
										}
										scaleByZoom();
										picStartPoint = new Point();
										if (isNextPage) {
											// 下一页从右上角开始
											picStartPoint.x = picPane.getWidth() - complexImage.getWidth(null);
										} else {
											// 上一页从左下角开始
											picStartPoint.y = picPane.getHeight() - complexImage.getHeight(null);
										}
										movePic(0, 0);
										picPane.repaint();
									}
								}
								try {
									RecentViewHelper
											.updateRecentView(channelInfo, comicId, comicName, sectionId, sectionName, pageNo, pageMap.size());
								} catch (Exception e) {
									logger.log(e.getMessage(), e);
								}
							} catch (Exception e) {
								logger.log(e.getMessage(), e);
								complexImage = null;
								picPane.repaint();
								JOptionPane.showMessageDialog(null, "加载图片失败," + e.getMessage());
								return;
							}
						}
					} finally {
						if (continueViewItem.isSelected()) {
							// 连续显示的加载状态由 updateContinueTitle 管，别把" loading"冲掉
							updateContinueTitle();
						} else {
							frame.setTitle(getBasicTitle());
						}
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
					// 本章没有下一页了，换章
					SectionInfo nextSection = getAdjacentSection(isNextPage);
					if (nextSection == null) {
						if (isFullScreen) {
							exitFullScreen();
						}
						JOptionPane.showMessageDialog(null, isNextPage ? "已是最后一章" : "已是第一章");
						return;
					}
					try {
						List<PageInfo> pages = picEngine.getPages(comicId, nextSection.getSectionId());
						if (pages == null || pages.isEmpty()) {
							// 没拿到下一章的页面信息，就不要换章，原地不动，用户还能再试
							JOptionPane.showMessageDialog(null, "获取页面信息失败 , 没有页面信息");
							nextChapterPromptShown = false;
							return;
						}
						// 页面都拿到了再换章，中途失败就一直停在原来这一章
						pageMap.clear();
						for (PageInfo pageInfo1 : pages) {
							pageMap.put(pageInfo1.getPageNo(), pageInfo1);
						}
						sectionId = nextSection.getSectionId();
						sectionName = nextSection.getName();
						pageNo = isNextPage ? 1 : pageMap.size();
						// 换章了，缓存里还是上一章的图片，要清掉
						continueModel.setPageCount(pageMap.size());
						continueModel.reset(pageNo);
						success = true;
					} catch (Exception e1) {
						logger.log(e1.getMessage(), e1);
						JOptionPane.showMessageDialog(null, "获取页面信息失败 , " + e1.getMessage());
						// 失败了还是停在原来这一章，用户可以再试一次
						nextChapterPromptShown = false;
					}
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
		return loadImage(pageInfo, true);
	}

	/** 这一页在本地对应的文件，本地路径就是图片本身，远程地址对应缓存目录里的文件 */
	private File getLocalFile(PageInfo pageInfo) {
		// 本地文件 c:/xxx 或者 /xx/xx xxx/xxx
		if (pageInfo.getPageUrl().charAt(1) == ':' || pageInfo.getPageUrl().indexOf(':') == -1) {
			return new File(pageInfo.getPageUrl());
		}
		return new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + channelInfo.getName() + "/" + comicId + "/"
				+ sectionId + "/" + StringUtils.defaultIfBlank(pageInfo.getFilename(), FilenameUtils.getName(pageInfo.getPageUrl())));
	}

	/**
	 * 读本地缓存的图片。
	 *
	 * 文件如果没下完整（下载中途断掉留下的），读的时候会报EOFException这类错。
	 * 这时把文件删了，下次才会重新下载；不删的话会一直读这个坏文件，永远自动恢复不了。
	 */
	static Image readCachedImage(File cacheFile) throws IOException {
		try {
			Image image = ImageIO.read(cacheFile);
			if (image == null) {
				deleteCacheFile(cacheFile);
				throw new IOException("缓存文件损坏，读不出来:" + cacheFile);
			}
			return image;
		} catch (IOException e) {
			// 损坏的文件删掉，这样才能重新下载
			deleteCacheFile(cacheFile);
			throw e;
		}
	}

	/** 删掉损坏的缓存文件 */
	private static void deleteCacheFile(File cacheFile) {
		if (cacheFile.exists() && !cacheFile.delete()) {
			logger.log("删除损坏的缓存文件失败 {}", cacheFile);
		}
	}

	/**
	 * 加载图片。
	 *
	 * @param showError 加载失败时是否弹窗，后台预加载时传 false，避免在非事件线程弹窗
	 */
	private Image loadImage(PageInfo pageInfo, boolean showError) throws IOException {
		Image img = null;
		// 本地文件 c:/xxx 或者 /xx/xx xxx/xxx
		if (pageInfo.getPageUrl().charAt(1) == ':' || pageInfo.getPageUrl().indexOf(':') == -1) {
			img = ImageIO.read(new File(pageInfo.getPageUrl()));
		} else {
			// 缓存
			File cacheFile = getLocalFile(pageInfo);
			if (cacheFile.exists()) {
				img = readCachedImage(cacheFile);
			} else {
				if (!cacheFile.getParentFile().exists()) {
					cacheFile.getParentFile().mkdirs();
				}
				// 预加载会同时下载多页，HttpClient 本身是线程安全的，不用串行，
				// 否则一页超时会把其它页都堵住
				picEngine.loadRemoteImage(comicId, sectionId, pageInfo.getPageNo(), pageInfo.getPageUrl());
				if (cacheFile.exists()) {
					// 刚下好的文件也可能不完整，读不出来就删掉，下次重新下
					img = readCachedImage(cacheFile);
				} else if (showError) {
					JOptionPane.showMessageDialog(null, "加载图片失败");
				}
			}
		}
		return img;
	}

	private void moveViewDown() {
		if (complexImage != null) {
			if (continueViewItem.isSelected() && pageMap.size() > 0) {
				// 连续显示：按页滚动，翻到本章最后一页问问要不要继续下一章
				if (continueModel.isAtBottom()) {
					promptNextChapter();
				} else {
					scrollContinue((int) (picPane.getHeight() * 0.9));
				}
				picPane.repaint();
				return;
			}
			if (complexImage.getHeight(null) + picStartPoint.y > picPane.getHeight()) {
				// 没有到底,往下翻3/4个高度
				movePic(0, 0 - (int) (picPane.getHeight() * 0.9));
				picPane.repaint();
			} else if (picStartPoint.x < 0) {
				// 到底了，没在最左边，往左翻3/4个宽度
				picStartPoint.y = 0;
				movePic((int) (picPane.getWidth() * 0.9), 0);
				picPane.repaint();
			} else if (pageNo != null && pageNo >= pageMap.size()) {
				// 本章最后一页了，问一下要不要继续下一章
				promptNextChapter();
			} else {
				// 本页看完了，下一页
				changePage(true);
			}
		}
	}

	/**
	 * 章节列表。优先用弹出窗口里的，窗口里没有再自己取一次。
	 */
	private List<SectionInfo> getSectionList() {
		List<SectionInfo> sections = openComicDialog.getSections();
		if (sections != null && sections.size() > 0) {
			return sections;
		}
		if (sectionList == null || sectionList.size() == 0) {
			try {
				sectionList = picEngine.getSections(comicId).getSections();
			} catch (Exception e) {
				logger.log(e.getMessage(), e);
			}
		}
		return sectionList;
	}

	/**
	 * 找当前章节相邻的章节。
	 *
	 * @param isNext true找下一章，false找上一章
	 * @return 相邻的章节，没有了返回null
	 */
	private SectionInfo getAdjacentSection(boolean isNext) {
		return getAdjacentSection(getSectionList(), sectionId, isNext);
	}

	/**
	 * 根据章节列表找相邻的章节。
	 * 章节列表是倒序的（最新的一话在最前面），所以下一章在列表里是往前一个。
	 *
	 * @param sections 章节列表
	 * @param sectionId 当前章节
	 * @param isNext true找下一章，false找上一章
	 * @return 相邻的章节，没有了返回null
	 */
	static SectionInfo getAdjacentSection(List<SectionInfo> sections, String sectionId, boolean isNext) {
		if (sections == null || sections.size() == 0) {
			return null;
		}
		int index = -1;
		for (int i = 0; i < sections.size(); i++) {
			if (StringUtils.equals(sections.get(i).getSectionId(), sectionId)) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return null;
		}
		int next = isNext ? index - 1 : index + 1;
		if (next < 0 || next >= sections.size()) {
			return null;
		}
		return sections.get(next);
	}

	/**
	 * 已经到本章最后一页了，提示要不要继续下一章。
	 * 已经是最后一章就说明一下。
	 */
	private void promptNextChapter() {
		List<SectionInfo> sections = getSectionList();
		if (sections == null || sections.size() == 0) {
			JOptionPane.showMessageDialog(null, "没有获取到章节信息");
			return;
		}
		if (getAdjacentSection(sections, sectionId, true) == null) {
			JOptionPane.showMessageDialog(null, "已经是最后一章");
			return;
		}
		int option = JOptionPane.showConfirmDialog(null, "本章已经看完了，是否继续下一章？", "提示", JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			// 换到下一章的第一页
			changePage(true);
		}
	}

	private void openComic() {
		if (openComicDialog.getPageInfo() != null && openComicDialog.getPageInfo().size() > 0) {
			initPicEngine(openComicDialog.getChannelInfo().getScriptFile());
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
			picStartPoint.y = 0;
			// 新章节，重新建一个滚动模型，缓存清空
			continueModel = new ContinuousScrollModel(pageMap.size(), continuePageLoader, new Runnable() {
				@Override
				public void run() {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							// 之前滚过去但没滚到的那段距离，平滑地补上
							int pending = continueModel.takePendingApply();
							if (pending != 0) {
								smoothScroll(pending);
							}
							updateContinueTitle();
							picPane.repaint();
						}
					});
				}
			});
			continueModel.setSeamless(seamlessViewItem.isSelected());
			continueModel.setContinuousDownload(continuousDownloadItem.isSelected());
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
			if (complexImage != null) {
				frame.setTitle(getBasicTitle());
			}
			frame.setVisible(true);
		}
	}

	private void moveViewUp() {
		if (complexImage != null) {
			if (continueViewItem.isSelected() && pageMap.size() > 0) {
				// 连续显示：按页滚动，翻到本章第一页才换章
				if (continueModel.isAtTop()) {
					changePage(false);
				} else {
					scrollContinue(0 - (int) (picPane.getHeight() * 0.9));
				}
				picPane.repaint();
				return;
			}
			if (picStartPoint.y < 0) {
				// 没有到最上面,往上翻3/4个高度
				movePic(0, (int) (picPane.getHeight() * 0.9));
				picPane.repaint();
			} else if (complexImage.getWidth(null) + picStartPoint.x > picPane.getWidth()) {
				// 到最上面，没在最右边，往右翻3/4个宽度,移动到右上
				movePic(0 - (int) (picPane.getWidth() * 0.9), 0 - complexImage.getHeight(null));
				picPane.repaint();
			} else {
				// 本页看完了，上一页
				changePage(false);
			}
		}
	}

	private void gotoPage() {
		if (!isFullScreen) {
			if (channelInfo != null) {
				String page = JOptionPane.showInputDialog("请输入跳转的页码" + pageNo + "/" + pageMap.size());
				if (StringUtils.isNotBlank(page)) {
					int pageIndex = Integer.parseInt(page);
					if (pageIndex < 1) {
						pageIndex = 1;
					} else if (pageIndex > pageMap.size()) {
						pageIndex = pageMap.size();
					}
					pageNo = pageIndex;
					showImage(true);
				}
			} else {
				JOptionPane.showMessageDialog(null, "没有打开的漫画");
			}
		}
	}

	private void scaleToHeight() {
		double heightRadio = (double)complexImage.getHeight(null)/picPane.getHeight();
		int scaledWidth = (int) (complexImage.getWidth(null)/heightRadio);
		complexImage = complexImage.getScaledInstance(scaledWidth, picPane.getHeight(), Image.SCALE_SMOOTH);
	}

	private void initPicEngine(File scriptFile) {
        // 清理掉以前的引擎
        if (picEngine != null) {
            picEngine.close();
            picEngine = null;
        }
        try {
            picEngine = new ComicPluginEngine();
            picEngine.init(scriptFile);
        } catch (IOException e1) {
            logger.log(e1.getMessage(), e1);
            JOptionPane.showMessageDialog(null, "打开脚本失败," + e1.getMessage());
            return;
        }
    }

}
