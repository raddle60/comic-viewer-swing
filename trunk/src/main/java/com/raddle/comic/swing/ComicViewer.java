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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;

public class ComicViewer {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ComicViewer.class));
	private JFrame frame;
	private JPanel picPane;
	private Point pressedPoint;
	private Point picStartPoint = new Point();
	private Image image;

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
			}
		});
		menu.add(menuItem);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		picPane = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
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

		};
		picPane.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() == 1) {
					movePic(0, -30);
				}
				if (e.getWheelRotation() == -1) {
					movePic(0, 30);
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
		});
		//
		picPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
		image = picPane.getToolkit().getImage("D:/017_23889.png");
	}

	private void movePic(int changedX, int changedY) {
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
