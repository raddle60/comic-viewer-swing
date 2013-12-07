package com.raddle.comic.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;
import com.raddle.comic.engine.ChannelInfo;
import com.raddle.comic.engine.ComicPluginEngine;
import com.raddle.comic.engine.PageInfo;

public class ComicViewer {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ComicViewer.class));
	private JFrame frame;
	private JPanel picPane;
	private Point pressedPoint;
	private Point picStartPoint = new Point();
	private OpenComicDialog openComicDialog = new OpenComicDialog();
	private Image image;
	private ComicPluginEngine picEngine;
	private Map<Integer, PageInfo> pageMap = new HashMap<Integer, PageInfo>();
	private String comicId;
	private String sectionId;
	private ChannelInfo channelInfo;
	private Integer pageNo;
	private boolean loading = false;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final ComicViewer window = new ComicViewer();
					window.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
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
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 651, 467);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu menu = new JMenu("文件");
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem("打开");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openComicDialog.setModal(true);
				openComicDialog.setVisible(true);
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
						logger.log(e1.getMessage(), e);
						JOptionPane.showMessageDialog(null, "打开脚本失败," + e1.getMessage());
						return;
					}
					pageMap.clear();
					for (PageInfo pageInfo : openComicDialog.getPageInfo()) {
						pageMap.put(pageInfo.getPageNo(), pageInfo);
					}
					comicId = openComicDialog.getComicId();
					sectionId = openComicDialog.getSectionId();
					pageNo = openComicDialog.getPageNo();
					channelInfo = openComicDialog.getChannelInfo();
					showImage();
				}
			}
		});
		menu.add(menuItem);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		picPane = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				if (image != null) {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());
					int x = picStartPoint.x;
					if (this.getWidth() > image.getWidth(null)) {
						x = (int) ((this.getWidth() - image.getWidth(null)) / 2.0);
					}
					int y = picStartPoint.y;
					if (this.getHeight() > image.getHeight(null)) {
						y = (int) ((this.getHeight() - image.getHeight(null)) / 2.0);
					}
					g.drawImage(image, x, y, this);
				}
			}

		};
		picPane.addMouseWheelListener(new MouseWheelListener() {
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
				if (pageNo != null && !loading) {
					PageInfo pageInfo = pageMap.get(pageNo + 1);
					if (pageInfo != null) {
						pageNo = pageNo + 1;
						showImage();
					} else {
						JOptionPane.showMessageDialog(null, "已是最后一页");
						return;
					}
				}
			}
		});
		//
		picPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void movePic(int changedX, int changedY) {
		if (image != null) {
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
		}
	}

	private synchronized void showImage() {
		if (!loading) {
			loading = true;
			frame.setTitle(channelInfo.getName() + " - " + comicId + "/" + sectionId + "/" + pageNo + " - loading");
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
								image = ImageIO.read(new URL(pageInfo.getPageUrl()));
								picStartPoint = new Point();
								picPane.repaint();
							} catch (Exception e) {
								logger.log(e.getMessage(), e);
								JOptionPane.showMessageDialog(null, "加载图片失败," + e.getMessage());
								return;
							}
						}
					} finally {
						frame.setTitle(channelInfo.getName() + " - " + comicId + "/" + sectionId + "/" + pageNo);
						loading = false;
					}
				}
			}.start();
		}

	}
}
